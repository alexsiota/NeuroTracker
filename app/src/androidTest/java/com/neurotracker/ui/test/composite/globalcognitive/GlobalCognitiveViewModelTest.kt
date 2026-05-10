package com.neurotracker.ui.test.composite.globalcognitive

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.composite.globalCognitive.GlobalCognitiveViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [GlobalCognitiveViewModel].
 *
 * Cubre:
 *  - Estado inicial (IDLE, todos los scores a cero).
 *  - Constantes del test (totalPhases, vmTotalRounds).
 *  - precisionPercent antes de iniciar devuelve 0.
 *  - resetTest restaura el estado a IDLE.
 *  - Guard conditions: acciones de cada fase se ignoran cuando el estado es IDLE.
 *  - startTest avanza al estado TRANSITION (primera transición antes del primer delay).
 *
 * Nota: los bucles de fase usan System.currentTimeMillis() para la duración real,
 * por lo que las pruebas se detienen en el primer delay de transición y no avanzan
 * a las fases para evitar loops indefinidos con tiempo virtual.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GlobalCognitiveViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: GlobalCognitiveViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = GlobalCognitiveViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun initialState_isIdleWithZeroScores() {
        assertEquals(GlobalCognitiveViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.reactionScore.value)
        assertEquals(0, viewModel.stroopScore.value)
        assertEquals(0, viewModel.nbackScore.value)
        assertEquals(0, viewModel.visualScore.value)
        assertEquals(0, viewModel.globalScore.value)
    }

    @Test
    fun initialState_countersAreZero() {
        assertEquals(0, viewModel.currentPhase.value)
        assertEquals(0, viewModel.reactionHits.value)
        assertEquals(0, viewModel.reactionErrors.value)
        assertEquals(0, viewModel.stroopHits.value)
        assertEquals(0, viewModel.stroopErrors.value)
        assertEquals(0, viewModel.nbackHits.value)
        assertEquals(0, viewModel.nbackErrors.value)
        assertEquals(0, viewModel.vmHits.value)
        assertEquals(0, viewModel.vmErrors.value)
    }

    @Test
    fun initialState_stimuliAreNull() {
        assertNull(viewModel.reactionStimulus.value)
        assertNull(viewModel.stroopItem.value)
        assertNull(viewModel.nbackSymbol.value)
    }

    @Test
    fun totalPhases_isFour() {
        assertEquals(4, viewModel.totalPhases)
    }

    @Test
    fun vmTotalRounds_isFive() {
        assertEquals(5, viewModel.vmTotalRounds)
    }

    @Test
    fun precisionPercent_beforeStart_returnsZero() {
        assertEquals(0, viewModel.precisionPercent())
    }

    @Test
    fun resetTest_setsStateToIdle() {
        viewModel.resetTest()
        assertEquals(GlobalCognitiveViewModel.TestState.IDLE, viewModel.testState.value)
    }

    @Test
    fun onReactionPress_whenIdle_doesNotIncrementHits() {
        viewModel.onReactionPress()
        assertEquals(0, viewModel.reactionHits.value)
        assertEquals(0, viewModel.reactionErrors.value)
    }

    @Test
    fun onStroopAnswer_whenIdle_doesNotIncrementHits() {
        viewModel.onStroopAnswer(true)
        viewModel.onStroopAnswer(false)
        assertEquals(0, viewModel.stroopHits.value)
        assertEquals(0, viewModel.stroopErrors.value)
    }

    @Test
    fun onNBackYes_whenIdle_doesNotIncrementHits() {
        viewModel.onNBackYes()
        assertEquals(0, viewModel.nbackHits.value)
        assertEquals(0, viewModel.nbackErrors.value)
    }

    @Test
    fun onNBackNo_whenIdle_doesNotIncrementHits() {
        viewModel.onNBackNo()
        assertEquals(0, viewModel.nbackHits.value)
        assertEquals(0, viewModel.nbackErrors.value)
    }

    @Test
    fun onVmCellPressed_whenIdle_doesNotAddInput() {
        viewModel.onVmCellPressed(3)
        assertTrue(viewModel.vmUserInput.value.isEmpty())
    }

    @Test
    fun startTest_advancesToTransitionState() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        assertEquals(GlobalCognitiveViewModel.TestState.TRANSITION, viewModel.testState.value)
    }

    @Test
    fun startTest_resetsAllScores() {
        viewModel.startTest()
        assertEquals(0, viewModel.reactionScore.value)
        assertEquals(0, viewModel.stroopScore.value)
        assertEquals(0, viewModel.nbackScore.value)
        assertEquals(0, viewModel.visualScore.value)
    }

    @Test
    fun startTest_setsCurrentPhaseToOne() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.currentPhase.value)
    }

    @Test
    fun startTest_setsTransitionMessageForPhase1() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.transitionMessage.value.contains("Fase 1"))
    }

    @Test
    fun startTest_setsPhaseDescriptionNotBlank() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.phaseDescription.value.isNotBlank())
    }

    @Test
    fun reactionSec_inPhaseReaction_isThirty() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        assertEquals(30, viewModel.reactionSec.value)
    }

    @Test
    fun reactionStimulus_inPhaseReaction_isNotNull() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        assertNotNull(viewModel.reactionStimulus.value)
    }

    @Test
    fun stroopAnswered_inPhaseReaction_isFalse() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        assertFalse(viewModel.stroopAnswered.value)
    }

    @Test
    fun nbackAnswered_inPhaseReaction_isFalse() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        assertFalse(viewModel.nbackAnswered.value)
    }

    @Test
    fun vmHighlight_inPhaseReaction_isNegativeOne() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        assertEquals(-1, viewModel.vmHighlight.value)
    }

    @Test
    fun vmPhase_inPhaseReaction_isShowing() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        assertEquals("showing", viewModel.vmPhase.value)
    }

    @Test
    fun onVmCellPressed_inPhaseReaction_doesNotAddInput() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        viewModel.onVmCellPressed(2)
        assertTrue(viewModel.vmUserInput.value.isEmpty())
    }

    @Test
    fun onNBackNo_inPhaseReaction_doesNotIncrementNback() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        viewModel.onNBackNo()
        assertEquals(0, viewModel.nbackHits.value)
        assertEquals(0, viewModel.nbackErrors.value)
    }

    @Test
    fun stroopLastCorrect_inPhaseReaction_isNull() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        assertNull(viewModel.stroopLastCorrect.value)
    }

    @Test
    fun nbackIndex_inPhaseReaction_isZero() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        assertEquals(0, viewModel.nbackIndex.value)
    }

    @Test
    fun startTest_secondCall_resetsPhaseToOne() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        viewModel.resetTest()
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.currentPhase.value)
    }

    @Test
    fun afterFirstTransitionDelay_stateIsPhaseReaction() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        assertEquals(GlobalCognitiveViewModel.TestState.PHASE_REACTION, viewModel.testState.value)
    }

    @Test
    fun onReactionPress_inPhaseReaction_registersExactlyOneResponse() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        assertEquals(GlobalCognitiveViewModel.TestState.PHASE_REACTION, viewModel.testState.value)
        viewModel.onReactionPress()
        assertEquals(1, viewModel.reactionHits.value + viewModel.reactionErrors.value)
    }

    @Test
    fun onReactionPress_doublePress_isIgnored() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        viewModel.onReactionPress()
        val totalAfterFirst = viewModel.reactionHits.value + viewModel.reactionErrors.value
        viewModel.onReactionPress()
        assertEquals(totalAfterFirst, viewModel.reactionHits.value + viewModel.reactionErrors.value)
    }

    @Test
    fun onStroopAnswer_whenInPhaseReaction_doesNotIncrementStroop() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        viewModel.onStroopAnswer(true)
        assertEquals(0, viewModel.stroopHits.value)
        assertEquals(0, viewModel.stroopErrors.value)
    }

    @Test
    fun onNBackYes_whenInPhaseReaction_doesNotIncrementNback() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        viewModel.onNBackYes()
        assertEquals(0, viewModel.nbackHits.value)
        assertEquals(0, viewModel.nbackErrors.value)
    }

    @Test
    fun resetTest_fromPhaseReaction_returnsToIdle() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(4001L)
        viewModel.resetTest()
        assertEquals(GlobalCognitiveViewModel.TestState.IDLE, viewModel.testState.value)
    }
}
