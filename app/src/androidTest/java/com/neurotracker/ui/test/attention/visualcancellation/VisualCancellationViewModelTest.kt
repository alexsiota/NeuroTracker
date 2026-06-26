package com.neurotracker.ui.test.attention.visualcancellation

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.attention.visualCancellation.VisualCancellationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [VisualCancellationViewModel].
 *
 * Verifica la generación de la cuadrícula, el sistema de puntuación y la precisión.
 * Precisión: hits / (hits + errors) × 100.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class VisualCancellationViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: VisualCancellationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = VisualCancellationViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE y contadores a cero.
     */
    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(VisualCancellationViewModel.TestState.IDLE, viewModel.testState.value)
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
     * Verifica que [startTest] genera la cuadrícula y cambia el estado a RUNNING.
     */
    @Test
    fun startTest_generatesGridAndChangesStateToRunning() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        assertEquals(VisualCancellationViewModel.TestState.RUNNING, viewModel.testState.value)
        assertEquals(36, viewModel.gridItems.value.size)
    }

    /**
     * Verifica que la cuadrícula generada contiene exactamente 10 círculos objetivo.
     */
    @Test
    fun startTest_gridContains10Circles() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        val circles = viewModel.gridItems.value.count { it.shape == VisualCancellationViewModel.ShapeType.CIRCLE }
        assertEquals(10, circles)
    }

    /**
     * Verifica que pulsar un círculo incrementa los aciertos.
     */
    @Test
    fun onItemClicked_circle_incrementsHits() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        val circleId = viewModel.gridItems.value.first { it.shape == VisualCancellationViewModel.ShapeType.CIRCLE }.id
        viewModel.onItemClicked(circleId)
        assertEquals(1, viewModel.hits.value)
    }

    /**
     * Verifica que pulsar un cuadrado incrementa los errores.
     */
    @Test
    fun onItemClicked_square_incrementsErrors() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        val squareId = viewModel.gridItems.value.firstOrNull { it.shape == VisualCancellationViewModel.ShapeType.SQUARE }?.id
        if (squareId != null) {
            viewModel.onItemClicked(squareId)
            assertEquals(1, viewModel.errors.value)
        }
    }

    /**
     * Verifica que la precisión está en rango válido tras pulsar elementos.
     */
    @Test
    fun precisionPercent_afterInteraction_isInValidRange() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        val circleId = viewModel.gridItems.value.first { it.shape == VisualCancellationViewModel.ShapeType.CIRCLE }.id
        viewModel.onItemClicked(circleId)
        assertTrue(viewModel.precisionPercent() in 0..100)
    }

    /**
     * Verifica que [resetTest] restaura el estado inicial.
     */
    @Test
    fun resetTest_restoresInitialState() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        viewModel.resetTest()
        assertEquals(VisualCancellationViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
        assertTrue(viewModel.gridItems.value.isEmpty())
    }
}
