package com.neurotracker.ui.test.attention.sustained

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.attention.sustainedAttention.SustainedAttentionViewModel
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
 * Tests instrumentados del [SustainedAttentionViewModel].
 *
 * Verifica los estados, contadores y precisión del test de atención sostenida.
 * Precisión: hits / (hits + omissions) × 100.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SustainedAttentionViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SustainedAttentionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SustainedAttentionViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE y contadores a cero.
     */
    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(SustainedAttentionViewModel.TestState.IDLE, viewModel.testState.value)
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
     * Verifica que [SustainedAttentionViewModel.startTest] cambia el estado a RUNNING.
     */
    @Test
    fun startTest_changesStateToRunning() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        assertEquals(SustainedAttentionViewModel.TestState.RUNNING, viewModel.testState.value)
    }

    /**
     * Verifica que [SustainedAttentionViewModel.resetTest] restaura el estado inicial.
     */
    @Test
    fun resetTest_restoresInitialState() = runTest {
        viewModel.startTest()
        advanceTimeBy(500)
        viewModel.resetTest()
        assertEquals(SustainedAttentionViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
    }

    /**
     * Verifica que la precisión está en rango válido tras completar el test.
     * 30 rondas × (1000ms estímulo + 600ms pausa) = 48_000ms.
     */
    @Test
    fun precisionPercent_afterTestCompletes_isInValidRange() = runTest {
        viewModel.startTest()
        advanceTimeBy(55_000)
        assertTrue(viewModel.precisionPercent() in 0..100)
    }

    /**
     * Verifica que el estado cambia a FINISHED tras completar todas las rondas.
     */
    @Test
    fun afterAllRounds_stateChangesToFinished() = runTest {
        viewModel.startTest()
        advanceTimeBy(55_000)
        assertEquals(SustainedAttentionViewModel.TestState.FINISHED, viewModel.testState.value)
    }
}
