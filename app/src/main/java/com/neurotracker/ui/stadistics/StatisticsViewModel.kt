package com.neurotracker.ui.stadistics

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.repository.TestResultRepository
import com.neurotracker.data.session.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Punto de datos para gráficas de evolución de precisión.
 *
 * @param label Etiqueta del punto (nombre del test abreviado).
 * @param value Valor de precisión (0-100).
 * @param timestamp Marca de tiempo del test.
 */
data class StatPoint(
    val label: String,
    val value: Float,
    val timestamp: Long
)

/**
 * Estadísticas agregadas por bloque cognitivo.
 *
 * @param blockName Nombre del bloque (ej: "Atención").
 * @param points Lista de puntos de precisión por test.
 * @param avgPrecision Precisión media del bloque.
 * @param trend Tendencia (diferencia entre segunda y primera mitad).
 */
data class BlockStats(
    val blockName: String,
    val points: List<StatPoint>,
    val avgPrecision: Float,
    val trend: Float
)

/**
 * Punto de datos EEG para gráficas de evolución.
 *
 * @param timestamp Marca de tiempo del test.
 * @param alpha Potencia de la banda Alpha.
 * @param beta Potencia de la banda Beta.
 * @param gamma Potencia de la banda Gamma.
 * @param theta Potencia de la banda Theta.
 */
data class EegPoint(
    val timestamp: Long,
    val alpha: Float,
    val beta:  Float,
    val gamma: Float,
    val theta: Float
)

/**
 * Promedios de bandas EEG y banda dominante.
 *
 * @param alpha Promedio de la banda Alpha.
 * @param beta Promedio de la banda Beta.
 * @param gamma Promedio de la banda Gamma.
 * @param theta Promedio de la banda Theta.
 * @param dominant Banda dominante (Alpha, Beta, Gamma, Theta).
 */
data class EegAverages(
    val alpha: Float,
    val beta:  Float,
    val gamma: Float,
    val theta: Float,
    val dominant: String
)

/**
 * ViewModel para la pantalla de estadísticas.
 *
 * Gestiona la carga y agregación de datos de tests y EEG.
 * Calcula estadísticas globales, por bloque cognitivo y evolución temporal.
 *
 * @param application Contexto de la aplicación.
 */
class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val db             = NeuroTrackerDatabase.getDatabase(application)
    private val sessionManager = SessionManager(application)
    private val repository     = TestResultRepository(db.testResultDao())

    /** Indica si los datos están cargando. */
    var isLoading   = mutableStateOf(true);  private set
    /** Número total de tests realizados. */
    var totalTests  = mutableStateOf(0);     private set
    /** Precisión global media. */
    var avgGlobal   = mutableStateOf(0f);    private set
    /** Nombre del bloque con mejor precisión media. */
    var bestBlock   = mutableStateOf("");    private set

    /** Estadísticas del bloque de Atención. */
    var attentionStats  = mutableStateOf<BlockStats?>(null); private set
    /** Estadísticas del bloque de Memoria. */
    var memoryStats     = mutableStateOf<BlockStats?>(null); private set
    /** Estadísticas del bloque de Velocidad. */
    var speedStats      = mutableStateOf<BlockStats?>(null); private set
    /** Estadísticas del bloque de Funciones Ejecutivas. */
    var executiveStats  = mutableStateOf<BlockStats?>(null); private set
    /** Estadísticas del bloque de Coordinación. */
    var coordStats      = mutableStateOf<BlockStats?>(null); private set
    /** Estadísticas del bloque de Tests Compuestos. */
    var compositeStats  = mutableStateOf<BlockStats?>(null); private set

    /** Evolución global de precisión (últimos 20 tests). */
    var globalEvolution = mutableStateOf<List<StatPoint>>(emptyList()); private set

    /** Promedios EEG del usuario. */
    var eegAverages  = mutableStateOf<EegAverages?>(null);          private set
    /** Evolución de bandas EEG (últimos 20 tests con datos EEG). */
    var eegEvolution = mutableStateOf<List<EegPoint>>(emptyList()); private set
    /** Indica si existen tests con datos EEG. */
    var hasEegData   = mutableStateOf(false);                       private set

    // Tipos de tests por bloque cognitivo
    private val attentionTypes = setOf("reaction_simple","attention_sustained","attention_selective","stroop","visual_cancellation")
    private val memoryTypes    = setOf("digit_span","n_back","visual_memory","object_memory","associative_memory")
    private val speedTypes     = setOf("quick_comparison","sequence_ordering","decision_time")
    private val executiveTypes = setOf("task_switching","response_inhibition","planning")
    private val coordTypes     = setOf("object_tracking","visuomotor","spatial_perception")
    private val compositeTypes = setOf("global_cognitive","cognitive_fatigue","dual_task")

    /** Inicializa la carga de estadísticas al crear el ViewModel. */
    init { loadStats() }

    /**
     * Carga todos los datos de estadísticas desde la base de datos.
     * Obtiene los resultados del usuario actual y calcula:
     * - Estadísticas globales
     * - Evolución temporal
     * - Estadísticas por bloque cognitivo
     * - Datos EEG agregados
     */
    private fun loadStats() {
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: run {
                isLoading.value = false
                return@launch
            }

            repository.getResultsForUserFlow(email).collectLatest { results ->
                if (results.isEmpty()) {
                    isLoading.value = false
                    return@collectLatest
                }

                totalTests.value = results.size

                // Usar precisionPct directamente de la BD
                val withPrecision = results.map { r ->
                    Triple(r.testType, r.precisionPct.toFloat(), r.timestamp)
                }

                avgGlobal.value = withPrecision.map { it.second }.average().toFloat()

                // Evolución global — últimos 20 tests ordenados por fecha
                globalEvolution.value = withPrecision
                    .sortedBy { it.third }
                    .takeLast(20)
                    .map { StatPoint(mapShortName(it.first), it.second, it.third) }

                // Estadísticas por bloque
                attentionStats.value  = buildBlockStats("Atención",              withPrecision.filter { it.first in attentionTypes })
                memoryStats.value     = buildBlockStats("Memoria",               withPrecision.filter { it.first in memoryTypes })
                speedStats.value      = buildBlockStats("Velocidad",             withPrecision.filter { it.first in speedTypes })
                executiveStats.value  = buildBlockStats("Funciones Ejecutivas",  withPrecision.filter { it.first in executiveTypes })
                coordStats.value      = buildBlockStats("Coordinación",          withPrecision.filter { it.first in coordTypes })
                compositeStats.value  = buildBlockStats("Tests Compuestos",      withPrecision.filter { it.first in compositeTypes })

                val allBlocks = listOfNotNull(
                    attentionStats.value, memoryStats.value, speedStats.value,
                    executiveStats.value, coordStats.value, compositeStats.value
                )
                bestBlock.value = allBlocks.maxByOrNull { it.avgPrecision }?.blockName ?: ""

                // ---- EEG: solo tests con datos reales ----
                val withEeg = results
                    .filter { r -> r.eegAlpha != 0f || r.eegBeta != 0f || r.eegGamma != 0f || r.eegTheta != 0f }
                    .sortedBy { it.timestamp }

                hasEegData.value = withEeg.isNotEmpty()

                if (withEeg.isNotEmpty()) {
                    val avgAlpha = withEeg.map { it.eegAlpha }.average().toFloat()
                    val avgBeta  = withEeg.map { it.eegBeta  }.average().toFloat()
                    val avgGamma = withEeg.map { it.eegGamma }.average().toFloat()
                    val avgTheta = withEeg.map { it.eegTheta }.average().toFloat()

                    val dominant = listOf(
                        "Alpha" to avgAlpha,
                        "Beta"  to avgBeta,
                        "Gamma" to avgGamma,
                        "Theta" to avgTheta
                    ).maxByOrNull { it.second }?.first ?: "—"

                    eegAverages.value = EegAverages(avgAlpha, avgBeta, avgGamma, avgTheta, dominant)

                    eegEvolution.value = withEeg.takeLast(20).map { r ->
                        EegPoint(r.timestamp, r.eegAlpha, r.eegBeta, r.eegGamma, r.eegTheta)
                    }
                }

                isLoading.value = false
            }
        }
    }

    /**
     * Construye las estadísticas agregadas para un bloque cognitivo.
     *
     * @param name Nombre del bloque.
     * @param data Lista de triples (tipoTest, precisión, timestamp).
     * @return BlockStats con los datos agregados, o null si no hay datos.
     */
    private fun buildBlockStats(name: String, data: List<Triple<String, Float, Long>>): BlockStats? {
        if (data.isEmpty()) return null
        val sorted = data.sortedBy { it.third }
        val points = sorted.map { StatPoint(mapShortName(it.first), it.second, it.third) }
        val avg    = points.map { it.value }.average().toFloat()
        val mid    = points.size / 2
        val trend  = if (points.size >= 2) {
            val first  = points.take(mid.coerceAtLeast(1)).map { it.value }.average().toFloat()
            val second = points.drop(mid).map { it.value }.average().toFloat()
            second - first
        } else 0f
        return BlockStats(name, points, avg, trend)
    }

    /**
     * Mapea el nombre interno del test a una abreviatura corta para gráficas.
     *
     * @param type Identificador interno del test.
     * @return Abreviatura de 3-6 caracteres.
     */
    private fun mapShortName(type: String): String = when (type) {
        "reaction_simple"      -> "Reac."
        "attention_sustained"  -> "Sost."
        "attention_selective"  -> "Sel."
        "stroop"               -> "Stroop"
        "visual_cancellation"  -> "Canc."
        "digit_span"           -> "Dígit."
        "n_back"               -> "N-Back"
        "visual_memory"        -> "Vis."
        "object_memory"        -> "Obj."
        "associative_memory"   -> "Asoc."
        "quick_comparison"     -> "Comp."
        "sequence_ordering"    -> "Seq."
        "decision_time"        -> "Dec."
        "task_switching"       -> "Switch"
        "response_inhibition"  -> "Inhib."
        "planning"             -> "Plan."
        "object_tracking"      -> "Track."
        "visuomotor"           -> "Visu."
        "spatial_perception"   -> "Esp."
        "global_cognitive"     -> "Global"
        "cognitive_fatigue"    -> "Fatiga"
        "dual_task"            -> "Doble"
        else                   -> type.take(6)
    }
}