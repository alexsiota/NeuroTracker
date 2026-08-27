package com.neurotracker.ui.tests.coordination.visuoMotor

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear instancias de [VisuomotorViewModel].
 *
 * Permite inyectar el contexto [Application] necesario para acceder
 * a la base de datos, al gestor de sesiones y a la fuente de datos EEG.
 *
 * @param application Contexto de la aplicación.
 */
class VisuomotorViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    /**
     * Crea una instancia de [VisuomotorViewModel].
     *
     * @param modelClass Clase del ViewModel a instanciar.
     * @return Instancia de [VisuomotorViewModel].
     * @throws IllegalArgumentException Si la clase solicitada no es [VisuomotorViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VisuomotorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return VisuomotorViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}