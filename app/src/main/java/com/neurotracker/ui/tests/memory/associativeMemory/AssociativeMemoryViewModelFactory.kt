package com.neurotracker.ui.tests.memory.associativeMemory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear instancias de [AssociativeMemoryViewModel].
 *
 * Esta clase es necesaria porque el ViewModel requiere un objeto
 * de tipo [Application] en su constructor, lo cual no puede ser
 * gestionado automáticamente por el sistema de ViewModel en Compose.
 *
 * Función principal:
 *  - Proporcionar una forma controlada de instanciar el ViewModel
 *    inyectando el contexto de la aplicación.
 *
 * Uso típico:
 *  - Se utiliza junto a viewModel(factory = ...) dentro de composables.
 *
 * Ejemplo:
 *  val viewModel: AssociativeMemoryViewModel = viewModel(
 *      factory = AssociativeMemoryViewModelFactory(application)
 *  )
 *
 * @param application Contexto global de la aplicación necesario
 *                    para acceder a base de datos, sesión y EEG.
 */
class AssociativeMemoryViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia del ViewModel solicitado.
     *
     * @param modelClass Clase del ViewModel que se desea crear.
     * @return Instancia del ViewModel correspondiente.
     *
     * @throws IllegalArgumentException si la clase no es reconocida.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(AssociativeMemoryViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return AssociativeMemoryViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}