package com.neurotracker.ui.tests.memory.visualMemory

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
 * ViewModel para el Test de Memoria Visual.
 *
 * Evalúa la memoria visual a corto plazo mediante el juego de secuencias.
 * El usuario debe memorizar una secuencia de celdas que se iluminan y luego repetirla.
 * La secuencia crece con cada acierto (longitud +1).
 *
 * Estados del test:
 *  - IDLE: Pantalla de instrucciones, esperando inicio.
 *  - SHOWING: Muestra la secuencia iluminando celdas una por una.
 *  - INPUT: Usuario introduce la secuencia pulsando las celdas.
 *  - FEEDBACK: Retroalimentación visual de acierto/error.
 *  - FINISHED: Test completado, mostrando la longitud máxima alcanzada.
 *
 * @param application Contexto de la aplicación.
 */
class VisualMemoryViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del flujo del test.
     */
    enum class TestState { IDLE, SHOWING, INPUT, FEEDBACK, FINISHED }

    private val db = NeuroTrackerDatabase.getDatabase(application)

    private val eegSource = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    private val gridSize = 9

    /** Estado actual del test. */
    var testState       = mutableStateOf(TestState.IDLE);               private set
    /** Índice de la celda actualmente iluminada (-1 si ninguna). */
    var highlightedCell = mutableStateOf(-1);                           private set
    /** Secuencia introducida por el usuario hasta el momento. */
    var userInput       = mutableStateOf<List<Int>>(emptyList());       private set
    /** Secuencia que el usuario debe memorizar. */
    var sequence        = mutableStateOf<List<Int>>(emptyList());       private set
    /** Longitud actual de la secuencia (ronda actual). */
    var currentLength   = mutableStateOf(3);                            private set
    /** Longitud máxima alcanzada durante el test. */
    var maxLength       = mutableStateOf(0);                            private set
    /** Indica si la última respuesta fue correcta (para feedback visual). */
    var feedbackCorrect = mutableStateOf<Boolean?>(null);               private set

    /**
     * Inicia el test, reseteando la longitud a 3 y comenzando la primera secuencia.
     */
    fun startTest() {
        currentLength.value = 3; maxLength.value = 0
        sequence.value = emptyList(); userInput.value = emptyList()
        testState.value = TestState.SHOWING
        generateAndShowSequence()
    }

    /**
     * Genera una nueva secuencia añadiendo un elemento aleatorio y la muestra al usuario.
     */
    private fun generateAndShowSequence() {
        val newSeq = if (sequence.value.isEmpty()) {
            List(currentLength.value) { Random.nextInt(0, gridSize) }
        } else {
            sequence.value + Random.nextInt(0, gridSize)
        }
        sequence.value = newSeq
        userInput.value = emptyList()

        viewModelScope.launch {
            delay(600)
            newSeq.forEach { cell ->
                highlightedCell.value = cell; delay(600)
                highlightedCell.value = -1;   delay(300)
            }
            testState.value = TestState.INPUT
        }
    }

    /**
     * Maneja la pulsación de una celda en la fase INPUT.
     *
     * @param cellIndex Índice de la celda pulsada (0-8).
     */
    fun onCellPressed(cellIndex: Int) {
        if (testState.value != TestState.INPUT) return
        val updated = userInput.value + cellIndex
        userInput.value = updated

        val expectedIndex = updated.size - 1
        if (updated[expectedIndex] != sequence.value[expectedIndex]) {
            showFeedback(correct = false)
            return
        }
        if (updated.size == sequence.value.size) showFeedback(correct = true)
    }

    /**
     * Muestra feedback visual y decide si continuar o finalizar.
     *
     * @param correct Indica si la respuesta fue correcta.
     */
    private fun showFeedback(correct: Boolean) {
        feedbackCorrect.value = correct
        testState.value = TestState.FEEDBACK

        viewModelScope.launch {
            delay(1000)
            if (correct) {
                if (currentLength.value > maxLength.value) maxLength.value = currentLength.value
                currentLength.value++
                feedbackCorrect.value = null
                testState.value = TestState.SHOWING
                generateAndShowSequence()
            } else {
                feedbackCorrect.value = null
                finishTest()
            }
        }
    }

    /**
     * Finaliza el test, cambia al estado FINISHED y guarda el resultado.
     */
    private fun finishTest() {
        testState.value = TestState.FINISHED
        saveResult()
    }

    /**
     * Calcula el porcentaje de precisión basado en la longitud máxima alcanzada.
     *
     * @return Porcentaje de precisión (0-100), donde 8 es la longitud máxima esperada.
     */
    fun precisionPercent(): Int {
        return ((maxLength.value.toFloat() / 8f) * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Guarda el resultado del test en la base de datos Room,
     * asociado al usuario actual y con los datos EEG simulados.
     */
    private fun saveResult() {
        if (maxLength.value == 0) return
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg = eegSource.getLastSample()
            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail = email,
                    testType = "visual_memory",
                    score = maxLength.value.toLong() * 100L,
                    precisionPct = precisionPercent(),
                    eegAlpha =  eeg.alpha,
                    eegBeta   = eeg.beta,
                    eegGamma  = eeg.gamma,
                    eegTheta  = eeg.theta)
            )
        }
    }

    /**
     * Resetea el test completamente.
     */
    fun resetTest() {
        testState.value = TestState.IDLE
        sequence.value = emptyList(); userInput.value = emptyList()
        highlightedCell.value = -1; feedbackCorrect.value = null
        currentLength.value = 3; maxLength.value = 0
    }
}