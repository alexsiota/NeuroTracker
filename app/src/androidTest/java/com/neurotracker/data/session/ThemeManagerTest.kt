package com.neurotracker.data.session

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [ThemeManager].
 *
 * Verifica la persistencia de la preferencia de tema (oscuro/claro)
 * en SharedPreferences:
 *  - El valor por defecto es modo oscuro.
 *  - Se puede activar y desactivar el modo oscuro.
 *  - La preferencia persiste entre instancias del manager.
 *  - Alternar entre modos funciona correctamente.
 *
 * Se ejecuta en dispositivo o emulador porque [ThemeManager]
 * usa [android.content.SharedPreferences], que requiere contexto Android.
 */
@RunWith(AndroidJUnit4::class)
class ThemeManagerTest {

    private lateinit var manager: ThemeManager

    @Before
    fun setUp() {
        ApplicationProvider.getApplicationContext<android.app.Application>()
            .getSharedPreferences("neurotracker_theme", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()
        manager = ThemeManager(ApplicationProvider.getApplicationContext())
    }

    /**
     * Verifica que [ThemeManager.isDarkMode] devuelve true por defecto,
     * ya que la app usa modo oscuro como tema inicial.
     */
    @Test
    fun isDarkMode_byDefault_returnsTrue() {
        assertTrue(manager.isDarkMode())
    }

    /**
     * Verifica que [ThemeManager.setDarkMode] con false activa el modo claro
     * y [ThemeManager.isDarkMode] lo refleja correctamente.
     */
    @Test
    fun setDarkMode_false_switchesToLightMode() {
        manager.setDarkMode(false)
        assertFalse(manager.isDarkMode())
    }

    /**
     * Verifica que [ThemeManager.setDarkMode] con true mantiene el modo oscuro.
     */
    @Test
    fun setDarkMode_true_keepsDarkMode() {
        manager.setDarkMode(true)
        assertTrue(manager.isDarkMode())
    }

    /**
     * Verifica que la preferencia de tema persiste entre instancias distintas
     * del manager, simulando lo que ocurre entre reinicios de la app.
     */
    @Test
    fun setDarkMode_persistsAcrossNewInstances() {
        manager.setDarkMode(false)
        val newInstance = ThemeManager(ApplicationProvider.getApplicationContext())
        assertFalse(newInstance.isDarkMode())
    }

    /**
     * Verifica que alternar entre modo oscuro y claro funciona correctamente
     * en múltiples cambios consecutivos.
     */
    @Test
    fun toggleDarkMode_multipleTimesWorksCorrectly() {
        manager.setDarkMode(false)
        assertFalse(manager.isDarkMode())
        manager.setDarkMode(true)
        assertTrue(manager.isDarkMode())
        manager.setDarkMode(false)
        assertFalse(manager.isDarkMode())
    }
}
