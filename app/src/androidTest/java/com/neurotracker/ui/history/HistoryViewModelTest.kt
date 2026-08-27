package com.neurotracker.ui.history

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [HistoryViewModel].
 *
 * Verifica el estado inicial, los cambios de filtro ([HistoryFilter]) y
 * de criterio de ordenación ([HistoryOrder]), y los valores exactos de los enums.
 *
 * Se ejecuta en dispositivo o emulador porque [HistoryViewModel]
 * extiende [AndroidViewModel] y usa Room y [SessionManager].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HistoryViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HistoryViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica que el estado inicial tiene filtro [HistoryFilter.ALL]
     * y orden [HistoryOrder.DESC] (más recientes primero).
     */
    @Test
    fun initialState_hasAllFilterAndDescOrder() {
        assertEquals(HistoryFilter.ALL, viewModel.activeFilter.value)
        assertEquals(HistoryOrder.DESC, viewModel.order.value)
    }

    /**
     * Verifica que la lista de tests es accesible y no nula tras la carga.
     */
    @Test
    fun initialState_testsListIsAccessible() = runTest {
        advanceUntilIdle()
        assertNotNull(viewModel.tests.value)
    }

    /**
     * Verifica que [changeFilter] actualiza el filtro activo a [HistoryFilter.ATTENTION].
     */
    @Test
    fun changeFilter_toAttention_updatesActiveFilter() {
        viewModel.changeFilter(HistoryFilter.ATTENTION)
        assertEquals(HistoryFilter.ATTENTION, viewModel.activeFilter.value)
    }

    /**
     * Verifica que [changeFilter] actualiza el filtro activo a [HistoryFilter.MEMORY].
     */
    @Test
    fun changeFilter_toMemory_updatesActiveFilter() {
        viewModel.changeFilter(HistoryFilter.MEMORY)
        assertEquals(HistoryFilter.MEMORY, viewModel.activeFilter.value)
    }

    /**
     * Verifica que [changeFilter] actualiza el filtro a todos los bloques disponibles.
     */
    @Test
    fun changeFilter_toAllBlocks_eachUpdatesCorrectly() {
        HistoryFilter.values().forEach { filter ->
            viewModel.changeFilter(filter)
            assertEquals(filter, viewModel.activeFilter.value)
        }
    }

    /**
     * Verifica que [changeOrder] actualiza el orden a [HistoryOrder.ASC].
     */
    @Test
    fun changeOrder_toAsc_updatesOrder() {
        viewModel.changeOrder(HistoryOrder.ASC)
        assertEquals(HistoryOrder.ASC, viewModel.order.value)
    }

    /**
     * Verifica que [changeOrder] de ASC vuelve a DESC correctamente.
     */
    @Test
    fun changeOrder_backToDesc_updatesOrder() {
        viewModel.changeOrder(HistoryOrder.ASC)
        viewModel.changeOrder(HistoryOrder.DESC)
        assertEquals(HistoryOrder.DESC, viewModel.order.value)
    }

    /**
     * Verifica que [HistoryFilter] tiene exactamente 7 valores definidos:
     * ALL, ATTENTION, MEMORY, SPEED, EXECUTIVE, COORDINATION, COMPOSITE.
     */
    @Test
    fun historyFilter_hasSevenValues() {
        assertEquals(7, HistoryFilter.values().size)
    }

    /**
     * Verifica que [HistoryFilter.ALL] tiene la etiqueta "Todos".
     */
    @Test
    fun historyFilter_allHasLabelTodos() {
        assertEquals("Todos", HistoryFilter.ALL.label)
    }

    /**
     * Verifica que [HistoryFilter.ATTENTION] tiene la etiqueta "Atención".
     */
    @Test
    fun historyFilter_attentionHasCorrectLabel() {
        assertEquals("Atención", HistoryFilter.ATTENTION.label)
    }

    /**
     * Verifica que [HistoryOrder] tiene exactamente 2 valores: ASC y DESC.
     */
    @Test
    fun historyOrder_hasExactlyTwoValues() {
        assertEquals(2, HistoryOrder.values().size)
    }

    /**
     * Verifica que cambiar filtro y orden de forma independiente mantiene
     * ambos valores correctamente sin interferencia entre sí.
     */
    @Test
    fun changeFilterAndOrder_bothValuesAreMaintained() {
        viewModel.changeFilter(HistoryFilter.SPEED)
        viewModel.changeOrder(HistoryOrder.ASC)
        assertEquals(HistoryFilter.SPEED, viewModel.activeFilter.value)
        assertEquals(HistoryOrder.ASC, viewModel.order.value)
    }
}
