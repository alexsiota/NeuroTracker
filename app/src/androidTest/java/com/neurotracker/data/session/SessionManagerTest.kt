package com.neurotracker.data.session

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [SessionManager].
 *
 * Verifica el ciclo de vida completo de la sesión de usuario:
 *  - Guardar y recuperar el email de sesión.
 *  - Detectar correctamente si existe una sesión activa.
 *  - Limpiar la sesión y verificar que no quedan datos residuales.
 *  - Sesión temporal sin persistencia entre reinicios.
 *  - Activar y desactivar la opción de recordar sesión.
 *
 * Se ejecuta en el dispositivo o emulador porque [SessionManager] usa
 * [android.content.SharedPreferences], que requiere contexto Android.
 */
@RunWith(AndroidJUnit4::class)
class SessionManagerTest {

    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        sessionManager = SessionManager(ApplicationProvider.getApplicationContext())
        sessionManager.clearSession()
    }

    /**
     * Verifica que [SessionManager.getUserEmail] devuelve null cuando no hay sesión activa.
     */
    @Test
    fun getUserEmail_withNoSession_returnsNull() {
        assertNull(sessionManager.getUserEmail())
    }

    /**
     * Verifica que [SessionManager.saveSession] guarda el email correctamente
     * y [SessionManager.getUserEmail] lo devuelve.
     */
    @Test
    fun saveSession_andGetUserEmail_returnsCorrectEmail() {
        val email = "alex@neurotracker.com"
        sessionManager.saveSession(email)
        assertEquals(email, sessionManager.getUserEmail())
    }

    /**
     * Verifica que [SessionManager.clearSession] elimina el email y cualquier
     * otra preferencia guardada, dejando la sesión en estado limpio.
     */
    @Test
    fun clearSession_removesEmailAndPreferences() {
        sessionManager.saveSession("alex@neurotracker.com")
        sessionManager.setRememberMe(true)
        sessionManager.clearSession()
        assertNull(sessionManager.getUserEmail())
        assertFalse(sessionManager.hasSession())
    }

    /**
     * Verifica que [SessionManager.hasSession] devuelve true cuando hay un email
     * guardado y remember_me está activado.
     */
    @Test
    fun hasSession_withEmailAndRememberMe_returnsTrue() {
        sessionManager.saveSession("alex@neurotracker.com")
        sessionManager.setRememberMe(true)
        assertTrue(sessionManager.hasSession())
    }

    /**
     * Verifica que [SessionManager.hasSession] devuelve false cuando hay un email
     * guardado pero remember_me está desactivado.
     */
    @Test
    fun hasSession_withEmailButNoRememberMe_returnsFalse() {
        sessionManager.saveSession("alex@neurotracker.com")
        sessionManager.setRememberMe(false)
        assertFalse(sessionManager.hasSession())
    }

    /**
     * Verifica que [SessionManager.hasSession] devuelve false cuando no hay
     * ningún email guardado aunque remember_me esté activado.
     */
    @Test
    fun hasSession_withRememberMeButNoEmail_returnsFalse() {
        sessionManager.setRememberMe(true)
        assertFalse(sessionManager.hasSession())
    }

    /**
     * Verifica que [SessionManager.setTemporarySession] guarda el email pero
     * establece remember_me como false, por lo que [SessionManager.hasSession]
     * devuelve false aunque el email sea recuperable.
     */
    @Test
    fun setTemporarySession_savesEmailButHasSessionReturnsFalse() {
        val email = "temporal@neurotracker.com"
        sessionManager.setTemporarySession(email)
        assertEquals(email, sessionManager.getUserEmail())
        assertFalse(sessionManager.hasSession())
    }

    /**
     * Verifica que guardar un nuevo email sobreescribe el anterior correctamente.
     */
    @Test
    fun saveSession_withDifferentEmail_overwritesPreviousEmail() {
        sessionManager.saveSession("primero@neurotracker.com")
        sessionManager.saveSession("segundo@neurotracker.com")
        assertEquals("segundo@neurotracker.com", sessionManager.getUserEmail())
    }

    /**
     * Verifica que [SessionManager.setRememberMe] alterna correctamente entre
     * true y false sin afectar al email guardado.
     */
    @Test
    fun setRememberMe_togglesBehaviorWithoutAffectingEmail() {
        val email = "alex@neurotracker.com"
        sessionManager.saveSession(email)
        sessionManager.setRememberMe(true)
        assertTrue(sessionManager.hasSession())
        sessionManager.setRememberMe(false)
        assertFalse(sessionManager.hasSession())
        assertEquals(email, sessionManager.getUserEmail())
    }
}
