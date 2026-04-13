package com.neurotracker.ui.tests.memory.associativeMemory

/**
 * ViewModel del Test de Memoria Asociativa.
 *
 * Evalúa la capacidad del usuario para formar y recuperar asociaciones
 * entre estímulos visuales (en este caso, pares de emojis).
 *
 * Diseño del test:
 *  - Fase de MEMORIZACIÓN: el usuario observa pares de emojis durante un tiempo limitado.
 *  - Fase de RECALL: se muestra un emoji (cue) y el usuario debe seleccionar su pareja correcta.
 *  - Fase de FEEDBACK: se informa brevemente si la respuesta fue correcta o incorrecta.
 *
 * Flujo del test:
 *  IDLE → MEMORIZE → RECALL → FEEDBACK → FINISHED
 *
 * Métricas evaluadas:
 *  - hits: asociaciones correctamente recordadas
 *  - errors: asociaciones incorrectas
 *  - tiempo de reacción medio
 *  - precisión (%)
 *
 * Características cognitivas evaluadas:
 *  - Memoria asociativa
 *  - Codificación y recuperación de información
 *  - Reconocimiento con distractores
 *
 * Persistencia:
 *  - Los resultados se almacenan en Room
 *  - Se incluyen datos EEG simulados
 *
 * @param application Contexto global de la aplicación
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

class AssociativeMemoryViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados del test.
     */
    enum class TestState {
        IDLE,       // Pantalla inicial
        MEMORIZE,   // Visualización de pares
        RECALL,     // Elección de la pareja correcta
        FEEDBACK,   // Resultado inmediato
        FINISHED    // Test finalizado
    }

    /**
     * Representa un par de elementos asociados.
     *
     * @param first  Primer elemento (cue)
     * @param second Segundo elemento (respuesta correcta)
     */
    data class EmojiPair(val first: String, val second: String)

    /**
     * Representa una pregunta de recuperación.
     *
     * @param cue Emoji que actúa como pista
     * @param correctAnswer Respuesta correcta asociada
     * @param options Lista de opciones (1 correcta + 2 distractores)
     */
    data class RecallQuestion(
        val cue: String,
        val correctAnswer: String,
        val options: List<String>
    )

    private val db = NeuroTrackerDatabase.getDatabase(application)

    private val eegSource = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    /**
     * Configuración del test.
     */
    private val memorizeSec = 6
    private val pairCount   = 5

    /**
     * Pool de emojis para generar asociaciones.
     */
    private val emojiPool = listOf(
        "🍎", "🐶", "🚗", "🏠", "🌟", "🎸", "📚", "🌈",
        "🍕", "🦁", "✈️", "🎯", "🌙", "🔑", "🎨", "🐠",
        "🍦", "🎃", "🏆", "🌺", "🎲", "🦋", "🚀", "🍓",
        "🐸", "🎹", "🌍", "🧩", "🎤", "🦊"
    )

    /**
     * Estado general del test.
     */
    var testState = mutableStateOf(TestState.IDLE)
        private set

    /**
     * Lista de pares generados para memorizar.
     */
    var pairs = mutableStateOf<List<EmojiPair>>(emptyList())
        private set

    /**
     * Cuenta atrás de la fase de memorización.
     */
    var remainingMemorizeSec = mutableStateOf(memorizeSec)
        private set

    /**
     * Pregunta actual de recuperación.
     */
    var currentQuestion = mutableStateOf<RecallQuestion?>(null)
        private set

    /**
     * Índice de la pregunta actual.
     */
    var currentQuestionIndex = mutableStateOf(0)
        private set

    /**
     * Respuesta seleccionada por el usuario.
     */
    var selectedAnswer = mutableStateOf<String?>(null)
        private set

    /**
     * Resultado de la respuesta (true = correcto, false = incorrecto).
     */
    var feedbackCorrect = mutableStateOf<Boolean?>(null)
        private set

    /**
     * Métricas de rendimiento.
     */
    var hits = mutableStateOf(0)
        private set

    var errors = mutableStateOf(0)
        private set

    /**
     * Tiempos de reacción registrados.
     */
    private val reactionTimes = mutableListOf<Long>()

    /**
     * Momento en que se muestra la pregunta.
     */
    private var questionShownAt = 0L

    /**
     * Lista interna de preguntas generadas.
     */
    private var questions = listOf<RecallQuestion>()

    // ─────────────────────────────────────────────
    // INICIO DEL TEST
    // ─────────────────────────────────────────────

    /**
     * Inicializa el test y genera los pares de asociación.
     */
    fun startTest() {
        resetInternal()

        val shuffled = emojiPool.shuffled()

        val generated = mutableListOf<EmojiPair>()
        for (i in 0 until pairCount) {
            generated.add(EmojiPair(shuffled[i * 2], shuffled[i * 2 + 1]))
        }

        pairs.value = generated
        testState.value = TestState.MEMORIZE

        viewModelScope.launch {
            remainingMemorizeSec.value = memorizeSec

            while (remainingMemorizeSec.value > 0) {
                delay(1000)
                remainingMemorizeSec.value--
            }

            questions = buildQuestions(generated, shuffled)
            currentQuestionIndex.value = 0
            showNextQuestion()
        }
    }

    // ─────────────────────────────────────────────
    // GENERACIÓN DE PREGUNTAS
    // ─────────────────────────────────────────────

    /**
     * Genera las preguntas de recuperación con distractores.
     */
    private fun buildQuestions(
        pairs: List<EmojiPair>,
        allEmojis: List<String>
    ): List<RecallQuestion> {
        return pairs.map { pair ->

            val distractors = allEmojis
                .filter { it != pair.first && it != pair.second }
                .shuffled()
                .take(2)

            val options = (listOf(pair.second) + distractors).shuffled()

            RecallQuestion(
                cue           = pair.first,
                correctAnswer = pair.second,
                options       = options
            )
        }.shuffled()
    }

    // ─────────────────────────────────────────────
    // FLUJO DE PREGUNTAS
    // ─────────────────────────────────────────────

    /**
     * Muestra la siguiente pregunta o finaliza el test.
     */
    private fun showNextQuestion() {
        if (currentQuestionIndex.value >= questions.size) {
            finishTest()
            return
        }

        currentQuestion.value = questions[currentQuestionIndex.value]
        selectedAnswer.value  = null
        feedbackCorrect.value = null
        questionShownAt       = System.currentTimeMillis()

        testState.value = TestState.RECALL
    }

    /**
     * Maneja la selección del usuario.
     */
    fun onAnswerSelected(answer: String) {
        if (testState.value != TestState.RECALL) return
        if (selectedAnswer.value != null) return

        selectedAnswer.value = answer

        reactionTimes.add(System.currentTimeMillis() - questionShownAt)

        val correct = answer == currentQuestion.value?.correctAnswer
        feedbackCorrect.value = correct

        if (correct) hits.value++ else errors.value++

        testState.value = TestState.FEEDBACK

        viewModelScope.launch {
            delay(900)
            currentQuestionIndex.value++
            showNextQuestion()
        }
    }

    // ─────────────────────────────────────────────
    // FINALIZACIÓN Y RESULTADOS
    // ─────────────────────────────────────────────

    /**
     * Finaliza el test.
     */
    private fun finishTest() {
        testState.value = TestState.FINISHED
        saveResult()
    }

    /**
     * Guarda el resultado en la base de datos.
     */
    private fun saveResult() {
        val email = sessionManager.getUserEmail() ?: return

        val avgReaction = if (reactionTimes.isNotEmpty())
            reactionTimes.average().toLong()
        else 9999L

        val eeg = eegSource.getLastSample()

        viewModelScope.launch {
            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail = email,
                    testType  = "associative_memory",
                    score     = avgReaction,
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
     * Calcula el porcentaje de precisión.
     */
    fun precisionPercent(): Int {
        val total = hits.value + errors.value
        return if (total == 0) 0
        else ((hits.value.toFloat() / total.toFloat()) * 100).toInt()
    }

    /**
     * Número total de preguntas generadas.
     */
    fun totalQuestions() = questions.size

    /**
     * Reinicia el test.
     */
    fun resetTest() {
        resetInternal()
        testState.value = TestState.IDLE
    }

    /**
     * Limpia el estado interno.
     */
    private fun resetInternal() {
        pairs.value                = emptyList()
        currentQuestion.value      = null
        currentQuestionIndex.value = 0
        selectedAnswer.value       = null
        feedbackCorrect.value      = null
        hits.value                 = 0
        errors.value               = 0
        reactionTimes.clear()
        remainingMemorizeSec.value = memorizeSec
        questions                  = emptyList()
    }
}