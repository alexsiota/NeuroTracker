package com.neurotracker.ui.test.composite.dualtask

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.composite.dualTask.DualTaskViewModel
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
 * Tests instrumentados del [DualTaskViewModel].
 *
 * Cubre:
 *  - Estado inicial (IDLE, contadores a cero).
 *  - Funciones de precisión sin intentos registrados.
 *  - Guard conditions: las acciones del usuario no tienen efecto fuera de RUNNING.
 *  - startTest establece el estado a RUNNING de forma síncrona.
 *  - resetTest restaura el estado inicial.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DualTaskViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: DualTaskViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DualTaskViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(DualTaskViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.reactionHits.value)
        assertEquals(0, viewModel.reactionErrors.value)
        assertEquals(0, viewModel.nbackHits.value)
        assertEquals(0, viewModel.nbackErrors.value)
        assertEquals(0, viewModel.nbackIndex.value)
        assertEquals(60, viewModel.remainingSec.value)
    }

    @Test
    fun initialState_stimuliAreNull() {
        assertFalse(viewModel.reactionTarget.value)
        assertNull(viewModel.nbackSymbol.value)
    }

    @Test
    fun reactionPrecision_noAttempts_returnsZero() {
        assertEquals(0, viewModel.reactionPrecision())
    }

    @Test
    fun nbackPrecision_noAttempts_returnsZero() {
        assertEquals(0, viewModel.nbackPrecision())
    }

    @Test
    fun globalScore_noAttempts_returnsZero() {
        assertEquals(0, viewModel.globalScore())
    }

    @Test
    fun precisionPercent_noAttempts_returnsZero() {
        assertEquals(0, viewModel.precisionPercent())
    }

    @Test
    fun onReactionPress_whenIdle_doesNotIncrementHits() {
        viewModel.onReactionPress()
        assertEquals(0, viewModel.reactionHits.value)
        assertEquals(0, viewModel.reactionErrors.value)
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
    fun startTest_setsStateToRunningImmediately() {
        viewModel.startTest()
        assertEquals(DualTaskViewModel.TestState.RUNNING, viewModel.testState.value)
    }

    @Test
    fun startTest_resetsAllCounters() {
        viewModel.startTest()
        assertEquals(0, viewModel.reactionHits.value)
        assertEquals(0, viewModel.reactionErrors.value)
        assertEquals(0, viewModel.nbackHits.value)
        assertEquals(0, viewModel.nbackErrors.value)
        assertEquals(0, viewModel.nbackIndex.value)
        assertEquals(60, viewModel.remainingSec.value)
    }

    @Test
    fun resetTest_restoresIdleState() {
        viewModel.startTest()
        viewModel.resetTest()
        assertEquals(DualTaskViewModel.TestState.IDLE, viewModel.testState.value)
        assertFalse(viewModel.reactionTarget.value)
        assertNull(viewModel.nbackSymbol.value)
    }

    @Test
    fun precisionPercent_aliasMatchesGlobalScore() {
        assertEquals(viewModel.globalScore(), viewModel.precisionPercent())
    }

    @Test
    fun afterRunCurrent_nbackIndex_isOne() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.nbackIndex.value)
    }

    @Test
    fun afterRunCurrent_nbackSymbol_isNotNull() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        assertNotNull(viewModel.nbackSymbol.value)
    }

    @Test
    fun afterRunCurrent_nbackAnswered_isFalse() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.nbackAnswered.value)
    }

    @Test
    fun onReactionPress_whenRunning_registersExactlyOneResponse() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        viewModel.onReactionPress()
        assertEquals(1, viewModel.reactionHits.value + viewModel.reactionErrors.value)
    }

    @Test
    fun onReactionPress_whenRunning_doublePress_isIgnored() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        viewModel.onReactionPress()
        val total = viewModel.reactionHits.value + viewModel.reactionErrors.value
        viewModel.onReactionPress()
        assertEquals(total, viewModel.reactionHits.value + viewModel.reactionErrors.value)
    }

    @Test
    fun onNBackYes_onFirstStimulus_whenRunning_doesNotRegisterResponse() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.nbackIndex.value)
        viewModel.onNBackYes()
        assertEquals(0, viewModel.nbackHits.value)
        assertEquals(0, viewModel.nbackErrors.value)
    }

    @Test
    fun onNBackNo_onFirstStimulus_whenRunning_doesNotRegisterResponse() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.nbackIndex.value)
        viewModel.onNBackNo()
        assertEquals(0, viewModel.nbackHits.value)
        assertEquals(0, viewModel.nbackErrors.value)
    }

    @Test
    fun onNBackYes_onSecondStimulus_registersOneResponse() {
        viewModel.startTest()
        // advance past first stimulus (2000ms delay + 300ms inter) so index reaches 2
        testDispatcher.scheduler.advanceTimeBy(2301L)
        assertEquals(2, viewModel.nbackIndex.value)
        viewModel.onNBackYes()
        assertEquals(1, viewModel.nbackHits.value + viewModel.nbackErrors.value)
    }

    @Test
    fun onNBackNo_onSecondStimulus_registersOneResponse() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(2301L)
        assertEquals(2, viewModel.nbackIndex.value)
        viewModel.onNBackNo()
        assertEquals(1, viewModel.nbackHits.value + viewModel.nbackErrors.value)
    }

    @Test
    fun onNBackYes_doubleAnswer_secondIsIgnored() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(2301L)
        viewModel.onNBackYes()
        val total = viewModel.nbackHits.value + viewModel.nbackErrors.value
        viewModel.onNBackYes()
        assertEquals(total, viewModel.nbackHits.value + viewModel.nbackErrors.value)
    }

    @Test
    fun reactionPrecision_afterOneResponse_isZeroOrHundred() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        viewModel.onReactionPress()
        val p = viewModel.reactionPrecision()
        assertTrue(p == 0 || p == 100)
    }

    @Test
    fun globalScore_afterOneReactionResponse_isInValidRange() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        viewModel.onReactionPress()
        assertTrue(viewModel.globalScore() in 0..100)
    }

    @Test
    fun remainingSec_afterRunCurrent_isSixty() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        assertEquals(60, viewModel.remainingSec.value)
    }
}
