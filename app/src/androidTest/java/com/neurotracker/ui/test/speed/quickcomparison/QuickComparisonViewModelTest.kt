package com.neurotracker.ui.test.speed.quickcomparison

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.speed.quickComparison.QuickComparisonViewModel
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
 * Tests instrumentados del [QuickComparisonViewModel].
 *
 * Verifica la lógica de comparación de símbolos, la respuesta del usuario
 * y el cálculo de precisión: hits / (hits + errors) × 100.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class QuickComparisonViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: QuickComparisonViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = QuickComparisonViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE y contadores a cero.
     */
    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(QuickComparisonViewModel.TestState.IDLE, viewModel.testState.value)
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
     * Verifica que [startTest] cambia el estado a RUNNING y genera un estímulo.
     */
    @Test
    fun startTest_changesStateToRunning() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        assertEquals(QuickComparisonViewModel.TestState.RUNNING, viewModel.testState.value)
        assertTrue(viewModel.currentItem.value != null)
    }

    /**
     * Verifica que responder correctamente (iguales cuando son iguales) incrementa hits.
     */
    @Test
    fun onAnswer_correctResponseForEqual_incrementsHits() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        val item = viewModel.currentItem.value ?: return@runTest
        viewModel.onAnswer(item.areSame)
        assertEquals(1, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }

    /**
     * Verifica que responder incorrectamente incrementa los errores.
     */
    @Test
    fun onAnswer_incorrectResponse_incrementsErrors() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        val item = viewModel.currentItem.value ?: return@runTest
        viewModel.onAnswer(!item.areSame)
        assertEquals(0, viewModel.hits.value)
        assertEquals(1, viewModel.errors.value)
    }

    /**
     * Verifica que [onAnswer] no registra respuesta fuera de RUNNING.
     */
    @Test
    fun onAnswer_whenNotRunning_doesNotRegisterResponse() {
        viewModel.onAnswer(true)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }

    /**
     * Verifica que la precisión está en rango válido tras completar el test.
     * 20 rondas × 2000ms = 40_000ms.
     */
    @Test
    fun precisionPercent_afterTestCompletes_isInValidRange() = runTest {
        viewModel.startTest()
        advanceTimeBy(45_000)
        assertTrue(viewModel.precisionPercent() in 0..100)
    }

    /**
     * Verifica que el test termina en FINISHED tras todas las rondas.
     */
    @Test
    fun afterAllRounds_stateChangesToFinished() = runTest {
        viewModel.startTest()
        advanceTimeBy(45_000)
        assertEquals(QuickComparisonViewModel.TestState.FINISHED, viewModel.testState.value)
    }

    /**
     * Verifica que [resetTest] restaura el estado inicial.
     */
    @Test
    fun resetTest_restoresInitialState() = runTest {
        viewModel.startTest()
        advanceTimeBy(500)
        viewModel.resetTest()
        assertEquals(QuickComparisonViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }
}
