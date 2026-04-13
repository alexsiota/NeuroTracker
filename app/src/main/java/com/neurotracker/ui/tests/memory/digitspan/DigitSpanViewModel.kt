package com.neurotracker.ui.tests.memory.digitspan

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
 * ViewModel para el Test de Memoria de Dígitos (Digit Span).
 *
 * Evalúa la memoria a corto plazo mediante la repetición de secuencias numéricas.
 * La secuencia crece con cada acierto (span +1) hasta que el usuario falla.
 *
 * Estados del test:
 *  - IDLE: Pantalla de instrucciones, esperando inicio.
 *  - SHOWING: Muestra la secuencia dígito por dígito.
 *  - INPUT: Usuario introduce la secuencia mediante teclado numérico.
 *  - FEEDBACK: Retroalimentación visual de acierto/error.
 *  - FINISHED: Test completado, mostrando el span máximo alcanzado.
 *
 * @param application Contexto de la aplicación.
 */
class DigitSpanViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del flujo del test.
     */
    enum class TestState { IDLE, SHOWING, INPUT, FEEDBACK, FINISHED }

    private val db = NeuroTrackerDatabase.getDatabase(application)

    private val eegSource = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    /** Estado actual del test. */
    var testState      = mutableStateOf(TestState.IDLE);    private set
    /** Dígito actualmente visible durante la fase SHOWING. */
    var currentDigit   = mutableStateOf<Int?>(null);        private set
    /** Secuencia de dígitos que el usuario debe memorizar. */
    var sequence       = mutableStateOf<List<Int>>(emptyList()); private set
    /** Dígitos introducidos por el usuario hasta el momento. */
    var userInput      = mutableStateOf<List<Int>>(emptyList()); private set
    /** Longitud actual de la secuencia (span actual). */
    var currentSpan    = mutableStateOf(3);                 private set
    /** Span máximo alcanzado durante el test. */
    var maxSpan        = mutableStateOf(0);                 private set
    /** Indica si la última respuesta fue correcta (para feedback visual). */
    var feedbackCorrect = mutableStateOf<Boolean?>(null);   private set

    /**
     * Inicia el test, reseteando el span a 3 y comenzando la primera secuencia.
     */
    fun startTest() {
        currentSpan.value = 3; maxSpan.value = 0
        userInput.value = emptyList()
        testState.value = TestState.SHOWING
        generateAndShowSequence()
    }

    /**
     * Genera una secuencia aleatoria de dígitos y la muestra al usuario.
     */
    private fun generateAndShowSequence() {
        val newSeq = List(currentSpan.value) { Random.nextInt(1, 10) }
        sequence.value = newSeq
        userInput.value = emptyList()

        viewModelScope.launch {
            delay(600)
            newSeq.forEach { digit ->
                currentDigit.value = digit; delay(800)
                currentDigit.value = null;  delay(300)
            }
            testState.value = TestState.INPUT
        }
    }

    /**
     * Maneja la pulsación de un dígito en el teclado numérico.
     *
     * @param digit Dígito pulsado (0-9).
     */
    fun onDigitPressed(digit: Int) {
        if (testState.value != TestState.INPUT) return
        val updated = userInput.value + digit
        userInput.value = updated
        if (updated.size == sequence.value.size) validateInput(updated)
    }

    /**
     * Elimina el último dígito introducido por el usuario.
     */
    fun onBackspace() {
        if (testState.value != TestState.INPUT || userInput.value.isEmpty()) return
        userInput.value = userInput.value.dropLast(1)
    }

    /**
     * Valida la entrada del usuario contra la secuencia original.
     *
     * @param input Secuencia introducida por el usuario.
     */
    private fun validateInput(input: List<Int>) {
        val correct = input == sequence.value
        feedbackCorrect.value = correct
        testState.value = TestState.FEEDBACK

        viewModelScope.launch {
            delay(1000)
            if (correct) {
                if (currentSpan.value > maxSpan.value) maxSpan.value = currentSpan.value
                currentSpan.value++
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
     * Finaliza el test, guarda el resultado en la base de datos.
     */
    private fun finishTest() {
        testState.value = TestState.FINISHED
        saveResult()
    }

    /**
     * Calcula el porcentaje de precisión basado en el span máximo alcanzado.
     *
     * @return Porcentaje de precisión (0-100), donde 12 es el span máximo teórico.
     */
    fun precisionPercent(): Int {
        return ((maxSpan.value.toFloat() / 12f) * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Guarda el resultado del test en la base de datos Room,
     * asociado al usuario actual y con los datos EEG simulados.
     */
    private fun saveResult() {
        if (maxSpan.value == 0) return
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg = eegSource.getLastSample()
            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail = email,
                    testType = "digit_span",
                    score = maxSpan.value.toLong() * 100L,
                    precisionPct = precisionPercent(),
                    eegAlpha  = eeg.alpha,
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
        currentDigit.value = null; feedbackCorrect.value = null
        currentSpan.value = 3; maxSpan.value = 0
    }
}