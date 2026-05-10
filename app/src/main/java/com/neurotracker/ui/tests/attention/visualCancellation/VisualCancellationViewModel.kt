package com.neurotracker.ui.tests.attention.visualCancellation

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.eeg.SimulatedEegDataSource
import com.neurotracker.data.entities.TestResultEntity
import com.neurotracker.data.session.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel para el Test de Cancelación Visual.
 *
 * Gestiona la lógica del test de búsqueda visual donde el usuario debe
 * encontrar y pulsar todos los círculos en una cuadrícula 6×6,
 * ignorando cuadrados y triángulos, con un límite de 45 segundos.
 *
 * Características:
 *  - Cuadrícula 6×6 (36 ítems totales)
 *  - 10 círculos objetivo (verdes)
 *  - El resto se reparte entre cuadrados rojos y triángulos azules
 *  - Duración máxima: 45 segundos
 *
 * Estados del test:
 *  - IDLE: Pantalla de instrucciones, esperando inicio.
 *  - RUNNING: Test en curso, cuadrícula interactiva con temporizador.
 *  - FINISHED: Test completado (tiempo agotado o todos los círculos encontrados).
 *
 * @param application Contexto de la aplicación.
 */
class VisualCancellationViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del flujo del test.
     */
    enum class TestState { IDLE, RUNNING, FINISHED }

    /**
     * Tipos de formas que aparecen en la cuadrícula.
     */
    enum class ShapeType { CIRCLE, SQUARE, TRIANGLE }

    /**
     * Item individual de la cuadrícula.
     *
     * @param id Identificador único del item.
     * @param shape Tipo de forma (círculo, cuadrado, triángulo).
     * @param cancelled Indica si ya fue pulsado (marcado).
     */
    data class CancellationItem(
        val id: Int,
        val shape: ShapeType,
        var cancelled: Boolean = false
    )

    private val db = NeuroTrackerDatabase.getDatabase(application)

    private val eegSource = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    private val gridSize     = 36   // 6×6
    private val targetCount  = 10   // círculos a encontrar
    private val testDuration = 45   // segundos

    /** Estado actual del test. */
    var testState       = mutableStateOf(TestState.IDLE);               private set
    /** Lista de items de la cuadrícula. */
    var gridItems       = mutableStateOf<List<CancellationItem>>(emptyList()); private set
    /** Número de aciertos (círculos encontrados correctamente). */
    var hits            = mutableStateOf(0);   private set
    /** Número de errores (pulsación sobre cuadrado o triángulo). */
    var errors          = mutableStateOf(0);   private set
    /** Segundos restantes para completar el test. */
    var remainingSec    = mutableStateOf(testDuration); private set
    /** Tiempo medio por acierto en milisegundos. */
    var avgTimePerHitMs = mutableStateOf(0L);  private set

    private var testJob: Job? = null
    private var startedAt = 0L

    /**
     * Inicia el test, genera la cuadrícula aleatoria y arranca el temporizador.
     */
    fun startTest() {
        hits.value = 0; errors.value = 0; remainingSec.value = testDuration
        gridItems.value = generateGrid()
        startedAt = System.currentTimeMillis()
        testState.value = TestState.RUNNING

        testJob = viewModelScope.launch {
            while (remainingSec.value > 0 && testState.value == TestState.RUNNING) {
                delay(1000)
                remainingSec.value--
            }
            if (testState.value == TestState.RUNNING) finishTest()
        }
    }

    /**
     * Genera una cuadrícula aleatoria con 10 círculos y el resto
     * repartidos entre cuadrados y triángulos.
     *
     * @return Lista de [CancellationItem] ordenada aleatoriamente.
     */
    private fun generateGrid(): List<CancellationItem> {
        val shapes = mutableListOf<ShapeType>()
        repeat(targetCount) { shapes.add(ShapeType.CIRCLE) }
        // Resto: mitad cuadrados, mitad triángulos (más variedad)
        val remaining = gridSize - targetCount
        repeat(remaining / 2) { shapes.add(ShapeType.SQUARE) }
        repeat(remaining - remaining / 2) { shapes.add(ShapeType.TRIANGLE) }
        shapes.shuffle(Random(System.currentTimeMillis()))
        return shapes.mapIndexed { index, shape -> CancellationItem(id = index, shape = shape) }
    }

    /**
     * Maneja la pulsación sobre un item de la cuadrícula.
     *
     * - Si es un círculo no pulsado: cuenta acierto.
     * - Si es cuadrado o triángulo: cuenta error.
     * - Si se alcanzan los 10 aciertos, finaliza el test automáticamente.
     *
     * @param itemId Identificador del item pulsado.
     */
    fun onItemClicked(itemId: Int) {
        if (testState.value != TestState.RUNNING) return
        val index = gridItems.value.indexOfFirst { it.id == itemId }
        if (index == -1) return
        val item = gridItems.value[index]
        if (item.cancelled) return

        val updated = gridItems.value.toMutableList()
        updated[index] = item.copy(cancelled = true)
        gridItems.value = updated

        if (item.shape == ShapeType.CIRCLE) {
            hits.value++
            if (hits.value == targetCount) finishTest()
        } else {
            errors.value++
        }
    }

    /**
     * Finaliza el test, calcula el tiempo medio por acierto,
     * cambia al estado FINISHED y guarda el resultado.
     */
    private fun finishTest() {
        if (testState.value == TestState.FINISHED) return
        testJob?.cancel()
        if (hits.value > 0) {
            avgTimePerHitMs.value = (System.currentTimeMillis() - startedAt) / hits.value
        }
        testState.value = TestState.FINISHED
        saveResult()
    }

    /**
     * Guarda el resultado del test en la base de datos Room,
     * asociado al usuario actual y con los datos EEG simulados.
     */
    private fun saveResult() {
        if (hits.value == 0) return
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg = eegSource.getLastSample()
            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail = email,
                    testType = "visual_cancellation",
                    score = avgTimePerHitMs.value,
                    precisionPct = precisionPercent(),
                    eegAlpha  = eeg.alpha,
                    eegBeta   = eeg.beta,
                    eegGamma  = eeg.gamma,
                    eegTheta  = eeg.theta)
            )
        }
    }

    /**
     * Calcula el porcentaje de precisión basado en aciertos y errores.
     *
     * @return Porcentaje de precisión (0-100).
     */
    fun precisionPercent(): Int {
        val total = hits.value + errors.value
        return if (total == 0) 0 else ((hits.value.toFloat() / total) * 100).toInt()
    }

    /**
     * Resetea el test completamente, cancelando cualquier ejecución en curso.
     */
    fun resetTest() {
        testJob?.cancel()
        testState.value = TestState.IDLE
        gridItems.value = emptyList()
        hits.value = 0; errors.value = 0
        remainingSec.value = testDuration
    }
}