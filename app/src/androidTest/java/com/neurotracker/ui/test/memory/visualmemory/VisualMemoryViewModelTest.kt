package com.neurotracker.ui.test.memory.visualmemory

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.memory.visualMemory.VisualMemoryViewModel
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
 * Tests instrumentados del [VisualMemoryViewModel].
 *
 * Verifica el flujo de memorización/reproducción de secuencias de celdas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class VisualMemoryViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: VisualMemoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = VisualMemoryViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE, longitud 3, maxLength 0.
     */
    @Test
    fun initialState_isIdleWithLength3() {
        assertEquals(VisualMemoryViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(3, viewModel.currentLength.value)
        assertEquals(0, viewModel.maxLength.value)
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
        assertEquals(VisualMemoryViewModel.TestState.SHOWING, viewModel.testState.value)
    }

    /**
     * Verifica que [onCellPressed] no registra pulsaciones fuera de INPUT.
     */
    @Test
    fun onCellPressed_whenNotInInput_doesNotAddToInput() {
        viewModel.onCellPressed(0)
        assertTrue(viewModel.userInput.value.isEmpty())
    }

    /**
     * Verifica que [resetTest] restaura el estado inicial.
     */
    @Test
    fun resetTest_restoresInitialState() = runTest {
        viewModel.startTest()
        advanceTimeBy(500)
        viewModel.resetTest()
        assertEquals(VisualMemoryViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(3, viewModel.currentLength.value)
        assertEquals(0, viewModel.maxLength.value)
        assertTrue(viewModel.userInput.value.isEmpty())
    }

    /**
     * Verifica que la precisión está en rango válido.
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
        // delay(600) + span3 × (delay(600) + delay(300)) = 3300ms
        advanceTimeBy(3400)
        assertEquals(VisualMemoryViewModel.TestState.INPUT, viewModel.testState.value)
    }

    @Test
    fun onCellPressed_inInputState_addsToUserInput() = runTest {
        viewModel.startTest()
        advanceTimeBy(3400)
        assertEquals(VisualMemoryViewModel.TestState.INPUT, viewModel.testState.value)
        val before = viewModel.userInput.value.size
        viewModel.onCellPressed(0)
        assertEquals(before + 1, viewModel.userInput.value.size)
    }

    @Test
    fun wrongCell_atFirstPosition_triggersFeedbackFalse() = runTest {
        viewModel.startTest()
        advanceTimeBy(3400)
        assertEquals(VisualMemoryViewModel.TestState.INPUT, viewModel.testState.value)
        // find a cell NOT in the sequence as first cell
        val seq = viewModel.sequence.value
        val wrongCell = (0..8).first { it != seq[0] }
        viewModel.onCellPressed(wrongCell)
        assertEquals(VisualMemoryViewModel.TestState.FEEDBACK, viewModel.testState.value)
        assertEquals(false, viewModel.feedbackCorrect.value)
    }

    @Test
    fun correctFullSequence_triggersFeedbackTrue() = runTest {
        viewModel.startTest()
        advanceTimeBy(3400)
        assertEquals(VisualMemoryViewModel.TestState.INPUT, viewModel.testState.value)
        val seq = viewModel.sequence.value
        seq.forEach { viewModel.onCellPressed(it) }
        assertEquals(VisualMemoryViewModel.TestState.FEEDBACK, viewModel.testState.value)
        assertEquals(true, viewModel.feedbackCorrect.value)
    }

    @Test
    fun afterCorrectFeedback_stateIsShowingWithLargerLength() = runTest {
        viewModel.startTest()
        advanceTimeBy(3400)
        val seq = viewModel.sequence.value
        seq.forEach { viewModel.onCellPressed(it) }
        advanceTimeBy(1100) // feedback delay(1000) + margin
        assertEquals(VisualMemoryViewModel.TestState.SHOWING, viewModel.testState.value)
        assertEquals(seq.size + 1, viewModel.currentLength.value)
    }

    @Test
    fun afterWrongFeedback_andDelay_stateIsFinished() = runTest {
        viewModel.startTest()
        advanceTimeBy(3400)
        val seq = viewModel.sequence.value
        val wrongCell = (0..8).first { it != seq[0] }
        viewModel.onCellPressed(wrongCell)
        advanceTimeBy(1100)
        assertEquals(VisualMemoryViewModel.TestState.FINISHED, viewModel.testState.value)
    }

    @Test
    fun precisionPercent_afterOneCorrectRound_isGreaterThanZero() = runTest {
        viewModel.startTest()
        advanceTimeBy(3400)
        // correct round
        viewModel.sequence.value.forEach { viewModel.onCellPressed(it) }
        advanceTimeBy(1100) // now in SHOWING for round 2 (length=4)
        // delay(600) + 4×(600+300) = 4200ms to reach INPUT
        advanceTimeBy(4200)
        val seq2 = viewModel.sequence.value
        val wrongCell = (0..8).first { it != seq2[0] }
        viewModel.onCellPressed(wrongCell) // triggers wrong feedback
        // maxLength was set to 3 after the first correct round
        assertTrue(viewModel.precisionPercent() > 0)
    }
}
