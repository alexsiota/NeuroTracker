package com.neurotracker.ui.tests.attention.sustainedAttention

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para la creación de instancias de [SustainedAttentionViewModel].
 *
 * Responsabilidades:
 * - Proveer una instancia del ViewModel con el contexto de aplicación.
 * - Permitir la correcta inicialización desde Jetpack Compose cuando el ViewModel requiere Application.
 *
 * @property application Contexto de la aplicación necesario para el ViewModel.
 */
class SustainedAttentionViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia del ViewModel solicitado.
     *
     * @param modelClass Clase del ViewModel que se desea crear.
     * @return Instancia de SustainedAttentionViewModel.
     * @throws IllegalArgumentException Si la clase no corresponde a SustainedAttentionViewModel.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SustainedAttentionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SustainedAttentionViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}