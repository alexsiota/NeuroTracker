package com.neurotracker.viewmodel

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados de [RecentTestUi].
 *
 * Cubre las propiedades computadas [RecentTestUi.hasEegData] y [RecentTestUi.dominantBand]
 * con todas sus ramas: sin datos EEG, con datos y cada banda como dominante.
 */
@RunWith(AndroidJUnit4::class)
class RecentTestUiTest {

    private fun makeTest(
        alpha: Float = 0f,
        beta:  Float = 0f,
        gamma: Float = 0f,
        theta: Float = 0f
    ) = RecentTestUi(
        dateTime     = "08 May · 10:00",
        testName     = "Reacción simple",
        precisionPct = 85,
        eegAlpha     = alpha,
        eegBeta      = beta,
        eegGamma     = gamma,
        eegTheta     = theta
    )

    // ─── hasEegData ────────────────────────────────────────────────────────────

    @Test
    fun hasEegData_whenAllZero_returnsFalse() {
        assertFalse(makeTest().hasEegData)
    }

    @Test
    fun hasEegData_whenAlphaNonZero_returnsTrue() {
        assertTrue(makeTest(alpha = 0.5f).hasEegData)
    }

    @Test
    fun hasEegData_whenBetaNonZero_returnsTrue() {
        assertTrue(makeTest(beta = 0.3f).hasEegData)
    }

    @Test
    fun hasEegData_whenGammaNonZero_returnsTrue() {
        assertTrue(makeTest(gamma = 0.2f).hasEegData)
    }

    @Test
    fun hasEegData_whenThetaNonZero_returnsTrue() {
        assertTrue(makeTest(theta = 0.4f).hasEegData)
    }

    // ─── dominantBand ──────────────────────────────────────────────────────────

    @Test
    fun dominantBand_withNoEegData_returnsDash() {
        assertEquals("—", makeTest().dominantBand)
    }

    @Test
    fun dominantBand_whenAlphaDominant_returnsAlpha() {
        assertEquals("Alpha", makeTest(alpha = 0.8f, beta = 0.3f, gamma = 0.2f, theta = 0.1f).dominantBand)
    }

    @Test
    fun dominantBand_whenBetaDominant_returnsBeta() {
        assertEquals("Beta", makeTest(alpha = 0.3f, beta = 0.9f, gamma = 0.2f, theta = 0.1f).dominantBand)
    }

    @Test
    fun dominantBand_whenGammaDominant_returnsGamma() {
        assertEquals("Gamma", makeTest(alpha = 0.2f, beta = 0.3f, gamma = 0.7f, theta = 0.1f).dominantBand)
    }

    @Test
    fun dominantBand_whenThetaDominant_returnsTheta() {
        assertEquals("Theta", makeTest(alpha = 0.1f, beta = 0.2f, gamma = 0.3f, theta = 0.8f).dominantBand)
    }

    // ─── Data class identity ───────────────────────────────────────────────────

    @Test
    fun equals_withSameValues_returnsTrue() {
        val a = makeTest(alpha = 0.5f)
        val b = makeTest(alpha = 0.5f)
        assertEquals(a, b)
    }

    @Test
    fun copy_preservesAllFields() {
        val original = makeTest(alpha = 0.5f, beta = 0.3f)
        val copy = original.copy(precisionPct = 90)
        assertEquals(90, copy.precisionPct)
        assertEquals(original.eegAlpha, copy.eegAlpha, 0.001f)
    }

    @Test
    fun defaultId_isZero() {
        assertEquals(0, makeTest().id)
    }
}
