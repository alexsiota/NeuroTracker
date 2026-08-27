package com.neurotracker.ui.tests.speed.quickComparison

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear instancias de [QuickComparisonViewModel].
 *
 * Permite inyectar el contexto [Application] necesario para acceder
 * a la base de datos, al gestor de sesiones y a la fuente de datos EEG.
 *
 * @param application Contexto de la aplicación.
 */
class QuickComparisonViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    /**
     * Crea una instancia de [QuickComparisonViewModel].
     *
     * @param modelClass Clase del ViewModel a instanciar.
     * @return Instancia de [QuickComparisonViewModel].
     * @throws IllegalArgumentException Si la clase solicitada no es [QuickComparisonViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuickComparisonViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return QuickComparisonViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}