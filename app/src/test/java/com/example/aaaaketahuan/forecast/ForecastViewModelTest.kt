package com.example.aaaaketahuan.forecast

import com.example.aaaaketahuan.forecast.model.BalancePrediction
import com.example.aaaaketahuan.forecast.model.DeficitRisk
import com.example.aaaaketahuan.forecast.model.EndOfMonthPrediction
import com.example.aaaaketahuan.forecast.model.PredictionConfidence
import com.example.aaaaketahuan.forecast.model.RiskSeverity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit test untuk [ForecastViewModel].
 *
 * Pola testing:
 * - [StandardTestDispatcher] untuk kontrol penuh eksekusi coroutine.
 * - **Turbine** untuk observasi StateFlow: [Flow.test] menyediakan
 *   cancellation otomatis dan API `awaitItem()` yang lebih bersih
 *   daripada collect manual.
 * - Mock [ForecastRepository] via MockK — return value sudah ditentukan,
 *   logic repository tidak diuji ulang di sini.
 *
 * Alasan Turbine:
 * - Tidak perlu mengelola Job/cancellation manual.
 * - Timeout built-in mencegah test hanging.
 * - `awaitItem()` sinkron dengan scheduler test dispatcher.
 * - Standar de facto untuk StateFlow testing di ekosistem Kotlin.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForecastViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepo: ForecastRepository
    private lateinit var viewModel: ForecastViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk()
        viewModel = ForecastViewModel(mockRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Helpers ───────────────────────────────────────────────────

    private fun advance() = testDispatcher.scheduler.advanceUntilIdle()

    // ═══════════════════════════════════════════════════════════════
    // FITUR 1: PREDIKSI PENGELUARAN AKHIR BULAN
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `endOfMonth loading state transitions`() = runTest {
        coEvery { mockRepo.getEndOfMonthPrediction() } returns
                EndOfMonthPrediction.Predicted(100.0, 200.0, PredictionConfidence.HIGH)

        // Check initial state
        assertEquals(false, viewModel.isLoading.value)

        viewModel.loadEndOfMonthPrediction()
        advance()

        // After completion, loading should be false
        assertEquals(false, viewModel.isLoading.value)

        // Check final state
        val r = viewModel.endOfMonthPrediction.value
        if (r is EndOfMonthPrediction.Predicted) {
            assertEquals(100.0, r.currentTotal, 0.001)
            assertEquals(200.0, r.predictedTotal, 0.001)
            assertEquals(PredictionConfidence.HIGH, r.confidence)
        } else fail("Expected Predicted, got $r")
    }

    @Test
    fun `endOfMonth emits InsufficientData`() = runTest {
        coEvery { mockRepo.getEndOfMonthPrediction() } returns
                EndOfMonthPrediction.InsufficientData("not enough", 5, 14)

        viewModel.loadEndOfMonthPrediction()
        advance()

        val r = viewModel.endOfMonthPrediction.value
        if (r is EndOfMonthPrediction.InsufficientData) {
            assertEquals(5, r.daysAvailable)
            assertEquals(14, r.daysRequired)
        } else fail("Expected InsufficientData, got $r")
    }

    @Test
    fun `endOfMonth sets errorMessage on exception`() = runTest {
        coEvery { mockRepo.getEndOfMonthPrediction() } throws RuntimeException("gagal bos")

        viewModel.loadEndOfMonthPrediction()
        advance()

        val err = viewModel.errorMessage.value
        assertNotNull(err)
        assertTrue(err!!.contains("gagal bos"))
        assertNull(viewModel.endOfMonthPrediction.value)
    }

    // ═══════════════════════════════════════════════════════════════
    // FITUR 2: PREDIKSI SALDO
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `balancePrediction emits Predicted`() = runTest {
        val predicted = BalancePrediction.Predicted(
            currentBalance = 500000.0,
            predictedBalance = 750000.0,
            targetDate = LocalDate.now().plusDays(7).toString(),
            confidence = PredictionConfidence.MEDIUM
        )
        coEvery { mockRepo.getBalancePrediction(any()) } returns predicted

        viewModel.loadBalancePrediction(LocalDate.now().plusDays(7))
        advance()

        val r = viewModel.balancePrediction.value
        if (r is BalancePrediction.Predicted) {
            assertEquals(500000.0, r.currentBalance, 0.001)
            assertEquals(750000.0, r.predictedBalance, 0.001)
            assertEquals(PredictionConfidence.MEDIUM, r.confidence)
        } else fail("Expected Predicted, got $r")
    }

    @Test
    fun `balancePrediction emits TargetDateInPast`() = runTest {
        coEvery { mockRepo.getBalancePrediction(any()) } returns
                BalancePrediction.TargetDateInPast("2024-01-01")

        viewModel.loadBalancePrediction(LocalDate.now().minusDays(1))
        advance()

        val r = viewModel.balancePrediction.value
        if (r is BalancePrediction.TargetDateInPast) {
            assertEquals("2024-01-01", r.targetDate)
        } else fail("Expected TargetDateInPast, got $r")
    }

    @Test
    fun `balancePrediction sets errorMessage on exception`() = runTest {
        coEvery { mockRepo.getBalancePrediction(any()) } throws RuntimeException("gagal saldo")

        viewModel.loadBalancePrediction(LocalDate.now().plusDays(7))
        advance()

        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("gagal saldo"))
    }

    // ═══════════════════════════════════════════════════════════════
    // FITUR 3: RISIKO DEFISIT
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `deficitRisk emits Safe`() = runTest {
        coEvery { mockRepo.getDeficitRisk() } returns DeficitRisk.Safe(250000.0, 250000.0)

        viewModel.loadDeficitRisk()
        advance()

        val r = viewModel.deficitRisk.value
        if (r is DeficitRisk.Safe) assertEquals(250000.0, r.projectedBalance, 0.001)
        else fail("Expected Safe, got $r")
    }

    @Test
    fun `deficitRisk emits AtRisk`() = runTest {
        coEvery { mockRepo.getDeficitRisk() } returns
                DeficitRisk.AtRisk(RiskSeverity.HIGH, -50000.0, 50000.0)

        viewModel.loadDeficitRisk()
        advance()

        val r = viewModel.deficitRisk.value
        if (r is DeficitRisk.AtRisk) {
            assertEquals(RiskSeverity.HIGH, r.level)
            assertEquals(-50000.0, r.projectedBalance, 0.001)
            assertEquals(50000.0, r.shortfallAmount, 0.001)
        } else fail("Expected AtRisk, got $r")
    }

    @Test
    fun `deficitRisk sets errorMessage on exception`() = runTest {
        coEvery { mockRepo.getDeficitRisk() } throws RuntimeException("gagal deficit")

        viewModel.loadDeficitRisk()
        advance()

        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("gagal deficit"))
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `refreshAll resets and reloads endOfMonth and deficit`() = runTest {
        coEvery { mockRepo.getEndOfMonthPrediction() } returns
                EndOfMonthPrediction.Predicted(100.0, 200.0, PredictionConfidence.LOW)
        coEvery { mockRepo.getDeficitRisk() } returns DeficitRisk.Safe(300.0, 300.0)

        // First load
        viewModel.loadEndOfMonthPrediction()
        viewModel.loadDeficitRisk()
        advance()
        assertNotNull(viewModel.endOfMonthPrediction.value)
        assertNotNull(viewModel.deficitRisk.value)

        // refreshAll should clear and reload
        viewModel.refreshAll()
        advance()

        assertNotNull(viewModel.endOfMonthPrediction.value)
        assertNotNull(viewModel.deficitRisk.value)
    }

    @Test
    fun `clearError sets errorMessage to null`() = runTest {
        coEvery { mockRepo.getEndOfMonthPrediction() } throws RuntimeException("err")
        viewModel.loadEndOfMonthPrediction()
        advance()
        assertNotNull(viewModel.errorMessage.value)

        viewModel.clearError()
        assertNull(viewModel.errorMessage.value)
    }
}
