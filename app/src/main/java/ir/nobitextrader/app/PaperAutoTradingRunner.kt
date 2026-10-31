package ir.nobitextrader.app

import android.content.Context
import ir.nobitextrader.app.scanner.MarketRefreshPolicy
import ir.nobitextrader.app.scanner.MarketScanner
import ir.nobitextrader.app.strategy.StrategyAnalysisCoordinator
import ir.nobitextrader.app.strategy.StrategyId
import ir.nobitextrader.app.strategy.TradeDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Executes one explicitly requested BTC paper-trading cycle.
 * It reads only public candles and public order-book data; it never calls
 * an authenticated account endpoint or submits a real exchange order.
 */
internal object PaperAutoTradingRunner {
    sealed interface Result {
        data class Executed(
            val portfolio: PaperPortfolio,
            val side: String,
            val quantity: Double,
            val price: Double,
            val reason: String
        ) : Result

        data class Rejected(val reason: String) : Result
    }

    suspend fun runBtcOnce(
        context: Context,
        settings: TraderSettings
    ): Result = withContext(Dispatchers.IO) {
        val feeRate = settings.paperFeePercent.toDouble() / 100.0
        if (!feeRate.isFinite() || feeRate !in 0.0000000001..0.02) {
            return@withContext Result.Rejected(
                "کارمزد معتبر تنظیم نشده است؛ ابتدا نرخ واقعی حساب را در تنظیمات وارد کن."
            )
        }

        val enabledStrategies = buildSet {
            if (settings.strategyFmcEnabled) add(StrategyId.FMC)
            if (settings.strategyFvgEnabled) add(StrategyId.FVG)
            if (settings.strategyLiquiditySweepEnabled) add(StrategyId.LIQUIDITY_SWEEP)
            if (settings.strategyOrderBlockEnabled) add(StrategyId.ORDER_BLOCK)
            if (settings.strategyMicroScalperEnabled) add(StrategyId.MICRO_SCALPER)
        }
        if (enabledStrategies.isEmpty()) {
            return@withContext Result.Rejected("همه استراتژی‌ها خاموش هستند.")
        }

        val intervalSeconds = 900L
        val nowSeconds = System.currentTimeMillis() / 1_000L
        val currentCandleOpen = nowSeconds - nowSeconds % intervalSeconds
        val lastClosedCandleOpen = currentCandleOpen - intervalSeconds
        val fromSeconds = lastClosedCandleOpen - 48L * 3_600L

        val analysis = StrategyAnalysisCoordinator().analyze(
            symbol = "BTCIRT",
            resolution = "15",
            fromUnixSeconds = fromSeconds,
            toUnixSeconds = lastClosedCandleOpen,
            enabledStrategyIds = enabledStrategies,
            minimumAgreement = settings.minimumStrategyAgreement.coerceIn(1, 5)
        )

        if (analysis.fetchError != null) {
            return@withContext Result.Rejected(
                "دریافت کندل‌های BTC ناموفق بود: ${analysis.fetchError}"
            )
        }
        val batch = analysis.batch
            ?: return@withContext Result.Rejected("خروجی تحلیل استراتژی موجود نیست.")
        if (!batch.isComplete) {
            return@withContext Result.Rejected(
                "تحلیل کامل نیست؛ به علت خطا یا اجرای ناقص استراتژی‌ها سفارشی ثبت نشد."
            )
        }
        val decision = analysis.decision
            ?: return@withContext Result.Rejected("تصمیم نهایی استراتژی موجود نیست.")
        if (!decision.isActionable) {
            return@withContext Result.Rejected(
                "سیگنال قابل معامله صادر نشد: ${decision.reason}"
            )
        }

        // Fetch the order book after analysis so the execution quote is recent.
        val marketPolicy = MarketRefreshPolicy(
            requestTimeoutMillis = settings.requestTimeoutSeconds.coerceIn(1, 60) * 1_000L,
            maxMarketsPerBatch = 1
        )
        val market = MarketScanner.scan(
            symbols = listOf("BTCIRT"),
            policy = marketPolicy
        ).firstOrNull()
            ?: return@withContext Result.Rejected("دفتر سفارش BTC دریافت نشد.")
        if (!market.success) {
            return@withContext Result.Rejected(
                market.error ?: "دریافت دفتر سفارش BTC ناموفق بود."
            )
        }

        val portfolio = PaperTradingStore.load(context)
        val orderBudget = when (decision.direction) {
            TradeDirection.LONG -> minOf(
                DEFAULT_ORDER_BUDGET_IRR,
                portfolio.cash * MAX_CASH_FRACTION_PER_ORDER
            )
            TradeDirection.SHORT -> minOf(
                DEFAULT_ORDER_BUDGET_IRR,
                portfolio.quantity("BTCIRT") * market.bestBid
            )
            TradeDirection.FLAT -> 0.0
        }
        val plan = PaperAutoTradingPolicy.plan(
            decision = decision,
            market = market,
            portfolio = portfolio,
            feeRate = feeRate,
            orderBudgetIrr = orderBudget,
            nowMillis = System.currentTimeMillis(),
            maxMarketAgeMillis = PaperAutoTradingPolicy.DEFAULT_MAX_MARKET_AGE_MS,
            maxSpreadPercent = settings.maxSpreadPercent.toDouble()
        )

        if (plan is PaperAutoTradePlan.Rejected) {
            return@withContext Result.Rejected(plan.reason)
        }
        plan as PaperAutoTradePlan.Approved

        val lastClosedCandle = analysis.candles.lastOrNull()
            ?: return@withContext Result.Rejected("کندل بسته‌شده برای جلوگیری از سفارش تکراری موجود نیست.")
        val signalKey = "BTCIRT:${lastClosedCandle.timestampMillis}"
        val updated = try {
            PaperTradingStore.execute(
                context = context,
                side = plan.side,
                symbol = plan.symbol,
                quantity = plan.quantity,
                price = plan.limitPrice,
                feeRate = feeRate,
                idempotencyKey = signalKey
            )
        } catch (exception: IllegalArgumentException) {
            return@withContext Result.Rejected(
                exception.message ?: "سفارش مجازی رد شد."
            )
        }

        Result.Executed(
            portfolio = updated,
            side = plan.side,
            quantity = plan.quantity,
            price = plan.limitPrice,
            reason = plan.reason
        )
    }

    private const val DEFAULT_ORDER_BUDGET_IRR = 10_000_000.0
    private const val MAX_CASH_FRACTION_PER_ORDER = 0.01
}
