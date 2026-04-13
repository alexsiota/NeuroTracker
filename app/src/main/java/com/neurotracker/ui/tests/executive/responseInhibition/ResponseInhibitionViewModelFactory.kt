package com.neurotracker.ui.tests.executive.responseInhibition

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para instanciar [ResponseInhibitionViewModel].
 *
 * Este patrón es necesario porque [ResponseInhibitionViewModel] extiende
 * [AndroidViewModel] y requiere una instancia de [Application] en su constructor.
 * El sistema de ViewModels de Jetpack Compose no puede instanciar automáticamente
 * ViewModels con dependencias personalizadas, por lo que esta factory actúa como
 * punto de creación controlado.
 *
 * Uso desde un composable:
 * ```kotlin
 * val viewModel: ResponseInhibitionViewModel = viewModel(
 *     factory = ResponseInhibitionViewModelFactory(application)
 * )
 * ```
 *
 * @param application Instancia de [Application] necesaria para que el ViewModel
 *                    pueda acceder a Room, SessionManager y SimulatedEegDataSource.
 */
class ResponseInhibitionViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia del ViewModel solicitado.
     *
     * Verifica si la clase solicitada es [ResponseInhibitionViewModel] y
     * devuelve una instancia correctamente inicializada con [application].
     *
     * @param modelClass Clase del ViewModel a instanciar.
     * @return Instancia tipada de [ResponseInhibitionViewModel].
     * @throws IllegalArgumentException si [modelClass] no es [ResponseInhibitionViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResponseInhibitionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ResponseInhibitionViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}