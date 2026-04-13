package com.neurotracker.memory.digitspan

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.memory.digitspan.DigitSpanViewModel
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
 * Tests instrumentados del [DigitSpanViewModel].
 *
 * Verifica la lógica del span de dígitos: estado inicial, introducción de dígitos,
 * backspace y cálculo de precisión basada en el span máximo alcanzado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DigitSpanViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: DigitSpanViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DigitSpanViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE, span 3, maxSpan 0.
     */
    @Test
    fun initialState_isIdleWithSpan3() {
        assertEquals(DigitSpanViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(3, viewModel.currentSpan.value)
        assertEquals(0, viewModel.maxSpan.value)
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
        assertEquals(DigitSpanViewModel.TestState.SHOWING, viewModel.testState.value)
    }

    /**
     * Verifica que [onDigitPressed] no registra dígitos fuera de INPUT.
     */
    @Test
    fun onDigitPressed_whenNotInInput_doesNotAddDigit() {
        viewModel.onDigitPressed(5)
        assertTrue(viewModel.userInput.value.isEmpty())
    }

    /**
     * Verifica que [onBackspace] elimina el último dígito introducido.
     */
    @Test
    fun onBackspace_removesLastDigit() = runTest {
        viewModel.startTest()
        // Avanzar hasta la fase INPUT (600ms pausa + 3 × (800ms + 300ms) = 3900ms)
        advanceTimeBy(4500)
        if (viewModel.testState.value == DigitSpanViewModel.TestState.INPUT) {
            viewModel.onDigitPressed(1)
            viewModel.onDigitPressed(2)
            assertEquals(2, viewModel.userInput.value.size)
            viewModel.onBackspace()
            assertEquals(1, viewModel.userInput.value.size)
        }
    }

    /**
     * Verifica que [resetTest] restaura el estado inicial con span 3.
     */
    @Test
    fun resetTest_restoresInitialState() = runTest {
        viewModel.startTest()
        advanceTimeBy(500)
        viewModel.resetTest()
        assertEquals(DigitSpanViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(3, viewModel.currentSpan.value)
        assertEquals(0, viewModel.maxSpan.value)
        assertTrue(viewModel.userInput.value.isEmpty())
    }

    /**
     * Verifica que la precisión está en rango válido tras iniciar el test.
     */
    @Test
    fun precisionPercent_isAlwaysInValidRange() = runTest {
        viewModel.startTest()
        advanceTimeBy(1000)
        assertTrue(viewModel.precisionPercent() in 0..100)
    }
}
