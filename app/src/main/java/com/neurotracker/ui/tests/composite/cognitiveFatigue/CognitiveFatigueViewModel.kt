package com.neurotracker.ui.tests.composite.cognitiveFatigue

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
 * Resultado de un bloque del Test de Fatiga Cognitiva.
 *
 * @property block         Número de bloque (1-indexed).
 * @property label         Etiqueta del bloque ("Inicio", "Intermedio", "Final").
 * @property precision     Precisión del bloque en porcentaje (0–100).
 * @property avgReactionMs Tiempo de reacción medio en ms para estímulos GO correctos.
 */
data class BlockResult(
    val block:         Int,
    val label:         String,
    val precision:     Int,
    val avgReactionMs: Long
)

/**
 * ViewModel del Test de Fatiga Cognitiva.
 *
 * Evalúa la evolución del rendimiento cognitivo a lo largo de 3 bloques
 * consecutivos de una tarea Go/No-Go. Cada bloque dura 30 segundos.
 *
 * Sistema de puntuación por bloque:
 *  - Pulsar círculo verde (GO)    → acierto.
 *  - Pulsar cuadrado rojo (NOGO)  → error.
 *  - Círculo verde sin pulsar     → error de omisión.
 *  - Cuadrado rojo sin pulsar     → no suma nada.
 *
 * Flujo de estados:
 *  IDLE → TRANSITION → BLOCK_RUNNING → TRANSITION → … → FINISHED
 *
 * El índice de fatiga se calcula como la diferencia de precisión entre
 * el último y el primer bloque. Valores negativos indican fatiga.
 *
 * @param application Contexto de aplicación para acceder a Room, SessionManager y EEG.
 */
class CognitiveFatigueViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del test.
     *
     *  - [IDLE]          : pantalla de instrucciones.
     *  - [TRANSITION]    : pantalla de transición entre bloques.
     *  - [BLOCK_RUNNING] : bloque activo, presentando estímulos.
     *  - [FINISHED]      : test completado, mostrando análisis de fatiga.
     */
    enum class TestState { IDLE, TRANSITION, BLOCK_RUNNING, FINISHED }

    private val db             = NeuroTrackerDatabase.getDatabase(application)
    private val eegSource      = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    val totalBlocks = 3

    var testState         = mutableStateOf(TestState.IDLE); private set
    var currentBlock      = mutableStateOf(0);             private set
    var transitionMessage = mutableStateOf("");            private set
    var remainingSec      = mutableStateOf(30);            private set
    var stimulus          = mutableStateOf(false);         private set
    var blockHits         = mutableStateOf(0);             private set
    var blockErrors       = mutableStateOf(0);             private set
    var blockResults      = mutableStateOf<List<BlockResult>>(emptyList()); private set

    private val reactionTimes   = mutableListOf<Long>()
    private var stimulusShownAt = 0L
    private var pressed         = false
    private var stimulusActive  = false
    private var testJob: Job?   = null

    /**
     * Tipo del estímulo de la ronda actual guardado por separado.
     *
     * A diferencia de [stimulus], este campo NO se pone a false al
     * ocultar el estímulo. Permite que [onPress] evalúe correctamente el
     * tipo aunque la corrutina ya haya limpiado [stimulus] tras el delay.
     */
    private var lastIsGo = false

    /**
     * Inicia el test desde cero, ejecutando los 3 bloques secuencialmente.
     */
    fun startTest() {
        blockResults.value = emptyList()
        viewModelScope.launch { runAllBlocks() }
    }

    /**
     * Ejecuta los 3 bloques del test secuencialmente.
     *
     * Cada bloque va precedido de una pantalla de transición de 3 segundos.
     * Al terminar cada bloque calcula la precisión y la añade a [blockResults].
     */
    private suspend fun runAllBlocks() {
        val labels = listOf("Inicio", "Intermedio", "Final")
        repeat(totalBlocks) { i ->
            currentBlock.value      = i + 1
            transitionMessage.value = "Bloque ${i + 1} de $totalBlocks — ${labels[i]}"
            testState.value         = TestState.TRANSITION
            delay(3000)

            blockHits.value   = 0
            blockErrors.value = 0
            reactionTimes.clear()
            runBlock()

            val avg       = if (reactionTimes.isNotEmpty()) reactionTimes.average().toLong() else 0L
            val precision = if (blockHits.value + blockErrors.value == 0) 0
            else ((blockHits.value.toFloat() / (blockHits.value + blockErrors.value)) * 100).toInt()

            blockResults.value = blockResults.value + BlockResult(
                block         = i + 1,
                label         = labels[i],
                precision     = precision,
                avgReactionMs = avg
            )
        }
        testState.value = TestState.FINISHED
        saveResult()
    }

    /**
     * Ejecuta un bloque de 30 segundos generando estímulos GO/NOGO.
     *
     * Cada estímulo permanece visible 1200 ms. Entre estímulos hay una pausa
     * de 800 ms para que el cambio sea perceptible. La probabilidad de GO es 70%.
     *
     * Al terminar el tiempo del estímulo sin respuesta:
     *  - GO sin pulsar → error de omisión.
     *  - NOGO sin pulsar → no suma nada (inhibición correcta).
     */
    private suspend fun runBlock() {
        testState.value    = TestState.BLOCK_RUNNING
        remainingSec.value = 30
        val start          = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < 30_000L) {
            remainingSec.value = (30 - ((System.currentTimeMillis() - start) / 1000))
                .toInt().coerceAtLeast(0)

            val isGo        = (0..99).random() < 70
            lastIsGo        = isGo
            stimulus.value  = isGo
            stimulusActive  = true
            pressed         = false
            stimulusShownAt = System.currentTimeMillis()

            delay(1200)

            if (!pressed && isGo) blockErrors.value++

            stimulusActive = false
            stimulus.value = false

            delay(800)
        }

        stimulus.value = false
    }

    /**
     * Registra la pulsación del usuario sobre el área del estímulo.
     *
     * Usa [lastIsGo] en lugar de [stimulus] para evaluar el tipo del estímulo,
     * evitando que lecturas tardías reciban false cuando la corrutina ya limpió
     * [stimulus] tras el delay.
     *
     * Reglas: GO pulsado → acierto. NOGO pulsado → error.
     */
    fun onPress() {
        if (testState.value != TestState.BLOCK_RUNNING || pressed || !stimulusActive) return
        pressed = true
        if (lastIsGo) {
            reactionTimes.add(System.currentTimeMillis() - stimulusShownAt)
            blockHits.value++
        } else {
            blockErrors.value++
        }
    }

    /**
     * Calcula el índice de fatiga cognitiva.
     *
     * Se define como la diferencia de precisión entre el último y el primer bloque.
     * Valores negativos indican degradación del rendimiento (fatiga).
     *
     * @return Diferencia de precisión entre el último y el primer bloque. 0 si hay menos de 2 bloques.
     */
    fun fatigueIndex(): Int {
        val results = blockResults.value
        if (results.size < 2) return 0
        return results.last().precision - results.first().precision
    }

    /**
     * Calcula la precisión media global del test.
     *
     * @return Media de las precisiones de los 3 bloques. 0 si no hay resultados.
     */
    fun precisionPercent(): Int {
        val results = blockResults.value
        if (results.isEmpty()) return 0
        return results.map { it.precision }.average().toInt()
    }

    /**
     * Reinicia el test al estado inicial sin guardar resultados.
     */
    fun resetTest() {
        testJob?.cancel()
        testState.value    = TestState.IDLE
        blockResults.value = emptyList()
        stimulus.value     = false
        blockHits.value    = 0
        blockErrors.value  = 0
    }

    /**
     * Guarda el resultado final del test en Room con datos EEG simulados.
     *
     * El score almacenado es el índice de fatiga. La precisión es la media
     * de los 3 bloques.
     */
    private fun saveResult() {
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg   = eegSource.getLastSample()
            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail    = email,
                    testType     = "cognitive_fatigue",
                    score        = fatigueIndex().toLong(),
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