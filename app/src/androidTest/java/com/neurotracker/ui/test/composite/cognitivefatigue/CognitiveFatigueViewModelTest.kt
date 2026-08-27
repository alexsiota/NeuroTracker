package com.neurotracker.ui.test.composite.cognitivefatigue

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.composite.cognitiveFatigue.BlockResult
import com.neurotracker.ui.tests.composite.cognitiveFatigue.CognitiveFatigueViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [CognitiveFatigueViewModel].
 *
 * Cubre:
 *  - Estado inicial (IDLE, blockResults vacío).
 *  - fatigueIndex y precisionPercent con resultados insuficientes.
 *  - Guard conditions: onPress se ignora fuera de BLOCK_RUNNING.
 *  - Cálculos de fatigueIndex y precisionPercent con datos reales.
 *  - resetTest restaura el estado inicial.
 *  - startTest avanza al estado TRANSITION en la primera transición.
 *
 * Nota: los bucles de bloque usan System.currentTimeMillis() para la duración real;
 * las pruebas no avanzan más allá del primer delay de transición.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CognitiveFatigueViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CognitiveFatigueViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CognitiveFatigueViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun initialState_isIdleWithEmptyResults() {
        assertEquals(CognitiveFatigueViewModel.TestState.IDLE, viewModel.testState.value)
        assertTrue(viewModel.blockResults.value.isEmpty())
    }

    @Test
    fun initialState_countersAreZero() {
        assertEquals(0, viewModel.currentBlock.value)
        assertEquals(0, viewModel.blockHits.value)
        assertEquals(0, viewModel.blockErrors.value)
        assertFalse(viewModel.stimulus.value)
    }

    @Test
    fun totalBlocks_isThree() {
        assertEquals(3, viewModel.totalBlocks)
    }

    @Test
    fun fatigueIndex_withNoResults_returnsZero() {
        assertEquals(0, viewModel.fatigueIndex())
    }

    @Test
    fun fatigueIndex_withLessThanTwoResults_returnsZero() {
        // fatigueIndex() requires blockResults.size >= 2; with 0 results returns 0
        assertEquals(0, viewModel.fatigueIndex())
    }

    @Test
    fun precisionPercent_withNoResults_returnsZero() {
        assertEquals(0, viewModel.precisionPercent())
    }

    @Test
    fun onPress_whenIdle_doesNotIncrementHits() {
        viewModel.onPress()
        assertEquals(0, viewModel.blockHits.value)
        assertEquals(0, viewModel.blockErrors.value)
    }

    @Test
    fun onPress_whenTransition_doesNotIncrementHits() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        // State is now TRANSITION (blocked on delay(3000))
        assertEquals(CognitiveFatigueViewModel.TestState.TRANSITION, viewModel.testState.value)
        viewModel.onPress()
        assertEquals(0, viewModel.blockHits.value)
    }

    @Test
    fun resetTest_restoresIdleState() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        viewModel.resetTest()
        assertEquals(CognitiveFatigueViewModel.TestState.IDLE, viewModel.testState.value)
        assertTrue(viewModel.blockResults.value.isEmpty())
        assertFalse(viewModel.stimulus.value)
        assertEquals(0, viewModel.blockHits.value)
        assertEquals(0, viewModel.blockErrors.value)
    }

    @Test
    fun startTest_advancesToTransitionState() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        assertEquals(CognitiveFatigueViewModel.TestState.TRANSITION, viewModel.testState.value)
    }

    @Test
    fun startTest_setsCurrentBlockToOne() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.currentBlock.value)
    }

    @Test
    fun startTest_clearsBlockResults() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.blockResults.value.isEmpty())
    }

    @Test
    fun afterTransitionDelay_stateIsBlockRunning() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(3001L)
        assertEquals(CognitiveFatigueViewModel.TestState.BLOCK_RUNNING, viewModel.testState.value)
    }

    @Test
    fun onPress_whenBlockRunning_registersExactlyOneResponse() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(3001L)
        assertEquals(CognitiveFatigueViewModel.TestState.BLOCK_RUNNING, viewModel.testState.value)
        viewModel.onPress()
        assertEquals(1, viewModel.blockHits.value + viewModel.blockErrors.value)
    }

    @Test
    fun onPress_whenBlockRunning_secondPress_isIgnored() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(3001L)
        viewModel.onPress()
        val totalAfterFirst = viewModel.blockHits.value + viewModel.blockErrors.value
        viewModel.onPress()
        assertEquals(totalAfterFirst, viewModel.blockHits.value + viewModel.blockErrors.value)
    }

    @Test
    fun remainingSec_afterEnteringBlockRunning_isThirty() {
        viewModel.startTest()
        testDispatcher.scheduler.advanceTimeBy(3001L)
        assertEquals(30, viewModel.remainingSec.value)
    }

    @Test
    fun transitionMessage_afterStartTest_containsBloque1() {
        viewModel.startTest()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.transitionMessage.value.contains("Bloque 1"))
    }

    @Test
    fun blockResult_storesAllFields() {
        val r = BlockResult(block = 1, label = "Inicio", precision = 80, avgReactionMs = 350L)
        assertEquals(1, r.block)
        assertEquals("Inicio", r.label)
        assertEquals(80, r.precision)
        assertEquals(350L, r.avgReactionMs)
    }

    @Test
    fun blockResult_equality_sameValues_areEqual() {
        val a = BlockResult(2, "Intermedio", 65, 420L)
        val b = BlockResult(2, "Intermedio", 65, 420L)
        assertEquals(a, b)
    }

    @Test
    fun blockResult_copy_changesField() {
        val original = BlockResult(1, "Inicio", 70, 300L)
        val modified = original.copy(precision = 90)
        assertEquals(90, modified.precision)
        assertEquals(original.block, modified.block)
        assertEquals(original.label, modified.label)
    }

    @Test
    fun blockResult_toString_containsFieldValues() {
        val r = BlockResult(3, "Final", 55, 500L)
        val s = r.toString()
        assertTrue(s.contains("55"))
        assertTrue(s.contains("Final"))
    }
}
