package com.neurotracker.ui.tests.composite.dualTask

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

/**
 * ViewModel del Test de Doble Tarea (Dual Task).
 *
 * ─── Nota: omisiones N-Back siempre cuentan como error ────────────────────────
 *
 * Problema original: la omisión N-Back solo se penalizaba si el símbolo actual
 * era igual al anterior (`symbol == nbackPrev`):
 * ```kotlin
 * if (!nbackAnswered.value && nbackIndex.value > 1 && symbol == nbackPrev) nbackErrors.value++
 * ```
 *
 * Esto es incorrecto: en N-Back, no responder también es un error cuando el
 * símbolo NO coincide con el anterior (el usuario debería haber pulsado "NO").
 * La omisión siempre es un error, independientemente de si la respuesta correcta
 * era SÍ o NO. Consistente con [GlobalCognitiveViewModel.runNBack] y
 * [NBackViewModel].
 *
 * Solución: la condición de omisión se simplifica a:
 * ```kotlin
 * if (!nbackAnswered.value && nbackIndex.value > 1) nbackErrors.value++
 * ```
 *
 * @param application Contexto de aplicación.
 */
class DualTaskViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del flujo del test.
     */
    enum class TestState { IDLE, RUNNING, FINISHED }

    private val db             = NeuroTrackerDatabase.getDatabase(application)
    private val eegSource      = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    private val testDurationMs = 60_000L
    private val nbackPool      = listOf("A","B","C","D","1","2","3","4")

    // ─── Estado general ───────────────────────────────────────────────────────

    var testState    = mutableStateOf(TestState.IDLE); private set
    var remainingSec = mutableStateOf(60);             private set

    // ─── Tarea 1: Reacción ────────────────────────────────────────────────────

    var reactionTarget = mutableStateOf(false); private set
    var reactionHits   = mutableStateOf(0);     private set
    var reactionErrors = mutableStateOf(0);     private set
    private var reactionPressed = false
    private var reactionActive  = false

    // ─── Tarea 2: N-Back ─────────────────────────────────────────────────────

    var nbackSymbol   = mutableStateOf<String?>(null); private set
    var nbackIndex    = mutableStateOf(0);             private set
    var nbackHits     = mutableStateOf(0);             private set
    var nbackErrors   = mutableStateOf(0);             private set
    var nbackAnswered = mutableStateOf(false);         private set
    private var nbackPrev:    String? = null
    private var nbackCurrent: String? = null

    /**
     * Inicia el test lanzando tres corrutinas paralelas:
     * timer, loop de reacción y loop N-Back.
     */
    fun startTest() {
        reactionHits.value = 0; reactionErrors.value = 0
        nbackHits.value    = 0; nbackErrors.value    = 0; nbackIndex.value = 0
        remainingSec.value = 60; nbackPrev = null
        testState.value    = TestState.RUNNING

        viewModelScope.launch {
            val start = System.currentTimeMillis()

            // Timer regresivo
            launch {
                while (System.currentTimeMillis() - start < testDurationMs) {
                    delay(1000)
                    if (remainingSec.value > 0) remainingSec.value--
                }
            }

            // Loop de reacción
            launch {
                while (System.currentTimeMillis() - start < testDurationMs) {
                    val isTarget    = Random.nextInt(100) < 60
                    reactionTarget.value = isTarget
                    reactionActive  = true
                    reactionPressed = false
                    delay(1500)
                    if (!reactionPressed && isTarget) reactionErrors.value++
                    reactionActive       = false
                    reactionTarget.value = false
                    delay(Random.nextLong(300, 700))
                }
            }

            // Loop N-Back
            launch {
                while (System.currentTimeMillis() - start < testDurationMs) {
                    val symbol = nbackPool.random()
                    nbackCurrent          = symbol
                    nbackSymbol.value     = symbol
                    nbackIndex.value++
                    nbackAnswered.value   = false
                    delay(2000)

                    // Nota: omisión siempre es error a partir del 2º estímulo,
                    // independientemente de si debía responder SÍ o NO.
                    // Antes solo penalizaba cuando symbol == nbackPrev (solo omisiones de SÍ).
                    if (!nbackAnswered.value && nbackIndex.value > 1) {
                        nbackErrors.value++
                    }

                    nbackPrev             = symbol
                    nbackSymbol.value     = null
                    delay(300)
                }
            }

            delay(testDurationMs)
            finishTest()
        }
    }

    /**
     * Registra la pulsación en la tarea de reacción.
     *
     * GO activo → acierto. Cualquier otra pulsación → error.
     */
    fun onReactionPress() {
        if (testState.value != TestState.RUNNING || reactionPressed || !reactionActive) return
        reactionPressed = true
        if (reactionTarget.value) reactionHits.value++ else reactionErrors.value++
    }

    /**
     * Respuesta "SÍ" en N-Back (símbolo actual == anterior).
     */
    fun onNBackYes() {
        if (testState.value != TestState.RUNNING || nbackAnswered.value || nbackIndex.value <= 1) return
        nbackAnswered.value = true
        if (nbackCurrent == nbackPrev) nbackHits.value++ else nbackErrors.value++
    }

    /**
     * Respuesta "NO" en N-Back (símbolo actual != anterior).
     */
    fun onNBackNo() {
        if (testState.value != TestState.RUNNING || nbackAnswered.value || nbackIndex.value <= 1) return
        nbackAnswered.value = true
        if (nbackCurrent != nbackPrev) nbackHits.value++ else nbackErrors.value++
    }

    /**
     * Precisión de la tarea de reacción.
     *
     * @return Porcentaje 0–100.
     */
    fun reactionPrecision() = if (reactionHits.value + reactionErrors.value == 0) 0
        else ((reactionHits.value.toFloat() / (reactionHits.value + reactionErrors.value)) * 100).toInt()

    /**
     * Precisión de la tarea N-Back.
     *
     * @return Porcentaje 0–100.
     */
    fun nbackPrecision() = if (nbackHits.value + nbackErrors.value == 0) 0
        else ((nbackHits.value.toFloat() / (nbackHits.value + nbackErrors.value)) * 100).toInt()

    /**
     * Score global = media de ambas precisiones.
     *
     * @return Porcentaje 0–100.
     */
    fun globalScore() = (reactionPrecision() + nbackPrecision()) / 2

    /** Alias de [globalScore] para uniformidad con TestResultActions. */
    fun precisionPercent(): Int = globalScore()

    /**
     * Reinicia el test al estado inicial.
     */
    fun resetTest() {
        testState.value       = TestState.IDLE
        reactionTarget.value  = false
        nbackSymbol.value     = null
    }

    /**
     * Finaliza ambas tareas y prepara la pantalla de resultados.
     */
    private fun finishTest() {
        testState.value = TestState.FINISHED
        saveResult()
    }

    /**
     * Guarda el resultado combinado del test junto a la muestra EEG.
     */
    private fun saveResult() {
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg   = eegSource.getLastSample()
            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail    = email,
                    testType     = "dual_task",
                    score        = globalScore().toLong() * 10L,
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
