package com.neurotracker.ui.test.attention.stroop

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.attention.stroop.StroopViewModel
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
 * Tests instrumentados del [StroopViewModel].
 *
 * Verifica los estados del test, el cálculo de precisión y la lógica de respuesta.
 * Precisión Stroop: hits / (hits + errors + omissions) × 100.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class StroopViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: StroopViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = StroopViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE y contadores a cero.
     */
    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(StroopViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
        assertEquals(0, viewModel.omissions.value)
    }

    /**
     * Verifica que la precisión es 0 sin intentos.
     */
    @Test
    fun precisionPercent_withNoAttempts_returnsZero() {
        assertEquals(0, viewModel.precisionPercent())
    }

    /**
     * Verifica que [StroopViewModel.startTest] cambia el estado a RUNNING.
     */
    @Test
    fun startTest_changesStateToRunning() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        assertEquals(StroopViewModel.TestState.RUNNING, viewModel.testState.value)
    }

    /**
     * Verifica que [StroopViewModel.onUserAnswer] no registra respuesta fuera de RUNNING.
     */
    @Test
    fun onUserAnswer_whenNotRunning_doesNotRegisterResponse() {
        viewModel.onUserAnswer(true)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }

    /**
     * Verifica que [StroopViewModel.resetTest] restaura el estado inicial.
     */
    @Test
    fun resetTest_restoresInitialState() = runTest {
        viewModel.startTest()
        advanceTimeBy(500)
        viewModel.resetTest()
        assertEquals(StroopViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }

    /**
     * Verifica que la precisión está en rango válido tras completar el test.
     * 25 rondas × 2500ms = 62_500ms.
     */
    @Test
    fun precisionPercent_afterTestCompletes_isInValidRange() = runTest {
        viewModel.startTest()
        advanceTimeBy(70_000)
        assertTrue(viewModel.precisionPercent() in 0..100)
    }

    /**
     * Verifica que la ronda actual incrementa durante el test.
     */
    @Test
    fun currentRound_incrementsDuringTest() = runTest {
        viewModel.startTest()
        advanceTimeBy(3000)
        assertTrue(viewModel.currentRound.value > 0)
    }
}
