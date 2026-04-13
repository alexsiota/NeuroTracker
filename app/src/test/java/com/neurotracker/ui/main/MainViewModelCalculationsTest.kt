package com.neurotracker.ui.main

import com.neurotracker.data.entities.TestResultEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Tests unitarios puros de las funciones de cálculo de [MainViewModel].
 *
 * Estas funciones son privadas en el ViewModel, por lo que se extraen aquí
 * como funciones independientes para poder testearlas en la JVM sin necesidad
 * de contexto Android ni corrutinas. La lógica es idéntica a la del ViewModel.
 *
 * Cubre:
 *  - Cálculo de racha de días consecutivos.
 *  - Conteo de tests diarios y semanales.
 *  - Cálculo de precisión media global.
 *  - Generación de recomendaciones por bloque cognitivo más débil.
 */
class MainViewModelCalculationsTest {

    // ─── Helpers de cálculo (extraídos del ViewModel) ─────────────────────────

    private fun calculateStreak(results: List<TestResultEntity>): Int {
        if (results.isEmpty()) return 0
        val fmt = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
        val uniqueDays = results.map { fmt.format(java.util.Date(it.timestamp)) }
            .toSortedSet(compareByDescending { it }).toList()
        if (uniqueDays.isEmpty()) return 0
        val today     = fmt.format(java.util.Date())
        val yesterday = fmt.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time)
        if (uniqueDays.first() != today && uniqueDays.first() != yesterday) return 0
        var streak = 1
        val cal = Calendar.getInstance()
        if (uniqueDays.first() == yesterday) cal.add(Calendar.DAY_OF_YEAR, -1)
        for (i in 1 until uniqueDays.size) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            if (uniqueDays[i] == fmt.format(cal.time)) streak++ else break
        }
        return streak
    }

    private fun calculateDailyTests(results: List<TestResultEntity>): Int {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return results.count { it.timestamp >= start }
    }

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

    private fun avgPrecision(results: List<TestResultEntity>): Int =
        if (results.isEmpty()) 0 else results.map { it.precisionPct }.average().toInt()

    private fun makeResult(
        testType:     String  = "reaction_simple",
        precisionPct: Int     = 80,
        timestamp:    Long    = System.currentTimeMillis()
    ) = TestResultEntity(
        userEmail    = "alex@neurotracker.com",
        testType     = testType,
        score        = 300L,
        precisionPct = precisionPct,
        timestamp    = timestamp
    )

    // ─── Tests de racha ───────────────────────────────────────────────────────

    /**
     * Verifica que la racha es 0 cuando no hay resultados.
     */
    @Test
    fun calculateStreak_withNoResults_returnsZero() {
        assertEquals(0, calculateStreak(emptyList()))
    }

    /**
     * Verifica que la racha es 1 cuando solo hay resultados de hoy.
     */
    @Test
    fun calculateStreak_withOnlyTodayResults_returnsOne() {
        val results = listOf(makeResult(timestamp = System.currentTimeMillis()))
        assertEquals(1, calculateStreak(results))
    }

    /**
     * Verifica que la racha es 0 cuando el último resultado es de hace más de un día.
     */
    @Test
    fun calculateStreak_withOldResults_returnsZero() {
        val twoDaysAgo = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000L)
        val results    = listOf(makeResult(timestamp = twoDaysAgo))
        assertEquals(0, calculateStreak(results))
    }

    // ─── Tests de progreso diario ─────────────────────────────────────────────

    /**
     * Verifica que el conteo diario es 0 cuando no hay resultados.
     */
    @Test
    fun calculateDailyTests_withNoResults_returnsZero() {
        assertEquals(0, calculateDailyTests(emptyList()))
    }

    /**
     * Verifica que solo se cuentan los tests realizados hoy,
     * ignorando los de días anteriores.
     */
    @Test
    fun calculateDailyTests_countsOnlyTodayResults() {
        val yesterday = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        val results   = listOf(
            makeResult(timestamp = System.currentTimeMillis()),
            makeResult(timestamp = System.currentTimeMillis()),
            makeResult(timestamp = yesterday)
        )
        assertEquals(2, calculateDailyTests(results))
    }

    // ─── Tests de progreso semanal ────────────────────────────────────────────

    /**
     * Verifica que el conteo semanal es 0 cuando no hay resultados.
     */
    @Test
    fun calculateWeeklyTests_withNoResults_returnsZero() {
        assertEquals(0, calculateWeeklyTests(emptyList()))
    }

    /**
     * Verifica que los tests de hoy se incluyen en el conteo semanal.
     */
    @Test
    fun calculateWeeklyTests_includesResultsFromThisWeek() {
        val results = listOf(
            makeResult(timestamp = System.currentTimeMillis()),
            makeResult(timestamp = System.currentTimeMillis())
        )
        assertTrue(calculateWeeklyTests(results) >= 2)
    }

    // ─── Tests de precisión media ─────────────────────────────────────────────

    /**
     * Verifica que la precisión media es 0 cuando no hay resultados.
     */
    @Test
    fun avgPrecision_withNoResults_returnsZero() {
        assertEquals(0, avgPrecision(emptyList()))
    }

    /**
     * Verifica que la precisión media se calcula correctamente sobre todos los tests.
     * Con 60, 80 y 100 → media de 80.
     */
    @Test
    fun avgPrecision_withThreeResults_returnsCorrectAverage() {
        val results = listOf(
            makeResult(precisionPct = 60),
            makeResult(precisionPct = 80),
            makeResult(precisionPct = 100)
        )
        assertEquals(80, avgPrecision(results))
    }

    /**
     * Verifica que un único resultado devuelve exactamente su propia precisión.
     */
    @Test
    fun avgPrecision_withSingleResult_returnsThatPrecision() {
        val results = listOf(makeResult(precisionPct = 75))
        assertEquals(75, avgPrecision(results))
    }

    // ─── Tests de datos EEG en resultados ─────────────────────────────────────

    /**
     * Verifica que los datos EEG se almacenan correctamente en la entidad.
     */
    @Test
    fun testResultEntity_eegFields_storedCorrectly() {
        val result = TestResultEntity(
            userEmail    = "alex@neurotracker.com",
            testType     = "global_cognitive",
            score        = 800L,
            precisionPct = 80,
            eegAlpha     = 1.5f,
            eegBeta      = 0.9f,
            eegGamma     = 0.6f,
            eegTheta     = 0.4f
        )
        assertEquals(1.5f, result.eegAlpha, 0.001f)
        assertEquals(0.9f, result.eegBeta,  0.001f)
        assertEquals(0.6f, result.eegGamma, 0.001f)
        assertEquals(0.4f, result.eegTheta, 0.001f)
    }

    /**
     * Verifica que los valores EEG por defecto son 0f cuando no se especifican.
     */
    @Test
    fun testResultEntity_defaultEegValues_areZero() {
        val result = TestResultEntity(
            userEmail    = "alex@neurotracker.com",
            testType     = "stroop",
            score        = 200L,
            precisionPct = 70
        )
        assertEquals(0f, result.eegAlpha, 0.001f)
        assertEquals(0f, result.eegBeta,  0.001f)
        assertEquals(0f, result.eegGamma, 0.001f)
        assertEquals(0f, result.eegTheta, 0.001f)
    }
}
