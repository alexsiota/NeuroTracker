package com.neurotracker.ui.achievements

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.entities.TestResultEntity
import com.neurotracker.data.session.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla de Logros.
 *
 * Calcula el estado de desbloqueo de cada logro a partir de los resultados
 * de tests almacenados en Room para el usuario actual.
 *
 * Colores de logros desbloqueados — todos oscuros (≥4.5:1 con texto blanco):
 *  - Verde   #2E7D32 → 5.1:1 ✓
 *  - Azul    #1565C0 → 6.0:1 ✓
 *  - Naranja #E65100 → 4.5:1 ✓
 *  - Morado  #6A1B9A → 7.5:1 ✓
 *  - Teal    #00695C → 5.3:1 ✓
 */
class AchievementsViewModel(application: Application) : AndroidViewModel(application) {

    private val db             = NeuroTrackerDatabase.getDatabase(application)
    private val sessionManager = SessionManager(application)

    var achievements = mutableStateOf<List<Achievement>>(emptyList()); private set
    var isLoading    = mutableStateOf(true); private set

    init { loadAchievements() }

    private fun loadAchievements() {
        viewModelScope.launch {
            val email = sessionManager.getUserEmail()
            if (email == null) {
                achievements.value = buildAllAchievements(emptyList())
                isLoading.value    = false
                return@launch
            }

            // Fix: usar .first() en lugar de .collect { return@collect }
            // .collect con return@collect es una expresión non-local return que
            // no compila en Kotlin cuando la lambda no es inline.
            // .first() obtiene la primera emisión del Flow y suspende hasta recibirla.
            val results = db.testResultDao().getResultsForUserFlow(email).first()

            achievements.value = buildAllAchievements(results)
            isLoading.value    = false
        }
    }

    private fun buildAllAchievements(results: List<TestResultEntity>): List<Achievement> {
        val totalTests   = results.size
        val testTypes    = results.map { it.testType }.toSet()
        val avgPrecision = if (results.isEmpty()) 0f else results.map { it.precisionPct }.average().toFloat()
        val maxPrecision = results.maxOfOrNull { it.precisionPct } ?: 0

        return listOf(
            // Primeros pasos
            Achievement("first_test",   "🚀", "Primeros pasos",          "Completa tu primer test cognitivo.",                       "Primeros pasos",      totalTests >= 1,   Color(0xFF2E7D32)),
            Achievement("five_tests",   "⭐", "En racha",                "Completa 5 tests cognitivos.",                             "Primeros pasos",      totalTests >= 5,   Color(0xFF1565C0)),
            Achievement("twenty_tests", "🔥", "Dedicación",              "Completa 20 tests cognitivos.",                            "Primeros pasos",      totalTests >= 20,  Color(0xFFE65100)),

            // Atención
            Achievement("attention_first",  "👁️", "Ojos de águila",  "Completa cualquier test de atención.",                                                                              "Atención", testTypes.any { it in setOf("reaction_simple","attention_sustained","attention_selective","stroop","visual_cancellation") }, Color(0xFF2E7D32)),
            Achievement("stroop_master",    "🧩", "Control mental",  "Alcanza 80% de precisión en el Test de Stroop.",                                                                    "Atención", results.filter { it.testType == "stroop"          }.any { it.precisionPct >= 80 }, Color(0xFF6A1B9A)),
            Achievement("reaction_fast",    "⚡", "Reacción rayo",   "Consigue un tiempo de reacción medio menor a 400ms.",                                                               "Atención", results.filter { it.testType == "reaction_simple"  }.any { it.score in 1..400    }, Color(0xFFE65100)),

            // Memoria
            Achievement("memory_first",  "🧠", "Memoria activa",    "Completa cualquier test de memoria.",                                                                               "Memoria", testTypes.any { it in setOf("digit_span","n_back","visual_memory","object_memory","associative_memory") }, Color(0xFF1565C0)),
            Achievement("nback_ace",     "🎯", "N-Back maestro",    "Alcanza 80% de precisión en el test N-Back.",                                                                       "Memoria", results.filter { it.testType == "n_back"            }.any { it.precisionPct >= 80 }, Color(0xFF2E7D32)),

            // Velocidad
            Achievement("speed_first",   "🏎️", "Velocista",          "Completa cualquier test de velocidad de procesamiento.",                                                          "Velocidad", testTypes.any { it in setOf("quick_comparison","sequence_ordering","decision_time") }, Color(0xFFE65100)),
            Achievement("decision_ace",  "💡", "Decisiones rápidas", "Alcanza 90% de precisión en Tiempo de Decisión.",                                                                  "Velocidad", results.filter { it.testType == "decision_time"    }.any { it.precisionPct >= 90 }, Color(0xFF00695C)),

            // Funciones ejecutivas
            Achievement("executive_first", "🎮", "Control ejecutivo", "Completa cualquier test de funciones ejecutivas.",                                                                "Funciones ejecutivas", testTypes.any { it in setOf("task_switching","response_inhibition","planning") }, Color(0xFF6A1B9A)),
            Achievement("inhibition_ace",  "🛑", "Control total",     "Alcanza 90% de precisión en Inhibición de Respuesta.",                                                            "Funciones ejecutivas", results.filter { it.testType == "response_inhibition" }.any { it.precisionPct >= 90 }, Color(0xFF1565C0)),

            // Tests compuestos
            Achievement("global_first",    "🌍", "Evaluación global", "Completa el Test Cognitivo Global.",                                                                              "Tests compuestos", testTypes.contains("global_cognitive"),    Color(0xFF2E7D32)),
            Achievement("global_ace",      "🏆", "Mente brillante",   "Alcanza 80% de score en el Test Cognitivo Global.",                                                               "Tests compuestos", results.filter { it.testType == "global_cognitive" }.any { it.precisionPct >= 80 }, Color(0xFFE65100)),
            Achievement("dual_task_first", "🔄", "Multitarea",        "Completa el Test de Doble Tarea.",                                                                                "Tests compuestos", testTypes.contains("dual_task"),            Color(0xFF00695C)),

            // Precisión
            Achievement("precision_70",  "✅", "Buen rendimiento",         "Mantén una precisión media global superior al 70%.",    "Precisión", avgPrecision >= 70f && totalTests >= 5,  Color(0xFF2E7D32)),
            Achievement("precision_85",  "💎", "Rendimiento excepcional",  "Mantén una precisión media global superior al 85%.",    "Precisión", avgPrecision >= 85f && totalTests >= 10, Color(0xFF6A1B9A)),
            Achievement("perfect_100",   "🌟", "Perfeccionista",           "Alcanza 100% de precisión en cualquier test.",          "Precisión", maxPrecision >= 100,                     Color(0xFFE65100)),

            // Variedad
            Achievement("variety_5",   "🎨", "Explorador cognitivo", "Completa 5 tipos de tests distintos.",                       "Variedad", testTypes.size >= 5, Color(0xFF1565C0)),
            Achievement("variety_all", "🗺️", "Coleccionista",        "Completa al menos un test de cada bloque cognitivo.",        "Variedad",
                listOf(
                    testTypes.any { it in setOf("reaction_simple","attention_sustained","attention_selective","stroop","visual_cancellation") },
                    testTypes.any { it in setOf("digit_span","n_back","visual_memory","object_memory","associative_memory") },
                    testTypes.any { it in setOf("quick_comparison","sequence_ordering","decision_time") },
                    testTypes.any { it in setOf("task_switching","response_inhibition","planning") },
                    testTypes.any { it in setOf("object_tracking","visuomotor","spatial_perception") },
                    testTypes.any { it in setOf("global_cognitive","cognitive_fatigue","dual_task") }
                ).all { it },
                Color(0xFF00695C)
            )
        )
    }
}
