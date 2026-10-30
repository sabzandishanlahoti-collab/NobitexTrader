package ir.nobitextrader.app.scanner

import ir.nobitextrader.app.strategy.StrategyAnalysisCoordinator
import ir.nobitextrader.app.strategy.Candle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketScannerViewModelAnalysisTest {

    @Test
    fun analyzeMarketNormalizesSymbolAndRequestsDefaultClosedCandleWindow() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var requestedSymbol: String? = null
            var requestedResolution: String? = null
            var requestedFrom: Long? = null
            var requestedTo: Long? = null

            val coordinator = StrategyAnalysisCoordinator(
                candleLoader = { symbol, resolution, from, to ->
                    requestedSymbol = symbol
                    requestedResolution = resolution
                    requestedFrom = from
                    requestedTo = to
                    emptyList()
                }
            )
            val viewModel = MarketScannerViewModel(coordinator)

            viewModel.analyzeMarket("  btcirt ")
            advanceUntilIdle()

            assertEquals("BTCIRT", requestedSymbol)
            assertEquals("15", requestedResolution)
            assertNotNull(requestedFrom)
            assertNotNull(requestedTo)
            assertEquals(48L * 60L * 60L, requestedTo!! - requestedFrom!!)
            assertTrue(requestedTo!! <= System.currentTimeMillis() / 1_000L)
            val state = viewModel.uiState.value.analysisBySymbol["BTCIRT"]
            assertNotNull(state)
            assertFalse(state!!.isLoading)
            assertNotNull(state.result)
            assertNull(state.error)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun duplicateActiveRequestForSameSymbolIsIgnored() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var loaderCalls = 0
            val coordinator = StrategyAnalysisCoordinator(
                candleLoader = { _, _, _, _ ->
                    loaderCalls++
                    awaitCancellation()
                }
            )
            val viewModel = MarketScannerViewModel(coordinator)

            viewModel.analyzeMarket(" btcirt ")
            viewModel.analyzeMarket("BTCIRT")
            runCurrent()

            assertEquals(1, loaderCalls)
            assertTrue(
                viewModel.uiState.value.analysisBySymbol["BTCIRT"]?.isLoading == true
            )

            viewModel.clearResults()
            runCurrent()
            assertTrue(viewModel.uiState.value.analysisBySymbol.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun candleFetchFailureIsStoredAsAnalysisResultError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val coordinator = StrategyAnalysisCoordinator(
                candleLoader = { _, _, _, _ ->
                    throw IllegalStateException("Nobitex unavailable")
                }
            )
            val viewModel = MarketScannerViewModel(coordinator)

            viewModel.analyzeMarket("BTCIRT")
            advanceUntilIdle()

            val state = viewModel.uiState.value.analysisBySymbol["BTCIRT"]
            assertNotNull(state)
            assertFalse(state!!.isLoading)
            assertNotNull(state.result)
            assertEquals("Nobitex unavailable", state.result?.fetchError)
            assertNull(state.error)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun clearResultsCancelsInFlightAnalysisAndClearsItsState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var cancellationObserved = false
            val coordinator = StrategyAnalysisCoordinator(
                candleLoader = { _, _, _, _ ->
                    try {
                        awaitCancellation()
                    } catch (exception: CancellationException) {
                        cancellationObserved = true
                        throw exception
                    }
                }
            )
            val viewModel = MarketScannerViewModel(coordinator)

            viewModel.analyzeMarket("ETHIRT")
            runCurrent()
            assertTrue(
                viewModel.uiState.value.analysisBySymbol["ETHIRT"]?.isLoading == true
            )

            viewModel.clearResults()
            runCurrent()

            assertTrue(cancellationObserved)
            assertTrue(viewModel.uiState.value.analysisBySymbol.isEmpty())
            assertFalse(viewModel.uiState.value.isScanning)
            assertTrue(viewModel.uiState.value.results.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun cancelledOldRequestCannotRemoveNewRequestForSameSymbol() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var loaderCalls = 0
            val coordinator = StrategyAnalysisCoordinator(
                candleLoader = { _, _, _, _ ->
                    loaderCalls++
                    awaitCancellation()
                }
            )
            val viewModel = MarketScannerViewModel(coordinator)

            viewModel.analyzeMarket("BTCIRT")
            runCurrent()
            assertEquals(1, loaderCalls)

            viewModel.clearResults()
            viewModel.analyzeMarket("BTCIRT")
            runCurrent()
            assertEquals(2, loaderCalls)

            // The cancelled request's finally block must not unregister
            // the newer active request for the same normalized symbol.
            viewModel.analyzeMarket(" btcirt ")
            runCurrent()
            assertEquals(2, loaderCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun differentSymbolsCanBeAnalyzedIndependently() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val requestedSymbols = mutableListOf<String>()
            val coordinator = StrategyAnalysisCoordinator(
                candleLoader = { symbol, _, _, _ ->
                    requestedSymbols += symbol
                    emptyList<Candle>()
                }
            )
            val viewModel = MarketScannerViewModel(coordinator)

            viewModel.analyzeMarket("BTCIRT")
            viewModel.analyzeMarket("ETHIRT")
            advanceUntilIdle()

            assertEquals(setOf("BTCIRT", "ETHIRT"), requestedSymbols.toSet())
            assertNotNull(viewModel.uiState.value.analysisBySymbol["BTCIRT"]?.result)
            assertNotNull(viewModel.uiState.value.analysisBySymbol["ETHIRT"]?.result)
        } finally {
            Dispatchers.resetMain()
        }
    }
}


class CandleAnalysisWindowBoundaryTest {

    @Test
    fun windowEndsAtOpeningTimeOfLastFullyClosedCandle() {
        val intervalSeconds = 900L
        val nowSeconds = 1_700_000_123L
        val window = CandleAnalysisWindow.forNow(
            nowMillis = nowSeconds * 1_000L,
            intervalSeconds = intervalSeconds,
            lookbackHours = 48L
        )
        val currentCandleOpen = nowSeconds - (nowSeconds % intervalSeconds)
        assertEquals(currentCandleOpen - intervalSeconds, window.toUnixSeconds)
        assertTrue(window.toUnixSeconds < nowSeconds)
        assertEquals(0L, window.toUnixSeconds % intervalSeconds)
    }

    @Test
    fun windowHasExactlyConfiguredLookbackDuration() {
        val window = CandleAnalysisWindow.forNow(
            nowMillis = 1_700_000_123_000L,
            intervalSeconds = 900L,
            lookbackHours = 48L
        )
        assertEquals(48L * 3_600L, window.toUnixSeconds - window.fromUnixSeconds)
    }

    @Test
    fun exactCandleBoundaryExcludesCurrentlyOpeningCandle() {
        val intervalSeconds = 900L
        val nowSeconds = 1_700_000_100L
        val exactBoundary = nowSeconds - (nowSeconds % intervalSeconds)
        val window = CandleAnalysisWindow.forNow(
            nowMillis = exactBoundary * 1_000L,
            intervalSeconds = intervalSeconds,
            lookbackHours = 48L
        )
        assertEquals(exactBoundary - intervalSeconds, window.toUnixSeconds)
    }

    @Test
    fun rejectsNonPositiveWindowParameters() {
        assertIllegalArgument {
            CandleAnalysisWindow.forNow(1_700_000_000_000L, 0L, 48L)
        }
        assertIllegalArgument {
            CandleAnalysisWindow.forNow(1_700_000_000_000L, 900L, 0L)
        }
        assertIllegalArgument {
            CandleAnalysisWindow.forNow(0L, 900L, 48L)
        }
    }

    private fun assertIllegalArgument(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected validation failure.
        }
    }
}
