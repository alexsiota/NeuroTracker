package com.neurotracker.ui.tests.attention.selectiveAttention

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
 * ViewModel del Test de Atención Selectiva.
 *
 * ─── Fix: avance inmediato al pulsar ─────────────────────────────────────────
 *
 * Problema original: la corrutina [runRounds] siempre esperaba los
 * [ROUND_DURATION_MS] completos aunque el usuario ya hubiera respondido.
 * Esto hacía que el usuario tuviera que esperar ~2.5s hasta la siguiente
 * ronda incluso habiendo pulsado inmediatamente.
 *
 * Solución: se añade el flag [advanceNow] (igual que en [TaskSwitchingViewModel]).
 * La corrutina usa un bucle de polling cada 50ms en lugar de un `delay` fijo:
 * en cuanto [advanceNow] = true la ronda avanza de inmediato.
 * En [onStimulusPressed] se activa [advanceNow] tras registrar la respuesta.
 *
 * ─── Diseño del test ─────────────────────────────────────────────────────────
 *
 * 20 rondas · cuadrícula 2×2 · 1 target + 3 distractores · 2.5s por ronda.
 * El target cambia aleatoriamente cada ronda del pool de 6 símbolos.
 *
 * @param application Contexto de aplicación para Room y SessionManager.
 */
class SelectiveAttentionViewModel(application: Application) : AndroidViewModel(application) {

    enum class TestState { IDLE, RUNNING, FINISHED }

    /**
     * Modelo de un estímulo individual en la cuadrícula.
     *
     * @param id       Índice único (0–3).
     * @param label    Símbolo visible (ej: "▲", "●").
     * @param isTarget true si es el objetivo a pulsar en esta ronda.
     */
    data class Stimulus(val id: Int, val label: String, val isTarget: Boolean)

    private val db             = NeuroTrackerDatabase.getDatabase(application)
    private val eegSource      = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    companion object {
        private const val TOTAL_ROUNDS      = 20
        private const val ROUND_DURATION_MS = 2500L
    }

    private val labels = listOf("▲", "●", "■", "◆", "★", "✦")

    var testState     = mutableStateOf(TestState.IDLE);             private set
    var stimuli       = mutableStateOf<List<Stimulus>>(emptyList()); private set
    var targetLabel   = mutableStateOf("");                          private set
    var currentRound  = mutableStateOf(0);                          private set
    var hits          = mutableStateOf(0);                          private set
    var errors        = mutableStateOf(0);                          private set
    var omissions     = mutableStateOf(0);                          private set
    var avgReactionMs = mutableStateOf(0L);                         private set

    private val reactionTimes = mutableListOf<Long>()
    private var roundStartedAt = 0L
    private var answered       = false

    /**
     * Flag de avance inmediato.
     *
     * Se activa en [onStimulusPressed] para que el bucle de polling
     * de [runRounds] avance a la siguiente ronda sin esperar el timeout.
     */
    private var advanceNow = false

    private var testJob: Job? = null

    /**
     * Inicia el test desde cero.
     */
    fun startTest() {
        hits.value = 0; errors.value = 0; omissions.value = 0
        currentRound.value = 0; reactionTimes.clear()
        testState.value = TestState.RUNNING
        testJob = viewModelScope.launch { runRounds() }
    }

    /**
     * Registra la pulsación del usuario sobre un estímulo.
     *
     * FIX: al final activa [advanceNow] = true para que la corrutina
     * avance inmediatamente a la siguiente ronda sin esperar el timeout.
     *
     * @param stimulus Estímulo pulsado por el usuario.
     */
    fun onStimulusPressed(stimulus: Stimulus) {
        if (testState.value != TestState.RUNNING || answered) return
        answered = true
        if (stimulus.isTarget) {
            reactionTimes.add(System.currentTimeMillis() - roundStartedAt)
            hits.value++
        } else {
            errors.value++
        }
        advanceNow = true // ← FIX: avance inmediato
    }

    /**
     * Calcula el porcentaje de precisión.
     *
     * Fórmula: aciertos / (aciertos + omisiones) × 100.
     *
     * @return Precisión 0–100. 0 si no hay intentos.
     */
    fun precisionPercent(): Int {
        val total = hits.value + omissions.value
        return if (total == 0) 0 else ((hits.value.toFloat() / total) * 100).toInt()
    }

    /**
     * Reinicia el test al estado inicial.
     */
    fun resetTest() {
        testJob?.cancel()
        testState.value    = TestState.IDLE
        stimuli.value      = emptyList()
        hits.value         = 0; errors.value = 0; omissions.value = 0
        avgReactionMs.value = 0L; currentRound.value = 0
        reactionTimes.clear()
    }

    /**
     * Ejecuta las [TOTAL_ROUNDS] rondas del test secuencialmente.
     *
     * FIX: en lugar de `delay(ROUND_DURATION_MS)` fijo, usa un bucle de
     * polling de 50ms que sale cuando [advanceNow] = true (usuario respondió)
     * o cuando se agota el tiempo máximo (omisión).
     */
    private suspend fun runRounds() {
        repeat(TOTAL_ROUNDS) { index ->
            currentRound.value = index + 1
            answered           = false
            advanceNow         = false

            val target      = labels.random()
            targetLabel.value = target
            val distractors = labels.filter { it != target }.shuffled().take(3)
            val all         = (listOf(target) + distractors).shuffled()
            stimuli.value   = all.mapIndexed { i, lbl ->
                Stimulus(id = i, label = lbl, isTarget = lbl == target)
            }

            roundStartedAt = System.currentTimeMillis()

            // Polling de 50ms: avanza en cuanto el usuario responde o se acaba el tiempo
            val deadline = System.currentTimeMillis() + ROUND_DURATION_MS
            while (!advanceNow && System.currentTimeMillis() < deadline) {
                delay(50)
            }

            if (!answered) omissions.value++

            stimuli.value = emptyList()
            delay(300)
        }
        finishTest()
    }

    private fun finishTest() {
        avgReactionMs.value = if (reactionTimes.isNotEmpty())
            reactionTimes.average().toLong() else 0L
        testState.value = TestState.FINISHED
        saveResult()
    }

    private fun saveResult() {
        val avg = avgReactionMs.value.takeIf { it > 0 } ?: return
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg   = eegSource.getLastSample()
            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail    = email,
                    testType     = "attention_selective",
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
}
