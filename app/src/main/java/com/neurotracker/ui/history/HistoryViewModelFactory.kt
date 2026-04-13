package com.neurotracker.ui.history

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neurotracker.ui.history.HistoryViewModel

/**
 * Factory personalizada para la creación de instancias de [HistoryViewModel].
 *
 * Se utiliza cuando el ViewModel requiere parámetros en su constructor,
 * en este caso el objeto [Application].
 *
 * Responsabilidades:
 * - Proveer instancias de HistoryViewModel correctamente inicializadas.
 * - Validar que la clase solicitada corresponde al ViewModel esperado.
 *
 * @property application Contexto de la aplicación necesario para crear el ViewModel.
 */
class HistoryViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia del ViewModel solicitado.
     *
     * Comprueba si la clase solicitada es [HistoryViewModel] y, en ese caso,
     * devuelve una nueva instancia con el parámetro necesario.
     *
     * @param modelClass Clase del ViewModel que se desea crear.
     * @return Instancia del ViewModel correspondiente.
     * @throws IllegalArgumentException Si la clase solicitada no es reconocida.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(application) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}