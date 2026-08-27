package com.neurotracker.ui.tests.attention.sustainedAttention

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
 * ViewModel para el Test de Atención Sostenida.
 *
 * Gestiona la lógica del test GO/NOGO donde el usuario debe responder
 * solo al estímulo objetivo (círculo verde) e ignorar el distractor (cuadrado azul).
 *
 * Características:
 *  - 30 rondas totales
 *  - Probabilidad del 30% de que aparezca el estímulo objetivo
 *  - Cada estímulo dura 1000ms
 *  - Intervalo entre estímulos: 600ms
 *
 * Estados del test:
 *  - IDLE: Pantalla de instrucciones, esperando inicio.
 *  - RUNNING: Test en curso, mostrando estímulos.
 *  - FINISHED: Test completado, mostrando resultados.
 *
 * @param application Contexto de la aplicación.
 */
class SustainedAttentionViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del flujo del test.
     */
    enum class TestState { IDLE, RUNNING, FINISHED }

    /**
     * Tipos de estímulo que pueden aparecer.
     */
    enum class StimulusType { TARGET, DISTRACTOR, NONE }

    private val db = NeuroTrackerDatabase.getDatabase(application)

    private val eegSource = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    companion object {
        private const val TOTAL_ROUNDS     = 30
        private const val STIMULUS_MS      = 1000L
        private const val INTER_STIMULUS_MS = 600L
        private const val TARGET_PROB      = 30   // 30% de probabilidad de target
    }

    /** Estado actual del test. */
    var testState      = mutableStateOf(TestState.IDLE);     private set
    /** Tipo de estímulo actualmente visible. */
    var currentStimulus = mutableStateOf(StimulusType.NONE); private set
    /** Ronda actual (1 a TOTAL_ROUNDS). */
    var currentRound   = mutableStateOf(0);                  private set
    /** Número de aciertos (respuesta correcta al estímulo objetivo). */
    var hits           = mutableStateOf(0);                  private set
    /** Número de errores (respuesta al estímulo distractor). */
    var errors         = mutableStateOf(0);                  private set
    /** Número de omisiones (no respuesta al estímulo objetivo). */
    var omissions      = mutableStateOf(0);                  private set
    /** Tiempo medio de reacción en milisegundos. */
    var avgReactionMs  = mutableStateOf(0L);                 private set

    private val reactionTimes = mutableListOf<Long>()
    private var stimulusShownAt = 0L
    private var pressed = false
    private var testJob: Job? = null

    /**
     * Inicia el test, reseteando todas las variables de estado.
     */
    fun startTest() {
        hits.value = 0; errors.value = 0; omissions.value = 0
        currentRound.value = 0; reactionTimes.clear()
        testState.value = TestState.RUNNING
        testJob = viewModelScope.launch { runRounds() }
    }

    /**
     * Ejecuta las rondas del test de forma asíncrona.
     *
     * Por cada ronda:
     * 1. Determina aleatoriamente si es estímulo objetivo o distractor.
     * 2. Muestra el estímulo durante STIMULUS_MS.
     * 3. Si no hubo pulsación durante un objetivo, cuenta omisión.
     * 4. Espera INTER_STIMULUS_MS antes de la siguiente ronda.
     */
    private suspend fun runRounds() {
        repeat(TOTAL_ROUNDS) { index ->
            currentRound.value = index + 1
            pressed = false

            val isTarget = Random.nextInt(100) < TARGET_PROB
            currentStimulus.value = if (isTarget) StimulusType.TARGET else StimulusType.DISTRACTOR
            stimulusShownAt = System.currentTimeMillis()

            delay(STIMULUS_MS)

            if (!pressed) {
                if (isTarget) omissions.value++ // no pulsó cuando debía
            }

            currentStimulus.value = StimulusType.NONE
            delay(INTER_STIMULUS_MS)
        }
        finishTest()
    }

    /**
     * Maneja la pulsación del usuario en el área de respuesta.
     *
     * - Si es estímulo objetivo: registra acierto y tiempo de reacción.
     * - Si es estímulo distractor: registra error.
     * - No se procesan pulsaciones fuera del período de estímulo.
     */
    fun onPress() {
        if (testState.value != TestState.RUNNING || pressed) return
        if (currentStimulus.value == StimulusType.NONE) return
        pressed = true

        if (currentStimulus.value == StimulusType.TARGET) {
            reactionTimes.add(System.currentTimeMillis() - stimulusShownAt)
            hits.value++
        } else {
            errors.value++
        }
    }

    /**
     * Finaliza el test, calcula el tiempo medio de reacción,
     * cambia al estado FINISHED y guarda el resultado.
     */
    private fun finishTest() {
        avgReactionMs.value = if (reactionTimes.isNotEmpty())
            reactionTimes.average().toLong() else 0L
        testState.value = TestState.FINISHED
        saveResult()
    }

    /**
     * Guarda el resultado del test en la base de datos Room,
     * asociado al usuario actual y con los datos EEG simulados.
     */
    private fun saveResult() {
        val avg = avgReactionMs.value.takeIf { it > 0 } ?: return
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg = eegSource.getLastSample()
            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail = email,
                    testType = "attention_sustained",
                    score = avg,
                    precisionPct = precisionPercent(),
                    eegAlpha  = eeg.alpha,
                    eegBeta   = eeg.beta,
                    eegGamma  = eeg.gamma,
                    eegTheta  = eeg.theta)
            )
        }
    }

    /**
     * Calcula el porcentaje de precisión basado en aciertos y omisiones.
     *
     * @return Porcentaje de precisión (0-100).
     */
    fun precisionPercent(): Int {
        val total = hits.value + omissions.value
        return if (total == 0) 0 else ((hits.value.toFloat() / total) * 100).toInt()
    }

    /**
     * Resetea el test completamente, cancelando cualquier ejecución en curso.
     */
    fun resetTest() {
        testJob?.cancel()
        testState.value = TestState.IDLE
        currentStimulus.value = StimulusType.NONE
        hits.value = 0; errors.value = 0; omissions.value = 0
        avgReactionMs.value = 0L; currentRound.value = 0
        reactionTimes.clear()
    }
}