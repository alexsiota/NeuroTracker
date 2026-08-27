package com.neurotracker.ui.tests.attention.visualCancellation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para la creación de instancias de [VisualCancellationViewModel].
 *
 * Responsabilidades:
 * - Proveer una instancia del ViewModel con el contexto de aplicación.
 * - Permitir la inicialización correcta desde Jetpack Compose.
 *
 * @property application Contexto de la aplicación necesario para el ViewModel.
 */
class VisualCancellationViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia del ViewModel solicitado.
     *
     * @param modelClass Clase del ViewModel a crear.
     * @return Instancia de VisualCancellationViewModel.
     * @throws IllegalArgumentException Si la clase no corresponde a VisualCancellationViewModel.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VisualCancellationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VisualCancellationViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}