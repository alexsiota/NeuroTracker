package com.neurotracker.ui.tests.coordination.objectTracking

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear instancias de ObjectTrackingViewModel.
 *
 * Es necesaria porque ObjectTrackingViewModel requiere un objeto Application
 * en su constructor, y ViewModelProvider no puede instanciarlo automáticamente
 * sin una fábrica personalizada.
 *
 * Responsabilidades:
 *  - Proporcionar el contexto de aplicación al ViewModel.
 *  - Crear la instancia correcta del ViewModel cuando se solicita.
 *  - Validar el tipo de ViewModel para evitar errores en tiempo de ejecución.
 *
 * @param application Contexto de la aplicación utilizado por el ViewModel.
 */
class ObjectTrackingViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia del ViewModel solicitado.
     *
     * @param modelClass Clase del ViewModel que se quiere instanciar.
     * @return Instancia de ObjectTrackingViewModel.
     * @throws IllegalArgumentException si la clase no coincide con el ViewModel esperado.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ObjectTrackingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ObjectTrackingViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}