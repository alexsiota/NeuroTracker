package com.neurotracker.ui.achievements

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.graphics.Color
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [AchievementsViewModel].
 *
 * Verifica únicamente propiedades estructurales de la lista de logros que
 * son constantes e independientes del estado de sesión o de los datos en Room:
 * número total, ids únicos, títulos, emojis, categorías y descripciones.
 *
 * Se ejecuta en dispositivo o emulador porque [AchievementsViewModel]
 * extiende [AndroidViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AchievementsViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AchievementsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AchievementsViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica que la carga termina y [AchievementsViewModel.isLoading]
     * pasa a false independientemente de si hay sesión activa o no.
     */
    @Test
    fun afterLoading_isLoadingIsFalse() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.isLoading.value)
    }

    /**
     * Verifica que la lista de logros tiene exactamente 20 elementos,
     * número constante definido en [buildAllAchievements] que no varía
     * con el estado de sesión ni con los datos en Room.
     */
    @Test
    fun afterLoading_achievementsListHasTwentyItems() = runTest {
        advanceUntilIdle()
        assertEquals(20, viewModel.achievements.value.size)
    }

    /**
     * Verifica que todos los logros tienen ids únicos, condición necesaria
     * para que [LazyColumn] en la UI no tenga duplicados.
     */
    @Test
    fun achievements_haveUniqueIds() = runTest {
        advanceUntilIdle()
        val ids = viewModel.achievements.value.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    /**
     * Verifica que todos los logros tienen título y emoji no vacíos,
     * ambos requeridos para renderizar cada tarjeta en [AchievementsScreen].
     */
    @Test
    fun achievements_haveNonEmptyTitlesAndEmojis() = runTest {
        advanceUntilIdle()
        viewModel.achievements.value.forEach { achievement ->
            assertTrue("Título vacío en ${achievement.id}", achievement.title.isNotBlank())
            assertTrue("Emoji vacío en ${achievement.id}", achievement.emoji.isNotBlank())
        }
    }

    /**
     * Verifica que existe el logro "first_test" con emoji 🚀
     * y categoría "Primeros pasos".
     */
    @Test
    fun achievements_containsFirstTestAchievementWithCorrectAttributes() = runTest {
        advanceUntilIdle()
        val firstTest = viewModel.achievements.value.find { it.id == "first_test" }
        assertNotNull("No se encontró el logro 'first_test'", firstTest)
        assertEquals("🚀", firstTest!!.emoji)
        assertEquals("Primeros pasos", firstTest.category)
    }

    /**
     * Verifica que la lista contiene logros de las seis categorías cognitivas
     * esperadas, que son constantes independientemente de la sesión activa.
     */
    @Test
    fun achievements_containsAllExpectedCategories() = runTest {
        advanceUntilIdle()
        val categories = viewModel.achievements.value.map { it.category }.toSet()
        assertTrue(categories.contains("Primeros pasos"))
        assertTrue(categories.contains("Atención"))
        assertTrue(categories.contains("Memoria"))
        assertTrue(categories.contains("Velocidad"))
        assertTrue(categories.contains("Precisión"))
        assertTrue(categories.contains("Variedad"))
    }

    /**
     * Verifica que todos los logros tienen descripción no vacía.
     */
    @Test
    fun achievements_haveNonEmptyDescriptions() = runTest {
        advanceUntilIdle()
        viewModel.achievements.value.forEach { achievement ->
            assertTrue(
                "Descripción vacía en ${achievement.id}",
                achievement.description.isNotBlank()
            )
        }
    }

    /**
     * Verifica que el logro "variety_all" existe con la categoría "Variedad".
     */
    @Test
    fun achievements_containsVarietyAllAchievement() = runTest {
        advanceUntilIdle()
        val varietyAll = viewModel.achievements.value.find { it.id == "variety_all" }
        assertNotNull("No se encontró el logro 'variety_all'", varietyAll)
        assertEquals("Variedad", varietyAll!!.category)
    }

    @Test
    fun initialState_isLoadingIsTrue() {
        assertTrue(viewModel.isLoading.value)
    }

    @Test
    fun afterLoading_withNoSession_allAchievementsAreNotUnlocked() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.achievements.value.all { !it.unlocked })
    }

    @Test
    fun achievements_containsReactionFastAchievement() = runTest {
        advanceUntilIdle()
        assertNotNull(viewModel.achievements.value.find { it.id == "reaction_fast" })
    }

    @Test
    fun achievements_containsNBackAceAchievement() = runTest {
        advanceUntilIdle()
        assertNotNull(viewModel.achievements.value.find { it.id == "nback_ace" })
    }

    @Test
    fun achievements_containsPrecision70Achievement() = runTest {
        advanceUntilIdle()
        assertNotNull(viewModel.achievements.value.find { it.id == "precision_70" })
    }

    @Test
    fun achievements_containsGlobalAceAchievement() = runTest {
        advanceUntilIdle()
        val a = viewModel.achievements.value.find { it.id == "global_ace" }
        assertNotNull(a)
        assertEquals("Tests compuestos", a!!.category)
    }

    @Test
    fun achievements_containsDualTaskFirstAchievement() = runTest {
        advanceUntilIdle()
        assertNotNull(viewModel.achievements.value.find { it.id == "dual_task_first" })
    }

    @Test
    fun achievements_executiveFunctionsCategory_hasAchievements() = runTest {
        advanceUntilIdle()
        val executive = viewModel.achievements.value.filter { it.category == "Funciones ejecutivas" }
        assertTrue(executive.size >= 2)
    }

    @Test
    fun achievement_dataClass_storesAllFields() {
        val a = Achievement(
            id            = "test_id",
            emoji         = "🏆",
            title         = "Título test",
            description   = "Descripción test",
            category      = "Categoría",
            unlocked      = true,
            unlockedColor = Color(0xFF1565C0)
        )
        assertEquals("test_id", a.id)
        assertEquals("🏆", a.emoji)
        assertEquals("Título test", a.title)
        assertEquals("Descripción test", a.description)
        assertEquals("Categoría", a.category)
        assertTrue(a.unlocked)
        assertEquals(Color(0xFF1565C0), a.unlockedColor)
    }

    @Test
    fun achievement_dataClass_defaultColor_isGreen() {
        val a = Achievement("id", "🚀", "T", "D", "Cat", false)
        assertEquals(Color(0xFF2E7D32), a.unlockedColor)
    }

    @Test
    fun achievement_dataClass_equality() {
        val a = Achievement("id1", "⭐", "T", "D", "Cat", true)
        val b = Achievement("id1", "⭐", "T", "D", "Cat", true)
        assertEquals(a, b)
    }

    @Test
    fun achievement_dataClass_copy_changesUnlocked() {
        val locked   = Achievement("id1", "🔥", "T", "D", "Cat", false)
        val unlocked = locked.copy(unlocked = true)
        assertFalse(locked.unlocked)
        assertTrue(unlocked.unlocked)
        assertEquals(locked.id, unlocked.id)
    }

    @Test
    fun achievements_allHaveNonNullUnlockedColor() = runTest {
        advanceUntilIdle()
        viewModel.achievements.value.forEach { a ->
            assertNotNull("Color null en ${a.id}", a.unlockedColor)
        }
    }
}