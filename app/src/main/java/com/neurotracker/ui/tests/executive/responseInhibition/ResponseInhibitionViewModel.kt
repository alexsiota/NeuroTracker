package com.neurotracker.ui.tests.executive.responseInhibition

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
 * ViewModel del Test de Inhibición de Respuesta (Go / No-Go).
 *
 * Evalúa la capacidad de control inhibitorio del usuario: su habilidad
 * para responder cuando corresponde (GO) y para no responder cuando no
 * debe hacerlo (NOGO).
 *
 * Diseño del test:
 *  - 25 rondas en total.
 *  - ~75% de estímulos GO (círculo verde) — el usuario debe pulsar.
 *  - ~25% de estímulos NOGO (cuadrado rojo) — el usuario NO debe pulsar.
 *  - Cada estímulo es visible durante 1500 ms.
 *
 * Sistema de puntuación:
 *  - Pulsar círculo verde (GO)     → [hits]++ (acierto).
 *  - Pulsar cuadrado rojo (NOGO)   → [commissions]++ (error de comisión).
 *  - Verde sin pulsar (GO omitido) → [omissions]++ (error de omisión).
 *  - Rojo sin pulsar               → no suma nada (inhibición correcta).
 *
 * Flujo de estados:
 *  IDLE → RUNNING → FINISHED
 *
 * Métricas persistidas en Room:
 *  - Tiempo de reacción medio en estímulos GO correctos.
 *  - Precisión global: hits / (hits + commissions + omissions) × 100.
 *  - Datos EEG simulados.
 *
 * @param application Contexto de aplicación para acceder a Room, SessionManager y EEG.
 */
class ResponseInhibitionViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Estados posibles del test.
     *
     *  - [IDLE]     : pantalla de instrucciones antes de iniciar.
     *  - [RUNNING]  : test en curso, presentando estímulos GO/NOGO.
     *  - [FINISHED] : test finalizado, mostrando resultados.
     */
    enum class TestState { IDLE, RUNNING, FINISHED }

    /**
     * Tipos de estímulo que se presentan al usuario.
     *
     *  - [GO]   : círculo verde — el usuario debe pulsar.
     *  - [NOGO] : cuadrado rojo — el usuario NO debe pulsar.
     */
    enum class StimulusType { GO, NOGO }

    private val db             = NeuroTrackerDatabase.getDatabase(application)
    private val eegSource      = SimulatedEegDataSource.instance
    private val sessionManager = SessionManager(application)

    /** Número total de rondas del test. */
    private val totalRounds = 25

    /** Duración en ms que cada estímulo permanece visible en pantalla. */
    private val stimulusDurationMs = 1500L

    /** Probabilidad (%) de que el estímulo generado sea NOGO. El resto serán GO. */
    private val nogoProb = 25

    /** Estado actual del test. */
    var testState = mutableStateOf(TestState.IDLE)
        private set

    /**
     * Tipo de estímulo visible en pantalla en este momento.
     *
     * Se pone a null entre estímulos. La Screen lo usa para renderizar
     * el círculo verde o el cuadrado rojo.
     */
    var currentType = mutableStateOf<StimulusType?>(null)
        private set

    /**
     * Tipo del estímulo de la ronda actual guardado por separado.
     *
     * A diferencia de [currentType], este campo NO se pone a null al
     * ocultar el estímulo. Permite que [onPress] evalúe correctamente el
     * tipo aunque el usuario pulse justo cuando la corrutina ya limpió
     * [currentType] tras el delay. Sin esto, pulsaciones tardías leían
     * null y no registraban el error de comisión.
     */
    private var lastType: StimulusType? = null

    /** Número de ronda actual (1-indexed). */
    var currentRound = mutableStateOf(0)
        private set

    /**
     * Número de aciertos acumulados.
     *
     * Solo se incrementa cuando el usuario pulsa un estímulo GO (círculo verde).
     */
    var hits = mutableStateOf(0)
        private set

    /**
     * Número de errores de comisión acumulados.
     *
     * Se incrementa cuando el usuario pulsa un estímulo NOGO (cuadrado rojo).
     */
    var commissions = mutableStateOf(0)
        private set

    /**
     * Número de errores de omisión acumulados.
     *
     * Se incrementa cuando aparece un estímulo GO y el usuario no pulsa
     * antes de que se agote el tiempo de presentación.
     * No pulsar un NOGO no genera omisión.
     */
    var omissions = mutableStateOf(0)
        private set

    /**
     * Indica si el usuario ya ha pulsado en la ronda actual.
     *
     * Evita que pulsaciones múltiples dentro de la misma ronda registren
     * más de una respuesta.
     */
    var hasPressed = mutableStateOf(false)
        private set

    /** Tiempos de reacción en ms registrados en estímulos GO respondidos correctamente. */
    private val reactionTimes = mutableListOf<Long>()

    /** Marca temporal en ms del momento en que se mostró el estímulo actual. */
    private var shownAt = 0L

    /**
     * Inicia el test desde cero.
     *
     * Resetea todos los contadores y lanza la corrutina que ejecuta las rondas.
     */
    fun startTest() {
        hits.value         = 0
        commissions.value  = 0
        omissions.value    = 0
        currentRound.value = 0
        reactionTimes.clear()
        lastType        = null
        testState.value = TestState.RUNNING
        viewModelScope.launch { runRounds() }
    }

    /**
     * Ejecuta todas las rondas del test secuencialmente.
     *
     * En cada ronda:
     *  1. Genera un estímulo GO o NOGO según [nogoProb].
     *  2. Guarda el tipo en [lastType] antes de mostrarlo, para que [onPress]
     *     pueda leerlo aunque [currentType] ya se haya limpiado.
     *  3. Muestra el estímulo durante [stimulusDurationMs].
     *  4. Si el usuario no pulsó y era GO → suma omisión.
     *     Si no pulsó y era NOGO → no pasa nada.
     *  5. Oculta el estímulo y espera 300 ms antes de la siguiente ronda.
     */
    private suspend fun runRounds() {
        repeat(totalRounds) {
            val type = if (Random.nextInt(100) < nogoProb) StimulusType.NOGO else StimulusType.GO

            lastType           = type   // guardar ANTES de mostrar
            currentType.value  = type
            currentRound.value++
            hasPressed.value   = false
            shownAt            = System.currentTimeMillis()

            delay(stimulusDurationMs)

            if (!hasPressed.value && type == StimulusType.GO) {
                omissions.value++
            }

            currentType.value = null
            delay(300)
        }
        finishTest()
    }

    /**
     * Registra la pulsación del usuario sobre el área del estímulo.
     *
     * Usa [lastType] en lugar de [currentType] para evaluar el tipo,
     * evitando el caso en que [currentType] ya sea null si el usuario
     * pulsa justo al final del tiempo del estímulo.
     *
     * Reglas de puntuación:
     *  - Pulsar GO (verde)  → [hits]++ y registra el tiempo de reacción.
     *  - Pulsar NOGO (rojo) → [commissions]++ (fallo inhibitorio).
     *
     * La función ignora pulsaciones si el test no está en RUNNING o si
     * el usuario ya pulsó en esta ronda ([hasPressed] = true).
     */
    fun onPress() {
        if (testState.value != TestState.RUNNING || hasPressed.value) return
        hasPressed.value = true

        when (lastType) {
            StimulusType.GO -> {
                reactionTimes.add(System.currentTimeMillis() - shownAt)
                hits.value++
            }
            StimulusType.NOGO -> {
                commissions.value++
            }
            null -> {}
        }
    }

    /**
     * Calcula el porcentaje de precisión global del test.
     *
     * Fórmula: hits / (hits + commissions + omissions) × 100.
     *
     * @return Precisión entera entre 0 y 100. Devuelve 0 si no hay intentos.
     */
    fun precisionPercent(): Int {
        val total = hits.value + commissions.value + omissions.value
        return if (total == 0) 0 else ((hits.value.toFloat() / total) * 100).toInt()
    }

    /**
     * Reinicia el test al estado inicial sin guardar resultados.
     *
     * Permite repetir el test desde la pantalla de resultados.
     */
    fun resetTest() {
        testState.value   = TestState.IDLE
        currentType.value = null
        lastType          = null
        hits.value        = 0
        commissions.value = 0
        omissions.value   = 0
    }

    /**
     * Finaliza el test, calcula el tiempo medio de reacción y persiste el resultado en Room.
     *
     * El tiempo medio se calcula solo sobre GO respondidos correctamente.
     * Si no hubo ninguno, se usa 9999 ms como fallback.
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
                    testType     = "response_inhibition",
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
}