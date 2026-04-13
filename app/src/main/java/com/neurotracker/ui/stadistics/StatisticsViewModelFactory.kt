package com.neurotracker.ui.stadistics

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear instancias de [StatisticsViewModel].
 *
 * Permite inyectar el contexto [Application] necesario para acceder
 * a la base de datos y al gestor de sesiones.
 *
 * @param application Contexto de la aplicación.
 */
class StatisticsViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia de [StatisticsViewModel].
     *
     * @param modelClass Clase del ViewModel a instanciar.
     * @return Instancia de [StatisticsViewModel].
     * @throws IllegalArgumentException Si la clase solicitada no es [StatisticsViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatisticsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}