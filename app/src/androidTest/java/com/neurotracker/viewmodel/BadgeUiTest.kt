package com.neurotracker.viewmodel

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados de [BadgeUi].
 *
 * Verifica la construcción con y sin valores por defecto, igualdad y copia.
 */
@RunWith(AndroidJUnit4::class)
class BadgeUiTest {

    @Test
    fun constructor_withAllFields_storesCorrectly() {
        val badge = BadgeUi(
            emoji            = "🚀",
            title            = "Primer paso",
            description      = "Completa tu primer test",
            unlocked         = true,
            progressText     = "¡Completado!",
            progressFraction = 1f
        )
        assertEquals("🚀", badge.emoji)
        assertEquals("Primer paso", badge.title)
        assertEquals("Completa tu primer test", badge.description)
        assertTrue(badge.unlocked)
        assertEquals("¡Completado!", badge.progressText)
        assertEquals(1f, badge.progressFraction, 0.001f)
    }

    @Test
    fun constructor_withDefaultOptionals_hasEmptyProgressText() {
        val badge = BadgeUi(
            emoji       = "🔥",
            title       = "Racha 3 días",
            description = "Entrena 3 días seguidos",
            unlocked    = false
        )
        assertEquals("", badge.progressText)
        assertEquals(0f, badge.progressFraction, 0.001f)
        assertFalse(badge.unlocked)
    }

    @Test
    fun equals_withSameValues_returnsTrue() {
        val a = BadgeUi("💯", "Perfecto", "100%", unlocked = true, progressText = "¡Completado!", progressFraction = 1f)
        val b = BadgeUi("💯", "Perfecto", "100%", unlocked = true, progressText = "¡Completado!", progressFraction = 1f)
        assertEquals(a, b)
    }

    @Test
    fun copy_withUnlockedChange_updatesField() {
        val locked   = BadgeUi("🏆", "Maestro", "50 tests", unlocked = false)
        val unlocked = locked.copy(unlocked = true)
        assertFalse(locked.unlocked)
        assertTrue(unlocked.unlocked)
        assertEquals(locked.title, unlocked.title)
    }

    @Test
    fun progressFraction_isCoercedAt1f_whenProvided() {
        val badge = BadgeUi("⭐", "Experto", "50 tests", unlocked = true, progressFraction = 1f)
        assertEquals(1f, badge.progressFraction, 0.001f)
    }
}
