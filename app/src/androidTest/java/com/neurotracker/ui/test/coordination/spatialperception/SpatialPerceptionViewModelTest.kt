package com.neurotracker.coordination.spatialperception

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.coordination.spatialPerception.SpatialPerceptionViewModel
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
 * Tests instrumentados del [SpatialPerceptionViewModel].
 *
 * Verifica la generación de estímulos, la selección de opciones y el cálculo de precisión.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SpatialPerceptionViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SpatialPerceptionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SpatialPerceptionViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE y contadores a cero.
     */
    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(SpatialPerceptionViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }

    /**
     * Verifica que [startTest] cambia el estado a RUNNING y genera un estímulo con 4 opciones.
     */
    @Test
    fun startTest_changesStateToRunning() = runTest {
        viewModel.startTest()
        advanceTimeBy(200)
        assertEquals(SpatialPerceptionViewModel.TestState.RUNNING, viewModel.testState.value)
        assertEquals(4, viewModel.currentItem.value?.options?.size)
    }

    /**
     * Verifica que seleccionar la opción correcta incrementa los aciertos.
     */
    @Test
    fun onOptionSelected_correctIndex_incrementsHits() = runTest {
        viewModel.startTest()
        advanceTimeBy(200)
        val correct = viewModel.currentItem.value?.correctIndex ?: return@runTest
        viewModel.onOptionSelected(correct)
        assertEquals(1, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }

    /**
     * Verifica que seleccionar una opción incorrecta incrementa los errores.
     */
    @Test
    fun onOptionSelected_wrongIndex_incrementsErrors() = runTest {
        viewModel.startTest()
        advanceTimeBy(200)
        val correct = viewModel.currentItem.value?.correctIndex ?: return@runTest
        val wrong   = (0..3).first { it != correct }
        viewModel.onOptionSelected(wrong)
        assertEquals(0, viewModel.hits.value)
        assertEquals(1, viewModel.errors.value)
    }

    /**
     * Verifica que la precisión está en rango válido tras completar el test.
     * 12 rondas × 6000ms = 72_000ms.
     */
    @Test
    fun precisionPercent_afterTestCompletes_isInValidRange() = runTest {
        viewModel.startTest()
        advanceTimeBy(80_000)
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
        assertEquals(SpatialPerceptionViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
    }
}
