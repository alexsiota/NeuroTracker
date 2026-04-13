package com.neurotracker.memory.visualmemory

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.memory.visualMemory.VisualMemoryViewModel
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
 * Tests instrumentados del [VisualMemoryViewModel].
 *
 * Verifica el flujo de memorización/reproducción de secuencias de celdas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class VisualMemoryViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: VisualMemoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = VisualMemoryViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE, longitud 3, maxLength 0.
     */
    @Test
    fun initialState_isIdleWithLength3() {
        assertEquals(VisualMemoryViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(3, viewModel.currentLength.value)
        assertEquals(0, viewModel.maxLength.value)
    }

    /**
     * Verifica que la precisión es 0 antes de iniciar.
     */
    @Test
    fun precisionPercent_beforeStart_returnsZero() {
        assertEquals(0, viewModel.precisionPercent())
    }

    /**
     * Verifica que [startTest] cambia el estado a SHOWING.
     */
    @Test
    fun startTest_changesStateToShowing() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        assertEquals(VisualMemoryViewModel.TestState.SHOWING, viewModel.testState.value)
    }

    /**
     * Verifica que [onCellPressed] no registra pulsaciones fuera de INPUT.
     */
    @Test
    fun onCellPressed_whenNotInInput_doesNotAddToInput() {
        viewModel.onCellPressed(0)
        assertTrue(viewModel.userInput.value.isEmpty())
    }

    /**
     * Verifica que [resetTest] restaura el estado inicial.
     */
    @Test
    fun resetTest_restoresInitialState() = runTest {
        viewModel.startTest()
        advanceTimeBy(500)
        viewModel.resetTest()
        assertEquals(VisualMemoryViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(3, viewModel.currentLength.value)
        assertEquals(0, viewModel.maxLength.value)
        assertTrue(viewModel.userInput.value.isEmpty())
    }

    /**
     * Verifica que la precisión está en rango válido.
     */
    @Test
    fun precisionPercent_isAlwaysInValidRange() = runTest {
        viewModel.startTest()
        advanceTimeBy(1000)
        assertTrue(viewModel.precisionPercent() in 0..100)
    }
}
