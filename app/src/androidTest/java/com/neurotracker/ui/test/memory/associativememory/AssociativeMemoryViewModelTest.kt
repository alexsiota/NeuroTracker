package com.neurotracker.memory.associativememory

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.memory.associativeMemory.AssociativeMemoryViewModel
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
 * Tests instrumentados del [AssociativeMemoryViewModel].
 *
 * Verifica el flujo memorización → recall → feedback y el cálculo de precisión.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AssociativeMemoryViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AssociativeMemoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AssociativeMemoryViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE y contadores a cero.
     */
    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(AssociativeMemoryViewModel.TestState.IDLE, viewModel.testState.value)
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
     * Verifica que [startTest] cambia el estado a MEMORIZE y genera 5 pares.
     */
    @Test
    fun startTest_changesStateToMemorizeWithPairs() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        assertEquals(AssociativeMemoryViewModel.TestState.MEMORIZE, viewModel.testState.value)
        assertEquals(5, viewModel.pairs.value.size)
    }

    /**
     * Verifica que tras la memorización (6s) el estado cambia a RECALL.
     */
    @Test
    fun afterMemorization_stateChangesToRecall() = runTest {
        viewModel.startTest()
        advanceTimeBy(7000)
        assertEquals(AssociativeMemoryViewModel.TestState.RECALL, viewModel.testState.value)
    }

    /**
     * Verifica que responder correctamente incrementa los aciertos.
     */
    @Test
    fun onAnswerSelected_correctAnswer_incrementsHits() = runTest {
        viewModel.startTest()
        advanceTimeBy(7000)
        val correct = viewModel.currentQuestion.value?.correctAnswer ?: return@runTest
        viewModel.onAnswerSelected(correct)
        assertEquals(1, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }

    /**
     * Verifica que responder incorrectamente incrementa los errores.
     */
    @Test
    fun onAnswerSelected_wrongAnswer_incrementsErrors() = runTest {
        viewModel.startTest()
        advanceTimeBy(7000)
        val question = viewModel.currentQuestion.value ?: return@runTest
        val wrong    = question.options.firstOrNull { it != question.correctAnswer } ?: return@runTest
        viewModel.onAnswerSelected(wrong)
        assertEquals(0, viewModel.hits.value)
        assertEquals(1, viewModel.errors.value)
    }

    /**
     * Verifica que la precisión está en rango válido.
     */
    @Test
    fun precisionPercent_afterInteraction_isInValidRange() = runTest {
        viewModel.startTest()
        advanceTimeBy(7000)
        val correct = viewModel.currentQuestion.value?.correctAnswer ?: return@runTest
        viewModel.onAnswerSelected(correct)
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
        assertEquals(AssociativeMemoryViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertTrue(viewModel.pairs.value.isEmpty())
    }
}
