package com.neurotracker.ui.tests.coordination.visuoMotor

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
 * ViewModel para el Test de Coordinación Visomotora.
 *
 * Gestiona la lógica del test donde el usuario debe pulsar círculos
 * que aparecen en posiciones aleatorias dentro de un límite de 30 segundos.
 *
 * Reglas:
 *  - Acierto: pulsar dentro del círculo visible.
 *  - Error: pulsar fuera del círculo mientras está visible.
 *  - Omisión: no pulsar nada antes de que desaparezca el círculo.
 *  - Solo se cuenta un error por círculo, incluso si se pulsa varias veces.
 *
 * Estados del test:
 *  - IDLE: Pantalla de instrucciones, esperando inicio.
 *  - RUNNING: Test en curso, mostrando círculos en posiciones aleatorias.
 *  - FINISHED: Test completado (tiempo agotado), mostrando resultados.
 *
 * @param application Contexto de la aplicación.
 */
class VisuomotorViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del flujo del test.
     */
    enum class TestState { IDLE, RUNNING, FINISHED }

    /**
     * Objetivo (círculo) que aparece en la pantalla.
     *
     * @param id Identificador único del objetivo.
     * @param x Posición horizontal relativa (0.0 a 1.0).
     * @param y Posición vertical relativa (0.0 a 1.0).
     */
    data class Target(val id: Int, val x: Float, val y: Float)

    private val db             = NeuroTrackerDatabase.getDatabase(application)
    private val eegSource      = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    private val testDurationMs = 30_000L
    private val targetLifeMs   = 1500L

    /** Estado actual del test. */
    var testState    = mutableStateOf(TestState.IDLE);  private set
    /** Objetivo actualmente visible (null si no hay círculo). */
    var currentTarget = mutableStateOf<Target?>(null);  private set
    /** Número de aciertos (pulsación correcta del círculo). */
    var hits         = mutableStateOf(0);               private set
    /** Número de errores (pulsación fuera del círculo o sin pulsar). */
    var errors       = mutableStateOf(0);               private set
    /** Segundos restantes para completar el test. */
    var remainingSec = mutableStateOf(30);              private set

    private val reactionTimes   = mutableListOf<Long>()
    private var targetShownAt   = 0L
    private var targetHit       = false
    private var errorThisInterval = false  // máximo 1 error por intervalo entre círculos
    private var circleVisible   = false
    private var targetIdCounter = 0

    /**
     * Inicia el test, arranca el temporizador y el bucle de generación de círculos.
     */
    fun startTest() {
        hits.value = 0; errors.value = 0
        remainingSec.value = 30; reactionTimes.clear()
        testState.value = TestState.RUNNING

        viewModelScope.launch {
            val start = System.currentTimeMillis()

            // Timer regresivo
            launch {
                while (System.currentTimeMillis() - start < testDurationMs) {
                    delay(1000)
                    if (remainingSec.value > 0) remainingSec.value--
                }
            }

            // Loop de estímulos
            while (System.currentTimeMillis() - start < testDurationMs) {
                spawnTarget()
                targetHit         = false
                errorThisInterval = false
                circleVisible     = true
                targetShownAt     = System.currentTimeMillis()

                delay(targetLifeMs)

                // Si no pulsó nada en todo el intervalo → error (omisión)
                if (!targetHit && !errorThisInterval) errors.value++

                circleVisible         = false
                currentTarget.value   = null
                delay(400)
            }

            finishTest()
        }
    }

    /**
     * Genera un nuevo objetivo en una posición aleatoria dentro de la pantalla.
     */
    private fun spawnTarget() {
        currentTarget.value = Target(
            id = targetIdCounter++,
            x  = Random.nextFloat() * 0.7f + 0.1f,
            y  = Random.nextFloat() * 0.7f + 0.1f
        )
    }

    /**
     * Maneja la pulsación sobre el círculo.
     *
     * @param id Identificador del círculo pulsado.
     */
    fun onTargetTapped(id: Int) {
        if (testState.value != TestState.RUNNING) return
        if (currentTarget.value?.id != id || targetHit) return
        targetHit     = true
        circleVisible = false
        reactionTimes.add(System.currentTimeMillis() - targetShownAt)
        hits.value++
        currentTarget.value = null
    }

    /**
     * Maneja la pulsación sobre el fondo (fuera del círculo).
     * Solo se cuenta un error por círculo, incluso si se pulsa varias veces.
     */
    fun onBackgroundTapped() {
        if (testState.value != TestState.RUNNING) return
        if (circleVisible && !errorThisInterval && !targetHit) {
            errorThisInterval = true
            errors.value++
        }
    }

    /**
     * Finaliza el test, guarda el resultado en la base de datos.
     */
    private fun finishTest() {
        testState.value = TestState.FINISHED
        val avg = if (reactionTimes.isNotEmpty()) reactionTimes.average().toLong() else 9999L
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val eeg   = eegSource.getLastSample()
            db.testResultDao().insertResult(TestResultEntity(
                userEmail    = email,
                testType     = "visuomotor",
                score        = avg,
                precisionPct = precisionPercent(),
                eegAlpha     = eeg.alpha,
                eegBeta      = eeg.beta,
                eegGamma     = eeg.gamma,
                eegTheta     = eeg.theta
            ))
        }
    }

    /**
     * Calcula el porcentaje de precisión.
     *
     * Precisión = aciertos / (aciertos + errores)
     * Los errores incluyen: pulsar fuera del círculo y no pulsar nada (omisiones).
     *
     * @return Porcentaje de precisión (0-100).
     */
    fun precisionPercent(): Int {
        val total = hits.value + errors.value
        return if (total == 0) 0 else ((hits.value.toFloat() / total) * 100).toInt()
    }

    /**
     * Resetea el test completamente.
     */
    fun resetTest() {
        testState.value     = TestState.IDLE
        currentTarget.value = null
        hits.value          = 0
        errors.value        = 0
        circleVisible       = false
        errorThisInterval   = false
    }
}