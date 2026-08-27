package com.neurotracker.data.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios puros de [TestResultEntity].
 *
 * Verifica la construcción correcta de la entidad, los valores por defecto
 * y el comportamiento de la data class (copy, equals, hashCode).
 *
 * Se ejecuta en la JVM sin necesidad de contexto Android.
 */
class TestResultEntityTest {

    private val baseEntity = TestResultEntity(
        userEmail    = "alex@neurotracker.com",
        testType     = "reaction_simple",
        score        = 320L,
        precisionPct = 85,
        eegAlpha     = 1.2f,
        eegBeta      = 0.8f,
        eegGamma     = 0.5f,
        eegTheta     = 0.3f
    )

    /**
     * Verifica que el id por defecto es 0 (autogenerado por Room al insertar).
     */
    @Test
    fun defaultId_isZero() {
        assertEquals(0, baseEntity.id)
    }

    /**
     * Verifica que el timestamp por defecto es mayor que 0 (se asigna al construir).
     */
    @Test
    fun defaultTimestamp_isGreaterThanZero() {
        assertTrue(baseEntity.timestamp > 0L)
    }

    /**
     * Verifica que los valores EEG por defecto son 0f cuando no se especifican.
     */
    @Test
    fun defaultEegValues_areZeroFloat() {
        val entity = TestResultEntity(
            userEmail    = "alex@neurotracker.com",
            testType     = "stroop",
            score        = 200L,
            precisionPct = 70
        )
        assertEquals(0f, entity.eegAlpha, 0.001f)
        assertEquals(0f, entity.eegBeta,  0.001f)
        assertEquals(0f, entity.eegGamma, 0.001f)
        assertEquals(0f, entity.eegTheta, 0.001f)
    }

    /**
     * Verifica que [TestResultEntity.copy] crea una nueva entidad con el campo
     * modificado manteniendo el resto de valores intactos.
     */
    @Test
    fun copy_withNewEmail_updatesOnlyEmail() {
        val newEmail = "nuevo@neurotracker.com"
        val copied   = baseEntity.copy(userEmail = newEmail)
        assertEquals(newEmail, copied.userEmail)
        assertEquals(baseEntity.testType,     copied.testType)
        assertEquals(baseEntity.score,        copied.score)
        assertEquals(baseEntity.precisionPct, copied.precisionPct)
        assertEquals(baseEntity.eegAlpha,     copied.eegAlpha, 0.001f)
    }

    /**
     * Verifica que dos entidades con los mismos valores son iguales (equals de data class).
     */
    @Test
    fun equals_withSameValues_returnsTrue() {
        val other = baseEntity.copy()
        assertEquals(baseEntity, other)
    }

    /**
     * Verifica que dos entidades con distintos testType no son iguales.
     */
    @Test
    fun equals_withDifferentTestType_returnsFalse() {
        val other = baseEntity.copy(testType = "stroop")
        assertNotEquals(baseEntity, other)
    }

    /**
     * Verifica que la precisión se almacena correctamente en el rango 0–100.
     */
    @Test
    fun precisionPct_isStoredCorrectly() {
        val entity = TestResultEntity(
            userEmail    = "alex@neurotracker.com",
            testType     = "global_cognitive",
            score        = 700L,
            precisionPct = 73
        )
        assertEquals(73, entity.precisionPct)
        assertTrue(entity.precisionPct in 0..100)
    }

    /**
     * Verifica que el score se almacena como Long y conserva valores grandes
     * (por ejemplo, tiempos de reacción en milisegundos o scores globales).
     */
    @Test
    fun score_storedAsLong_handlesLargeValues() {
        val entity = TestResultEntity(
            userEmail    = "alex@neurotracker.com",
            testType     = "reaction_simple",
            score        = 999_999L,
            precisionPct = 50
        )
        assertEquals(999_999L, entity.score)
    }
}
