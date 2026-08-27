package com.neurotracker.ui.tests.memory.nback

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear instancias de [NBackViewModel].
 *
 * Permite inyectar el contexto [Application] necesario para acceder
 * a la base de datos, al gestor de sesiones y a la fuente de datos EEG.
 *
 * @param application Contexto de la aplicación.
 */
class NBackViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia de [NBackViewModel].
     *
     * @param modelClass Clase del ViewModel a instanciar.
     * @return Instancia de [NBackViewModel].
     * @throws IllegalArgumentException Si la clase solicitada no es [NBackViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NBackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NBackViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}