package com.neurotracker.ui.eeg

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurotracker.data.eeg.EegDataSource
import com.neurotracker.data.eeg.SimulatedEegDataSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel encargado de la gestión y procesamiento de datos EEG.
 *
 * Responsabilidades principales:
 * - Gestionar el flujo de datos provenientes de la capa de datos (DataSource).
 * - Mantener el estado reactivo de la UI (buffers, potencia, estado cognitivo).
 * - Procesar las señales EEG en tiempo real.
 * - Controlar el ciclo de vida del stream de datos.
 *
 * Arquitectura:
 * - Sigue el patrón MVVM.
 * - Actúa como intermediario entre la fuente de datos [EegDataSource]
 *   y la capa de presentación (Compose UI).
 *
 * Fuente de datos:
 * - Actualmente utiliza [SimulatedEegDataSource].
 * - Preparado para sustituirse por una implementación real (hardware EEG).
 *
 * Estado gestionado:
 * - Buffers de señal por banda (Alpha, Beta, Gamma, Theta).
 * - Potencia relativa de cada banda.
 * - Estado cognitivo estimado.
 * - Estado de ejecución del stream.
 * - Modo de visualización de la UI.
 *
 * @param application Contexto de aplicación requerido por AndroidViewModel.
 */
class EegViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Modos de visualización disponibles en la UI.
     */
    enum class ViewMode { COMPACT, DETAIL }

    companion object {
        /**
         * Tamaño máximo de los buffers de señal.
         * Limita la cantidad de muestras almacenadas para evitar crecimiento indefinido.
         */
        private const val BUFFER_SIZE = 100
    }

    /**
     * Fuente de datos EEG.
     *
     * Abstracción que permite desacoplar la UI de la implementación concreta.
     * Actualmente simulada, pero preparada para hardware real.
     */
    private val dataSource: EegDataSource = SimulatedEegDataSource.instance

    var viewMode       = mutableStateOf(ViewMode.COMPACT); private set
    var isRunning      = mutableStateOf(false);            private set

    var alphaBuffer = mutableStateOf<List<Float>>(emptyList()); private set
    var betaBuffer  = mutableStateOf<List<Float>>(emptyList()); private set
    var gammaBuffer = mutableStateOf<List<Float>>(emptyList()); private set
    var thetaBuffer = mutableStateOf<List<Float>>(emptyList()); private set

    var alphaPower = mutableStateOf(0f); private set
    var betaPower  = mutableStateOf(0f); private set
    var gammaPower = mutableStateOf(0f); private set
    var thetaPower = mutableStateOf(0f); private set

    var cognitiveState = mutableStateOf("Iniciando..."); private set

    /**
     * Job que controla la ejecución del stream de datos EEG.
     * Permite cancelarlo de forma segura.
     */
    private var streamJob: Job? = null

    /**
     * Inicialización del ViewModel.
     *
     * Inicia automáticamente el stream de datos EEG al crearse el ViewModel.
     */
    init { startStream() }

    /**
     * Inicia la recepción de datos EEG en tiempo real.
     *
     * - Lanza una coroutine en el scope del ViewModel.
     * - Recoge continuamente muestras desde el DataSource.
     * - Actualiza los buffers de cada banda.
     * - Recalcula la potencia de señal.
     * - Actualiza el estado cognitivo estimado.
     *
     * Controla que no se inicie múltiples veces si ya está en ejecución.
     */
    fun startStream() {
        if (isRunning.value) return
        isRunning.value = true
        streamJob = viewModelScope.launch {
            dataSource.streamSamples().collectLatest { sample ->
                alphaBuffer.value = (alphaBuffer.value + sample.alpha).takeLast(BUFFER_SIZE)
                betaBuffer.value  = (betaBuffer.value  + sample.beta ).takeLast(BUFFER_SIZE)
                gammaBuffer.value = (gammaBuffer.value + sample.gamma).takeLast(BUFFER_SIZE)
                thetaBuffer.value = (thetaBuffer.value + sample.theta).takeLast(BUFFER_SIZE)
                updateBandPower()
                updateCognitiveState()
            }
        }
    }

    /**
     * Detiene el flujo de datos EEG.
     *
     * - Cancela la coroutine activa.
     * - Actualiza el estado de ejecución.
     */
    fun stopStream() {
        streamJob?.cancel()
        isRunning.value = false
    }

    /**
     * Alterna el modo de visualización de la interfaz.
     *
     * Cambia entre:
     * - Modo compacto
     * - Modo detallado
     */
    fun toggleViewMode() {
        viewMode.value = if (viewMode.value == ViewMode.COMPACT) ViewMode.DETAIL else ViewMode.COMPACT
    }

    /**
     * Calcula la potencia de cada banda EEG.
     *
     * Estrategia:
     * - Se toman las últimas muestras del buffer (ventana reciente).
     * - Se calcula la media de valores absolutos.
     * - Se escala a porcentaje (0–100).
     *
     * Este enfoque permite una estimación simple pero estable
     * de la actividad de cada banda en tiempo real.
     */
    private fun updateBandPower() {
        fun power(buf: List<Float>) =
            if (buf.isEmpty()) 0f
            else buf.takeLast(20).map { kotlin.math.abs(it) }.average().toFloat() * 100f

        alphaPower.value = power(alphaBuffer.value)
        betaPower.value  = power(betaBuffer.value)
        gammaPower.value = power(gammaBuffer.value)
        thetaPower.value = power(thetaBuffer.value)
    }

    /**
     * Determina el estado cognitivo actual del usuario.
     *
     * Se basa en la banda con mayor potencia:
     * - Alpha → estado relajado
     * - Beta → concentración
     * - Gamma → alta carga cognitiva
     * - Theta → memoria/creatividad
     *
     * Este cálculo es una simplificación orientada a visualización,
     * no un diagnóstico clínico.
     */
    private fun updateCognitiveState() {
        val max = maxOf(alphaPower.value, betaPower.value, gammaPower.value, thetaPower.value)

        cognitiveState.value = when (max) {
            alphaPower.value -> "Relajado · Atención pasiva"
            betaPower.value  -> "Concentrado · Alerta"
            gammaPower.value -> "Procesamiento cognitivo alto"
            thetaPower.value -> "Memoria activa · Creativo"
            else             -> "—"
        }
    }

    /**
     * Callback del ciclo de vida del ViewModel.
     *
     * Se ejecuta cuando el ViewModel es destruido.
     * Garantiza la liberación de recursos deteniendo el stream.
     */
    override fun onCleared() {
        super.onCleared()
        stopStream()
    }
}