package com.neurotracker.ui.tests.speed.decisionTime

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para crear instancias de [DecisionTimeViewModel].
 *
 * Permite inyectar el contexto [Application] necesario para acceder
 * a la base de datos, al gestor de sesiones y a la fuente de datos EEG.
 *
 * @param application Contexto de la aplicación.
 */
class DecisionTimeViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    /**
     * Crea una instancia de [DecisionTimeViewModel].
     *
     * @param modelClass Clase del ViewModel a instanciar.
     * @return Instancia de [DecisionTimeViewModel].
     * @throws IllegalArgumentException Si la clase solicitada no es [DecisionTimeViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DecisionTimeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return DecisionTimeViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}