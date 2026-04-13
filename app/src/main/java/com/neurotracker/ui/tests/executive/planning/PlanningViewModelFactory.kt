package com.neurotracker.ui.tests.executive.planning

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear instancias de [PlanningViewModel].
 *
 * Este patrón es necesario porque el ViewModel requiere un objeto
 * de tipo [Application] en su constructor, lo cual no puede ser
 * instanciado automáticamente por el sistema de ViewModels de Compose.
 *
 * Responsabilidades:
 *  - Proveer una instancia correctamente inicializada de PlanningViewModel.
 *  - Gestionar la creación manual cuando se requiere contexto de aplicación.
 *
 * Uso:
 *  val viewModel: PlanningViewModel = viewModel(
 *      factory = PlanningViewModelFactory(application)
 *  )
 *
 * @param application Contexto global de la aplicación necesario para
 *                    acceso a recursos, base de datos o lógica persistente.
 */
class PlanningViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    /**
     * Crea una instancia del ViewModel solicitado.
     *
     * Comprueba si la clase solicitada corresponde a [PlanningViewModel]
     * y devuelve una instancia con el contexto adecuado.
     *
     * @param modelClass Clase del ViewModel que se desea instanciar.
     * @return Instancia de PlanningViewModel.
     *
     * @throws IllegalArgumentException si la clase no es reconocida.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlanningViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlanningViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}