package com.neurotracker.data.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Datos de un logro recién desbloqueado para mostrar la notificación global.
 *
 * Definido aquí porque es el payload del bus de eventos [SessionEvents].
 */
data class BadgeUnlockInfo(
    val emoji: String,
    val title: String,
    val description: String
)

/**
 * Bus de eventos de sesión compartido entre ViewModels y composables.
 *
 * Permite que [com.neurotracker.ui.profile.ProfileViewModel] notifique
 * cambios de email y de foto de perfil de forma instantánea, sin depender
 * del ciclo de vida de ninguna pantalla ni acumular latencia de un StateFlow.
 *
 * Es un singleton de Kotlin para garantizar que todos los consumidores
 * comparten la misma instancia del Flow.
 */
object SessionEvents {

    private val _emailChanged  = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val _photoChanged  = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _badgeUnlocked = MutableStateFlow<BadgeUnlockInfo?>(null)

    val emailChanged  = _emailChanged.asSharedFlow()
    val photoChanged  = _photoChanged.asSharedFlow()
    val badgeUnlocked: StateFlow<BadgeUnlockInfo?> = _badgeUnlocked

    /**
     * Emite el nuevo email al bus de eventos.
     *
     * Debe llamarse desde [com.neurotracker.ui.profile.ProfileViewModel]
     * justo después de actualizar el email en Room y en [SessionManager].
     *
     * @param newEmail Nuevo email del usuario tras el cambio.
     */
    fun notifyEmailChanged(newEmail: String) {
        _emailChanged.tryEmit(newEmail)
    }

    /**
     * Emite un evento de cambio de foto al bus de eventos.
     *
     * Debe llamarse desde [com.neurotracker.ui.profile.ProfileViewModel]
     * cada vez que la foto de perfil se guarda o se elimina, para que
     * [com.neurotracker.ui.main.MainScreen] actualice el avatar del TopBar
     * de forma inmediata sin esperar a un evento ON_RESUME.
     */
    fun notifyPhotoChanged() {
        _photoChanged.tryEmit(Unit)
    }

    /**
     * Emite el logro recién desbloqueado al bus de eventos.
     *
     * Debe llamarse desde [com.neurotracker.ui.main.MainViewModel] cuando se
     * detecta un logro nuevo, para que [com.neurotracker.ui.main.BadgeUnlockOverlay]
     * pueda mostrarlo desde cualquier pantalla activa.
     */
    fun notifyBadgeUnlocked(info: BadgeUnlockInfo) {
        _badgeUnlocked.value = info
    }

    /**
     * Limpia el logro pendiente de mostrar una vez que el usuario lo ha visto.
     */
    fun clearBadgeUnlocked() {
        _badgeUnlocked.value = null
    }
}