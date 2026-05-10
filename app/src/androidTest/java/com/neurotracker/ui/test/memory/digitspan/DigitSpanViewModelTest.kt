package com.neurotracker.ui.test.memory.digitspan

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.memory.digitspan.DigitSpanViewModel
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
 * Tests instrumentados del [DigitSpanViewModel].
 *
 * Verifica la lógica del span de dígitos: estado inicial, introducción de dígitos,
 * backspace y cálculo de precisión basada en el span máximo alcanzado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DigitSpanViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: DigitSpanViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DigitSpanViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE, span 3, maxSpan 0.
     */
    @Test
    fun initialState_isIdleWithSpan3() {
        assertEquals(DigitSpanViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(3, viewModel.currentSpan.value)
        assertEquals(0, viewModel.maxSpan.value)
    }

    /**
     * Verifica que la precisión es 0 antes de iniciar.
     */
    @Test
    fun precisionPercent_beforeStart_returnsZero() {
        assertEquals(0, viewModel.precisionPercent())
    }

    /**
     * Verifica que [startTest] cambia el estado a SHOWING.
     */
    @Test
    fun startTest_changesStateToShowing() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        assertEquals(DigitSpanViewModel.TestState.SHOWING, viewModel.testState.value)
    }

    /**
     * Verifica que [onDigitPressed] no registra dígitos fuera de INPUT.
     */
    @Test
    fun onDigitPressed_whenNotInInput_doesNotAddDigit() {
        viewModel.onDigitPressed(5)
        assertTrue(viewModel.userInput.value.isEmpty())
    }

    /**
     * Verifica que [onBackspace] elimina el último dígito introducido.
     */
    @Test
    fun onBackspace_removesLastDigit() = runTest {
        viewModel.startTest()
        // Avanzar hasta la fase INPUT (600ms pausa + 3 × (800ms + 300ms) = 3900ms)
        advanceTimeBy(4500)
        if (viewModel.testState.value == DigitSpanViewModel.TestState.INPUT) {
            viewModel.onDigitPressed(1)
            viewModel.onDigitPressed(2)
            assertEquals(2, viewModel.userInput.value.size)
            viewModel.onBackspace()
            assertEquals(1, viewModel.userInput.value.size)
        }
    }

    /**
     * Verifica que [resetTest] restaura el estado inicial con span 3.
     */
    @Test
    fun resetTest_restoresInitialState() = runTest {
        viewModel.startTest()
        advanceTimeBy(500)
        viewModel.resetTest()
        assertEquals(DigitSpanViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(3, viewModel.currentSpan.value)
        assertEquals(0, viewModel.maxSpan.value)
        assertTrue(viewModel.userInput.value.isEmpty())
    }

    /**
     * Verifica que la precisión está en rango válido tras iniciar el test.
     */
    @Test
    fun precisionPercent_isAlwaysInValidRange() = runTest {
        viewModel.startTest()
        advanceTimeBy(1000)
        assertTrue(viewModel.precisionPercent() in 0..100)
    }

    @Test
    fun afterShowingPhase_stateIsInput() = runTest {
        viewModel.startTest()
        // delay(600) + span3 × (delay(800) + delay(300)) = 3900ms; use 4000 to be safe
        advanceTimeBy(4000)
        assertEquals(DigitSpanViewModel.TestState.INPUT, viewModel.testState.value)
    }

    @Test
    fun onDigitPressed_inInputState_addsDigitToUserInput() = runTest {
        viewModel.startTest()
        advanceTimeBy(4000)
        assertEquals(DigitSpanViewModel.TestState.INPUT, viewModel.testState.value)
        val before = viewModel.userInput.value.size
        viewModel.onDigitPressed(7)
        assertEquals(before + 1, viewModel.userInput.value.size)
    }

    @Test
    fun onBackspace_whenInputIsEmpty_doesNotCrash() = runTest {
        viewModel.startTest()
        advanceTimeBy(4000)
        assertEquals(DigitSpanViewModel.TestState.INPUT, viewModel.testState.value)
        assertTrue(viewModel.userInput.value.isEmpty())
        viewModel.onBackspace() // must not throw
        assertTrue(viewModel.userInput.value.isEmpty())
    }

    @Test
    fun validateInput_withCorrectSequence_setsFeedbackCorrectTrue() = runTest {
        viewModel.startTest()
        advanceTimeBy(4000)
        assertEquals(DigitSpanViewModel.TestState.INPUT, viewModel.testState.value)
        val seq = viewModel.sequence.value
        seq.forEach { viewModel.onDigitPressed(it) }
        assertEquals(DigitSpanViewModel.TestState.FEEDBACK, viewModel.testState.value)
        assertEquals(true, viewModel.feedbackCorrect.value)
    }

    @Test
    fun validateInput_withWrongSequence_setsFeedbackCorrectFalse() = runTest {
        viewModel.startTest()
        advanceTimeBy(4000)
        assertEquals(DigitSpanViewModel.TestState.INPUT, viewModel.testState.value)
        val seq = viewModel.sequence.value
        seq.dropLast(1).forEach { viewModel.onDigitPressed(it) }
        val wrongLast = (seq.last() + 1) % 10
        viewModel.onDigitPressed(if (wrongLast == seq.last()) (wrongLast + 1) % 10 else wrongLast)
        assertEquals(DigitSpanViewModel.TestState.FEEDBACK, viewModel.testState.value)
        assertEquals(false, viewModel.feedbackCorrect.value)
    }

    @Test
    fun afterCorrectInput_andFeedbackDelay_stateIsShowingWithLargerSpan() = runTest {
        viewModel.startTest()
        advanceTimeBy(4000)
        val seq = viewModel.sequence.value
        seq.forEach { viewModel.onDigitPressed(it) }
        advanceTimeBy(1100) // feedback delay(1000) + margin
        assertEquals(DigitSpanViewModel.TestState.SHOWING, viewModel.testState.value)
        assertEquals(seq.size + 1, viewModel.currentSpan.value)
    }

    @Test
    fun afterWrongInput_andFeedbackDelay_stateIsFinished() = runTest {
        viewModel.startTest()
        advanceTimeBy(4000)
        val seq = viewModel.sequence.value
        seq.dropLast(1).forEach { viewModel.onDigitPressed(it) }
        val wrongLast = (seq.last() + 1) % 10
        viewModel.onDigitPressed(if (wrongLast == seq.last()) (wrongLast + 1) % 10 else wrongLast)
        advanceTimeBy(1100)
        assertEquals(DigitSpanViewModel.TestState.FINISHED, viewModel.testState.value)
    }

    @Test
    fun precisionPercent_afterOneCorrectSpan_isGreaterThanZero() = runTest {
        viewModel.startTest()
        advanceTimeBy(4000)
        val seq = viewModel.sequence.value
        seq.forEach { viewModel.onDigitPressed(it) }
        advanceTimeBy(1100) // now maxSpan = 3
        // Enter wrong sequence in the next round to finish
        val seq2 = viewModel.sequence.value
        seq2.dropLast(1).forEach { viewModel.onDigitPressed(it) }
        viewModel.onDigitPressed((seq2.last() + 1) % 10)
        assertTrue(viewModel.precisionPercent() > 0)
    }
}
