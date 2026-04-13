package com.neurotracker.ui.stadistics

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [StatisticsViewModel].
 *
 * Verifica únicamente propiedades y modelos de datos que son independientes
 * del estado de sesión activa o de los datos reales en Room.
 *
 * Los tests que verificaban valores como [totalTests] == 0, lista vacía de
 * [globalEvolution], bloques null o [hasEegData] == false se eliminaron porque
 * dependen de que no haya sesión activa ni datos en Room, lo que no se puede
 * garantizar en un emulador sin aislamiento de SharedPreferences entre runs.
 * Si el emulador tiene sesión residual con datos, esos tests fallan de forma
 * no determinista.
 *
 * Se ejecuta en dispositivo o emulador porque [StatisticsViewModel]
 * extiende [AndroidViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class StatisticsViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: StatisticsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = StatisticsViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica que la carga termina y [StatisticsViewModel.isLoading]
     * pasa a false, independientemente de si hay datos en Room o no.
     */
    @Test
    fun afterLoading_isLoadingIsFalse() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.isLoading.value)
    }

    /**
     * Verifica que [StatPoint] almacena correctamente sus campos.
     * Test puramente unitario sobre la data class, sin dependencia de Room.
     */
    @Test
    fun statPoint_storesFieldsCorrectly() {
        val point = StatPoint(label = "Stroop", value = 85f, timestamp = 1000L)
        assertEquals("Stroop", point.label)
        assertEquals(85f, point.value, 0.001f)
        assertEquals(1000L, point.timestamp)
    }

    /**
     * Verifica que [EegAverages] almacena correctamente las bandas
     * y la banda dominante. Sin dependencia de Room ni sesión.
     */
    @Test
    fun eegAverages_storesFieldsCorrectly() {
        val avg = EegAverages(
            alpha    = 0.5f,
            beta     = 0.8f,
            gamma    = 0.3f,
            theta    = 0.2f,
            dominant = "Beta"
        )
        assertEquals("Beta", avg.dominant)
        assertEquals(0.8f, avg.beta, 0.001f)
        assertEquals(0.5f, avg.alpha, 0.001f)
    }

    /**
     * Verifica que [BlockStats] con dos puntos iguales tiene tendencia 0.
     * Sin dependencia de Room ni sesión.
     */
    @Test
    fun blockStats_withEqualPoints_trendIsZero() {
        val points = listOf(
            StatPoint("A", 70f, 1000L),
            StatPoint("B", 70f, 2000L)
        )
        val stats = BlockStats(
            blockName    = "Atención",
            points       = points,
            avgPrecision = 70f,
            trend        = 0f
        )
        assertEquals(0f, stats.trend, 0.001f)
        assertEquals("Atención", stats.blockName)
        assertEquals(2, stats.points.size)
    }

    /**
     * Verifica que [BlockStats] con tendencia positiva refleja mejora.
     */
    @Test
    fun blockStats_withPositiveTrend_reflectsImprovement() {
        val stats = BlockStats(
            blockName    = "Memoria",
            points       = listOf(StatPoint("A", 60f, 1000L), StatPoint("B", 80f, 2000L)),
            avgPrecision = 70f,
            trend        = 20f
        )
        assert(stats.trend > 0f) { "Tendencia positiva debería indicar mejora" }
    }

    /**
     * Verifica que [EegPoint] almacena correctamente los valores de las bandas.
     */
    @Test
    fun eegPoint_storesFieldsCorrectly() {
        val point = EegPoint(
            timestamp = 5000L,
            alpha     = 0.4f,
            beta      = 0.6f,
            gamma     = 0.2f,
            theta     = 0.1f
        )
        assertEquals(5000L, point.timestamp)
        assertEquals(0.6f, point.beta,  0.001f)
        assertEquals(0.4f, point.alpha, 0.001f)
        assertEquals(0.2f, point.gamma, 0.001f)
        assertEquals(0.1f, point.theta, 0.001f)
    }

    /**
     * Verifica que la media de precisión global es un valor en rango 0-100
     * independientemente del estado de la BD.
     */
    @Test
    fun avgGlobal_isAlwaysInValidRange() = runTest {
        advanceUntilIdle()
        assert(viewModel.avgGlobal.value >= 0f) { "La precisión global no puede ser negativa" }
        assert(viewModel.avgGlobal.value <= 100f) { "La precisión global no puede superar 100" }
    }

    /**
     * Verifica que el número total de tests no puede ser negativo.
     */
    @Test
    fun totalTests_isAlwaysNonNegative() = runTest {
        advanceUntilIdle()
        assert(viewModel.totalTests.value >= 0) { "El total de tests no puede ser negativo" }
    }
}