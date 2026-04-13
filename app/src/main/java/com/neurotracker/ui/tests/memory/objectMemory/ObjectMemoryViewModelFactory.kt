package com.neurotracker.ui.tests.memory.objectMemory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear instancias de [ObjectMemoryViewModel].
 *
 * Permite inyectar el contexto [Application] necesario para acceder
 * a la base de datos, al gestor de sesiones y a la fuente de datos EEG.
 *
 * @param application Contexto de la aplicación.
 */
class ObjectMemoryViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia de [ObjectMemoryViewModel].
     *
     * @param modelClass Clase del ViewModel a instanciar.
     * @return Instancia de [ObjectMemoryViewModel].
     * @throws IllegalArgumentException Si la clase solicitada no es [ObjectMemoryViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ObjectMemoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ObjectMemoryViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}