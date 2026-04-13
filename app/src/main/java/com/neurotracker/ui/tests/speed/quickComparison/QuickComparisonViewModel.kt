package com.neurotracker.ui.tests.speed.quickComparison

/**
 * ViewModel del Test de Comparación Rápida.
 *
 * Evalúa la velocidad de procesamiento visual y la discriminación perceptiva.
 *
 * Dinámica del test:
 *  - Se presentan 20 rondas.
 *  - En cada ronda aparecen dos símbolos.
 *  - El usuario debe decidir si ambos son iguales o diferentes.
 *  - Tiempo máximo por estímulo: 2000 ms.
 *
 * Flujo de estados:
 *  IDLE → RUNNING → FINISHED
 *
 * Métricas registradas:
 *  - hits: respuestas correctas
 *  - errors: respuestas incorrectas o ausencia de respuesta
 *  - tiempo medio de reacción
 *  - precisión (%)
 *
 * Características del diseño:
 *  - Probabilidad equilibrada entre estímulos iguales y diferentes.
 *  - Los símbolos se seleccionan de un conjunto visual variado.
 *  - En caso de diferencia, se garantiza que los símbolos no coinciden.
 *  - El tiempo limitado obliga a respuestas rápidas, reduciendo el procesamiento consciente.
 *
 * Capacidades cognitivas evaluadas:
 *  - Velocidad de procesamiento visual
 *  - Atención selectiva
 *  - Discriminación perceptiva
 *  - Tiempo de reacción
 *
 * Persistencia:
 *  - Resultados almacenados en Room
 *  - Inclusión de datos EEG simulados
 */

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.eeg.SimulatedEegDataSource
import com.neurotracker.data.entities.TestResultEntity
import com.neurotracker.data.session.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class QuickComparisonViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados del test.
     */
    enum class TestState { IDLE, RUNNING, FINISHED }

    /**
     * Representa un estímulo de comparación.
     *
     * @param left símbolo izquierdo
     * @param right símbolo derecho
     * @param areSame indica si ambos símbolos son iguales
     */
    data class ComparisonItem(
        val left: String,
        val right: String,
        val areSame: Boolean
    )

    private val db = NeuroTrackerDatabase.getDatabase(application)
    private val eegSource = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    /**
     * Pool de símbolos utilizados en el test.
     */
    private val symbols = listOf("★", "●", "▲", "■", "◆", "✦", "❋", "⬟")

    /**
     * Configuración del test.
     */
    private val totalRounds = 20
    private val stimulusDurationMs = 2000L

    /**
     * Estado observable.
     */
    var testState = mutableStateOf(TestState.IDLE)
        private set
    var currentItem = mutableStateOf<ComparisonItem?>(null)
        private set
    var currentRound = mutableStateOf(0)
        private set
    var hits = mutableStateOf(0)
        private set
    var errors = mutableStateOf(0)
        private set
    var hasAnswered = mutableStateOf(false)
        private set

    /**
     * Tiempos de reacción.
     */
    private val reactionTimes = mutableListOf<Long>()

    /**
     * Variables internas de control.
     */
    private var shownAt = 0L

    /**
     * Inicia el test.
     */
    fun startTest() {
        hits.value = 0; errors.value = 0; currentRound.value = 0
        reactionTimes.clear()
        testState.value = TestState.RUNNING
        viewModelScope.launch { runRounds() }
    }

    /**
     * Lógica principal del test.
     */
    private suspend fun runRounds() {
        repeat(totalRounds) { index ->

            /**
             * Generación del estímulo:
             * - 50% probabilidad de que sean iguales.
             * - Si son distintos, se garantiza diferencia real.
             */
            val same = Random.nextBoolean()
            val left = symbols.random()
            val right = if (same) left else symbols.filter { it != left }.random()

            currentItem.value = ComparisonItem(left, right, same)
            currentRound.value = index + 1
            hasAnswered.value = false
            shownAt = System.currentTimeMillis()

            delay(stimulusDurationMs)

            /**
             * Si no hay respuesta, se considera error.
             */
            if (!hasAnswered.value) errors.value++
        }

        finishTest()
    }

    /**
     * Maneja la respuesta del usuario.
     *
     * @param userSaysEqual true si el usuario considera que son iguales
     */
    fun onAnswer(userSaysEqual: Boolean) {
        if (testState.value != TestState.RUNNING || hasAnswered.value) return

        hasAnswered.value = true

        reactionTimes.add(System.currentTimeMillis() - shownAt)

        if (userSaysEqual == currentItem.value?.areSame) hits.value++
        else errors.value++
    }

    /**
     * Finaliza el test.
     */
    private fun finishTest() {
        testState.value = TestState.FINISHED

        val avg = if (reactionTimes.isNotEmpty())
            reactionTimes.average().toLong()
        else 9999L

        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg = eegSource.getLastSample()

            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail = email,
                    testType = "quick_comparison",
                    score = avg,
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
     * Calcula la precisión del test.
     */
    fun precisionPercent() =
        if (hits.value + errors.value == 0) 0
        else ((hits.value.toFloat() / (hits.value + errors.value)) * 100).toInt()

    /**
     * Reinicia el test.
     */
    fun resetTest() {
        testState.value = TestState.IDLE
        currentItem.value = null
        hits.value = 0
        errors.value = 0
    }
}