package com.neurotracker.ui.tests.executive.tasksWitching

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

/**
 * ViewModel del Test de Cambio de Tarea (Task Switching).
 *
 * ─── Nota daltonismo — colores del estímulo ───────────────────────────────────
 *
 * Problema original: los colores usados eran saturados directos de Compose
 * (Color.Red, Color.Blue, Color.Green) que no tienen contraste suficiente
 * sobre fondo claro y no son distinguibles en deuteranopia.
 *
 * Solución: se sustituyen por colores oscuros de alto contraste:
 *  - Rojo   : Color(0xFFC62828) — rojo oscuro WCAG AA.
 *  - Azul   : Color(0xFF1565C0) — azul oscuro WCAG AA.
 *  - Verde  : Color(0xFF2E7D32) — verde oscuro WCAG AA.
 *  - Naranja: Color(0xFFE65100) — naranja oscuro WCAG AA.
 *    (Sustituye amarillo, que no tiene contraste sobre fondo blanco.)
 *
 * Además, [Stimulus.colorName] es el nombre del color que la pantalla muestra
 * como texto en la instrucción de regla COLOR, eliminando la dependencia
 * exclusiva del color para la tarea.
 *
 * @param application Contexto de aplicación para Room y SessionManager.
 */
class TaskSwitchingViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del flujo del test.
     */
    enum class TestState { IDLE, RUNNING, FINISHED }

    /**
     * Reglas cognitivas del test.
     *
     *  - [COLOR]: el usuario responde con el nombre del color del estímulo.
     *  - [SHAPE]: el usuario responde con el nombre de la forma del estímulo.
     */
    enum class Rule { COLOR, SHAPE }

    /**
     * Modelo de un estímulo visual para el test.
     *
     * @param shape     Nombre de la forma en español ("Círculo", "Cuadrado").
     * @param color     Color visual para renderizar la figura.
     * @param colorName Nombre del color en español ("Rojo", "Azul", "Verde", "Naranja").
     *                  Se usa en la respuesta de la regla COLOR y como etiqueta accesible.
     */
    data class Stimulus(val shape: String, val color: Color, val colorName: String)

    private val db             = NeuroTrackerDatabase.getDatabase(application)
    private val eegSource      = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    private val totalRounds        = 20
    private val stimulusDurationMs = 3000L

    /**
     * Paleta de colores accesibles para daltónicos.
     *
     * Nota: sustituye Color.Red/Green/Blue/Yellow (saturados) por versiones
     * oscuras de alto contraste. Naranja sustituye amarillo que no tiene
     * contraste suficiente sobre fondos claros.
     */
    val colors = listOf(
        Pair(Color(0xFFC62828), "Rojo"),
        Pair(Color(0xFF1565C0), "Azul"),
        Pair(Color(0xFF2E7D32), "Verde"),
        Pair(Color(0xFFE65100), "Naranja")
    )

    val shapes = listOf("Círculo", "Cuadrado")

    var testState       = mutableStateOf(TestState.IDLE);    private set
    var currentRule     = mutableStateOf(Rule.COLOR);        private set
    var currentStimulus = mutableStateOf<Stimulus?>(null);   private set
    var currentRound    = mutableStateOf(0);                 private set
    var hits            = mutableStateOf(0);                 private set
    var errors          = mutableStateOf(0);                 private set
    var hasAnswered     = mutableStateOf(false);             private set
    var options         = mutableStateOf<List<String>>(emptyList()); private set
    var correctAnswer   = mutableStateOf("");                private set

    private val reactionTimes = mutableListOf<Long>()
    private var shownAt       = 0L
    private var advanceNow    = false

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
     * Ejecuta las 20 rondas con alternancia de regla cada 4 rondas.
     */
    private suspend fun runRounds() {
        repeat(totalRounds) { index ->
            advanceNow = false

            currentRule.value = if ((index / 4) % 2 == 0) Rule.COLOR else Rule.SHAPE

            val colorPair = colors.random()
            val shape     = shapes.random()
            currentStimulus.value = Stimulus(shape, colorPair.first, colorPair.second)
            currentRound.value    = index + 1
            hasAnswered.value     = false

            if (currentRule.value == Rule.COLOR) {
                correctAnswer.value = colorPair.second
                options.value       = colors.map { it.second }.shuffled()
            } else {
                correctAnswer.value = shape
                options.value       = shapes.shuffled()
            }

            shownAt = System.currentTimeMillis()

            val deadline = System.currentTimeMillis() + stimulusDurationMs
            while (!advanceNow && System.currentTimeMillis() < deadline) {
                delay(50)
            }

            if (!hasAnswered.value) errors.value++
            delay(300)
        }
        finishTest()
    }

    /**
     * Registra la opción seleccionada por el usuario.
     *
     * @param option Opción pulsada (nombre de color o forma).
     */
    fun onOptionSelected(option: String) {
        if (testState.value != TestState.RUNNING || hasAnswered.value) return
        hasAnswered.value = true
        reactionTimes.add(System.currentTimeMillis() - shownAt)
        if (option == correctAnswer.value) hits.value++ else errors.value++
        advanceNow = true
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
                    testType     = "task_switching",
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
        testState.value       = TestState.IDLE
        currentStimulus.value = null
        hits.value            = 0; errors.value = 0
    }
}
