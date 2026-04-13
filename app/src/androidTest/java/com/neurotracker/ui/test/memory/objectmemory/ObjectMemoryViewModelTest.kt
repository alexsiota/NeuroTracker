package com.neurotracker.memory.objectmemory

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.memory.objectMemory.ObjectMemoryViewModel
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
 * Tests instrumentados del [ObjectMemoryViewModel].
 *
 * Verifica la generación de objetos, la selección/deselección y
 * el cálculo de métricas (hits, misses, falseAlarms).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ObjectMemoryViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ObjectMemoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ObjectMemoryViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE y contadores a cero.
     */
    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(ObjectMemoryViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.misses.value)
        assertEquals(0, viewModel.falseAlarms.value)
    }

    /**
     * Verifica que [startTest] genera 6 objetos objetivo y 12 de recall.
     */
    @Test
    fun startTest_generatesCorrectNumberOfObjects() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        assertEquals(6, viewModel.targetObjects.value.size)
        assertEquals(12, viewModel.recallObjects.value.size)
    }

    /**
     * Verifica que [startTest] cambia el estado a MEMORIZE.
     */
    @Test
    fun startTest_changesStateToMemorize() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        assertEquals(ObjectMemoryViewModel.TestState.MEMORIZE, viewModel.testState.value)
    }

    /**
     * Verifica que tras el periodo de memorización el estado cambia a RECALL.
     * Memorización: 5 segundos.
     */
    @Test
    fun afterMemorization_stateChangesToRecall() = runTest {
        viewModel.startTest()
        advanceTimeBy(6000)
        assertEquals(ObjectMemoryViewModel.TestState.RECALL, viewModel.testState.value)
    }

    /**
     * Verifica que [onObjectToggled] añade y quita objetos de la selección.
     */
    @Test
    fun onObjectToggled_togglesSelectionCorrectly() = runTest {
        viewModel.startTest()
        advanceTimeBy(6000)
        val obj = viewModel.recallObjects.value.first()
        viewModel.onObjectToggled(obj)
        assertTrue(viewModel.selectedObjects.value.contains(obj))
        viewModel.onObjectToggled(obj)
        assertTrue(viewModel.selectedObjects.value.isEmpty())
    }

    /**
     * Verifica que no se pueden seleccionar más de 6 objetos.
     */
    @Test
    fun onObjectToggled_maxSixObjects_doesNotExceedLimit() = runTest {
        viewModel.startTest()
        advanceTimeBy(6000)
        viewModel.recallObjects.value.take(7).forEach { viewModel.onObjectToggled(it) }
        assertTrue(viewModel.selectedObjects.value.size <= 6)
    }

    /**
     * Verifica que [onConfirm] calcula hits, misses y falseAlarms correctamente.
     */
    @Test
    fun onConfirm_calculatesMetricsCorrectly() = runTest {
        viewModel.startTest()
        advanceTimeBy(6000)
        // Seleccionar todos los objetos objetivo
        viewModel.targetObjects.value.forEach { viewModel.onObjectToggled(it) }
        viewModel.onConfirm()
        assertEquals(6, viewModel.hits.value)
        assertEquals(0, viewModel.misses.value)
        assertEquals(0, viewModel.falseAlarms.value)
    }

    /**
     * Verifica que [resetTest] restaura el estado inicial.
     */
    @Test
    fun resetTest_restoresInitialState() = runTest {
        viewModel.startTest()
        advanceTimeBy(500)
        viewModel.resetTest()
        assertEquals(ObjectMemoryViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertTrue(viewModel.targetObjects.value.isEmpty())
    }
}
