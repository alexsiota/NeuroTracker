package com.neurotracker.executive.taskswitching

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.executive.tasksWitching.TaskSwitchingViewModel
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
 * Tests instrumentados del [TaskSwitchingViewModel].
 *
 * Verifica la lógica de cambio de regla (COLOR/SHAPE), el sistema de puntuación
 * y el cálculo de precisión: hits / (hits + errors) × 100.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TaskSwitchingViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: TaskSwitchingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TaskSwitchingViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE y contadores a cero.
     */
    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(TaskSwitchingViewModel.TestState.IDLE, viewModel.testState.value)
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
        advanceTimeBy(200)
        assertEquals(TaskSwitchingViewModel.TestState.RUNNING, viewModel.testState.value)
        assertTrue(viewModel.currentStimulus.value != null)
    }

    /**
     * Verifica que seleccionar la respuesta correcta incrementa los aciertos.
     */
    @Test
    fun onOptionSelected_correctAnswer_incrementsHits() = runTest {
        viewModel.startTest()
        advanceTimeBy(200)
        val correct = viewModel.correctAnswer.value
        viewModel.onOptionSelected(correct)
        assertEquals(1, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }

    /**
     * Verifica que seleccionar una respuesta incorrecta incrementa los errores.
     */
    @Test
    fun onOptionSelected_wrongAnswer_incrementsErrors() = runTest {
        viewModel.startTest()
        advanceTimeBy(200)
        val correct  = viewModel.correctAnswer.value
        val wrong    = viewModel.options.value.firstOrNull { it != correct } ?: return@runTest
        viewModel.onOptionSelected(wrong)
        assertEquals(0, viewModel.hits.value)
        assertEquals(1, viewModel.errors.value)
    }

    /**
     * Verifica que [onOptionSelected] no registra nada fuera de RUNNING.
     */
    @Test
    fun onOptionSelected_whenNotRunning_doesNotRegisterResponse() {
        viewModel.onOptionSelected("Rojo")
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }

    /**
     * Verifica que la precisión está en rango válido tras completar el test.
     * 20 rondas × 3000ms = 60_000ms.
     */
    @Test
    fun precisionPercent_afterTestCompletes_isInValidRange() = runTest {
        viewModel.startTest()
        advanceTimeBy(65_000)
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
        assertEquals(TaskSwitchingViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }
}
