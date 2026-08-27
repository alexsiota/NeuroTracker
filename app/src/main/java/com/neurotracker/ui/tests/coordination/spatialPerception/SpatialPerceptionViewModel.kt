package com.neurotracker.ui.tests.coordination.spatialPerception

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

/**
 * ViewModel para el Test de Percepción Espacial.
 *
 * Evalúa la capacidad de reconocer figuras geométricas rotadas en el espacio.
 * El usuario debe identificar cuál de las 4 opciones coincide con la figura
 * de referencia, teniendo en cuenta la rotación.
 *
 * Características:
 *  - 12 rondas totales
 *  - Cada estímulo dura 6 segundos máximo
 *  - Pool de 16 formas diferentes (F, T, L, J, S, Z, P, R, E, C, H, N, K, Y, G, Q)
 *  - Las opciones distractor utilizan formas diferentes (no rotaciones de la misma)
 *
 * Estados del test:
 *  - IDLE: Pantalla de instrucciones, esperando inicio.
 *  - RUNNING: Test en curso, mostrando figura de referencia y opciones.
 *  - FINISHED: Test completado, mostrando resultados.
 *
 * @param application Contexto de la aplicación.
 */
class SpatialPerceptionViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del flujo del test.
     */
    enum class TestState { IDLE, RUNNING, FINISHED }

    /**
     * Item de una ronda del test.
     *
     * @param referenceShape Forma de referencia (ej: "F").
     * @param referenceRotation Rotación aplicada a la forma de referencia.
     * @param options Lista de opciones entre las que elegir.
     * @param correctIndex Índice de la opción correcta dentro de [options].
     */
    data class SpatialItem(
        val referenceShape: String,
        val referenceRotation: Float,
        val options: List<OptionItem>,
        val correctIndex: Int
    )

    /**
     * Opción individual de respuesta.
     *
     * @param shape Forma mostrada.
     * @param rotation Rotación aplicada a la forma.
     * @param isCorrect Indica si esta opción es la correcta.
     */
    data class OptionItem(
        val shape: String,
        val rotation: Float,
        val isCorrect: Boolean
    )

    private val db             = NeuroTrackerDatabase.getDatabase(application)
    private val eegSource      = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    // Pool de formas disponibles para generar estímulos
    private val shapePool = listOf(
        "F", "T", "L", "J", "S", "Z", "P", "R",
        "E", "C", "H", "N", "K", "Y", "G", "Q"
    )

    private val totalRounds        = 12
    private val stimulusDurationMs = 6000L

    /** Estado actual del test. */
    var testState         = mutableStateOf(TestState.IDLE);     private set
    /** Item actual de la ronda en curso. */
    var currentItem       = mutableStateOf<SpatialItem?>(null); private set
    /** Ronda actual (1 a totalRounds). */
    var currentRound      = mutableStateOf(0);                  private set
    /** Número de aciertos. */
    var hits              = mutableStateOf(0);                  private set
    /** Número de errores (respuesta incorrecta o tiempo agotado). */
    var errors            = mutableStateOf(0);                  private set
    /** Indica si el usuario ya respondió en la ronda actual. */
    var hasAnswered       = mutableStateOf(false);              private set
    /** Índice de la opción seleccionada por el usuario. */
    var selectedIndex     = mutableStateOf<Int?>(null);         private set
    /** Indica si la última respuesta fue correcta (para feedback visual). */
    var lastAnswerCorrect = mutableStateOf<Boolean?>(null);     private set

    private val reactionTimes  = mutableListOf<Long>()
    private var shownAt        = 0L
    private var testJob: Job?  = null
    private var answered       = false

    /**
     * Inicia el test, reseteando todas las variables de estado.
     */
    fun startTest() {
        hits.value = 0; errors.value = 0; currentRound.value = 0
        reactionTimes.clear()
        testState.value = TestState.RUNNING
        testJob = viewModelScope.launch { runRounds() }
    }

    /**
     * Ejecuta las rondas del test de forma asíncrona.
     *
     * Por cada ronda:
     * 1. Selecciona una forma de referencia aleatoria.
     * 2. Genera una rotación correcta (90°, 180° o 270°).
     * 3. Crea 3 opciones distractor con formas diferentes.
     * 4. Muestra el estímulo durante stimulusDurationMs o hasta respuesta.
     * 5. Si no hay respuesta, cuenta error.
     */
    private suspend fun runRounds() {
        repeat(totalRounds) {
            val reference       = shapePool.random()
            val correctRotation = listOf(90f, 180f, 270f).random()
            val distractorShapes = shapePool.filter { it != reference }.shuffled().take(3)

            val allOptions = mutableListOf(
                OptionItem(shape = reference, rotation = correctRotation, isCorrect = true)
            )
            distractorShapes.forEach { shape ->
                allOptions.add(OptionItem(shape = shape, rotation = 0f, isCorrect = false))
            }
            allOptions.shuffle()

            val correctIndex = allOptions.indexOfFirst { it.isCorrect }

            currentItem.value       = SpatialItem(
                referenceShape    = reference,
                referenceRotation = 0f,
                options           = allOptions,
                correctIndex      = correctIndex
            )
            currentRound.value      = currentRound.value + 1
            hasAnswered.value       = false
            selectedIndex.value     = null
            lastAnswerCorrect.value = null
            answered                = false
            shownAt                 = System.currentTimeMillis()

            // Espera hasta stimulusDurationMs o hasta que el usuario responda
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < stimulusDurationMs && !answered) {
                delay(50)
            }

            if (!answered) errors.value++

            // Feedback visual breve (150ms) solo si respondió
            if (answered) delay(150)
        }
        finishTest()
    }

    /**
     * Maneja la selección de una opción por parte del usuario.
     *
     * @param index Índice de la opción seleccionada.
     */
    fun onOptionSelected(index: Int) {
        if (testState.value != TestState.RUNNING || hasAnswered.value) return
        hasAnswered.value       = true
        answered                = true
        selectedIndex.value     = index
        reactionTimes.add(System.currentTimeMillis() - shownAt)

        val correct             = index == currentItem.value?.correctIndex
        lastAnswerCorrect.value = correct
        if (correct) hits.value++ else errors.value++
    }

    /**
     * Finaliza el test, guarda el resultado en la base de datos.
     */
    private fun finishTest() {
        testState.value = TestState.FINISHED
        val avg = if (reactionTimes.isNotEmpty()) reactionTimes.average().toLong() else 9999L
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg   = eegSource.getLastSample()
            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail    = email,
                    testType     = "spatial_perception",
                    score        = avg,
                    precisionPct = precisionPercent(),
                    eegAlpha     = eeg.alpha,
                    eegBeta      = eeg.beta,
                    eegGamma     = eeg.gamma,
                    eegTheta     = eeg.theta
                )
            )
        }
    }

    /**
     * Calcula el porcentaje de precisión basado en aciertos y errores.
     *
     * @return Porcentaje de precisión (0-100).
     */
    fun precisionPercent() = if (hits.value + errors.value == 0) 0
    else ((hits.value.toFloat() / (hits.value + errors.value)) * 100).toInt()

    /**
     * Resetea el test completamente, cancelando cualquier ejecución en curso.
     */
    fun resetTest() {
        testJob?.cancel()
        testState.value         = TestState.IDLE
        currentItem.value       = null
        hits.value              = 0; errors.value = 0
        selectedIndex.value     = null
        lastAnswerCorrect.value = null
        answered                = false
    }
}