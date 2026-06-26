package com.neurotracker.ui.test.executive.planning

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.executive.planning.PlanningViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [PlanningViewModel].
 *
 * Verifica la lógica del juego de Torres de Hanoi:
 *  - Estado inicial de las torres.
 *  - Selección y deselección de torres.
 *  - Movimientos válidos e inválidos.
 *  - Cálculo de eficiencia (movimientos óptimos vs realizados).
 *
 * Nota sobre la representación de las torres: la lista de discos representa
 * la pila de abajo hacia arriba, por lo que el último elemento es el disco
 * superior (el que se puede mover). Con [1, 2, 3] el disco superior es el 3
 * (el más grande), lo que es el estado inicial antes de cualquier movimiento.
 * El disco 1 (más pequeño) llega arriba tras moverlo desde otra torre.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PlanningViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PlanningViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = PlanningViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: IDLE, torre A con [1,2,3], B y C vacías.
     */
    @Test
    fun initialState_isIdleWithCorrectTowers() {
        assertEquals(PlanningViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(listOf(1, 2, 3), viewModel.towers.value[0])
        assertTrue(viewModel.towers.value[1].isEmpty())
        assertTrue(viewModel.towers.value[2].isEmpty())
    }

    /**
     * Verifica que [startTest] cambia el estado a RUNNING y resetea los movimientos.
     */
    @Test
    fun startTest_changesStateToRunning() {
        viewModel.startTest()
        assertEquals(PlanningViewModel.TestState.RUNNING, viewModel.testState.value)
        assertEquals(0, viewModel.moveCount.value)
    }

    /**
     * Verifica que [precisionPercent] devuelve 0 antes de iniciar el test.
     */
    @Test
    fun precisionPercent_beforeStart_returnsZero() {
        assertEquals(0, viewModel.precisionPercent())
    }

    /**
     * Verifica que seleccionar una torre vacía muestra el mensaje de error correcto.
     */
    @Test
    fun onTowerTapped_emptyTower_showsErrorMessage() {
        viewModel.startTest()
        viewModel.onTowerTapped(1) // Torre B vacía al inicio
        assertEquals("Esa pila está vacía", viewModel.errorMessage.value)
    }

    /**
     * Verifica que seleccionar la misma torre dos veces la deselecciona.
     */
    @Test
    fun onTowerTapped_sameTowerTwice_deselects() {
        viewModel.startTest()
        viewModel.onTowerTapped(0)
        assertEquals(0, viewModel.selectedTower.value)
        viewModel.onTowerTapped(0)
        assertNull(viewModel.selectedTower.value)
    }

    /**
     * Verifica que seleccionar la torre A la marca como seleccionada.
     */
    @Test
    fun onTowerTapped_nonEmptyTower_selectsTower() {
        viewModel.startTest()
        viewModel.onTowerTapped(0)
        assertEquals(0, viewModel.selectedTower.value)
    }

    /**
     * Verifica que intentar colocar el disco 3 (grande, top de A) sobre
     * la torre C vacía es un movimiento válido y el contador incrementa.
     *
     * La torre A tiene [1,2,3] por lo que el disco superior es el 3.
     * Moverlo a C vacía siempre es válido.
     */
    @Test
    fun onTowerTapped_moveToEmptyTower_incrementsMoveCount() {
        viewModel.startTest()
        viewModel.onTowerTapped(0) // seleccionar A (disco 3 arriba)
        viewModel.onTowerTapped(2) // mover a C vacía — siempre válido
        assertEquals(1, viewModel.moveCount.value)
        assertTrue(viewModel.towers.value[2].isNotEmpty())
    }

    /**
     * Verifica que [efficiencyPercent] es 100% cuando se realizan
     * exactamente 7 movimientos válidos (el óptimo para 3 discos).
     *
     * Secuencia válida de 7 movimientos (A=[1,2,3], disco superior=3):
     *  1. A→C: disco 3.  A=[1,2],   B=[],    C=[3]
     *  2. A→B: disco 2.  A=[1],     B=[2],   C=[3]
     *  3. A→B: disco 1.  A=[],      B=[2,1], C=[3]
     *  4. C→A: disco 3.  A=[3],     B=[2,1], C=[]
     *  5. B→C: disco 1.  A=[3],     B=[2],   C=[1]
     *  6. B→A: disco 2.  A=[3,2],   B=[],    C=[1]
     *  7. C→A: disco 1.  A=[3,2,1], B=[],    C=[]  ← A no vacía: no dispara victoria
     */
    @Test
    fun efficiencyPercent_withOptimalMoves_returns100() {
        viewModel.startTest()
        viewModel.onTowerTapped(0); viewModel.onTowerTapped(2) // A→C: disco 3
        viewModel.onTowerTapped(0); viewModel.onTowerTapped(1) // A→B: disco 2
        viewModel.onTowerTapped(0); viewModel.onTowerTapped(1) // A→B: disco 1
        viewModel.onTowerTapped(2); viewModel.onTowerTapped(0) // C→A: disco 3
        viewModel.onTowerTapped(1); viewModel.onTowerTapped(2) // B→C: disco 1
        viewModel.onTowerTapped(1); viewModel.onTowerTapped(0) // B→A: disco 2
        viewModel.onTowerTapped(2); viewModel.onTowerTapped(0) // C→A: disco 1
        assertEquals(7, viewModel.moveCount.value)
        assertEquals(100, viewModel.efficiencyPercent())
    }

    /**
     * Verifica que [resetTest] restaura el estado inicial con las torres originales.
     */
    @Test
    fun resetTest_restoresTowersToInitialState() {
        viewModel.startTest()
        viewModel.onTowerTapped(0); viewModel.onTowerTapped(2)
        viewModel.resetTest()
        assertEquals(PlanningViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(listOf(1, 2, 3), viewModel.towers.value[0])
        assertTrue(viewModel.towers.value[1].isEmpty())
        assertTrue(viewModel.towers.value[2].isEmpty())
        assertEquals(0, viewModel.moveCount.value)
    }
}