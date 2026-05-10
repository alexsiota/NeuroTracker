package com.neurotracker.ui.tests.coordination.spatialPerception

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear instancias de [SpatialPerceptionViewModel].
 *
 * Permite inyectar el contexto [Application] necesario para acceder
 * a la base de datos, al gestor de sesiones y a la fuente de datos EEG.
 *
 * @param application Contexto de la aplicación.
 */
class SpatialPerceptionViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    /**
     * Crea una instancia de [SpatialPerceptionViewModel].
     *
     * @param modelClass Clase del ViewModel a instanciar.
     * @return Instancia de [SpatialPerceptionViewModel].
     * @throws IllegalArgumentException Si la clase solicitada no es [SpatialPerceptionViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpatialPerceptionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return SpatialPerceptionViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}