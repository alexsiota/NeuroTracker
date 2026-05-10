package com.neurotracker.ui.tests.attention.reaction

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para la creación de instancias de [ReactionTestViewModel].
 *
 * Responsabilidades:
 * - Proveer una instancia de ReactionTestViewModel con el contexto de aplicación.
 * - Permitir a Jetpack Compose instanciar correctamente un ViewModel que requiere Application.
 *
 * @property application Contexto de la aplicación necesario para inicializar el ViewModel.
 */
class ReactionTestViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia del ViewModel solicitado.
     *
     * @param modelClass Clase del ViewModel que se desea crear.
     * @return Instancia de ReactionTestViewModel.
     * @throws IllegalArgumentException Si la clase solicitada no corresponde a ReactionTestViewModel.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReactionTestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReactionTestViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}