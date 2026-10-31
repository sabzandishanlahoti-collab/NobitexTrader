package ir.nobitextrader.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PaperTradingMathTest {
    @Test
    fun buyCapitalizesFeeIntoWeightedAverageEntry() {
        val result = PaperTradingMath.calculate(
            side = "BUY",
            oldCash = 1_000.0,
            oldQuantity = 1.0,
            oldAverageEntry = 100.0,
            quantity = 1.0,
            price = 120.0,
            feeRate = 0.01
        )

        assertEquals(878.8, result.cash, 0.000001)
        assertEquals(2.0, result.quantity, 0.000001)
        assertEquals(110.6, result.averageEntry, 0.000001)
        assertEquals(0.0, result.realizedPnl, 0.000001)
        assertEquals(1.2, result.fee, 0.000001)
    }

    @Test
    fun partialSellKeepsAverageEntryAndDeductsSellFeeFromRealizedPnl() {
        val result = PaperTradingMath.calculate(
            side = "SELL",
            oldCash = 500.0,
            oldQuantity = 1.0,
            oldAverageEntry = 100.0,
            quantity = 0.5,
            price = 130.0,
            feeRate = 0.01
        )

        assertEquals(564.35, result.cash, 0.000001)
        assertEquals(0.5, result.quantity, 0.000001)
        assertEquals(100.0, result.averageEntry, 0.000001)
        assertEquals(14.35, result.realizedPnl, 0.000001)
        assertEquals(0.65, result.fee, 0.000001)
    }

    @Test
    fun fullSellClosesPositionAndResetsAverageEntry() {
        val result = PaperTradingMath.calculate(
            side = "SELL",
            oldCash = 500.0,
            oldQuantity = 0.5,
            oldAverageEntry = 110.6,
            quantity = 0.5,
            price = 100.0,
            feeRate = 0.01
        )

        assertEquals(549.5, result.cash, 0.000001)
        assertEquals(0.0, result.quantity, 0.000001)
        assertEquals(0.0, result.averageEntry, 0.000001)
        assertEquals(-5.8, result.realizedPnl, 0.000001)
        assertEquals(0.5, result.fee, 0.000001)
    }

    @Test
    fun rejectsSellLargerThanHeldPosition() {
        assertThrows(IllegalArgumentException::class.java) {
            PaperTradingMath.calculate(
                side = "SELL",
                oldCash = 500.0,
                oldQuantity = 0.2,
                oldAverageEntry = 100.0,
                quantity = 0.3,
                price = 110.0,
                feeRate = 0.01
            )
        }
    }

    @Test
    fun rejectsBuyWhenCashCannotCoverFee() {
        assertThrows(IllegalArgumentException::class.java) {
            PaperTradingMath.calculate(
                side = "BUY",
                oldCash = 100.0,
                oldQuantity = 0.0,
                oldAverageEntry = 0.0,
                quantity = 1.0,
                price = 100.0,
                feeRate = 0.01
            )
        }
    }
}
