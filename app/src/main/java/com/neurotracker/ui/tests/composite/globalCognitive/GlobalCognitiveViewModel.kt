package com.neurotracker.ui.tests.composite.globalCognitive

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
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
 * ViewModel del Test Cognitivo Global.
 *
 * Gestiona la lógica de un test compuesto por cuatro fases encadenadas:
 *  1. Reacción simple (Go/No-Go) — 30 segundos.
 *  2. Stroop (color-palabra) — 30 segundos.
 *  3. N-Back 1-atrás — 20 estímulos.
 *  4. Memoria visual secuencial — 5 rondas de longitud creciente (3→7 celdas).
 *
 * El score global es la media aritmética de las precisiones de las 4 fases.
 *
 * Sistema de puntuación — Fase 1 (Reacción):
 *  - Pulsar círculo rojo (GO)    → acierto.
 *  - Pulsar cuadrado azul (NOGO) → error.
 *  - Círculo rojo sin pulsar     → error de omisión.
 *  - Cuadrado azul sin pulsar    → no suma nada.
 *
 * Sistema de puntuación — Fase 2 (Stroop):
 *  - Respuesta correcta → acierto.
 *  - Respuesta incorrecta u omisión → error.
 *
 * Sistema de puntuación — Fase 3 (N-Back):
 *  - Respuesta correcta (SÍ/NO) → acierto.
 *  - Respuesta incorrecta u omisión → error.
 *
 * Sistema de puntuación — Fase 4 (Memoria Visual):
 *  - Secuencia reproducida correctamente → acierto de ronda.
 *  - Secuencia incorrecta o tiempo agotado → error de ronda.
 *
 * Flujo de estados:
 *  IDLE → TRANSITION → PHASE_* → TRANSITION → … → FINISHED
 *
 * @param application Contexto de aplicación para acceder a Room, SessionManager y EEG.
 */
class GlobalCognitiveViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del test global.
     *
     *  - [IDLE]               : pantalla de instrucciones generales.
     *  - [TRANSITION]         : pantalla de transición entre fases.
     *  - [PHASE_REACTION]     : fase 1 activa — reacción simple.
     *  - [PHASE_STROOP]       : fase 2 activa — test de Stroop.
     *  - [PHASE_NBACK]        : fase 3 activa — N-Back 1-atrás.
     *  - [PHASE_VISUAL_MEMORY]: fase 4 activa — memoria visual secuencial.
     *  - [FINISHED]           : test completado, mostrando resultados.
     */
    enum class TestState {
        IDLE, TRANSITION, PHASE_REACTION, PHASE_STROOP, PHASE_NBACK, PHASE_VISUAL_MEMORY, FINISHED
    }

    /**
     * Estímulo de la fase de reacción.
     *
     * @property isTarget true si es GO (círculo rojo — pulsar). false si es NOGO (cuadrado azul — no pulsar).
     * @property shownAt  Marca temporal en ms del momento en que se mostró el estímulo.
     */
    data class ReactionStimulus(val isTarget: Boolean, val shownAt: Long)

    /**
     * Ítem de la fase Stroop.
     *
     * @property text    Palabra mostrada al usuario (ej: "ROJO").
     * @property color   Color de la tinta con que se pinta la palabra.
     * @property isMatch true si el significado de la palabra coincide con el color de la tinta.
     */
    data class StroopItem(val text: String, val color: Color, val isMatch: Boolean)

    private val db             = NeuroTrackerDatabase.getDatabase(application)
    private val eegSource      = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    val totalPhases = 4

    var currentPhase      = mutableStateOf(0);   private set
    var testState         = mutableStateOf(TestState.IDLE); private set
    var transitionMessage = mutableStateOf("");  private set
    var phaseDescription  = mutableStateOf("");  private set
    var reactionScore     = mutableStateOf(0);   private set
    var stroopScore       = mutableStateOf(0);   private set
    var nbackScore        = mutableStateOf(0);   private set
    var visualScore       = mutableStateOf(0);   private set
    var globalScore       = mutableStateOf(0);   private set

    // Fase 1
    var reactionStimulus = mutableStateOf<ReactionStimulus?>(null); private set
    var reactionHits     = mutableStateOf(0);  private set
    var reactionErrors   = mutableStateOf(0);  private set
    var reactionSec      = mutableStateOf(30); private set
    private val reactionTimes          = mutableListOf<Long>()
    private var lastReactionStimulus: ReactionStimulus? = null
    private var reactionPressed        = false

    // Fase 2
    var stroopItem        = mutableStateOf<StroopItem?>(null); private set
    var stroopHits        = mutableStateOf(0);     private set
    var stroopErrors      = mutableStateOf(0);     private set
    var stroopSec         = mutableStateOf(30);    private set
    var stroopAnswered    = mutableStateOf(false); private set
    var stroopLastCorrect = mutableStateOf<Boolean?>(null); private set
    private var stroopShownAt = 0L

    // Fase 3
    var nbackSymbol   = mutableStateOf<String?>(null); private set
    var nbackIndex    = mutableStateOf(0);             private set
    var nbackHits     = mutableStateOf(0);             private set
    var nbackErrors   = mutableStateOf(0);             private set
    var nbackAnswered = mutableStateOf(false);         private set
    private var nbackPrev:    String? = null
    private var nbackCurrent: String? = null
    private val nbackPool = listOf("A","B","C","D","1","2","3","4")

    // Fase 4
    val vmTotalRounds = 5
    var vmRound     = mutableStateOf(0);                      private set
    var vmHighlight = mutableStateOf(-1);                     private set
    var vmUserInput = mutableStateOf<List<Int>>(emptyList()); private set
    var vmSequence  = mutableStateOf<List<Int>>(emptyList()); private set
    var vmPhase     = mutableStateOf("showing");              private set
    var vmHits      = mutableStateOf(0);  private set
    var vmErrors    = mutableStateOf(0);  private set

    private val stroopWords = listOf("ROJO", "VERDE", "AZUL", "NARANJA")
    private val stroopColorPairs = listOf(
        Pair(Color(0xFFC62828), "Rojo"),
        Pair(Color(0xFF2E7D32), "Verde"),
        Pair(Color(0xFF1565C0), "Azul"),
        Pair(Color(0xFFE65100), "Naranja")
    )

    /**
     * Calcula la longitud de la secuencia para una ronda de memoria visual.
     *
     * La progresión es de 3 a 7 celdas: ronda 1 → 3, ronda 2 → 4, …, ronda 5 → 7.
     *
     * @param round Número de ronda (1-indexed).
     * @return Longitud de la secuencia para esa ronda.
     */
    private fun vmLengthForRound(round: Int) = 2 + round

    /**
     * Inicia el test global reiniciando todos los contadores de las 4 fases.
     */
    fun startTest() {
        currentPhase.value = 0
        reactionHits.value = 0; reactionErrors.value = 0; reactionSec.value = 30
        reactionTimes.clear(); lastReactionStimulus = null
        stroopHits.value   = 0; stroopErrors.value   = 0; stroopSec.value = 30
        nbackHits.value    = 0; nbackErrors.value    = 0; nbackIndex.value = 0; nbackPrev = null
        vmRound.value      = 0; vmHits.value         = 0; vmErrors.value   = 0
        viewModelScope.launch { runAllPhases() }
    }

    /**
     * Ejecuta las 4 fases del test secuencialmente.
     *
     * Cada fase va precedida de una pantalla de transición con instrucciones.
     * Al terminar todas las fases calcula el score global y persiste el resultado.
     */
    private suspend fun runAllPhases() {
        showTransition("Fase 1 de $totalPhases — Reacción Simple",
            "Pulsa el CÍRCULO ROJO cuando aparezca.\nIgnora el CUADRADO AZUL.\nTienes 30 segundos.",
            TestState.PHASE_REACTION)
        runReaction()
        reactionScore.value = ratio(reactionHits.value, reactionErrors.value)

        showTransition("Fase 2 de $totalPhases — Test de Stroop",
            "Indica si el COLOR de la tinta coincide con el significado de la palabra.\nPulsa ✓ SÍ o ✗ NO.\nTienes 30 segundos.",
            TestState.PHASE_STROOP)
        runStroop()
        stroopScore.value = ratio(stroopHits.value, stroopErrors.value)

        showTransition("Fase 3 de $totalPhases — N-Back",
            "Aparecerán 20 símbolos uno a uno.\nIndica si el símbolo actual es igual al ANTERIOR.\nPulsa ✓ SÍ o ✗ NO.\nNo responder también cuenta como error.",
            TestState.PHASE_NBACK)
        runNBack()
        nbackScore.value = ratio(nbackHits.value, nbackErrors.value)

        showTransition("Fase 4 de $totalPhases — Memoria Visual",
            "Memoriza qué celdas se iluminan y repite la secuencia.\nSon $vmTotalRounds rondas con longitud creciente: 3, 4, 5, 6 y 7 celdas.",
            TestState.PHASE_VISUAL_MEMORY)
        runVisualMemory()
        visualScore.value = ratio(vmHits.value, vmErrors.value)

        globalScore.value = (reactionScore.value + stroopScore.value + nbackScore.value + visualScore.value) / 4
        testState.value   = TestState.FINISHED
        saveResult()
    }

    /**
     * Calcula la precisión porcentual a partir de aciertos y errores.
     *
     * @param hits   Número de aciertos.
     * @param errors Número de errores.
     * @return Precisión 0–100. Devuelve 0 si no hay intentos.
     */
    private fun ratio(hits: Int, errors: Int): Int {
        val total = hits + errors
        return if (total == 0) 0 else ((hits.toFloat() / total) * 100).toInt()
    }

    /**
     * Muestra la pantalla de transición previa a una fase y espera 4 segundos.
     *
     * @param title       Título de la transición.
     * @param description Instrucciones de la siguiente fase.
     * @param nextState   Estado al que se avanzará al terminar la transición.
     */
    private suspend fun showTransition(title: String, description: String, nextState: TestState) {
        currentPhase.value = when (nextState) {
            TestState.PHASE_REACTION      -> 1
            TestState.PHASE_STROOP        -> 2
            TestState.PHASE_NBACK         -> 3
            TestState.PHASE_VISUAL_MEMORY -> 4
            else                          -> currentPhase.value
        }
        transitionMessage.value = title
        phaseDescription.value  = description
        testState.value         = TestState.TRANSITION
        delay(4000)
        testState.value = nextState
    }

    /**
     * Ejecuta la fase de reacción simple durante 30 segundos.
     *
     * Genera estímulos GO (círculo rojo) y NOGO (cuadrado azul) con 60% de
     * probabilidad para GO. Penaliza los estímulos GO no respondidos.
     */
    private suspend fun runReaction() {
        val start = System.currentTimeMillis()
        reactionSec.value = 30
        while (System.currentTimeMillis() - start < 30_000L) {
            reactionSec.value = (30 - ((System.currentTimeMillis() - start) / 1000)).toInt()
            val stimulus = ReactionStimulus(Random.nextInt(100) < 60, System.currentTimeMillis())
            lastReactionStimulus   = stimulus
            reactionStimulus.value = stimulus
            reactionPressed        = false
            delay(1800)
            if (!reactionPressed && stimulus.isTarget) reactionErrors.value++
            reactionStimulus.value = null
            delay(300)
        }
    }

    /**
     * Registra la pulsación del usuario en la fase de reacción.
     *
     * Usa [lastReactionStimulus] en lugar de [reactionStimulus] para evaluar
     * el tipo del estímulo, evitando que lecturas tardías reciban null cuando
     * la corrutina ya ha limpiado [reactionStimulus].
     *
     * Reglas: GO pulsado → acierto. NOGO pulsado → error.
     */
    fun onReactionPress() {
        if (testState.value != TestState.PHASE_REACTION || reactionPressed) return
        val s = lastReactionStimulus ?: return
        reactionPressed = true
        if (s.isTarget) {
            reactionHits.value++
            reactionTimes.add(System.currentTimeMillis() - s.shownAt)
        } else {
            reactionErrors.value++
        }
    }

    /**
     * Ejecuta la fase Stroop durante 30 segundos.
     *
     * Genera estímulos combinando palabra y color de forma aleatoria.
     * Un estímulo es congruente si los índices de palabra y color coinciden.
     * Penaliza las omisiones.
     */
    private suspend fun runStroop() {
        val start = System.currentTimeMillis()
        stroopSec.value = 30
        while (System.currentTimeMillis() - start < 30_000L) {
            stroopSec.value = (30 - ((System.currentTimeMillis() - start) / 1000)).toInt()
            val wordIndex  = stroopWords.indices.random()
            val colorIndex = stroopColorPairs.indices.random()
            val (color, _) = stroopColorPairs[colorIndex]
            stroopItem.value        = StroopItem(stroopWords[wordIndex], color, wordIndex == colorIndex)
            stroopAnswered.value    = false
            stroopLastCorrect.value = null
            stroopShownAt           = System.currentTimeMillis()
            delay(2000)
            if (!stroopAnswered.value) {
                stroopErrors.value++
                stroopLastCorrect.value = false
            }
        }
        stroopItem.value = null
    }

    /**
     * Registra la respuesta del usuario en la fase Stroop.
     *
     * Actualiza [stroopLastCorrect] para que la Screen muestre feedback
     * visual inmediato (✅ o ❌) tras cada respuesta.
     *
     * @param yes true si el usuario pulsó "SÍ" (la palabra y el color coinciden).
     */
    fun onStroopAnswer(yes: Boolean) {
        if (testState.value != TestState.PHASE_STROOP || stroopAnswered.value) return
        stroopAnswered.value = true
        val correct = yes == stroopItem.value?.isMatch
        if (correct) stroopHits.value++ else stroopErrors.value++
        stroopLastCorrect.value = correct
    }

    /**
     * Ejecuta la fase N-Back con 20 estímulos.
     *
     * A partir del segundo estímulo, cualquier omisión se contabiliza como
     * error independientemente de si la respuesta correcta era SÍ o NO.
     */
    private suspend fun runNBack() {
        nbackPrev = null
        repeat(20) { index ->
            val symbol          = nbackPool.random()
            nbackCurrent        = symbol
            nbackSymbol.value   = symbol
            nbackIndex.value    = index + 1
            nbackAnswered.value = false
            delay(2000)
            if (index > 0 && !nbackAnswered.value) nbackErrors.value++
            nbackPrev         = symbol
            nbackSymbol.value = null
            delay(200)
        }
    }

    /**
     * Registra una respuesta afirmativa en la fase N-Back.
     *
     * Acierto si el símbolo actual coincide con el anterior, error si no.
     */
    fun onNBackYes() {
        if (testState.value != TestState.PHASE_NBACK || nbackAnswered.value || nbackIndex.value <= 1) return
        nbackAnswered.value = true
        if (nbackCurrent == nbackPrev) nbackHits.value++ else nbackErrors.value++
    }

    /**
     * Registra una respuesta negativa en la fase N-Back.
     *
     * Acierto si el símbolo actual es distinto al anterior, error si coinciden.
     */
    fun onNBackNo() {
        if (testState.value != TestState.PHASE_NBACK || nbackAnswered.value || nbackIndex.value <= 1) return
        nbackAnswered.value = true
        if (nbackCurrent != nbackPrev) nbackHits.value++ else nbackErrors.value++
    }

    /**
     * Ejecuta la fase de memoria visual con [vmTotalRounds] rondas.
     *
     * En cada ronda muestra la secuencia de celdas, espera la entrada del
     * usuario con límite de tiempo y evalúa si la reproducción es correcta.
     */
    private suspend fun runVisualMemory() {
        vmRound.value  = 0
        vmHits.value   = 0
        vmErrors.value = 0
        repeat(vmTotalRounds) { roundIndex ->
            vmRound.value     = roundIndex + 1
            val length        = vmLengthForRound(roundIndex + 1)
            val seq           = (0..8).shuffled().take(length)
            vmSequence.value  = seq
            vmUserInput.value = emptyList()
            vmPhase.value     = "showing"
            delay(500)
            seq.forEach { cell -> vmHighlight.value = cell; delay(600); vmHighlight.value = -1; delay(300) }
            vmPhase.value     = "input"
            val timeLimit     = 10_000L + (length - 3) * 2000L
            val inputStart    = System.currentTimeMillis()
            while (vmUserInput.value.size < seq.size && System.currentTimeMillis() - inputStart < timeLimit) {
                delay(100)
            }
            if (vmUserInput.value == seq) vmHits.value++ else vmErrors.value++
            delay(500)
        }
        vmHighlight.value = -1
    }

    /**
     * Registra la pulsación de una celda en la fase de memoria visual.
     *
     * Solo acepta entradas durante la fase "input". Ignora entradas cuando
     * la secuencia ya está completa.
     *
     * @param cell Índice de la celda pulsada (0–8 en una cuadrícula 3×3).
     */
    fun onVmCellPressed(cell: Int) {
        if (testState.value != TestState.PHASE_VISUAL_MEMORY || vmPhase.value != "input") return
        if (vmUserInput.value.size >= vmSequence.value.size) return
        vmUserInput.value = vmUserInput.value + cell
    }

    /**
     * Guarda el resultado final del test en Room con datos EEG simulados.
     */
    private fun saveResult() {
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg   = eegSource.getLastSample()
            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail    = email,
                    testType     = "global_cognitive",
                    score        = globalScore.value.toLong() * 10L,
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
     * Devuelve la precisión global del test.
     *
     * @return Score global en porcentaje entero (0–100).
     */
    fun precisionPercent(): Int = globalScore.value

    /**
     * Reinicia el test al estado inicial para permitir repetirlo.
     */
    fun resetTest() { testState.value = TestState.IDLE }
}