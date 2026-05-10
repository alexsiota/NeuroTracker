package com.neurotracker.ui.eeg

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory personalizada para la creación de [EegViewModel].
 *
 * Propósito:
 * - Permitir la inyección de dependencias en el ViewModel,
 *   en este caso el objeto [Application].
 *
 * Contexto:
 * - Android no permite pasar parámetros directamente al constructor
 *   de un ViewModel cuando se crea mediante los mecanismos estándar.
 * - Por ello, se utiliza una implementación de [ViewModelProvider.Factory].
 *
 * Uso:
 * - Se emplea desde la UI (Compose) al inicializar el ViewModel,
 *   garantizando que recibe el contexto necesario.
 *
 * Escalabilidad:
 * - Esta factory permite ampliar fácilmente la inyección de dependencias
 *   (por ejemplo, repositorios, data sources, etc.).
 *
 * @param application Contexto de aplicación requerido por el ViewModel.
 */
class EegViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia del ViewModel solicitado.
     *
     * Funcionamiento:
     * - Verifica si la clase solicitada corresponde a [EegViewModel].
     * - Si coincide, crea la instancia pasando el [Application].
     * - En caso contrario, lanza una excepción para evitar errores silenciosos.
     *
     * @param modelClass Clase del ViewModel que se desea instanciar.
     * @return Instancia del ViewModel correspondiente.
     * @throws IllegalArgumentException si la clase no está soportada por esta factory.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EegViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EegViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}