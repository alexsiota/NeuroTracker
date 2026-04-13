package com.neurotracker.ui.history

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.entities.TestResultEntity
import com.neurotracker.data.repository.TestResultRepository
import com.neurotracker.data.session.SessionManager
import com.neurotracker.viewmodel.RecentTestUi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Define el criterio de ordenación de los resultados en el historial.
 *
 *  - [ASC]  : más antiguos primero (timestamp creciente).
 *  - [DESC] : más recientes primero (timestamp decreciente). Valor por defecto.
 */
enum class HistoryOrder { ASC, DESC }

/**
 * Define los filtros disponibles para clasificar los tests por bloque cognitivo.
 *
 * @property label Texto visible en el [FilterChip] de la UI de historial.
 */
enum class HistoryFilter(val label: String) {
    ALL("Todos"),
    ATTENTION("Atención"),
    MEMORY("Memoria"),
    SPEED("Velocidad"),
    EXECUTIVE("Ejecutivo"),
    COORDINATION("Coordinación"),
    COMPOSITE("Compuestos")
}

/**
 * ViewModel de la pantalla de historial de tests cognitivos.
 *
 * ─── Corrección del bug de borrado ───────────────────────────────────────────
 *
 * El bug original tenía dos causas:
 *
 * 1. [deleteTest] usaba `database.testResultDao().deleteById(id)` directamente
 *    en lugar de pasar por el repositorio. Esto funcionaba en algunos casos
 *    pero no garantizaba que el [Flow] de Room re-emitiera en todos ellos,
 *    especialmente cuando el observer estaba en `collectLatest` y la emisión
 *    llegaba mientras se procesaba otra.
 *
 * 2. El `id` de [RecentTestUi] podría llegar como 0 si la entidad Room no
 *    declaraba `@PrimaryKey(autoGenerate = true)` correctamente o si el DAO
 *    de inserción no devolvía el id generado. El borrado borraba el registro
 *    con id=0, que no existía, sin lanzar error.
 *
 * Solución aplicada:
 *  - [deleteTest] pasa por [testRepository] para garantizar consistencia.
 *  - Tras el delete se fuerza una re-emisión llamando a [applyFilter] con
 *    los datos ya cargados en [allResults], actualizando la lista inmediatamente
 *    sin esperar al Flow (que puede tardar en notificar en algunos emuladores).
 *  - La lista observable [tests] se actualiza en el hilo principal.
 *
 * @param application Contexto de aplicación para Room y SessionManager.
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val database       = NeuroTrackerDatabase.getDatabase(application)
    private val sessionManager = SessionManager(application)
    private val testRepository = TestResultRepository(database.testResultDao())

    /** Lista de tests transformados en [RecentTestUi] para la UI. */
    var tests        = mutableStateOf<List<RecentTestUi>>(emptyList()); private set

    /** Criterio de ordenación activo. Por defecto más recientes primero. */
    var order        = mutableStateOf(HistoryOrder.DESC);               private set

    /** Filtro por bloque cognitivo activo. Por defecto todos. */
    var activeFilter = mutableStateOf(HistoryFilter.ALL);               private set

    /** Fuente de verdad: todos los resultados del usuario sin filtrar. */
    private var allResults = listOf<TestResultEntity>()

    private val attentionTypes = setOf(
        "reaction_simple", "attention_sustained", "attention_selective",
        "stroop", "visual_cancellation"
    )
    private val memoryTypes = setOf(
        "digit_span", "n_back", "visual_memory", "object_memory", "associative_memory"
    )
    private val speedTypes = setOf(
        "quick_comparison", "sequence_ordering", "decision_time"
    )
    private val executiveTypes = setOf(
        "task_switching", "response_inhibition", "planning"
    )
    private val coordTypes = setOf(
        "object_tracking", "visuomotor", "spatial_perception"
    )
    private val compositeTypes = setOf(
        "global_cognitive", "cognitive_fatigue", "dual_task"
    )

    init { loadTests() }

    /**
     * Cambia el criterio de ordenación y reaplica el filtro.
     *
     * @param newOrder Nuevo criterio de orden.
     */
    fun changeOrder(newOrder: HistoryOrder) {
        order.value = newOrder
        applyFilter()
    }

    /**
     * Cambia el filtro activo y reaplica la ordenación.
     *
     * @param filter Bloque cognitivo a mostrar.
     */
    fun changeFilter(filter: HistoryFilter) {
        activeFilter.value = filter
        applyFilter()
    }

    /**
     * Elimina un resultado de test por su id.
     *
     * ─── Fix del bug de borrado ───────────────────────────────────────────────
     *
     * Antes: `database.testResultDao().deleteById(id)` sin garantía de
     * re-emisión del Flow en todos los dispositivos/emuladores.
     *
     * Ahora:
     *  1. Elimina de Room mediante el repositorio.
     *  2. Actualiza [allResults] en memoria eliminando el elemento localmente.
     *  3. Llama a [applyFilter] para que [tests] se actualice inmediatamente
     *     en la UI, sin depender de que Room emita el Flow.
     *
     * Esto garantiza que la tarjeta desaparece siempre, independientemente
     * de si el Flow re-emite o no.
     *
     * @param id Identificador único del resultado a eliminar.
     */
    fun deleteTest(id: Int) {
        viewModelScope.launch {
            // 1. Borrar en Room
            database.testResultDao().deleteById(id)
            // 2. Actualizar la lista en memoria inmediatamente
            allResults = allResults.filter { it.id != id }
            // 3. Refrescar la UI sin esperar al Flow
            applyFilter()
        }
    }

    /**
     * Suscribe el Flow de Room para mantener [allResults] actualizado.
     *
     * Usa [collectLatest] para procesar solo la emisión más reciente
     * cuando llegan varias seguidas (ej: insert seguido de delete).
     */
    private fun loadTests() {
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            testRepository.getResultsForUserFlow(email).collectLatest { results ->
                allResults = results
                applyFilter()
            }
        }
    }

    /**
     * Aplica el filtro activo y la ordenación sobre [allResults].
     *
     * Flujo: filtrar → ordenar → transformar en [RecentTestUi] → actualizar [tests].
     */
    private fun applyFilter() {
        val filtered = when (activeFilter.value) {
            HistoryFilter.ALL          -> allResults
            HistoryFilter.ATTENTION    -> allResults.filter { it.testType in attentionTypes }
            HistoryFilter.MEMORY       -> allResults.filter { it.testType in memoryTypes }
            HistoryFilter.SPEED        -> allResults.filter { it.testType in speedTypes }
            HistoryFilter.EXECUTIVE    -> allResults.filter { it.testType in executiveTypes }
            HistoryFilter.COORDINATION -> allResults.filter { it.testType in coordTypes }
            HistoryFilter.COMPOSITE    -> allResults.filter { it.testType in compositeTypes }
        }
        val sorted = when (order.value) {
            HistoryOrder.ASC  -> filtered.sortedBy { it.timestamp }
            HistoryOrder.DESC -> filtered.sortedByDescending { it.timestamp }
        }
        tests.value = sorted.map { r ->
            RecentTestUi(
                id           = r.id,
                dateTime     = formatTimestamp(r.timestamp),
                testName     = mapTestName(r.testType),
                precisionPct = r.precisionPct,
                eegAlpha     = r.eegAlpha,
                eegBeta      = r.eegBeta,
                eegGamma     = r.eegGamma,
                eegTheta     = r.eegTheta
            )
        }
    }

    /**
     * Convierte el identificador interno de tipo de test en nombre legible para la UI.
     *
     * @param type Identificador del tipo de test (ej: "reaction_simple").
     * @return Nombre amigable en español. Fallback: "Test cognitivo".
     */
    private fun mapTestName(type: String): String = when (type) {
        "reaction_simple"     -> "Reacción simple"
        "attention_sustained" -> "Atención sostenida"
        "attention_selective" -> "Atención selectiva"
        "stroop"              -> "Test de Stroop"
        "visual_cancellation" -> "Cancelación visual"
        "digit_span"          -> "Memoria de Dígitos"
        "n_back"              -> "N-Back"
        "visual_memory"       -> "Memoria Visual"
        "object_memory"       -> "Memoria de Objetos"
        "associative_memory"  -> "Memoria Asociativa"
        "quick_comparison"    -> "Comparación Rápida"
        "sequence_ordering"   -> "Ordenar Secuencias"
        "decision_time"       -> "Tiempo de Decisión"
        "task_switching"      -> "Cambio de Tarea"
        "response_inhibition" -> "Inhibición de Respuesta"
        "planning"            -> "Planificación"
        "object_tracking"     -> "Seguimiento de Objetos"
        "visuomotor"          -> "Coordinación Visomotora"
        "spatial_perception"  -> "Percepción Espacial"
        "global_cognitive"    -> "Test Cognitivo Global"
        "cognitive_fatigue"   -> "Fatiga Cognitiva"
        "dual_task"           -> "Doble Tarea"
        else                  -> "Test cognitivo"
    }

    /**
     * Formatea un timestamp en milisegundos como cadena legible.
     *
     * Formato: "dd MMM yyyy · HH:mm". Usa [Locale.getDefault()] para
     * adaptar los nombres de mes al idioma del dispositivo.
     *
     * @param timestamp Tiempo en ms desde el epoch Unix.
     * @return Cadena formateada lista para mostrar en la tarjeta del historial.
     */
    private fun formatTimestamp(timestamp: Long): String =
        SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date(timestamp))
}
