package com.neurotracker.ui.tests.speed.sequenceOrdering

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
 * ViewModel del Test de Ordenación de Secuencias.
 *
 * ─── Fix: cambio de ronda automático por timeout ──────────────────────────────
 *
 * Problema original: el ViewModel solo avanzaba de ronda cuando el usuario
 * completaba la secuencia de 5 números. Si no la completaba (dejaba alguno
 * sin pulsar), la ronda nunca avanzaba y el test quedaba bloqueado.
 *
 * Solución: se añade un timeout de [ROUND_TIMEOUT_MS] = 10 segundos por ronda.
 * La corrutina [runRound] usa un bucle de polling de 100ms que sale cuando:
 *  a) El usuario completó la secuencia ([sequenceComplete] = true).
 *  b) Se agotaron los 10 segundos → ronda marcada como error.
 *
 * El temporizador visible [remainingMs] permite que la UI muestre una cuenta
 * atrás para orientar al usuario.
 *
 * @param application Contexto de aplicación para Room y SessionManager.
 */
class SequenceOrderingViewModel(application: Application) : AndroidViewModel(application) {

    enum class TestState { IDLE, RUNNING, FINISHED }

    private val db             = NeuroTrackerDatabase.getDatabase(application)
    private val eegSource      = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    companion object {
        private const val TOTAL_ROUNDS      = 10
        private const val SEQUENCE_SIZE     = 5
        /** Timeout por ronda en ms. Si el usuario no completa → error. */
        private const val ROUND_TIMEOUT_MS  = 10_000L
    }

    var testState       = mutableStateOf(TestState.IDLE);          private set
    var currentSequence = mutableStateOf<List<Int>>(emptyList());  private set
    var userOrder       = mutableStateOf<List<Int>>(emptyList());  private set
    var currentRound    = mutableStateOf(0);                       private set
    var hits            = mutableStateOf(0);                       private set
    var errors          = mutableStateOf(0);                       private set
    var avgRoundTimeMs  = mutableStateOf(0L);                      private set

    /**
     * Tiempo restante de la ronda actual en ms.
     * La UI puede mostrar una cuenta atrás con este valor.
     */
    var remainingMs = mutableStateOf(ROUND_TIMEOUT_MS); private set

    private val roundTimes     = mutableListOf<Long>()
    private var roundStartedAt = 0L

    /**
     * Flag de avance: se activa cuando la secuencia está completa.
     * Permite salir del bucle de polling sin esperar el timeout.
     */
    private var sequenceComplete = false

    private var testJob: Job? = null

    /**
     * Inicia el test desde cero.
     */
    fun startTest() {
        hits.value = 0; errors.value = 0; currentRound.value = 0
        roundTimes.clear()
        testState.value = TestState.RUNNING
        testJob = viewModelScope.launch { runAllRounds() }
    }

    /**
     * Registra la pulsación de un número por el usuario.
     *
     * Solo acepta números que no estén ya en [userOrder].
     * Cuando [userOrder] alcanza [SEQUENCE_SIZE], activa [sequenceComplete]
     * para que la corrutina avance inmediatamente a la siguiente ronda.
     *
     * @param number Número pulsado por el usuario.
     */
    fun onNumberTapped(number: Int) {
        if (testState.value != TestState.RUNNING) return
        if (userOrder.value.contains(number)) return

        val updated = userOrder.value + number
        userOrder.value = updated

        if (updated.size == SEQUENCE_SIZE) {
            // Evaluar si la secuencia es correcta
            val correct = updated == currentSequence.value.sorted()
            if (correct) hits.value++ else errors.value++
            roundTimes.add(System.currentTimeMillis() - roundStartedAt)
            sequenceComplete = true // ← avance inmediato
        }
    }

    /**
     * Calcula el porcentaje de precisión.
     *
     * @return Precisión 0–100. 0 si no hay intentos.
     */
    fun precisionPercent() =
        if (hits.value + errors.value == 0) 0
        else ((hits.value.toFloat() / (hits.value + errors.value)) * 100).toInt()

    /**
     * Reinicia el test al estado inicial.
     */
    fun resetTest() {
        testJob?.cancel()
        testState.value       = TestState.IDLE
        currentSequence.value = emptyList()
        userOrder.value       = emptyList()
        hits.value            = 0; errors.value = 0
        avgRoundTimeMs.value  = 0L
        remainingMs.value     = ROUND_TIMEOUT_MS
    }

    /**
     * Ejecuta las [TOTAL_ROUNDS] rondas secuencialmente.
     *
     * Cada ronda genera una secuencia, espera respuesta o timeout,
     * y evalúa el resultado.
     */
    private suspend fun runAllRounds() {
        repeat(TOTAL_ROUNDS) { index ->
            currentRound.value = index + 1
            sequenceComplete   = false
            userOrder.value    = emptyList()
            remainingMs.value  = ROUND_TIMEOUT_MS

            // Generar 5 números únicos entre 1 y 9
            currentSequence.value = (1..9).shuffled().take(SEQUENCE_SIZE)
            roundStartedAt        = System.currentTimeMillis()

            // Bucle de polling con countdown visible
            val deadline = System.currentTimeMillis() + ROUND_TIMEOUT_MS
            while (!sequenceComplete && System.currentTimeMillis() < deadline) {
                delay(100)
                remainingMs.value = (deadline - System.currentTimeMillis()).coerceAtLeast(0L)
            }

            // Timeout sin completar → error
            if (!sequenceComplete) {
                errors.value++
                roundTimes.add(ROUND_TIMEOUT_MS)
            }

            userOrder.value = emptyList()
            delay(300)
        }
        finishTest()
    }

    /**
     * Finaliza el test calculando métricas y persistiendo en Room.
     */
    private fun finishTest() {
        avgRoundTimeMs.value = if (roundTimes.isNotEmpty())
            roundTimes.average().toLong() else 0L
        testState.value = TestState.FINISHED

        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg   = eegSource.getLastSample()
            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail    = email,
                    testType     = "sequence_ordering",
                    score        = avgRoundTimeMs.value,
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
