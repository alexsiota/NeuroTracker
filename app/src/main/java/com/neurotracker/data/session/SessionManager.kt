package com.neurotracker.data.session

import android.content.Context

/**
 * Gestiona la sesión del usuario usando SharedPreferences.
 * Guarda el email del usuario autenticado y si quiere ser recordado.
 *
 * @param context Contexto de la aplicación para acceder a SharedPreferences.
 */
class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(
        "neurotracker_session",
        Context.MODE_PRIVATE
    )

    /**
     * Guarda la sesión del usuario con persistencia.
     * El usuario será recordado entre reinicios de la app.
     *
     * @param email Email del usuario autenticado.
     */
    fun saveSession(email: String) {
        prefs.edit().putString("user_email", email).apply()
    }

    /**
     * Verifica si existe una sesión activa y persistente.
     *
     * @return true si "recordar sesión" está activado y hay un email guardado.
     */
    fun hasSession(): Boolean {
        val remember = prefs.getBoolean("remember_me", false)
        return remember && prefs.contains("user_email")
    }

    /**
     * Activa o desactiva la persistencia de la sesión entre reinicios.
     *
     * @param remember true para mantener la sesión, false para no recordarla.
     */
    fun setRememberMe(remember: Boolean) {
        prefs.edit().putBoolean("remember_me", remember).apply()
    }

    /**
     * Obtiene el email del usuario con sesión activa.
     *
     * @return Email del usuario, o null si no hay sesión.
     */
    fun getUserEmail(): String? = prefs.getString("user_email", null)

    /**
     * Limpia completamente la sesión, eliminando email y preferencias de recordar.
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }

    /**
     * Marca la sesión como activa para esta ejecución aunque no haya remember_me.
     * Útil para mantener al usuario logueado durante la sesión actual
     * sin persistirlo entre reinicios de la app.
     *
     * @param email Email del usuario autenticado temporalmente.
     */
    fun setTemporarySession(email: String) {
        prefs.edit()
            .putString("user_email", email)
            .putBoolean("remember_me", false)
            .apply()
    }
}