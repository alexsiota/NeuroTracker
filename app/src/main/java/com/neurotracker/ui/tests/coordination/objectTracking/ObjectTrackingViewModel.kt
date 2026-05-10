package com.neurotracker.ui.tests.coordination.objectTracking

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
import kotlin.random.Random

/**
 * ViewModel del Test de Seguimiento de Objetos (Multiple Object Tracking).
 *
 * Gestiona toda la lógica del test:
 * - Generación de objetos y objetivos.
 * - Movimiento dinámico de objetos.
 * - Control de fases (memorizar, seguir, responder).
 * - Evaluación de respuestas del usuario.
 * - Persistencia de resultados con datos EEG.
 *
 * Flujo:
 * IDLE → MEMORIZE → MOVING → RESPONSE → FINISHED
 *
 * @param application Contexto de aplicación necesario para base de datos y sesión.
 */
class ObjectTrackingViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados del test.
     */
    enum class TestState { IDLE, MEMORIZE, MOVING, RESPONSE, FINISHED }

    /**
     * Representa un objeto en pantalla.
     *
     * @property id Identificador único del objeto.
     * @property x Posición horizontal (normalizada 0–1).
     * @property y Posición vertical (normalizada 0–1).
     * @property dx Velocidad horizontal.
     * @property dy Velocidad vertical.
     * @property isTarget Indica si es un objeto objetivo.
     */
    data class TrackedObject(
        val id: Int,
        var x: Float,
        var y: Float,
        var dx: Float,
        var dy: Float,
        val isTarget: Boolean
    )

    private val db = NeuroTrackerDatabase.getDatabase(application)
    private val eegSource = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    private val totalObjects = 6
    private val targetCount  = 2
    private val memorizeSec  = 3
    private val trackingSec  = 5
    val totalRounds          = 4

    var testState            = mutableStateOf(TestState.IDLE);                   private set
    var objects              = mutableStateOf<List<TrackedObject>>(emptyList()); private set
    var revealTargets        = mutableStateOf(false);                            private set
    var remainingMemorizeSec = mutableStateOf(memorizeSec);                      private set
    var remainingTrackingSec = mutableStateOf(trackingSec);                      private set
    var currentRound         = mutableStateOf(0);                                private set
    var userSelection        = mutableStateOf<Set<Int>>(emptySet());             private set
    var hits                 = mutableStateOf(0);                                private set
    var errors               = mutableStateOf(0);                                private set

    private var testJob: Job? = null

    /**
     * Señal interna para indicar que el usuario ha confirmado su selección
     * durante la fase RESPONSE.
     */
    private var confirmSignal = false

    /**
     * Inicia el test y resetea métricas.
     */
    fun startTest() {
        hits.value = 0
        errors.value = 0
        currentRound.value = 0
        testState.value = TestState.MEMORIZE
        testJob = viewModelScope.launch { runRounds() }
    }

    /**
     * Ejecuta todas las rondas del test.
     */
    private suspend fun runRounds() {
        repeat(totalRounds) {
            currentRound.value++
            userSelection.value = emptySet()
            confirmSignal = false

            // Generación de objetos
            val objs = List(totalObjects) { id ->
                TrackedObject(
                    id = id,
                    x  = Random.nextFloat() * 0.75f + 0.1f,
                    y  = Random.nextFloat() * 0.75f + 0.1f,
                    dx = (Random.nextFloat() * 0.005f + 0.003f) * if (Random.nextBoolean()) 1 else -1,
                    dy = (Random.nextFloat() * 0.005f + 0.003f) * if (Random.nextBoolean()) 1 else -1,
                    isTarget = id < targetCount
                )
            }
            objects.value = objs

            // ---- FASE MEMORIZACIÓN ----
            revealTargets.value = true
            testState.value = TestState.MEMORIZE
            remainingMemorizeSec.value = memorizeSec

            repeat(memorizeSec) {
                delay(1000)
                if (remainingMemorizeSec.value > 0) remainingMemorizeSec.value--
            }

            // ---- FASE MOVIMIENTO ----
            revealTargets.value = false
            testState.value = TestState.MOVING
            remainingTrackingSec.value = trackingSec

            val frames = trackingSec * 10

            repeat(frames) { frame ->
                delay(100)

                remainingTrackingSec.value = ((frames - frame - 1) / 10)

                objects.value = objects.value.map { obj ->
                    var nx = obj.x + obj.dx
                    var ny = obj.y + obj.dy
                    var ndx = obj.dx
                    var ndy = obj.dy

                    // Rebote en bordes
                    if (nx < 0.05f || nx > 0.92f) ndx = -ndx
                    if (ny < 0.05f || ny > 0.92f) ndy = -ndy

                    obj.copy(
                        x = nx.coerceIn(0.05f, 0.92f),
                        y = ny.coerceIn(0.05f, 0.92f),
                        dx = ndx,
                        dy = ndy
                    )
                }
            }

            // ---- FASE RESPUESTA ----
            testState.value = TestState.RESPONSE

            val responseStart = System.currentTimeMillis()

            while (!confirmSignal && System.currentTimeMillis() - responseStart < 10_000L) {
                delay(50)
            }

            // ---- EVALUACIÓN ----
            val targets  = objects.value.filter { it.isTarget }.map { it.id }.toSet()
            val selected = userSelection.value

            hits.value   += selected.count { it in targets }
            errors.value += selected.count { it !in targets }

            delay(400)
        }

        finishTest()
    }

    /**
     * Maneja la selección de un objeto por parte del usuario.
     *
     * @param id ID del objeto seleccionado.
     */
    fun onObjectSelected(id: Int) {
        if (testState.value != TestState.RESPONSE) return

        val current = userSelection.value.toMutableSet()

        if (current.contains(id)) current.remove(id)
        else current.add(id)

        userSelection.value = current
    }

    /**
     * Confirma la selección del usuario en la fase RESPONSE.
     */
    fun onConfirmSelection() {
        if (testState.value != TestState.RESPONSE) return
        confirmSignal = true
    }

    /**
     * Finaliza el test y guarda resultados.
     */
    private fun finishTest() {
        testState.value = TestState.FINISHED

        val total = (hits.value + errors.value).coerceAtLeast(1)

        // Nota: score penaliza error (cuanto mayor error, peor score)
        val score = ((errors.value.toFloat() / total) * 1000).toLong()

        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg = eegSource.getLastSample()

            db.testResultDao().insertResult(
                TestResultEntity(
                    userEmail = email,
                    testType = "object_tracking",
                    score = score,
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
     * Calcula la precisión del test.
     *
     * @return Porcentaje de aciertos.
     */
    fun precisionPercent(): Int {
        val total = hits.value + errors.value
        return if (total == 0) 0
        else ((hits.value.toFloat() / total) * 100).toInt()
    }

    /**
     * Reinicia el estado del test.
     */
    fun resetTest() {
        testJob?.cancel()
        testState.value = TestState.IDLE
        objects.value = emptyList()
        hits.value = 0
        errors.value = 0
        currentRound.value = 0
        confirmSignal = false
    }
}