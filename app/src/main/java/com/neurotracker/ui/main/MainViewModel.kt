package com.neurotracker.ui.main

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.entities.TestResultEntity
import com.neurotracker.data.repository.TestResultRepository
import com.neurotracker.data.repository.UserRepository
import com.neurotracker.data.session.SessionEvents
import com.neurotracker.data.session.SessionManager
import com.neurotracker.viewmodel.BadgeUi
import com.neurotracker.viewmodel.RecentTestUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ViewModel principal de la pantalla Home/Dashboard.
 *
 * Responsabilidades:
 *  - Cargar datos del usuario autenticado (nombre, email).
 *  - Obtener y transformar resultados de tests para la UI.
 *  - Calcular métricas: precisión media global, racha, progreso diario y semanal.
 *  - Generar recomendaciones personalizadas según el bloque cognitivo más débil.
 *  - Gestionar el sistema de logros con detección de nuevos desbloqueos.
 *  - Controlar las cuentas atrás de reinicio diario y semanal.
 *  - Escuchar [SessionEvents.emailChanged] para recargar los datos de forma
 *    instantánea cuando el email del usuario cambia desde la pantalla de perfil.
 *
 * @param application Contexto de aplicación para acceder a Room y SessionManager.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database             = NeuroTrackerDatabase.getDatabase(application)
    private val userRepository       = UserRepository(database.userDao())
    private val sessionManager       = SessionManager(application)
    private val testResultRepository = TestResultRepository(database.testResultDao())

    var userName        = mutableStateOf(""); private set
    var userEmail       = mutableStateOf(""); private set
    var recentTests     = mutableStateOf<List<RecentTestUi>>(emptyList()); private set
    var avgPrecisionAll = mutableStateOf(0);  private set
    var streakDays      = mutableStateOf(0);  private set
    var dailyTests      = mutableStateOf(0);  private set
    val dailyGoal       = 3
    var dailyCountdown  = mutableStateOf(""); private set
    var weeklyTests     = mutableStateOf(0);  private set
    val weeklyGoal      = 5
    var weeklyCountdown = mutableStateOf(""); private set
    var recommendation  = mutableStateOf(""); private set
    var badges          = mutableStateOf<List<BadgeUi>>(emptyList()); private set
    var totalTests      = mutableStateOf(0);  private set
    var isLoading       = mutableStateOf(true); private set
    var newlyUnlockedBadge = mutableStateOf<BadgeUi?>(null); private set

    private var previousUnlockedTitles = setOf<String>()
    private var isFirstLoad            = true
    private var dataJob: Job?          = null

    init {
        loadUser()
        startCountdowns()
        listenEmailChanges()
    }

    /**
     * Descarta la notificación del último logro desbloqueado.
     */
    fun dismissBadgeNotification() { newlyUnlockedBadge.value = null }

    /**
     * Cierra la sesión del usuario actual.
     */
    fun logout() { sessionManager.clearSession() }

    /**
     * Recarga todos los datos del dashboard desde el email actual de sesión.
     *
     * Cancela el Job de carga anterior para evitar que dos colectores del
     * Flow corran en paralelo, luego relanza la carga con el nuevo email.
     */
    fun reload() {
        isFirstLoad = true
        previousUnlockedTitles = setOf()
        dataJob?.cancel()
        loadUser()
    }

    /**
     * Suscribe el ViewModel a [SessionEvents.emailChanged].
     *
     * Cuando [com.neurotracker.ui.profile.ProfileViewModel] emite el nuevo
     * email tras un cambio, este colector llama a [reload] de forma inmediata,
     * sin necesitar que el usuario regrese manualmente al dashboard ni esperar
     * ningún evento del ciclo de vida.
     */
    private fun listenEmailChanges() {
        viewModelScope.launch {
            SessionEvents.emailChanged.collect {
                reload()
            }
        }
    }

    /**
     * Inicia la corrutina que actualiza las cuentas atrás cada segundo.
     */
    private fun startCountdowns() {
        viewModelScope.launch {
            while (true) {
                dailyCountdown.value  = timeUntilMidnight()
                weeklyCountdown.value = timeUntilMonday()
                delay(1000L)
            }
        }
    }

    /**
     * Calcula el tiempo restante hasta la próxima medianoche.
     *
     * @return Cadena en formato HH:mm:ss.
     */
    private fun timeUntilMidnight(): String {
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }
        val diff = (midnight.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val h = diff / 3_600_000; val m = (diff % 3_600_000) / 60_000; val s = (diff % 60_000) / 1000
        return "${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"
    }

    /**
     * Calcula el tiempo restante hasta el próximo lunes a las 00:00.
     *
     * @return Cadena en formato "Xd HH:mm:ss" si quedan días, o "HH:mm:ss" si es el mismo día.
     */
    private fun timeUntilMonday(): String {
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
            val days = when (get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY->7; Calendar.TUESDAY->6; Calendar.WEDNESDAY->5
                Calendar.THURSDAY->4; Calendar.FRIDAY->3; Calendar.SATURDAY->2
                Calendar.SUNDAY->1; else->7
            }
            add(Calendar.DAY_OF_YEAR, days)
        }
        val diff = (next.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val d = diff / 86_400_000; val h = (diff % 86_400_000) / 3_600_000
        val m = (diff % 3_600_000) / 60_000; val s = (diff % 60_000) / 1000
        return if (d > 0) "${d}d ${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"
        else "${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"
    }

    /**
     * Carga los datos del usuario autenticado y suscribe el Flow de resultados de tests.
     *
     * El Job se guarda en [dataJob] para poder cancelarlo desde [reload] antes
     * de lanzar un nuevo colector con el email actualizado.
     */
    private fun loadUser() {
        dataJob = viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: run { isLoading.value = false; return@launch }
            val user  = userRepository.getUserByEmail(email) ?: run { isLoading.value = false; return@launch }
            userName.value  = user.name
            userEmail.value = user.email

            testResultRepository.getResultsForUserFlow(email).collectLatest { results ->
                totalTests.value      = results.size
                avgPrecisionAll.value = if (results.isEmpty()) 0
                else results.map { it.precisionPct }.average().toInt()

                recentTests.value = results.take(5).map { r ->
                    RecentTestUi(
                        id           = r.id,
                        dateTime     = formatTimestamp(r.timestamp),
                        testName     = mapTestName(r.testType),
                        precisionPct = r.precisionPct,
                        eegAlpha     = r.eegAlpha, eegBeta  = r.eegBeta,
                        eegGamma     = r.eegGamma, eegTheta = r.eegTheta
                    )
                }

                streakDays.value     = calculateStreak(results)
                dailyTests.value     = calculateDailyTests(results)
                weeklyTests.value    = calculateWeeklyTests(results)
                recommendation.value = calculateRecommendation(results)

                val newBadges = calculateBadges(results)
                badges.value  = newBadges

                if (isFirstLoad) {
                    previousUnlockedTitles = newBadges.filter { it.unlocked }.map { it.title }.toSet()
                    isFirstLoad = false
                } else {
                    val currentUnlocked = newBadges.filter { it.unlocked }.map { it.title }.toSet()
                    val justUnlocked    = currentUnlocked - previousUnlockedTitles
                    if (justUnlocked.isNotEmpty()) {
                        newlyUnlockedBadge.value = newBadges.first { it.title in justUnlocked }
                    }
                    previousUnlockedTitles = currentUnlocked
                }

                isLoading.value = false
            }
        }
    }

    /**
     * Calcula la racha de días consecutivos con al menos un test realizado.
     *
     * @param results Lista de resultados del usuario.
     * @return Número de días consecutivos con actividad hasta hoy o ayer.
     */
    private fun calculateStreak(results: List<TestResultEntity>): Int {
        if (results.isEmpty()) return 0
        val fmt        = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val uniqueDays = results.map { fmt.format(Date(it.timestamp)) }
            .toSortedSet(compareByDescending { it }).toList()
        if (uniqueDays.isEmpty()) return 0
        val today     = fmt.format(Date())
        val yesterday = fmt.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time)
        if (uniqueDays.first() != today && uniqueDays.first() != yesterday) return 0
        var streak = 1
        val cal    = Calendar.getInstance()
        if (uniqueDays.first() == yesterday) cal.add(Calendar.DAY_OF_YEAR, -1)
        for (i in 1 until uniqueDays.size) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            if (uniqueDays[i] == fmt.format(cal.time)) streak++ else break
        }
        return streak
    }

    /**
     * Calcula el número de tests realizados hoy desde las 00:00.
     *
     * @param results Lista de resultados del usuario.
     * @return Número de tests del día actual.
     */
    private fun calculateDailyTests(results: List<TestResultEntity>): Int {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return results.count { it.timestamp >= start }
    }

    /**
     * Calcula el número de tests realizados en la semana actual desde el lunes.
     *
     * @param results Lista de resultados del usuario.
     * @return Número de tests de la semana actual.
     */
    private fun calculateWeeklyTests(results: List<TestResultEntity>): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
            val days = when (get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY->0; Calendar.TUESDAY->1; Calendar.WEDNESDAY->2
                Calendar.THURSDAY->3; Calendar.FRIDAY->4; Calendar.SATURDAY->5
                Calendar.SUNDAY->6; else->0
            }
            add(Calendar.DAY_OF_YEAR, -days)
        }
        return results.count { it.timestamp >= cal.timeInMillis }
    }

    /**
     * Genera una recomendación personalizada basada en el bloque cognitivo con menor precisión.
     *
     * @param results Lista de resultados del usuario.
     * @return Texto de recomendación. Cadena vacía si no hay datos suficientes.
     */
    private fun calculateRecommendation(results: List<TestResultEntity>): String {
        if (results.isEmpty()) return "¡Empieza tu primer test para obtener una recomendación!"
        val attentionTypes = setOf("reaction_simple","attention_sustained","attention_selective","stroop","visual_cancellation")
        val memoryTypes    = setOf("digit_span","n_back","visual_memory","object_memory","associative_memory")
        val speedTypes     = setOf("quick_comparison","sequence_ordering","decision_time")
        val executiveTypes = setOf("task_switching","response_inhibition","planning")
        val coordTypes     = setOf("object_tracking","visuomotor","spatial_perception")
        val compositeTypes = setOf("global_cognitive","cognitive_fatigue","dual_task")
        fun avg(types: Set<String>): Float? {
            val f = results.filter { it.testType in types }
            return if (f.isEmpty()) null else f.map { it.precisionPct }.average().toFloat()
        }
        val blockAverages = mapOf(
            "Atención" to avg(attentionTypes), "Memoria" to avg(memoryTypes),
            "Velocidad" to avg(speedTypes),    "Funciones Ejecutivas" to avg(executiveTypes),
            "Coordinación" to avg(coordTypes), "Tests Compuestos" to avg(compositeTypes)
        ).filterValues { it != null }
        if (blockAverages.isEmpty()) return "Realiza tests de distintos bloques para recibir recomendaciones."
        return when (blockAverages.minByOrNull { it.value!! }?.key) {
            "Atención"             -> "🎯 Trabaja la Atención. Prueba el Test de Reacción o el Stroop."
            "Memoria"              -> "🧠 Tu memoria necesita práctica. Prueba el N-Back o la Memoria de Dígitos."
            "Velocidad"            -> "⚡ Tu velocidad puede mejorar. Prueba Comparación Rápida o Tiempo de Decisión."
            "Funciones Ejecutivas" -> "🔀 Trabaja funciones ejecutivas — Cambio de Tarea o Planificación."
            "Coordinación"         -> "🎮 Tu coordinación es mejorable. Prueba el Seguimiento de Objetos."
            "Tests Compuestos"     -> "🌐 Prueba el Test Cognitivo Global para un entrenamiento completo."
            else                   -> ""
        }
    }

    /**
     * Calcula el estado de desbloqueo y el progreso de cada logro.
     *
     * @param results Lista de resultados del usuario.
     * @return Lista de [BadgeUi] con el estado actualizado de cada logro.
     */
    private fun calculateBadges(results: List<TestResultEntity>): List<BadgeUi> {
        val total       = results.size
        val perfect     = results.count { it.precisionPct == 100 }
        val streak      = calculateStreak(results)
        val blockTypes  = results.map { it.testType }.toSet()
        val allBlocks   = setOf("reaction_simple","digit_span","quick_comparison","task_switching","object_tracking","global_cognitive")
        val blocksFound = allBlocks.count { it in blockTypes }
        return listOf(
            BadgeUi("🚀", "Primer paso",    "Completa tu primer test",             unlocked = total >= 1,   progressText = if (total >= 1)   "¡Completado!" else "0 / 1 tests",            progressFraction = (total   / 1f ).coerceIn(0f,1f)),
            BadgeUi("🔥", "Racha 3 días",   "Entrena 3 días seguidos",             unlocked = streak >= 3,  progressText = if (streak >= 3)  "¡Completado!" else "$streak / 3 días",        progressFraction = (streak  / 3f ).coerceIn(0f,1f)),
            BadgeUi("💯", "Perfeccionista", "Consigue un 100% en algún test",      unlocked = perfect >= 1, progressText = if (perfect >= 1) "¡Completado!" else "0 / 1 perfecto",           progressFraction = (perfect / 1f ).coerceIn(0f,1f)),
            BadgeUi("🏃", "Constante",      "Completa 10 tests",                   unlocked = total >= 10,  progressText = if (total >= 10)  "¡Completado!" else "$total / 10 tests",        progressFraction = (total   / 10f).coerceIn(0f,1f)),
            BadgeUi("🧩", "Explorador",     "Prueba tests de 6 bloques distintos", unlocked = allBlocks.all { it in blockTypes }, progressText = if (allBlocks.all { it in blockTypes }) "¡Completado!" else "$blocksFound / 6 bloques", progressFraction = (blocksFound / 6f).coerceIn(0f,1f)),
            BadgeUi("⭐", "Experto",        "Completa 50 tests",                   unlocked = total >= 50,  progressText = if (total >= 50)  "¡Completado!" else "$total / 50 tests",        progressFraction = (total   / 50f).coerceIn(0f,1f)),
            BadgeUi("🗓️","Racha 7 días",   "Entrena 7 días seguidos",             unlocked = streak >= 7,  progressText = if (streak >= 7)  "¡Completado!" else "$streak / 7 días",         progressFraction = (streak  / 7f ).coerceIn(0f,1f)),
            BadgeUi("🏆", "Maestro",        "Consigue 5 puntuaciones perfectas",   unlocked = perfect >= 5, progressText = if (perfect >= 5) "¡Completado!" else "$perfect / 5 perfectos",  progressFraction = (perfect / 5f ).coerceIn(0f,1f))
        )
    }

    /**
     * Convierte el identificador interno de tipo de test en un nombre legible para la UI.
     *
     * @param type Identificador del tipo de test.
     * @return Nombre en español. Fallback: "Test cognitivo".
     */
    private fun mapTestName(type: String): String = when (type) {
        "reaction_simple"->"Reacción simple"; "attention_sustained"->"Atención sostenida"
        "attention_selective"->"Atención selectiva"; "stroop"->"Test de Stroop"
        "visual_cancellation"->"Cancelación visual"; "digit_span"->"Memoria de Dígitos"
        "n_back"->"N-Back"; "visual_memory"->"Memoria Visual"; "object_memory"->"Memoria de Objetos"
        "associative_memory"->"Memoria Asociativa"; "quick_comparison"->"Comparación Rápida"
        "sequence_ordering"->"Ordenar Secuencias"; "decision_time"->"Tiempo de Decisión"
        "task_switching"->"Cambio de Tarea"; "response_inhibition"->"Inhibición de Respuesta"
        "planning"->"Planificación"; "object_tracking"->"Seguimiento de Objetos"
        "visuomotor"->"Coordinación Visomotora"; "spatial_perception"->"Percepción Espacial"
        "global_cognitive"->"Test Cognitivo Global"; "cognitive_fatigue"->"Fatiga Cognitiva"
        "dual_task"->"Doble Tarea"; else->"Test cognitivo"
    }

    /**
     * Formatea un timestamp en milisegundos como cadena legible.
     *
     * @param ts Timestamp en ms desde el epoch Unix.
     * @return Cadena con formato "dd MMM · HH:mm".
     */
    private fun formatTimestamp(ts: Long): String =
        SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(Date(ts))
}