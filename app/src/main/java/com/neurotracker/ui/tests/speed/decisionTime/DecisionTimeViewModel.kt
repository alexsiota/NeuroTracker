package com.neurotracker.ui.tests.speed.decisionTime

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

/**
 * ViewModel del Test de Tiempo de Decisión.
 *
 * ─── Nota: condición de carrera entre omisión y respuesta tardía ───────────────
 *
 * Problema original: cuando el timeout de 3s se agotaba (omisión), la corrutina
 * ejecutaba `errors.value++` y avanzaba de ronda. Pero si el usuario pulsaba
 * exactamente en ese instante (antes de que la UI desapareciera), [onOptionSelected]
 * también contabilizaba un acierto o error adicional porque [hasAnswered] todavía
 * era false en el hilo de UI.
 *
 * Consecuencia: una ronda podía contabilizar un error de omisión Y un acierto
 * simultáneamente, inflando artificialmente las métricas.
 *
 * Solución:
 *  - [hasAnswered] pasa a ser un flag `@Volatile` privado en lugar de un
 *    [mutableStateOf], garantizando visibilidad inmediata entre hilos.
 *  - En el bucle de polling, se actualiza [hasAnswered] = true en el mismo
 *    instante en que se decide la omisión, ANTES de `errors.value++`.
 *    Esto bloquea cualquier llamada concurrente a [onOptionSelected].
 *  - [answeredUi] es el equivalente observable para la UI (deshabilitar botones).
 *
 * @param application Contexto de aplicación para Room y SessionManager.
 */
class DecisionTimeViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del flujo del test.
     */
    enum class TestState { IDLE, RUNNING, FINISHED }

    /**
     * Estímulo de una ronda del test.
     *
     * @param options      Lista de 4 opciones numéricas como cadenas.
     * @param correctIndex Índice de la opción correcta (0–3).
     * @param rule         Regla aplicada ("Mayor", "Menor", "Par", "Impar").
     */
    data class DecisionItem(
        val options:      List<String>,
        val correctIndex: Int,
        val rule:         String
    )

    private val db             = NeuroTrackerDatabase.getDatabase(application)
    private val eegSource      = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    companion object {
        private const val TOTAL_ROUNDS        = 15
        private const val STIMULUS_DURATION_MS = 3000L
    }

    var testState     = mutableStateOf(TestState.IDLE);      private set
    var currentItem   = mutableStateOf<DecisionItem?>(null); private set
    var currentRound  = mutableStateOf(0);                   private set
    var hits          = mutableStateOf(0);                   private set
    var errors        = mutableStateOf(0);                   private set
    var selectedIndex = mutableStateOf<Int?>(null);          private set

    /**
     * Estado observable de "ya respondido" para la UI.
     *
     * Se usa para deshabilitar los botones visualmente. No es el flag de
     * control de concurrencia (ese es [hasAnswered]).
     */
    var answeredUi = mutableStateOf(false); private set

    private val reactionTimes = mutableListOf<Long>()
    private var shownAt       = 0L

    /**
     * Flag de control de concurrencia.
     *
     * Nota: `@Volatile` garantiza que cualquier hilo (UI o corrutina) que
     * lo lea obtenga el valor más reciente, sin caché de CPU local.
     * Se activa en [onOptionSelected] Y en el timeout del bucle de polling,
     * en ambos casos ANTES de modificar los contadores.
     */
    @Volatile
    private var hasAnswered = false

    /** Flag para avanzar la corrutina tras respuesta o timeout. */
    @Volatile
    private var advanceNow = false

    /**
     * Inicia el test desde cero.
     */
    fun startTest() {
        hits.value = 0; errors.value = 0; currentRound.value = 0
        reactionTimes.clear()
        testState.value = TestState.RUNNING
        viewModelScope.launch { runRounds() }
    }

    /**
     * Ejecuta las [TOTAL_ROUNDS] rondas secuencialmente.
     *
     * Nota: el timeout ahora marca [hasAnswered] = true ANTES de contabilizar
     * la omisión, bloqueando cualquier llamada concurrente a [onOptionSelected].
     */
    private suspend fun runRounds() {
        repeat(TOTAL_ROUNDS) {
            advanceNow        = false
            hasAnswered       = false
            answeredUi.value  = false
            selectedIndex.value = null

            val item = generateItem()
            currentItem.value  = item
            currentRound.value++
            shownAt = System.currentTimeMillis()

            val deadline = System.currentTimeMillis() + STIMULUS_DURATION_MS
            while (!advanceNow && System.currentTimeMillis() < deadline) {
                delay(50)
            }

            // Nota: marcar como respondido ANTES de contabilizar omisión
            // para bloquear llamadas concurrentes desde la UI
            if (!hasAnswered) {
                hasAnswered      = true
                answeredUi.value = true
                errors.value++   // omisión
            }

            delay(300)
        }
        finishTest()
    }

    /**
     * Registra la respuesta del usuario.
     *
     * Nota: la primera línea comprueba el flag `@Volatile` [hasAnswered].
     * Si ya fue marcado por el timeout en la corrutina, retorna inmediatamente
     * sin contabilizar nada, eliminando la condición de carrera.
     *
     * @param index Índice de la opción pulsada (0–3).
     */
    fun onOptionSelected(index: Int) {
        // Nota: comprobación atómica del flag @Volatile
        if (testState.value != TestState.RUNNING || hasAnswered) return

        // Marcar respondido ANTES de modificar contadores
        hasAnswered       = true
        answeredUi.value  = true
        selectedIndex.value = index
        reactionTimes.add(System.currentTimeMillis() - shownAt)

        if (index == currentItem.value?.correctIndex) hits.value++
        else                                          errors.value++

        advanceNow = true
    }

    /**
     * Genera un estímulo con exactamente una respuesta correcta.
     *
     * Reglas: "Mayor" (número más grande), "Menor" (número más pequeño),
     * "Par" (único número par entre los 4), "Impar" (único número impar).
     */
    private fun generateItem(): DecisionItem {
        return when (val rule = listOf("Mayor", "Menor", "Par", "Impar").random()) {
            "Mayor", "Menor" -> {
                val numbers      = (1..99).shuffled().take(4).map { it.toString() }
                val correctIndex = if (rule == "Mayor")
                    numbers.indices.maxByOrNull { numbers[it].toInt() }!!
                else
                    numbers.indices.minByOrNull { numbers[it].toInt() }!!
                DecisionItem(numbers, correctIndex, rule)
            }
            "Par" -> {
                val even   = (2..98 step 2).toList().random()
                val odds   = (1..99 step 2).toList().shuffled().take(3)
                val all    = (listOf(even) + odds).map { it.toString() }.shuffled()
                val correct = all.indexOfFirst { it.toInt() % 2 == 0 }
                DecisionItem(all, correct, rule)
            }
            else -> { // Impar
                val odd    = (1..99 step 2).toList().random()
                val evens  = (2..98 step 2).toList().shuffled().take(3)
                val all    = (listOf(odd) + evens).map { it.toString() }.shuffled()
                val correct = all.indexOfFirst { it.toInt() % 2 != 0 }
                DecisionItem(all, correct, rule)
            }
        }
    }

    /**
     * Calcula el porcentaje de precisión.
     *
     * @return Precisión 0–100. 0 si no hay intentos.
     */
    fun precisionPercent() = if (hits.value + errors.value == 0) 0
        else ((hits.value.toFloat() / (hits.value + errors.value)) * 100).toInt()

    /**
     * Finaliza el test y persiste el resultado en Room.
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
                    testType     = "decision_time",
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
     * Reinicia el test al estado inicial.
     */
    fun resetTest() {
        testState.value    = TestState.IDLE
        currentItem.value  = null
        hits.value         = 0; errors.value = 0
        answeredUi.value   = false
        selectedIndex.value = null
    }
}
