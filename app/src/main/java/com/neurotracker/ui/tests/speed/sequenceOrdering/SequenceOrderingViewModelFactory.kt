package com.neurotracker.ui.tests.speed.sequenceOrdering

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear instancias de [SequenceOrderingViewModel].
 *
 * Permite inyectar el contexto [Application] necesario para acceder
 * a la base de datos, al gestor de sesiones y a la fuente de datos EEG.
 *
 * @param application Contexto de la aplicación.
 */
class SequenceOrderingViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    /**
     * Crea una instancia de [SequenceOrderingViewModel].
     *
     * @param modelClass Clase del ViewModel a instanciar.
     * @return Instancia de [SequenceOrderingViewModel].
     * @throws IllegalArgumentException Si la clase solicitada no es [SequenceOrderingViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SequenceOrderingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return SequenceOrderingViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}