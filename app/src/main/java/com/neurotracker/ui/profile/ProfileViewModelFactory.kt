package com.neurotracker.ui.profile

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para la creación de instancias de [ProfileViewModel].
 *
 * Responsabilidades:
 * - Proveer una instancia de ProfileViewModel con el contexto de aplicación.
 * - Validar el tipo de ViewModel solicitado.
 *
 * @property application Contexto de la aplicación necesario para inicializar el ViewModel.
 */
class ProfileViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia del ViewModel solicitado.
     *
     * @param modelClass Clase del ViewModel que se desea crear.
     * @return Instancia del ViewModel correspondiente.
     * @throws IllegalArgumentException Si la clase solicitada no corresponde a ProfileViewModel.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}