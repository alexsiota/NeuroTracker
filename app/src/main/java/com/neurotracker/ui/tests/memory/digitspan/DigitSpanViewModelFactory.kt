package com.neurotracker.ui.tests.memory.digitspan

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para la creación de instancias de [DigitSpanViewModel].
 *
 * Esta clase permite inyectar el contexto de tipo [Application] en el ViewModel,
 * lo cual es necesario cuando el ViewModel depende de recursos globales como:
 *  - Base de datos (Room)
 *  - Gestión de sesión (SessionManager)
 *  - Fuentes de datos externas (como EEG simulado)
 *
 * El sistema estándar de ViewModelProvider no puede crear automáticamente
 * ViewModels con parámetros en el constructor, por lo que se requiere esta factory.
 *
 * Uso en Compose:
 *  val viewModel: DigitSpanViewModel = viewModel(
 *      factory = DigitSpanViewModelFactory(application)
 *  )
 *
 * @param application Contexto global de la aplicación.
 */
class DigitSpanViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Método encargado de crear la instancia del ViewModel solicitado.
     *
     * @param modelClass Clase del ViewModel que se desea instanciar.
     * @return Instancia del ViewModel correspondiente.
     *
     * @throws IllegalArgumentException si el ViewModel no es reconocido.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(DigitSpanViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return DigitSpanViewModel(application) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}