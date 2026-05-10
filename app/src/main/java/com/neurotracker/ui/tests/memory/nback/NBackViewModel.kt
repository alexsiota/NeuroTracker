package com.neurotracker.ui.tests.memory.nback

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
 * ViewModel para el Test N-Back (1-Back).
 *
 * Evalúa la memoria de trabajo y la capacidad de actualización constante.
 * El usuario debe indicar si el estímulo actual es igual al anterior.
 *
 * Características:
 *  - 20 estímulos totales
 *  - Pool de 16 estímulos (letras A-H y números 1-8)
 *  - Cada estímulo dura 2000ms
 *  - Intervalo entre estímulos: 200ms
 *  - El primer estímulo no tiene respuesta válida
 *
 * Estados del test:
 *  - IDLE: Pantalla de instrucciones, esperando inicio.
 *  - RUNNING: Test en curso, mostrando estímulos secuencialmente.
 *  - FINISHED: Test completado, mostrando resultados.
 *
 * @param application Contexto de la aplicación.
 */
class NBackViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del flujo del test.
     */
    enum class TestState { IDLE, RUNNING, FINISHED }

    private val db = NeuroTrackerDatabase.getDatabase(application)

    private val eegSource = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    companion object {
        private const val TOTAL_STIMULI     = 20
        private const val STIMULUS_MS       = 2000L
        private const val INTER_STIMULUS_MS = 200L
    }

    private val stimulusPool = listOf(
        "A","B","C","D","E","F","G","H",
        "1","2","3","4","5","6","7","8"
    )

    /** Estado actual del test. */
    var testState       = mutableStateOf(TestState.IDLE);   private set
    /** Estímulo actualmente visible. */
    var currentStimulus = mutableStateOf<String?>(null);    private set
    /** Índice del estímulo actual (1 a TOTAL_STIMULI). */
    var currentIndex    = mutableStateOf(0);                private set
    /** Indica si el usuario ya respondió al estímulo actual. */
    var hasAnswered     = mutableStateOf(false);            private set
    /** Número de aciertos (respuesta correcta). */
    var hits            = mutableStateOf(0);                private set
    /** Número de errores (respuesta incorrecta). */
    var errors          = mutableStateOf(0);                private set
    /** Número de omisiones (falta de respuesta). */
    var omissions       = mutableStateOf(0);                private set

    private val reactionTimes = mutableListOf<Long>()
    private var stimulusShownAt   = 0L
    private var previousStimulus: String? = null
    private var currentStimulusValue: String? = null
    private var testJob: Job? = null

    /**
     * Inicia el test, reseteando todas las variables y comenzando la secuencia.
     */
    fun startTest() {
        resetInternal()
        testState.value = TestState.RUNNING
        testJob = viewModelScope.launch { runStimuli() }
    }

    /**
     * Ejecuta la secuencia de estímulos de forma asíncrona.
     *
     * Por cada estímulo:
     * 1. Muestra el estímulo durante STIMULUS_MS.
     * 2. Si no hay respuesta después del estímulo (y no es el primero), cuenta omisión.
     * 3. Espera INTER_STIMULUS_MS antes del siguiente.
     */
    private suspend fun runStimuli() {
        repeat(TOTAL_STIMULI) { index ->
            val stimulus = stimulusPool.random()
            currentStimulusValue  = stimulus
            currentStimulus.value = stimulus
            currentIndex.value    = index + 1
            hasAnswered.value     = false
            stimulusShownAt       = System.currentTimeMillis()

            delay(STIMULUS_MS)

            /*
             * LÓGICA DE OMISIÓN CORREGIDA:
             * - A partir del estímulo 2 (index > 0) el usuario SIEMPRE debe responder.
             * - Si no respondió → omisión, independientemente de si era match o no.
             */
            if (!hasAnswered.value && index > 0) {
                omissions.value++
            }

            previousStimulus      = stimulus
            currentStimulus.value = null
            delay(INTER_STIMULUS_MS)
        }
        finishTest()
    }

    /**
     * Maneja la pulsación del botón SÍ.
     * Indica que el usuario cree que el estímulo actual es igual al anterior.
     */
    fun onYesPressed() {
        if (testState.value != TestState.RUNNING) return
        if (hasAnswered.value) return
        if (currentIndex.value <= 1) return  // primer estímulo: sin respuesta válida

        hasAnswered.value = true
        reactionTimes.add(System.currentTimeMillis() - stimulusShownAt)

        if (currentStimulusValue == previousStimulus) hits.value++ else errors.value++
    }

    /**
     * Maneja la pulsación del botón NO.
     * Indica que el usuario cree que el estímulo actual es diferente al anterior.
     */
    fun onNoPressed() {
        if (testState.value != TestState.RUNNING) return
        if (hasAnswered.value) return
        if (currentIndex.value <= 1) return

        hasAnswered.value = true
        reactionTimes.add(System.currentTimeMillis() - stimulusShownAt)

        if (currentStimulusValue != previousStimulus) hits.value++ else errors.value++
    }

    /**
     * Finaliza el test, cambia al estado FINISHED y guarda el resultado.
     */
    private fun finishTest() {
        testState.value = TestState.FINISHED
        saveResult()
    }

    /**
     * Guarda el resultado del test en la base de datos Room,
     * asociado al usuario actual y con los datos EEG simulados.
     */
    private fun saveResult() {
        val avg = if (reactionTimes.isNotEmpty()) reactionTimes.average().toLong() else 9999L
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg = eegSource.getLastSample()
            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail = email,
                    testType = "n_back",
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
     * Calcula el porcentaje de precisión basado en aciertos, errores y omisiones.
     *
     * @return Porcentaje de precisión (0-100).
     */
    fun precisionPercent(): Int {
        val total = hits.value + errors.value + omissions.value
        return if (total == 0) 0
        else ((hits.value.toFloat() / total) * 100).toInt()
    }

    /**
     * Resetea el test completamente, cancelando cualquier ejecución en curso.
     */
    fun resetTest() {
        testJob?.cancel()
        resetInternal()
        testState.value = TestState.IDLE
    }

    /**
     * Resetea las variables internas del test sin cambiar el estado.
     */
    private fun resetInternal() {
        currentStimulus.value = null
        currentIndex.value    = 0
        hasAnswered.value     = false
        hits.value            = 0
        errors.value          = 0
        omissions.value       = 0
        reactionTimes.clear()
        previousStimulus      = null
        currentStimulusValue  = null
    }
}