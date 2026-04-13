package com.neurotracker.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios puros del [BadgeUi].
 *
 * Verifica la construcción, los valores por defecto y el comportamiento
 * de la data class en copias y comparaciones.
 *
 * Se ejecuta en la JVM sin necesidad de contexto Android.
 */
class BadgeUiTest {

    private val unlockedBadge = BadgeUi(
        emoji            = "🚀",
        title            = "Primeros pasos",
        description      = "Completa tu primer test cognitivo.",
        unlocked         = true,
        progressText     = "1 / 1 tests",
        progressFraction = 1f
    )

    private val lockedBadge = BadgeUi(
        emoji            = "⭐",
        title            = "En racha",
        description      = "Completa 5 tests cognitivos.",
        unlocked         = false,
        progressText     = "0 / 5 tests",
        progressFraction = 0f
    )

    /**
     * Verifica que [BadgeUi.unlocked] es true para un logro desbloqueado.
     */
    @Test
    fun unlockedBadge_unlockedIsTrue() {
        assertTrue(unlockedBadge.unlocked)
    }

    /**
     * Verifica que [BadgeUi.unlocked] es false para un logro bloqueado.
     */
    @Test
    fun lockedBadge_unlockedIsFalse() {
        assertFalse(lockedBadge.unlocked)
    }

    /**
     * Verifica que los valores por defecto de [progressText] y [progressFraction]
     * son cadena vacía y 0f respectivamente.
     */
    @Test
    fun badgeUi_defaultValues_areEmptyAndZero() {
        val badge = BadgeUi(
            emoji       = "🎯",
            title       = "Test",
            description = "Desc",
            unlocked    = false
        )
        assertEquals("", badge.progressText)
        assertEquals(0f, badge.progressFraction, 0.001f)
    }

    /**
     * Verifica que [BadgeUi.copy] crea un nuevo objeto con el campo
     * modificado sin afectar al original.
     */
    @Test
    fun badgeUi_copy_updatesUnlockedCorrectly() {
        val unlocked = lockedBadge.copy(unlocked = true, progressFraction = 1f)
        assertTrue(unlocked.unlocked)
        assertEquals(1f, unlocked.progressFraction, 0.001f)
        assertEquals(lockedBadge.title, unlocked.title)
    }

    /**
     * Verifica que dos [BadgeUi] con los mismos valores son iguales.
     */
    @Test
    fun badgeUi_equals_withSameValues_returnsTrue() {
        val copy = unlockedBadge.copy()
        assertEquals(unlockedBadge, copy)
    }

    /**
     * Verifica que la fracción de progreso está en el rango válido [0f, 1f].
     */
    @Test
    fun progressFraction_isAlwaysInValidRange() {
        assertTrue(unlockedBadge.progressFraction in 0f..1f)
        assertTrue(lockedBadge.progressFraction  in 0f..1f)
    }
}

/**
 * Tests unitarios puros del [RecentTestUi].
 *
 * Verifica la construcción, los valores computados ([hasEegData], [dominantBand])
 * y el comportamiento de la data class.
 *
 * Se ejecuta en la JVM sin necesidad de contexto Android.
 */
class RecentTestUiTest {

    private val resultWithEeg = RecentTestUi(
        id           = 1,
        dateTime     = "11 Apr 2026 · 10:30",
        testName     = "Test de Stroop",
        precisionPct = 85,
        eegAlpha     = 0.3f,
        eegBeta      = 0.7f,
        eegGamma     = 0.2f,
        eegTheta     = 0.1f
    )

    private val resultWithoutEeg = RecentTestUi(
        id           = 2,
        dateTime     = "11 Apr 2026 · 11:00",
        testName     = "Reacción simple",
        precisionPct = 70
    )

    /**
     * Verifica que [RecentTestUi.hasEegData] es true cuando al menos
     * una banda EEG tiene valor distinto de 0f.
     */
    @Test
    fun hasEegData_withEegValues_returnsTrue() {
        assertTrue(resultWithEeg.hasEegData)
    }

    /**
     * Verifica que [RecentTestUi.hasEegData] es false cuando todas
     * las bandas EEG son 0f (valor por defecto).
     */
    @Test
    fun hasEegData_withDefaultValues_returnsFalse() {
        assertFalse(resultWithoutEeg.hasEegData)
    }

    /**
     * Verifica que [RecentTestUi.dominantBand] devuelve "Beta"
     * cuando la banda Beta tiene el mayor valor.
     */
    @Test
    fun dominantBand_withHighestBeta_returnsBeta() {
        assertEquals("Beta", resultWithEeg.dominantBand)
    }

    /**
     * Verifica que [RecentTestUi.dominantBand] devuelve "—"
     * cuando no hay datos EEG.
     */
    @Test
    fun dominantBand_withNoEegData_returnsDash() {
        assertEquals("—", resultWithoutEeg.dominantBand)
    }

    /**
     * Verifica que la precisión se almacena correctamente.
     */
    @Test
    fun precisionPct_isStoredCorrectly() {
        assertEquals(85, resultWithEeg.precisionPct)
        assertEquals(70, resultWithoutEeg.precisionPct)
    }

    /**
     * Verifica que el id por defecto es 0 cuando no se especifica.
     */
    @Test
    fun defaultId_isZero() {
        val result = RecentTestUi(
            dateTime     = "01 Jan 2026",
            testName     = "N-Back",
            precisionPct = 60
        )
        assertEquals(0, result.id)
    }

    /**
     * Verifica que [dominantBand] devuelve Alpha cuando es la banda dominante.
     */
    @Test
    fun dominantBand_withHighestAlpha_returnsAlpha() {
        val result = RecentTestUi(
            dateTime     = "01 Jan 2026",
            testName     = "Test",
            precisionPct = 50,
            eegAlpha     = 0.9f,
            eegBeta      = 0.2f,
            eegGamma     = 0.1f,
            eegTheta     = 0.3f
        )
        assertEquals("Alpha", result.dominantBand)
    }
}
