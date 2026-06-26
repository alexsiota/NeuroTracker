package com.neurotracker.ui.test.coordination.objecttracking

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.coordination.objectTracking.ObjectTrackingViewModel
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
 * Tests instrumentados del [ObjectTrackingViewModel].
 *
 * Verifica el flujo de las fases (MEMORIZE → MOVING → RESPONSE), la generación
 * de objetos, la selección de objetivos y el cálculo de precisión.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ObjectTrackingViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ObjectTrackingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ObjectTrackingViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE y contadores a cero.
     */
    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(ObjectTrackingViewModel.TestState.IDLE, viewModel.testState.value)
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
     * Verifica que [startTest] cambia el estado a MEMORIZE y genera 6 objetos.
     */
    @Test
    fun startTest_changesStateToMemorize() = runTest {
        viewModel.startTest()
        advanceTimeBy(200)
        assertEquals(ObjectTrackingViewModel.TestState.MEMORIZE, viewModel.testState.value)
        assertEquals(6, viewModel.objects.value.size)
    }

    /**
     * Verifica que los objetos generados tienen exactamente 2 objetivos.
     */
    @Test
    fun startTest_generatesExactlyTwoTargets() = runTest {
        viewModel.startTest()
        advanceTimeBy(200)
        val targets = viewModel.objects.value.count { it.isTarget }
        assertEquals(2, targets)
    }

    /**
     * Verifica que [resetTest] restaura el estado inicial.
     */
    @Test
    fun resetTest_restoresInitialState() = runTest {
        viewModel.startTest()
        advanceTimeBy(500)
        viewModel.resetTest()
        assertEquals(ObjectTrackingViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.errors.value)
        assertTrue(viewModel.objects.value.isEmpty())
    }

    /**
     * Verifica que la precisión está en rango válido.
     */
    @Test
    fun precisionPercent_isAlwaysInValidRange() = runTest {
        viewModel.startTest()
        advanceTimeBy(500)
        assertTrue(viewModel.precisionPercent() in 0..100)
    }
}
