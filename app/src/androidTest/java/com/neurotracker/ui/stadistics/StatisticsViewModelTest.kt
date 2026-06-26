package com.neurotracker.ui.stadistics

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.entities.TestResultEntity
import com.neurotracker.data.entities.UserEntity
import com.neurotracker.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

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

    // ─── Data class tests ─────────────────────────────────────────────────────

    @Test
    fun statPoint_equality_sameValues_areEqual() {
        val a = StatPoint("Reac.", 80f, 1000L)
        val b = StatPoint("Reac.", 80f, 1000L)
        assertEquals(a, b)
    }

    @Test
    fun eegPoint_equality_sameValues_areEqual() {
        val a = EegPoint(1000L, 0.5f, 0.6f, 0.3f, 0.2f)
        val b = EegPoint(1000L, 0.5f, 0.6f, 0.3f, 0.2f)
        assertEquals(a, b)
    }

    @Test
    fun blockStats_copy_changesBlockName() {
        val stats = BlockStats("Original", emptyList(), 70f, 0f)
        val copy = stats.copy(blockName = "Nuevo")
        assertEquals("Nuevo", copy.blockName)
        assertEquals("Original", stats.blockName)
    }

    @Test
    fun eegAverages_copy_changesDominant() {
        val avg = EegAverages(0.5f, 0.6f, 0.3f, 0.2f, "Beta")
        val copy = avg.copy(dominant = "Alpha")
        assertEquals("Alpha", copy.dominant)
        assertEquals("Beta", avg.dominant)
    }

    @Test
    fun statPoint_copy_changesValue() {
        val p = StatPoint("N-Back", 70f, 2000L)
        val updated = p.copy(value = 90f)
        assertEquals(90f, updated.value, 0.001f)
        assertEquals(70f, p.value, 0.001f)
    }

    // ─── Tests con base de datos real y sesión ────────────────────────────────

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val db get() = NeuroTrackerDatabase.getDatabase(app)
    private val session get() = SessionManager(app)

    @Test
    fun withRealData_afterOneAttentionResult_attentionStatsNotNullAndTotalIsOne() = runTest {
        val email = "stats.att.${UUID.randomUUID()}@test.com"
        try {
            withContext(Dispatchers.IO) {
                db.userDao().insertUser(UserEntity(name = "Test", email = email, birthDate = 0L, passwordHash = "hash"))
                db.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "reaction_simple", score = 0L, precisionPct = 80))
            }
            session.saveSession(email)
            val vm = StatisticsViewModel(app)
            advanceUntilIdle()
            assertEquals(1, vm.totalTests.value)
            assertNotNull(vm.attentionStats.value)
            assertEquals("Atención", vm.attentionStats.value!!.blockName)
            assertNull(vm.memoryStats.value)
        } finally {
            session.clearSession()
        }
    }

    @Test
    fun withRealData_avgGlobal_isCorrect() = runTest {
        val email = "stats.avg.${UUID.randomUUID()}@test.com"
        try {
            withContext(Dispatchers.IO) {
                db.userDao().insertUser(UserEntity(name = "Test", email = email, birthDate = 0L, passwordHash = "hash"))
                db.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "stroop", score = 0L, precisionPct = 60))
                db.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "stroop", score = 0L, precisionPct = 80))
            }
            session.saveSession(email)
            val vm = StatisticsViewModel(app)
            advanceUntilIdle()
            assertEquals(70f, vm.avgGlobal.value, 0.001f)
        } finally {
            session.clearSession()
        }
    }

    @Test
    fun withRealData_ascendingPrecisions_attentionStatsHasPositiveTrend() = runTest {
        val email = "stats.trend.${UUID.randomUUID()}@test.com"
        try {
            withContext(Dispatchers.IO) {
                db.userDao().insertUser(UserEntity(name = "Test", email = email, birthDate = 0L, passwordHash = "hash"))
                db.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "reaction_simple", score = 0L, timestamp = 1000L, precisionPct = 60))
                db.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "reaction_simple", score = 0L, timestamp = 2000L, precisionPct = 80))
            }
            session.saveSession(email)
            val vm = StatisticsViewModel(app)
            advanceUntilIdle()
            val trend = vm.attentionStats.value?.trend ?: 0f
            assertTrue(trend > 0f)
        } finally {
            session.clearSession()
        }
    }

    @Test
    fun withRealData_eegResult_setsHasEegDataTrueAndDominantBandBeta() = runTest {
        val email = "stats.eeg.${UUID.randomUUID()}@test.com"
        try {
            withContext(Dispatchers.IO) {
                db.userDao().insertUser(UserEntity(name = "Test", email = email, birthDate = 0L, passwordHash = "hash"))
                db.testResultDao().insertResult(TestResultEntity(
                    userEmail = email, testType = "reaction_simple", score = 0L, precisionPct = 70,
                    eegAlpha = 0.2f, eegBeta = 0.9f, eegGamma = 0.3f, eegTheta = 0.1f
                ))
            }
            session.saveSession(email)
            val vm = StatisticsViewModel(app)
            advanceUntilIdle()
            assertTrue(vm.hasEegData.value)
            assertNotNull(vm.eegAverages.value)
            assertEquals("Beta", vm.eegAverages.value?.dominant)
            assertTrue(vm.eegEvolution.value.isNotEmpty())
        } finally {
            session.clearSession()
        }
    }

    @Test
    fun withRealData_memoryHigherThanAttention_bestBlockIsMemoria() = runTest {
        val email = "stats.best.${UUID.randomUUID()}@test.com"
        try {
            withContext(Dispatchers.IO) {
                db.userDao().insertUser(UserEntity(name = "Test", email = email, birthDate = 0L, passwordHash = "hash"))
                db.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "reaction_simple", score = 0L, precisionPct = 70))
                db.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "digit_span", score = 0L, precisionPct = 90))
            }
            session.saveSession(email)
            val vm = StatisticsViewModel(app)
            advanceUntilIdle()
            assertEquals("Memoria", vm.bestBlock.value)
        } finally {
            session.clearSession()
        }
    }

    @Test
    fun withRealData_stroopResult_globalEvolutionLabelIsStroop() = runTest {
        val email = "stats.labels.${UUID.randomUUID()}@test.com"
        try {
            withContext(Dispatchers.IO) {
                db.userDao().insertUser(UserEntity(name = "Test", email = email, birthDate = 0L, passwordHash = "hash"))
                db.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "stroop", score = 0L, precisionPct = 75))
            }
            session.saveSession(email)
            val vm = StatisticsViewModel(app)
            advanceUntilIdle()
            assertTrue(vm.globalEvolution.value.isNotEmpty())
            assertEquals("Stroop", vm.globalEvolution.value.first().label)
        } finally {
            session.clearSession()
        }
    }

    @Test
    fun withRealData_allSixBlockTypes_allBlockStatsAreNotNull() = runTest {
        val email = "stats.all6.${UUID.randomUUID()}@test.com"
        try {
            withContext(Dispatchers.IO) {
                db.userDao().insertUser(UserEntity(name = "Test", email = email, birthDate = 0L, passwordHash = "hash"))
                db.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "reaction_simple", score = 0L, precisionPct = 70))
                db.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "digit_span", score = 0L, precisionPct = 80))
                db.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "quick_comparison", score = 0L, precisionPct = 75))
                db.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "task_switching", score = 0L, precisionPct = 65))
                db.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "object_tracking", score = 0L, precisionPct = 85))
                db.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "global_cognitive", score = 0L, precisionPct = 90))
            }
            session.saveSession(email)
            val vm = StatisticsViewModel(app)
            advanceUntilIdle()
            assertNotNull(vm.attentionStats.value)
            assertNotNull(vm.memoryStats.value)
            assertNotNull(vm.speedStats.value)
            assertNotNull(vm.executiveStats.value)
            assertNotNull(vm.coordStats.value)
            assertNotNull(vm.compositeStats.value)
        } finally {
            session.clearSession()
        }
    }

    @Test
    fun withRealData_eegEvolution_timestampAndBandsMatchInsertedData() = runTest {
        val email = "stats.eegevo.${UUID.randomUUID()}@test.com"
        try {
            withContext(Dispatchers.IO) {
                db.userDao().insertUser(UserEntity(name = "Test", email = email, birthDate = 0L, passwordHash = "hash"))
                db.testResultDao().insertResult(TestResultEntity(
                    userEmail = email, testType = "n_back", score = 0L, timestamp = 5000L, precisionPct = 65,
                    eegAlpha = 0.4f, eegBeta = 0.3f, eegGamma = 0.7f, eegTheta = 0.1f
                ))
            }
            session.saveSession(email)
            val vm = StatisticsViewModel(app)
            advanceUntilIdle()
            val pt = vm.eegEvolution.value.firstOrNull()
            assertNotNull(pt)
            assertEquals(5000L, pt!!.timestamp)
            assertEquals(0.4f, pt.alpha, 0.001f)
            assertEquals(0.7f, pt.gamma, 0.001f)
        } finally {
            session.clearSession()
        }
    }
}