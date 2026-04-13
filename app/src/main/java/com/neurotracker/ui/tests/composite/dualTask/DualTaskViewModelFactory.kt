package com.neurotracker.ui.tests.composite.dualTask

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear instancias de DualTaskViewModel.
 *
 * Es necesaria porque DualTaskViewModel recibe un objeto Application
 * en su constructor, y el sistema de ViewModel por defecto no sabe
 * cómo proporcionarlo automáticamente en Compose.
 *
 * Se utiliza junto con:
 * viewModel(factory = DualTaskViewModelFactory(application))
 *
 * @param application Contexto global de la aplicación necesario
 *                    para acceder a base de datos, sesión y EEG.
 */
class DualTaskViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Método encargado de crear el ViewModel solicitado.
     *
     * @param modelClass Clase del ViewModel que se quiere instanciar.
     * @return Instancia de DualTaskViewModel.
     * @throws IllegalArgumentException si se solicita un ViewModel desconocido.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        /**
         * Verifica si el ViewModel solicitado es DualTaskViewModel.
         */
        if (modelClass.isAssignableFrom(DualTaskViewModel::class.java)) {

            /**
             * Se realiza cast manual porque el tipo genérico T no se puede inferir directamente.
             */
            @Suppress("UNCHECKED_CAST")
            return DualTaskViewModel(application) as T
        }

        /**
         * Se lanza excepción si se intenta crear otro ViewModel distinto.
         */
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}