package com.neurotracker.ui.tests.attention.selectiveAttention

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para la creación de instancias de [SelectiveAttentionViewModel].
 *
 * Responsabilidades:
 * - Proveer una instancia del ViewModel con el contexto de aplicación.
 * - Permitir a Jetpack Compose instanciar ViewModels que requieren Application.
 *
 * @property application Contexto de la aplicación necesario para inicializar el ViewModel.
 */
class SelectiveAttentionViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia del ViewModel solicitado.
     *
     * @param modelClass Clase del ViewModel que se desea crear.
     * @return Instancia de SelectiveAttentionViewModel.
     * @throws IllegalArgumentException Si la clase no corresponde a SelectiveAttentionViewModel.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelectiveAttentionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SelectiveAttentionViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}