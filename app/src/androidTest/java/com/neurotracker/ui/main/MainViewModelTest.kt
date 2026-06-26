package com.neurotracker.ui.main

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.entities.TestResultEntity
import com.neurotracker.data.entities.UserEntity
import com.neurotracker.data.session.SessionManager
import com.neurotracker.viewmodel.BadgeUi
import com.neurotracker.viewmodel.RecentTestUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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
 * Tests instrumentados del [MainViewModel].
 *
 * Cubre:
 *  - Constantes de objetivos (dailyGoal, weeklyGoal).
 *  - isLoading comienza en true y se resuelve a false cuando no hay sesión.
 *  - dismissBadgeNotification limpia newlyUnlockedBadge (que empieza a null).
 *  - Las cuentas atrás (dailyCountdown, weeklyCountdown) se rellenan al arrancar.
 *  - logout no lanza excepciones.
 *
 * Sin sesión activa, loadUser() retorna inmediatamente al no encontrar email,
 * lo que simplifica el ciclo de vida del ViewModel en pruebas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MainViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MainViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun dailyGoal_isThree() {
        assertEquals(3, viewModel.dailyGoal)
    }

    @Test
    fun weeklyGoal_isFive() {
        assertEquals(5, viewModel.weeklyGoal)
    }

    @Test
    fun isLoading_startsTrue() {
        assertTrue(viewModel.isLoading.value)
    }

    @Test
    fun isLoading_becomesFalseAfterInitWithNoSession() {
        testDispatcher.scheduler.runCurrent()
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun newlyUnlockedBadge_initiallyNull() {
        assertNull(viewModel.newlyUnlockedBadge.value)
    }

    @Test
    fun dismissBadgeNotification_leavesNullWhenAlreadyNull() {
        viewModel.dismissBadgeNotification()
        assertNull(viewModel.newlyUnlockedBadge.value)
    }

    @Test
    fun dailyCountdown_isSetAfterInit() {
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.dailyCountdown.value.isNotEmpty())
    }

    @Test
    fun weeklyCountdown_isSetAfterInit() {
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.weeklyCountdown.value.isNotEmpty())
    }

    @Test
    fun dailyCountdown_matchesHHmmssFormat() {
        testDispatcher.scheduler.runCurrent()
        val countdown = viewModel.dailyCountdown.value
        assertTrue("Expected HH:mm:ss format, got: $countdown",
            countdown.matches(Regex("\\d{2}:\\d{2}:\\d{2}")))
    }

    @Test
    fun logout_doesNotThrow() {
        viewModel.logout()
    }

    @Test
    fun initialState_recentTestsIsEmpty() {
        assertTrue(viewModel.recentTests.value.isEmpty())
    }

    @Test
    fun initialState_badgesIsEmpty() {
        assertTrue(viewModel.badges.value.isEmpty())
    }

    @Test
    fun initialState_avgPrecisionIsZero() {
        assertEquals(0, viewModel.avgPrecisionAll.value)
    }

    @Test
    fun initialState_streakIsZero() {
        assertEquals(0, viewModel.streakDays.value)
    }

    @Test
    fun reload_doesNotThrow() {
        viewModel.reload()
    }

    @Test
    fun reload_doesNotChangeGoalConstants() {
        viewModel.reload()
        assertEquals(3, viewModel.dailyGoal)
        assertEquals(5, viewModel.weeklyGoal)
    }

    @Test
    fun reload_thenRunCurrent_isLoadingIsFalse() {
        viewModel.reload()
        testDispatcher.scheduler.runCurrent()
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun reload_afterInit_recentTestsRemainsEmpty() {
        testDispatcher.scheduler.runCurrent()
        viewModel.reload()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.recentTests.value.isEmpty())
    }

    // ─── BadgeUi data class ───────────────────────────────────────────────────

    @Test
    fun badgeUi_storesAllFields() {
        val badge = BadgeUi("🚀", "Primer paso", "Completa tu primer test", unlocked = true, progressText = "¡Completado!", progressFraction = 1f)
        assertEquals("🚀", badge.emoji)
        assertEquals("Primer paso", badge.title)
        assertEquals("Completa tu primer test", badge.description)
        assertTrue(badge.unlocked)
        assertEquals("¡Completado!", badge.progressText)
        assertEquals(1f, badge.progressFraction, 0.001f)
    }

    @Test
    fun badgeUi_defaultValues_areCorrect() {
        val badge = BadgeUi("⭐", "Experto", "50 tests", unlocked = false)
        assertEquals("", badge.progressText)
        assertEquals(0f, badge.progressFraction, 0.001f)
        assertFalse(badge.unlocked)
    }

    @Test
    fun badgeUi_equality_sameValues_areEqual() {
        val a = BadgeUi("🔥", "Racha 3 días", "3 días seguidos", unlocked = false, progressFraction = 0.5f)
        val b = BadgeUi("🔥", "Racha 3 días", "3 días seguidos", unlocked = false, progressFraction = 0.5f)
        assertEquals(a, b)
    }

    @Test
    fun badgeUi_copy_changesUnlocked() {
        val badge = BadgeUi("💯", "Perfeccionista", "100%", unlocked = false)
        val unlocked = badge.copy(unlocked = true)
        assertFalse(badge.unlocked)
        assertTrue(unlocked.unlocked)
    }

    // ─── RecentTestUi data class ──────────────────────────────────────────────

    @Test
    fun recentTestUi_storesAllFields() {
        val ui = RecentTestUi(id = 7, dateTime = "01 May · 10:00", testName = "Test de Stroop", precisionPct = 85, eegBeta = 0.7f)
        assertEquals(7, ui.id)
        assertEquals("01 May · 10:00", ui.dateTime)
        assertEquals("Test de Stroop", ui.testName)
        assertEquals(85, ui.precisionPct)
        assertEquals(0.7f, ui.eegBeta, 0.001f)
    }

    @Test
    fun recentTestUi_withNoEeg_hasEegDataIsFalse() {
        val ui = RecentTestUi(dateTime = "01 May", testName = "Test", precisionPct = 50)
        assertFalse(ui.hasEegData)
    }

    @Test
    fun recentTestUi_withEegAlpha_hasEegDataIsTrue() {
        val ui = RecentTestUi(dateTime = "01 May", testName = "Test", precisionPct = 50, eegAlpha = 0.5f)
        assertTrue(ui.hasEegData)
    }

    @Test
    fun recentTestUi_withNoEeg_dominantBandIsDash() {
        val ui = RecentTestUi(dateTime = "", testName = "", precisionPct = 0)
        assertEquals("—", ui.dominantBand)
    }

    @Test
    fun recentTestUi_dominantBand_returnsHighestBand() {
        val ui = RecentTestUi(dateTime = "", testName = "", precisionPct = 0, eegAlpha = 0.1f, eegBeta = 0.9f, eegGamma = 0.3f, eegTheta = 0.2f)
        assertEquals("Beta", ui.dominantBand)
    }

    // ─── Tests con base de datos real y sesión ────────────────────────────────

    private val appCtx: Application get() = ApplicationProvider.getApplicationContext()
    private val dbRef get() = NeuroTrackerDatabase.getDatabase(appCtx)
    private val sessionRef get() = SessionManager(appCtx)

    @Test
    fun withRealSession_afterOneResultToday_streakIsOne() {
        val email = "main.streak.${UUID.randomUUID()}@test.com"
        try {
            runBlocking {
                dbRef.userDao().insertUser(UserEntity(name = "Test", email = email, birthDate = 0L, passwordHash = "hash"))
                dbRef.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "reaction_simple", score = 0L, timestamp = System.currentTimeMillis(), precisionPct = 80))
            }
            sessionRef.saveSession(email)
            val vm = MainViewModel(appCtx)
            testDispatcher.scheduler.runCurrent()
            Thread.sleep(500)
            testDispatcher.scheduler.runCurrent()
            Thread.sleep(300)
            testDispatcher.scheduler.runCurrent()
            assertEquals(1, vm.streakDays.value)
        } finally {
            sessionRef.clearSession()
        }
    }

    @Test
    fun withRealSession_afterOneResult_badgePrimerPasoIsUnlocked() {
        val email = "main.badge.${UUID.randomUUID()}@test.com"
        try {
            runBlocking {
                dbRef.userDao().insertUser(UserEntity(name = "Test", email = email, birthDate = 0L, passwordHash = "hash"))
                dbRef.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "reaction_simple", score = 0L, precisionPct = 70))
            }
            sessionRef.saveSession(email)
            val vm = MainViewModel(appCtx)
            testDispatcher.scheduler.runCurrent()
            Thread.sleep(500)
            testDispatcher.scheduler.runCurrent()
            Thread.sleep(300)
            testDispatcher.scheduler.runCurrent()
            val primer = vm.badges.value.find { it.title == "Primer paso" }
            assertNotNull(primer)
            assertTrue(primer!!.unlocked)
        } finally {
            sessionRef.clearSession()
        }
    }

    @Test
    fun withRealSession_afterOneResultToday_dailyAndWeeklyCountIsOne() {
        val email = "main.daily.${UUID.randomUUID()}@test.com"
        try {
            runBlocking {
                dbRef.userDao().insertUser(UserEntity(name = "Test", email = email, birthDate = 0L, passwordHash = "hash"))
                dbRef.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "stroop", score = 0L, timestamp = System.currentTimeMillis(), precisionPct = 60))
            }
            sessionRef.saveSession(email)
            val vm = MainViewModel(appCtx)
            testDispatcher.scheduler.runCurrent()
            Thread.sleep(500)
            testDispatcher.scheduler.runCurrent()
            Thread.sleep(300)
            testDispatcher.scheduler.runCurrent()
            assertEquals(1, vm.dailyTests.value)
            assertEquals(1, vm.weeklyTests.value)
        } finally {
            sessionRef.clearSession()
        }
    }

    @Test
    fun withRealSession_stroopResult_testNameMappedCorrectly() {
        val email = "main.map.${UUID.randomUUID()}@test.com"
        try {
            runBlocking {
                dbRef.userDao().insertUser(UserEntity(name = "User", email = email, birthDate = 0L, passwordHash = "hash"))
                dbRef.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "stroop", score = 0L, precisionPct = 75))
            }
            sessionRef.saveSession(email)
            val vm = MainViewModel(appCtx)
            testDispatcher.scheduler.runCurrent()
            Thread.sleep(500)
            testDispatcher.scheduler.runCurrent()
            Thread.sleep(300)
            testDispatcher.scheduler.runCurrent()
            assertTrue(vm.recentTests.value.isNotEmpty())
            assertEquals("Test de Stroop", vm.recentTests.value.first().testName)
        } finally {
            sessionRef.clearSession()
        }
    }

    @Test
    fun withRealSession_perfectResult_badgePerfeccionistaIsUnlocked() {
        val email = "main.perfect.${UUID.randomUUID()}@test.com"
        try {
            runBlocking {
                dbRef.userDao().insertUser(UserEntity(name = "Test", email = email, birthDate = 0L, passwordHash = "hash"))
                dbRef.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "n_back", score = 0L, precisionPct = 100))
            }
            sessionRef.saveSession(email)
            val vm = MainViewModel(appCtx)
            testDispatcher.scheduler.runCurrent()
            Thread.sleep(500)
            testDispatcher.scheduler.runCurrent()
            Thread.sleep(300)
            testDispatcher.scheduler.runCurrent()
            val perfec = vm.badges.value.find { it.title == "Perfeccionista" }
            assertNotNull(perfec)
            assertTrue(perfec!!.unlocked)
        } finally {
            sessionRef.clearSession()
        }
    }

    @Test
    fun withRealSession_attentionResult_recommendationContainsAtencion() {
        val email = "main.rec.${UUID.randomUUID()}@test.com"
        try {
            runBlocking {
                dbRef.userDao().insertUser(UserEntity(name = "Test", email = email, birthDate = 0L, passwordHash = "hash"))
                dbRef.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "reaction_simple", score = 0L, precisionPct = 50))
            }
            sessionRef.saveSession(email)
            val vm = MainViewModel(appCtx)
            testDispatcher.scheduler.runCurrent()
            Thread.sleep(500)
            testDispatcher.scheduler.runCurrent()
            Thread.sleep(300)
            testDispatcher.scheduler.runCurrent()
            assertTrue(vm.recommendation.value.contains("Atención"))
        } finally {
            sessionRef.clearSession()
        }
    }

    @Test
    fun withRealSession_avgPrecision_isCalculatedCorrectly() {
        val email = "main.avg.${UUID.randomUUID()}@test.com"
        try {
            runBlocking {
                dbRef.userDao().insertUser(UserEntity(name = "Test", email = email, birthDate = 0L, passwordHash = "hash"))
                dbRef.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "reaction_simple", score = 0L, precisionPct = 60))
                dbRef.testResultDao().insertResult(TestResultEntity(userEmail = email, testType = "stroop", score = 0L, precisionPct = 80))
            }
            sessionRef.saveSession(email)
            val vm = MainViewModel(appCtx)
            testDispatcher.scheduler.runCurrent()
            Thread.sleep(500)
            testDispatcher.scheduler.runCurrent()
            Thread.sleep(300)
            testDispatcher.scheduler.runCurrent()
            assertEquals(70, vm.avgPrecisionAll.value)
        } finally {
            sessionRef.clearSession()
        }
    }
}
