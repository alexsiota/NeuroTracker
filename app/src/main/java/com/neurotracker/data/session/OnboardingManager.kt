package com.neurotracker.data.session

import android.content.Context

/**
 * Gestiona si el onboarding ya fue mostrado al usuario.
 * Se guarda en SharedPreferences de forma permanente.
 *
 * @param context Contexto de la aplicación para acceder a SharedPreferences.
 */
class OnboardingManager(context: Context) {

    private val prefs = context.getSharedPreferences(
        "neurotracker_onboarding",
        Context.MODE_PRIVATE
    )

    /**
     * Verifica si el usuario ya ha visto la pantalla de onboarding.
     *
     * @return true si ya ha visto el onboarding, false en caso contrario.
     */
    fun hasSeenOnboarding(): Boolean =
        prefs.getBoolean("has_seen_onboarding", false)

    /**
     * Marca el onboarding como visto para que no se muestre nuevamente.
     */
    fun markOnboardingAsSeen() {
        prefs.edit().putBoolean("has_seen_onboarding", true).apply()
    }
}