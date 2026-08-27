package com.neurotracker.ui.tests.memory.objectMemory

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
 * ViewModel para el Test de Memoria de Objetos.
 *
 * Evalúa la memoria visual episódica mediante el reconocimiento de objetos.
 * El usuario memoriza 6 objetos durante 5 segundos, luego debe identificarlos
 * entre 12 objetos (6 originales + 6 distractores).
 *
 * Estados del test:
 *  - IDLE: Pantalla de instrucciones, esperando inicio.
 *  - MEMORIZE: Muestra los 6 objetos a memorizar con temporizador de 5 segundos.
 *  - RECALL: Muestra 12 objetos (originales + distractores) para seleccionar.
 *  - FINISHED: Test completado, mostrando resultados.
 *
 * Métricas:
 *  - hits: objetos correctamente identificados (verdaderos positivos)
 *  - misses: objetos originales no seleccionados (falsos negativos)
 *  - falseAlarms: objetos no originales seleccionados por error (falsos positivos)
 *
 * @param application Contexto de la aplicación.
 */
class ObjectMemoryViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del flujo del test.
     */
    enum class TestState { IDLE, MEMORIZE, RECALL, FINISHED }

    private val db = NeuroTrackerDatabase.getDatabase(application)

    private val eegSource = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    private val objectPool = listOf(
        "🍎","🐶","🚗","🏠","🌟","🎸","📚","🌈",
        "🍕","🦁","✈️","🎯","🌙","🔑","🎨","🐠",
        "🍦","🎃","🏆","🌺","🎲","🦋","🚀","🍓"
    )

    private val targetCount  = 6
    private val totalShown   = 12
    private val memorizeSec  = 5

    /** Estado actual del test. */
    var testState              = mutableStateOf(TestState.IDLE);           private set
    /** Lista de objetos originales que el usuario debe memorizar. */
    var targetObjects          = mutableStateOf<List<String>>(emptyList()); private set
    /** Lista completa de objetos para la fase de reconocimiento (originales + distractores). */
    var recallObjects          = mutableStateOf<List<String>>(emptyList()); private set
    /** Conjunto de objetos seleccionados por el usuario. */
    var selectedObjects        = mutableStateOf<Set<String>>(emptySet());  private set
    /** Segundos restantes en la fase de memorización. */
    var remainingMemorizeSec   = mutableStateOf(memorizeSec);              private set
    /** Número de aciertos (objetos originales correctamente seleccionados). */
    var hits                   = mutableStateOf(0);                        private set
    /** Número de omisiones (objetos originales no seleccionados). */
    var misses                 = mutableStateOf(0);                        private set
    /** Número de falsas alarmas (objetos no originales seleccionados por error). */
    var falseAlarms            = mutableStateOf(0);                        private set

    /**
     * Inicia el test, selecciona objetos aleatorios y comienza la fase de memorización.
     */
    fun startTest() {
        resetInternal()
        val shuffled = objectPool.shuffled()
        targetObjects.value  = shuffled.take(targetCount)
        val distractors      = shuffled.drop(targetCount).take(targetCount)
        recallObjects.value  = (targetObjects.value + distractors).shuffled()
        testState.value = TestState.MEMORIZE

        viewModelScope.launch {
            remainingMemorizeSec.value = memorizeSec
            while (remainingMemorizeSec.value > 0) {
                delay(1000); remainingMemorizeSec.value--
            }
            testState.value = TestState.RECALL
        }
    }

    /**
     * Alterna la selección de un objeto en la fase de RECALL.
     *
     * @param obj Objeto a seleccionar o deseleccionar.
     */
    fun onObjectToggled(obj: String) {
        if (testState.value != TestState.RECALL) return
        val current = selectedObjects.value.toMutableSet()

        if (current.contains(obj)) {
            // Siempre se puede deseleccionar
            current.remove(obj)
        } else {
            // Solo se puede añadir si hay menos de 6 seleccionados
            if (current.size >= targetCount) return
            current.add(obj)
        }
        selectedObjects.value = current
    }

    /**
     * Confirma la selección, calcula las métricas y finaliza el test.
     */
    fun onConfirm() {
        if (testState.value != TestState.RECALL) return
        val targets  = targetObjects.value.toSet()
        val selected = selectedObjects.value
        hits.value        = selected.count { it in targets }
        misses.value      = targets.count { it !in selected }
        falseAlarms.value = selected.count { it !in targets }
        testState.value = TestState.FINISHED
        saveResult()
    }

    /**
     * Guarda el resultado del test en la base de datos Room,
     * asociado al usuario actual y con los datos EEG simulados.
     */
    private fun saveResult() {
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg = eegSource.getLastSample()
            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail = email,
                    testType = "object_memory",
                    score = hits.value.toLong() * 100L,
                    precisionPct = precisionPercent(),
                    eegAlpha  = eeg.alpha,
                    eegBeta   = eeg.beta,
                    eegGamma  = eeg.gamma,
                    eegTheta  = eeg.theta)
            )
        }
    }

    /**
     * Calcula el porcentaje de precisión basado en aciertos sobre el total de objetos originales.
     *
     * @return Porcentaje de precisión (0-100).
     */
    fun precisionPercent() = ((hits.value.toFloat() / targetCount) * 100).toInt().coerceIn(0, 100)

    /**
     * Resetea el test completamente.
     */
    fun resetTest() { resetInternal(); testState.value = TestState.IDLE }

    /**
     * Resetea las variables internas del test.
     */
    private fun resetInternal() {
        targetObjects.value = emptyList(); recallObjects.value = emptyList()
        selectedObjects.value = emptySet()
        hits.value = 0; misses.value = 0; falseAlarms.value = 0
        remainingMemorizeSec.value = memorizeSec
    }
}