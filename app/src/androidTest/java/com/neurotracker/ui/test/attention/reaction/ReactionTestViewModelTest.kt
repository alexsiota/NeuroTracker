package com.neurotracker.attention.reaction

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.attention.reaction.ReactionTestViewModel
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
 * Tests instrumentados del [ReactionTestViewModel].
 *
 * Verifica los estados del test, el sistema de puntuación y el cálculo de precisión.
 * La precisión de reacción simple usa: hits / (hits + omissions) × 100.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ReactionTestViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ReactionTestViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ReactionTestViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica que el estado inicial es IDLE con contadores a cero.
     */
    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(ReactionTestViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
        assertEquals(0, viewModel.omissions.value)
    }

    /**
     * Verifica que [ReactionTestViewModel.precisionPercent] devuelve 0 sin intentos.
     */
    @Test
    fun precisionPercent_withNoAttempts_returnsZero() {
        assertEquals(0, viewModel.precisionPercent())
    }

    /**
     * Verifica que [ReactionTestViewModel.startTest] cambia el estado a WAITING.
     */
    @Test
    fun startTest_changesStateToWaiting() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        assertEquals(ReactionTestViewModel.TestState.WAITING, viewModel.testState.value)
    }

    /**
     * Verifica que [ReactionTestViewModel.resetTest] restaura el estado inicial.
     */
    @Test
    fun resetTest_restoresInitialState() = runTest {
        viewModel.startTest()
        advanceTimeBy(500)
        viewModel.resetTest()
        assertEquals(ReactionTestViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
        assertEquals(0, viewModel.omissions.value)
    }

    /**
     * Verifica que la precisión está en rango válido tras completar el test.
     */
    @Test
    fun precisionPercent_afterTestCompletes_isInValidRange() = runTest {
        viewModel.startTest()
        // 15 rondas × (2500ms espera max + 1200ms estímulo + 400ms pausa) ≈ 60_000ms
        advanceTimeBy(65_000)
        val precision = viewModel.precisionPercent()
        assertTrue("Precisión debe estar en 0–100", precision in 0..100)
    }

    /**
     * Verifica que el estado cambia a FINISHED tras completar todas las rondas.
     */
    @Test
    fun afterAllRounds_stateChangesToFinished() = runTest {
        viewModel.startTest()
        advanceTimeBy(65_000)
        assertEquals(ReactionTestViewModel.TestState.FINISHED, viewModel.testState.value)
    }

    /**
     * Verifica que la ronda actual incrementa durante el test.
     */
    @Test
    fun currentRound_incrementsDuringTest() = runTest {
        viewModel.startTest()
        advanceTimeBy(4000)
        assertTrue(viewModel.currentRound.value > 0)
    }
}
