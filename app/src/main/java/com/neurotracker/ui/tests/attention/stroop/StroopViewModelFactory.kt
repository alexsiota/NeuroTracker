import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neurotracker.ui.tests.attention.stroop.StroopViewModel

/**
 * Factory para la creación de instancias de [StroopViewModel].
 *
 * Responsabilidades:
 * - Proveer una instancia del ViewModel con el contexto de aplicación.
 * - Permitir la correcta inicialización desde Jetpack Compose.
 *
 * @property application Contexto de la aplicación necesario para el ViewModel.
 */
class StroopViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia del ViewModel solicitado.
     *
     * @param modelClass Clase del ViewModel a instanciar.
     * @return Instancia de StroopViewModel.
     * @throws IllegalArgumentException Si la clase no corresponde a StroopViewModel.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StroopViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StroopViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}