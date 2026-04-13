package com.neurotracker.data.session

import android.content.Context

/**
 * Gestiona la preferencia de tema (oscuro/claro)
 * usando SharedPreferences para persistirlo entre sesiones.
 *
 * @param context Contexto de la aplicación para acceder a SharedPreferences.
 */
class ThemeManager(context: Context) {

    private val prefs = context.getSharedPreferences(
        "neurotracker_theme",
        Context.MODE_PRIVATE
    )

    /**
     * Verifica si el modo oscuro está activado.
     * Por defecto devuelve true (tema oscuro por defecto).
     *
     * @return true si el modo oscuro está activado, false si está en modo claro.
     */
    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", true)

    /**
     * Establece el modo oscuro o claro.
     *
     * @param dark true para activar modo oscuro, false para modo claro.
     */
    fun setDarkMode(dark: Boolean) {
        prefs.edit().putBoolean("dark_mode", dark).apply()
    }
}