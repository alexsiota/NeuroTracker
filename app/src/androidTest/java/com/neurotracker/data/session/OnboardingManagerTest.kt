package com.neurotracker.data.session

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [OnboardingManager].
 *
 * Verifica el ciclo de vida completo del estado de onboarding:
 *  - Por defecto el onboarding no ha sido visto.
 *  - Después de marcarlo como visto, se persiste correctamente.
 *  - El estado persiste entre instancias del manager.
 *
 * Se ejecuta en dispositivo o emulador porque [OnboardingManager]
 * usa [android.content.SharedPreferences], que requiere contexto Android.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingManagerTest {

    private lateinit var manager: OnboardingManager

    @Before
    fun setUp() {
        manager = OnboardingManager(ApplicationProvider.getApplicationContext())
        // Limpiar estado previo para garantizar aislamiento entre tests
        ApplicationProvider.getApplicationContext<android.app.Application>()
            .getSharedPreferences("neurotracker_onboarding", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    /**
     * Verifica que [OnboardingManager.hasSeenOnboarding] devuelve false
     * cuando no se ha marcado el onboarding como visto.
     */
    @Test
    fun hasSeenOnboarding_byDefault_returnsFalse() {
        assertFalse(manager.hasSeenOnboarding())
    }

    /**
     * Verifica que [OnboardingManager.markOnboardingAsSeen] persiste el estado
     * y [OnboardingManager.hasSeenOnboarding] devuelve true tras llamarla.
     */
    @Test
    fun markOnboardingAsSeen_thenHasSeenOnboarding_returnsTrue() {
        manager.markOnboardingAsSeen()
        assertTrue(manager.hasSeenOnboarding())
    }

    /**
     * Verifica que el estado persiste entre instancias distintas del manager,
     * simulando lo que ocurre entre reinicios de la app.
     */
    @Test
    fun markOnboardingAsSeen_persistsAcrossNewInstances() {
        manager.markOnboardingAsSeen()
        val newInstance = OnboardingManager(ApplicationProvider.getApplicationContext())
        assertTrue(newInstance.hasSeenOnboarding())
    }

    /**
     * Verifica que limpiar las preferencias manualmente restablece el estado
     * a no visto, lo que equivale a una reinstalación de la app.
     */
    @Test
    fun afterClearingPreferences_hasSeenOnboarding_returnsFalse() {
        manager.markOnboardingAsSeen()
        ApplicationProvider.getApplicationContext<android.app.Application>()
            .getSharedPreferences("neurotracker_onboarding", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()
        assertFalse(manager.hasSeenOnboarding())
    }
}
