package com.neurotracker.speed.sequenceordering

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.speed.sequenceOrdering.SequenceOrderingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [SequenceOrderingViewModel].
 *
 * Verifica la generación de secuencias, la introducción de números,
 * el timeout por ronda y el cálculo de precisión.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SequenceOrderingViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SequenceOrderingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SequenceOrderingViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE y contadores a cero.
     */
    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(SequenceOrderingViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }

    /**
     * Verifica que la precisión es 0 sin intentos.
     */
    @Test
    fun precisionPercent_withNoAttempts_returnsZero() {
        assertEquals(0, viewModel.precisionPercent())
    }

    /**
     * Verifica que [startTest] cambia el estado a RUNNING y genera una secuencia.
     */
    @Test
    fun startTest_changesStateToRunning() = runTest {
        viewModel.startTest()
        advanceTimeBy(200)
        assertEquals(SequenceOrderingViewModel.TestState.RUNNING, viewModel.testState.value)
        assertEquals(5, viewModel.currentSequence.value.size)
    }

    /**
     * Verifica que [onNumberTapped] no registra pulsaciones fuera de RUNNING.
     */
    @Test
    fun onNumberTapped_whenNotRunning_doesNotRegisterNumber() {
        viewModel.onNumberTapped(5)
        assertTrue(viewModel.userOrder.value.isEmpty())
    }

    /**
     * Verifica que pulsar la secuencia correcta en orden incrementa los aciertos.
     */
    @Test
    fun onNumberTapped_correctOrder_incrementsHits() = runTest {
        viewModel.startTest()
        advanceTimeBy(200)
        val sorted = viewModel.currentSequence.value.sorted()
        sorted.forEach { viewModel.onNumberTapped(it) }
        assertEquals(1, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }

    /**
     * Verifica que la precisión está en rango válido tras completar el test.
     * 10 rondas × 10_000ms timeout = 100_000ms.
     */
    @Test
    fun precisionPercent_afterTestCompletes_isInValidRange() = runTest {
        viewModel.startTest()
        advanceTimeBy(110_000)
        assertTrue(viewModel.precisionPercent() in 0..100)
    }

    /**
     * Verifica que [resetTest] restaura el estado inicial.
     */
    @Test
    fun resetTest_restoresInitialState() = runTest {
        viewModel.startTest()
        advanceTimeBy(500)
        viewModel.resetTest()
        assertEquals(SequenceOrderingViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }
}
