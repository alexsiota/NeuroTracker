package com.neurotracker.ui.test.attention.selective

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.attention.selectiveAttention.SelectiveAttentionViewModel
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
 * Tests instrumentados del [SelectiveAttentionViewModel].
 *
 * Verifica el flujo del test de atención selectiva: generación de estímulos
 * en cuadrícula 2×2, respuesta del usuario y cálculo de precisión.
 * Precisión: hits / (hits + omissions) × 100.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SelectiveAttentionViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SelectiveAttentionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SelectiveAttentionViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE y contadores a cero.
     */
    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(SelectiveAttentionViewModel.TestState.IDLE, viewModel.testState.value)
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
     * Verifica que [startTest] cambia el estado a RUNNING y genera 4 estímulos.
     */
    @Test
    fun startTest_changesStateToRunning() = runTest {
        viewModel.startTest()
        advanceTimeBy(200)
        assertEquals(SelectiveAttentionViewModel.TestState.RUNNING, viewModel.testState.value)
        assertEquals(4, viewModel.stimuli.value.size)
    }

    /**
     * Verifica que la cuadrícula generada contiene exactamente 1 estímulo objetivo.
     */
    @Test
    fun startTest_gridContainsExactlyOneTarget() = runTest {
        viewModel.startTest()
        advanceTimeBy(200)
        val targets = viewModel.stimuli.value.count { it.isTarget }
        assertEquals(1, targets)
    }

    /**
     * Verifica que pulsar el estímulo objetivo incrementa los aciertos.
     */
    @Test
    fun onStimulusPressed_target_incrementsHits() = runTest {
        viewModel.startTest()
        advanceTimeBy(200)
        val target = viewModel.stimuli.value.first { it.isTarget }
        viewModel.onStimulusPressed(target)
        assertEquals(1, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }

    /**
     * Verifica que pulsar un estímulo distractor incrementa los errores.
     */
    @Test
    fun onStimulusPressed_distractor_incrementsErrors() = runTest {
        viewModel.startTest()
        advanceTimeBy(200)
        val distractor = viewModel.stimuli.value.first { !it.isTarget }
        viewModel.onStimulusPressed(distractor)
        assertEquals(0, viewModel.hits.value)
        assertEquals(1, viewModel.errors.value)
    }

    /**
     * Verifica que [onStimulusPressed] no registra una segunda respuesta en la misma ronda.
     */
    @Test
    fun onStimulusPressed_twice_onlyCountsFirst() = runTest {
        viewModel.startTest()
        advanceTimeBy(200)
        val target     = viewModel.stimuli.value.first { it.isTarget }
        val distractor = viewModel.stimuli.value.first { !it.isTarget }
        viewModel.onStimulusPressed(target)
        viewModel.onStimulusPressed(distractor)
        assertEquals(1, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }

    /**
     * Verifica que la precisión está en rango válido tras completar el test.
     * 20 rondas × (2500ms estímulo + 300ms pausa) = 56_000ms.
     */
    @Test
    fun precisionPercent_afterTestCompletes_isInValidRange() = runTest {
        viewModel.startTest()
        advanceTimeBy(60_000)
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
        assertEquals(SelectiveAttentionViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
        assertEquals(0, viewModel.omissions.value)
        assertTrue(viewModel.stimuli.value.isEmpty())
    }
}
