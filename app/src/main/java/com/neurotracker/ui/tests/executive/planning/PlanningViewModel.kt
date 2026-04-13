package com.neurotracker.ui.tests.executive.planning

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.eeg.SimulatedEegDataSource
import com.neurotracker.data.entities.TestResultEntity
import com.neurotracker.data.session.SessionManager
import kotlinx.coroutines.launch

/**
 * ViewModel para el Test de Planificación (Torres de Hanoi).
 *
 * Evalúa la capacidad de planificación y resolución de problemas mediante
 * el clásico rompecabezas de las Torres de Hanoi con 3 discos.
 *
 * Reglas:
 *  - Mover todos los discos de la torre A a la torre C.
 *  - No se puede colocar un disco grande sobre uno pequeño.
 *  - Solo se puede mover el disco superior de cada torre.
 *
 * Características:
 *  - 3 discos (tamaños 1, 2, 3)
 *  - 3 torres (A, B, C)
 *  - Número óptimo de movimientos: 7
 *
 * Estados del test:
 *  - IDLE: Pantalla de instrucciones, esperando inicio.
 *  - RUNNING: Test en curso, usuario interactuando con las torres.
 *  - FINISHED: Test completado, mostrando resultados.
 *
 * @param application Contexto de la aplicación.
 */
class PlanningViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del flujo del test.
     */
    enum class TestState { IDLE, RUNNING, FINISHED }

    private val db = NeuroTrackerDatabase.getDatabase(application)

    private val eegSource = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    /** Número óptimo de movimientos para resolver 3 discos. */
    val optimalMoves = 7

    /** Estado actual del test. */
    var testState       = mutableStateOf(TestState.IDLE);   private set
    /** Estado actual de las tres torres (lista de discos por torre). */
    var towers          = mutableStateOf(listOf(listOf(1, 2, 3), emptyList<Int>(), emptyList<Int>())); private set
    /** Índice de la torre actualmente seleccionada (para mover disco). */
    var selectedTower   = mutableStateOf<Int?>(null);       private set
    /** Número de movimientos realizados. */
    var moveCount       = mutableStateOf(0);                private set
    /** Mensaje de error para mostrar al usuario (movimiento inválido). */
    var errorMessage    = mutableStateOf<String?>(null);    private set
    /** Indica si el test ha sido completado. */
    var isComplete      = mutableStateOf(false);            private set
    /** Indica si visualmente parece completado para mostrar el botón de confirmación. */
    var canFinish       = mutableStateOf(false);            private set

    private var startedAt = 0L

    /**
     * Inicia el test, reseteando las torres a su estado inicial.
     */
    fun startTest() {
        towers.value        = listOf(listOf(1, 2, 3), emptyList(), emptyList())
        selectedTower.value = null
        moveCount.value     = 0
        isComplete.value    = false
        canFinish.value     = false
        errorMessage.value  = null
        startedAt           = System.currentTimeMillis()
        testState.value     = TestState.RUNNING
    }

    /**
     * Maneja la pulsación sobre una torre.
     *
     * Lógica de selección y movimiento:
     * 1. Si no hay torre seleccionada → selecciona la torre si tiene discos.
     * 2. Si hay torre seleccionada y es la misma → deselecciona.
     * 3. Si hay torre seleccionada diferente → intenta mover el disco.
     *
     * @param towerIndex Índice de la torre pulsada (0, 1, 2).
     */
    fun onTowerTapped(towerIndex: Int) {
        if (testState.value != TestState.RUNNING) return
        errorMessage.value = null

        val selected = selectedTower.value

        // No hay torre seleccionada
        if (selected == null) {
            if (towers.value[towerIndex].isEmpty()) {
                errorMessage.value = "Esa pila está vacía"
                return
            }
            selectedTower.value = towerIndex
            return
        }

        // Seleccionó la misma torre → deseleccionar
        if (selected == towerIndex) {
            selectedTower.value = null
            return
        }

        val fromDiscs = towers.value[selected].toMutableList()
        val toDiscs   = towers.value[towerIndex].toMutableList()

        if (fromDiscs.isEmpty()) {
            selectedTower.value = null
            return
        }

        val movingDisc = fromDiscs.last()

        // Regla de Hanoi: no se puede colocar un disco grande sobre uno pequeño
        if (toDiscs.isNotEmpty() && toDiscs.last() < movingDisc) {
            errorMessage.value  = "No puedes poner un disco grande sobre uno pequeño"
            selectedTower.value = null
            return
        }

        // Realizar el movimiento
        fromDiscs.removeAt(fromDiscs.lastIndex)
        toDiscs.add(movingDisc)

        val newTowers = towers.value.toMutableList()
        newTowers[selected]   = fromDiscs
        newTowers[towerIndex] = toDiscs
        towers.value          = newTowers
        moveCount.value++
        selectedTower.value   = null

        // Comprobar si la torre C tiene 3 discos (cualquier orden → mostrar botón)
        canFinish.value = towers.value[2].size == 3

        // Intentar detección automática: torre A y B vacías, torre C con 3 discos
        autoCheckVictory()
    }

    /**
     * Verifica automáticamente si se ha alcanzado la victoria.
     */
    private fun autoCheckVictory() {
        val pileA = towers.value[0]
        val pileB = towers.value[1]
        val pileC = towers.value[2]

        // Victoria si A y B vacías y C tiene los 3 discos (en cualquier orden interno)
        if (pileA.isEmpty() && pileB.isEmpty() && pileC.size == 3) {
            finishTest()
        }
    }

    /**
     * Botón manual para confirmar finalización.
     * El usuario confirma que cree haber terminado.
     */
    fun onConfirmFinish() {
        if (testState.value != TestState.RUNNING) return

        val pileA = towers.value[0]
        val pileB = towers.value[1]
        val pileC = towers.value[2]

        if (pileA.isEmpty() && pileB.isEmpty() && pileC.size == 3) {
            finishTest()
        } else {
            errorMessage.value = "Aún no están todos los discos en la pila C"
        }
    }

    /**
     * Calcula el porcentaje de precisión (eficiencia).
     *
     * @return Porcentaje de eficiencia (0-100).
     */
    fun precisionPercent(): Int = efficiencyPercent()

    /**
     * Finaliza el test, guarda el resultado en la base de datos.
     */
    private fun finishTest() {
        isComplete.value = true
        testState.value  = TestState.FINISHED
        val timeMs = System.currentTimeMillis() - startedAt
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg = eegSource.getLastSample()
            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail = email,
                    testType = "planning",
                    score = timeMs,
                    precisionPct = precisionPercent(),
                    eegAlpha  = eeg.alpha,
                    eegBeta   = eeg.beta,
                    eegGamma  = eeg.gamma,
                    eegTheta  = eeg.theta)
            )
        }
    }

    /**
     * Calcula el porcentaje de eficiencia basado en movimientos óptimos.
     *
     * Eficiencia = (movimientos_óptimos / movimientos_realizados) * 100
     *
     * @return Porcentaje de eficiencia (0-100).
     */
    fun efficiencyPercent(): Int {
        return if (moveCount.value == 0) 0
        else ((optimalMoves.toFloat() / moveCount.value) * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Resetea el test completamente.
     */
    fun resetTest() {
        testState.value     = TestState.IDLE
        towers.value        = listOf(listOf(1, 2, 3), emptyList(), emptyList())
        selectedTower.value = null
        moveCount.value     = 0
        isComplete.value    = false
        canFinish.value     = false
        errorMessage.value  = null
    }
}