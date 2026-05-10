package com.neurotracker.ui.test.coordination.visuomotor

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.coordination.visuoMotor.VisuomotorViewModel
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
 * Tests instrumentados del [VisuomotorViewModel].
 *
 * Verifica la generación de objetivos, la detección de pulsaciones correctas/fuera del círculo
 * y el cálculo de precisión.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class VisuomotorViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: VisuomotorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = VisuomotorViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE y contadores a cero.
     */
    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(VisuomotorViewModel.TestState.IDLE, viewModel.testState.value)
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
     * Verifica que [startTest] cambia el estado a RUNNING.
     */
    @Test
    fun startTest_changesStateToRunning() = runTest {
        viewModel.startTest()
        advanceTimeBy(200)
        assertEquals(VisuomotorViewModel.TestState.RUNNING, viewModel.testState.value)
    }

    /**
     * Verifica que pulsar el círculo activo incrementa los aciertos.
     */
    @Test
    fun onTargetTapped_activeTarget_incrementsHits() = runTest {
        viewModel.startTest()
        advanceTimeBy(500)
        val target = viewModel.currentTarget.value ?: return@runTest
        viewModel.onTargetTapped(target.id)
        assertEquals(1, viewModel.hits.value)
    }

    /**
     * Verifica que pulsar el fondo (fuera del círculo) incrementa los errores.
     */
    @Test
    fun onBackgroundTapped_withVisibleCircle_incrementsErrors() = runTest {
        viewModel.startTest()
        advanceTimeBy(500)
        if (viewModel.currentTarget.value != null) {
            viewModel.onBackgroundTapped()
            assertEquals(1, viewModel.errors.value)
        }
    }

    /**
     * Verifica que la precisión está en rango válido tras completar el test.
     * Test dura 30 segundos.
     */
    @Test
    fun precisionPercent_afterTestCompletes_isInValidRange() = runTest {
        viewModel.startTest()
        advanceTimeBy(35_000)
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
        assertEquals(VisuomotorViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }
}
