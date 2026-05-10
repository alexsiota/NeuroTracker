package com.neurotracker.ui.tests.attention.reaction

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
 * ViewModel para el Test de Reacción Simple.
 *
 * Gestiona la lógica del test: rondas, tiempos de espera aleatorios,
 * detección de aciertos, errores y omisiones, y persistencia de resultados.
 *
 * Estados del test:
 *  - IDLE: Pantalla de instrucciones, esperando inicio.
 *  - WAITING: Esperando la aparición del estímulo.
 *  - STIMULUS: Estímulo visible, esperando pulsación.
 *  - FINISHED: Test completado, mostrando resultados.
 *
 * @param application Contexto de la aplicación.
 */
class ReactionTestViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del flujo del test.
     */
    enum class TestState { IDLE, WAITING, STIMULUS, FINISHED }

    private val db = NeuroTrackerDatabase.getDatabase(application)

    private val eegSource = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    companion object {
        private const val TOTAL_ROUNDS     = 15
        private const val STIMULUS_MS      = 1200L  // tiempo visible el estímulo
        private const val MIN_WAIT_MS      = 800L
        private const val MAX_WAIT_MS      = 2500L
    }

    /** Estado actual del test. */
    var testState      = mutableStateOf(TestState.IDLE);    private set
    /** Ronda actual (1 a TOTAL_ROUNDS). */
    var currentRound   = mutableStateOf(0);                 private set
    /** Indica si el estímulo (círculo verde) está visible. */
    var showTarget     = mutableStateOf(false);             private set
    /** Número de aciertos (pulsación correcta durante el estímulo). */
    var hits           = mutableStateOf(0);                 private set
    /** Número de errores (pulsación antes de que aparezca el estímulo). */
    var errors         = mutableStateOf(0);                 private set
    /** Número de omisiones (no pulsar durante el estímulo). */
    var omissions      = mutableStateOf(0);                 private set
    /** Tiempo medio de reacción en milisegundos. */
    var avgReactionMs  = mutableStateOf(0L);                private set

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
        testState.value = TestState.WAITING
        testJob = viewModelScope.launch { runRounds() }
    }

    /**
     * Ejecuta las rondas del test de forma asíncrona.
     *
     * Por cada ronda:
     * 1. Espera un tiempo aleatorio (MIN_WAIT_MS a MAX_WAIT_MS).
     * 2. Muestra el estímulo durante STIMULUS_MS.
     * 3. Si no hay pulsación, cuenta omisión.
     * 4. Espera 400ms entre rondas.
     */
    private suspend fun runRounds() {
        repeat(TOTAL_ROUNDS) { index ->
            currentRound.value = index + 1
            showTarget.value = false
            pressed = false

            // Espera aleatoria (anticipación)
            delay(Random.nextLong(MIN_WAIT_MS, MAX_WAIT_MS))

            // Mostrar estímulo
            testState.value = TestState.STIMULUS
            showTarget.value = true
            stimulusShownAt = System.currentTimeMillis()

            delay(STIMULUS_MS)

            // Si no pulsó → omisión
            if (!pressed) omissions.value++
            showTarget.value = false
            testState.value = TestState.WAITING
            delay(400)
        }
        finishTest()
    }

    /**
     * Maneja la pulsación del usuario en el área de respuesta.
     *
     * - En estado STIMULUS: registra acierto y tiempo de reacción.
     * - En estado WAITING: registra error (pulsación anticipada).
     */
    fun onPress() {
        when (testState.value) {
            TestState.STIMULUS -> {
                if (pressed) return
                pressed = true
                reactionTimes.add(System.currentTimeMillis() - stimulusShownAt)
                hits.value++
            }
            TestState.WAITING -> {
                // Pulsó antes de que apareciera el estímulo → error
                errors.value++
            }
            else -> {}
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
        if (reactionTimes.isEmpty()) return
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg = eegSource.getLastSample()
            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail = email,
                    testType  = "reaction_simple",
                    score     = avgReactionMs.value,
                    precisionPct = precisionPercent(),
                    eegAlpha  = eeg.alpha,
                    eegBeta   = eeg.beta,
                    eegGamma  = eeg.gamma,
                    eegTheta  = eeg.theta
                )
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
        showTarget.value = false
        hits.value = 0; errors.value = 0; omissions.value = 0
        avgReactionMs.value = 0L; currentRound.value = 0
        reactionTimes.clear()
    }
}