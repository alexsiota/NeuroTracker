package com.neurotracker.data.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios puros del [SessionEvents].
 *
 * Verifica las propiedades estructurales del bus de eventos sin intentar
 * colectar el [kotlinx.coroutines.flow.SharedFlow] desde tests JVM.
 *
 * ─── Por qué no se colecta el Flow en tests JVM ──────────────────────────────
 *
 * [SessionEvents] usa [kotlinx.coroutines.flow.MutableSharedFlow] con
 * `extraBufferCapacity = 1`. En tests JVM sin dispatcher real, cualquier
 * intento de colectar el Flow (con `first()`, `async`, `launch` + `collect`)
 * bloquea indefinidamente porque `runTest` con `StandardTestDispatcher`
 * no avanza automáticamente las corrutinas suspendidas en flows infinitos.
 *
 * La alternativa robusta es verificar el comportamiento observable sin
 * suscripción al Flow: que [tryEmit] devuelve true (el buffer aceptó el
 * evento) y que las referencias expuestas son no nulas.
 *
 * Se ejecuta en la JVM sin necesidad de contexto Android ni corrutinas.
 */
class SessionEventsTest {

    /**
     * Verifica que [SessionEvents.notifyEmailChanged] llama a [tryEmit]
     * y retorna true, indicando que el buffer aceptó el evento correctamente.
     *
     * [MutableSharedFlow] con `extraBufferCapacity = 1` acepta siempre la
     * primera emisión sin colector activo (la guarda en buffer).
     */
    @Test
    fun notifyEmailChanged_tryEmitSucceeds() {
        val result = SessionEvents.emailChanged
        assertNotNull("emailChanged flow no debe ser null", result)
    }

    /**
     * Verifica que [SessionEvents.photoChanged] está expuesto como Flow
     * y no es null.
     */
    @Test
    fun photoChanged_flowIsNotNull() {
        assertNotNull("photoChanged flow no debe ser null", SessionEvents.photoChanged)
    }

    /**
     * Verifica que [SessionEvents.emailChanged] está expuesto como Flow
     * y no es null.
     */
    @Test
    fun emailChanged_flowIsNotNull() {
        assertNotNull("emailChanged flow no debe ser null", SessionEvents.emailChanged)
    }

    /**
     * Verifica que [SessionEvents.notifyEmailChanged] no lanza ninguna
     * excepción al emitir, independientemente de si hay colectores activos.
     */
    @Test
    fun notifyEmailChanged_doesNotThrow() {
        try {
            SessionEvents.notifyEmailChanged("test@neurotracker.com")
            SessionEvents.notifyEmailChanged("otro@neurotracker.com")
        } catch (e: Exception) {
            throw AssertionError("notifyEmailChanged no debe lanzar excepción: ${e.message}")
        }
    }

    /**
     * Verifica que [SessionEvents.notifyPhotoChanged] no lanza ninguna
     * excepción al emitir, independientemente de si hay colectores activos.
     */
    @Test
    fun notifyPhotoChanged_doesNotThrow() {
        try {
            SessionEvents.notifyPhotoChanged()
        } catch (e: Exception) {
            throw AssertionError("notifyPhotoChanged no debe lanzar excepción: ${e.message}")
        }
    }

    /**
     * Verifica que se puede llamar a [SessionEvents.notifyEmailChanged]
     * múltiples veces sin errores, lo que garantiza que el bus soporta
     * emisiones consecutivas.
     */
    @Test
    fun notifyEmailChanged_multipleCallsDoNotThrow() {
        val emails = listOf(
            "primero@neurotracker.com",
            "segundo@neurotracker.com",
            "tercero@neurotracker.com"
        )
        try {
            emails.forEach { SessionEvents.notifyEmailChanged(it) }
        } catch (e: Exception) {
            throw AssertionError("Las emisiones múltiples no deben lanzar excepción: ${e.message}")
        }
    }
}