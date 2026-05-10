package com.neurotracker.ui.tests.executive.tasksWitching

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear instancias de [TaskSwitchingViewModel].
 *
 * Permite inyectar el contexto [Application] necesario para acceder
 * a la base de datos, al gestor de sesiones y a la fuente de datos EEG.
 *
 * @param application Contexto de la aplicación.
 */
class TaskSwitchingViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    /**
     * Crea una instancia de [TaskSwitchingViewModel].
     *
     * @param modelClass Clase del ViewModel a instanciar.
     * @return Instancia de [TaskSwitchingViewModel].
     * @throws IllegalArgumentException Si la clase solicitada no es [TaskSwitchingViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskSwitchingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return TaskSwitchingViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}