package com.neurotracker.data.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios puros de [UserEntity].
 *
 * Verifica la construcción correcta de la entidad, los valores por defecto
 * y el comportamiento de la data class en operaciones de copia y comparación.
 *
 * Se ejecuta en la JVM sin necesidad de contexto Android.
 */
class UserEntityTest {

    private val baseUser = UserEntity(
        name         = "Álex Siota",
        birthDate    = 946684800000L,
        email        = "alex@neurotracker.com",
        passwordHash = "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3"
    )

    /**
     * Verifica que el id por defecto es 0 (autogenerado por Room al insertar).
     */
    @Test
    fun defaultId_isZero() {
        assertEquals(0, baseUser.id)
    }

    /**
     * Verifica que el timestamp de creación es mayor que 0.
     */
    @Test
    fun defaultCreatedAt_isGreaterThanZero() {
        assertTrue(baseUser.createdAt > 0L)
    }

    /**
     * Verifica que todos los campos se almacenan correctamente.
     */
    @Test
    fun allFields_areStoredCorrectly() {
        assertEquals("Álex Siota",                  baseUser.name)
        assertEquals("alex@neurotracker.com",        baseUser.email)
        assertEquals(946684800000L,                  baseUser.birthDate)
        assertEquals(64, baseUser.passwordHash.length)
    }

    /**
     * Verifica que [UserEntity.copy] actualiza correctamente el email
     * sin modificar los demás campos.
     */
    @Test
    fun copy_withNewEmail_updatesOnlyEmail() {
        val newEmail = "nuevo@neurotracker.com"
        val updated  = baseUser.copy(email = newEmail)
        assertEquals(newEmail,       updated.email)
        assertEquals(baseUser.name,         updated.name)
        assertEquals(baseUser.birthDate,    updated.birthDate)
        assertEquals(baseUser.passwordHash, updated.passwordHash)
    }

    /**
     * Verifica que [UserEntity.copy] actualiza correctamente el hash de la contraseña
     * sin modificar los demás campos.
     */
    @Test
    fun copy_withNewPasswordHash_updatesOnlyHash() {
        val newHash = "nuevo_hash_sha256_64_caracteres_aqui_completado_para_el_test_ok"
        val updated = baseUser.copy(passwordHash = newHash)
        assertEquals(newHash,        updated.passwordHash)
        assertEquals(baseUser.email, updated.email)
        assertEquals(baseUser.name,  updated.name)
    }

    /**
     * Verifica que dos usuarios con los mismos valores son iguales.
     */
    @Test
    fun equals_withSameValues_returnsTrue() {
        val other = baseUser.copy()
        assertEquals(baseUser, other)
    }

    /**
     * Verifica que dos usuarios con emails distintos no son iguales.
     */
    @Test
    fun equals_withDifferentEmail_returnsFalse() {
        val other = baseUser.copy(email = "otro@neurotracker.com")
        assertNotEquals(baseUser, other)
    }

    /**
     * Verifica que el hash de contraseña no es texto plano (no coincide con la contraseña original).
     */
    @Test
    fun passwordHash_isNotPlainText() {
        assertNotEquals("password123", baseUser.passwordHash)
    }
}
