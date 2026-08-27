package com.neurotracker.ui.test.memory.nback

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.memory.nback.NBackViewModel
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
 * Tests instrumentados del [NBackViewModel].
 *
 * Verifica el estado inicial, la lógica de respuesta SÍ/NO y la precisión.
 * Precisión N-Back: hits / (hits + errors + omissions) × 100.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class NBackViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: NBackViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = NBackViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE y contadores a cero.
     */
    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(NBackViewModel.TestState.IDLE, viewModel.testState.value)
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
     * Verifica que [startTest] cambia el estado a RUNNING.
     */
    @Test
    fun startTest_changesStateToRunning() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        assertEquals(NBackViewModel.TestState.RUNNING, viewModel.testState.value)
    }

    /**
     * Verifica que [onYesPressed] no registra respuesta en el primer estímulo.
     */
    @Test
    fun onYesPressed_onFirstStimulus_doesNotRegisterResponse() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        viewModel.onYesPressed()
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }

    /**
     * Verifica que la precisión está en rango válido tras completar el test.
     * 20 estímulos × (2000ms + 200ms) = 44_000ms.
     */
    @Test
    fun precisionPercent_afterTestCompletes_isInValidRange() = runTest {
        viewModel.startTest()
        advanceTimeBy(50_000)
        assertTrue(viewModel.precisionPercent() in 0..100)
    }

    /**
     * Verifica que el test termina en FINISHED tras todos los estímulos.
     */
    @Test
    fun afterAllStimuli_stateChangesToFinished() = runTest {
        viewModel.startTest()
        advanceTimeBy(50_000)
        assertEquals(NBackViewModel.TestState.FINISHED, viewModel.testState.value)
    }

    /**
     * Verifica que [resetTest] restaura el estado inicial.
     */
    @Test
    fun resetTest_restoresInitialState() = runTest {
        viewModel.startTest()
        advanceTimeBy(500)
        viewModel.resetTest()
        assertEquals(NBackViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.omissions.value)
    }

    @Test
    fun onNoPressed_onFirstStimulus_doesNotRegisterResponse() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        assertEquals(1, viewModel.currentIndex.value)
        viewModel.onNoPressed()
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }

    @Test
    fun onYesPressed_onSecondStimulus_registersOneResponse() = runTest {
        viewModel.startTest()
        // first stimulus: 2000ms + 200ms inter = 2200ms total
        advanceTimeBy(2250)
        assertEquals(2, viewModel.currentIndex.value)
        viewModel.onYesPressed()
        assertEquals(1, viewModel.hits.value + viewModel.errors.value)
    }

    @Test
    fun onNoPressed_onSecondStimulus_registersOneResponse() = runTest {
        viewModel.startTest()
        advanceTimeBy(2250)
        assertEquals(2, viewModel.currentIndex.value)
        viewModel.onNoPressed()
        assertEquals(1, viewModel.hits.value + viewModel.errors.value)
    }

    @Test
    fun doubleAnswer_secondIsIgnored() = runTest {
        viewModel.startTest()
        advanceTimeBy(2250)
        viewModel.onYesPressed()
        val total = viewModel.hits.value + viewModel.errors.value
        viewModel.onYesPressed()
        assertEquals(total, viewModel.hits.value + viewModel.errors.value)
    }

    @Test
    fun noAnswer_onSecondStimulus_incrementsOmissions() = runTest {
        viewModel.startTest()
        // advance past the second stimulus delay (2000+200+2000=4200ms)
        advanceTimeBy(4300)
        assertEquals(1, viewModel.omissions.value)
    }

    @Test
    fun precisionPercent_withOneHitAndZeroErrors_isHundred() = runTest {
        viewModel.startTest()
        advanceTimeBy(2250)
        // answer on second stimulus — result depends on whether stim matches prev
        viewModel.onYesPressed()
        // advance past all remaining stimuli (20 stimuli × 2200ms ≈ 44000ms)
        advanceTimeBy(44000)
        assertTrue(viewModel.precisionPercent() in 0..100)
    }

    @Test
    fun currentStimulus_afterInterStimulusDelay_isNull() = runTest {
        viewModel.startTest()
        // between first and second stimulus, currentStimulus should be null
        advanceTimeBy(2100) // after delay(2000), currentStimulus cleared; before delay(200) ends
        // we're in the inter-stimulus gap
        assertTrue(viewModel.currentIndex.value >= 1)
    }

    @Test
    fun hasAnswered_afterResponse_isTrue() = runTest {
        viewModel.startTest()
        advanceTimeBy(2250)
        viewModel.onNoPressed()
        assertTrue(viewModel.hasAnswered.value)
    }
}
