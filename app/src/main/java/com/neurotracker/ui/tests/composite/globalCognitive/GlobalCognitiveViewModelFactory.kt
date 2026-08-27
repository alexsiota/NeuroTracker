package com.neurotracker.ui.tests.composite.globalCognitive

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para instanciar [GlobalCognitiveViewModel].
 *
 * Es necesaria porque [GlobalCognitiveViewModel] extiende [AndroidViewModel]
 * y requiere una instancia de [Application] en su constructor. El sistema de
 * ViewModels de Jetpack Compose no puede instanciar automáticamente ViewModels
 * con dependencias personalizadas, por lo que esta factory actúa como punto
 * de creación controlado.
 *
 * Uso desde un composable:
 * ```kotlin
 * val viewModel: GlobalCognitiveViewModel = viewModel(
 *     factory = GlobalCognitiveViewModelFactory(application)
 * )
 * ```
 *
 * @param application Instancia de [Application] necesaria para que el ViewModel
 *                    pueda acceder a Room, SessionManager y SimulatedEegDataSource.
 */
class GlobalCognitiveViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia del ViewModel solicitado.
     *
     * Verifica si la clase solicitada es [GlobalCognitiveViewModel] y devuelve
     * una instancia correctamente inicializada con [application].
     *
     * @param modelClass Clase del ViewModel a instanciar.
     * @return Instancia tipada de [GlobalCognitiveViewModel].
     * @throws IllegalArgumentException si [modelClass] no es [GlobalCognitiveViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GlobalCognitiveViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GlobalCognitiveViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}