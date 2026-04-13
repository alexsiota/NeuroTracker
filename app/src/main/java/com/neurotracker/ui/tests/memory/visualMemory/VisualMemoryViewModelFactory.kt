package com.neurotracker.ui.tests.memory.visualMemory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neurotracker.ui.tests.memory.visualMemory.VisualMemoryViewModel

/**
 * Factory para la creación de instancias de [VisualMemoryViewModel].
 *
 * Esta clase permite inyectar el contexto de tipo [Application] en el ViewModel,
 * necesario para acceder a recursos globales como:
 *  - Base de datos (Room)
 *  - Gestión de sesión del usuario
 *  - Fuente de datos EEG simulada
 *
 * Debido a que ViewModelProvider no puede instanciar ViewModels con parámetros
 * en el constructor de forma automática, es obligatorio utilizar una factory
 * personalizada como esta.
 *
 * Uso en Compose:
 *  val viewModel: VisualMemoryViewModel = viewModel(
 *      factory = VisualMemoryViewModelFactory(application)
 *  )
 *
 * Nota:
 *  - Este ViewModel está ubicado en un paquete diferente (visualmemory),
 *    por lo que se requiere import explícito.
 *
 * @param application Contexto global de la aplicación.
 */
class VisualMemoryViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Método encargado de crear la instancia del ViewModel solicitado.
     *
     * @param modelClass Clase del ViewModel que se desea instanciar.
     * @return Instancia del ViewModel correspondiente.
     *
     * @throws IllegalArgumentException si la clase no es reconocida.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {


        if (modelClass.isAssignableFrom(VisualMemoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VisualMemoryViewModel(application) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}