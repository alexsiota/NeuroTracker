package com.neurotracker.ui.login

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.repository.LoginResult
import com.neurotracker.data.repository.UserRepository
import com.neurotracker.data.session.SessionManager
import kotlinx.coroutines.launch

/**
 * ViewModel encargado de gestionar la lógica de inicio de sesión.
 *
 * Responsabilidades:
 * - Gestionar el estado de los campos de entrada (email, contraseña).
 * - Validar los datos introducidos por el usuario.
 * - Realizar la autenticación a través del repositorio.
 * - Controlar estados de carga y error para la UI.
 * - Persistir la sesión del usuario si así se indica.
 *
 * @param application Contexto de la aplicación necesario para inicializar base de datos y sesión.
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    /* Base de datos */
    private val database = NeuroTrackerDatabase.Companion.getDatabase(application)

    /* Repositorio */
    private val userRepository = UserRepository(database.userDao())

    /* Gestor de sesión para recordar al usuario */
    private val sessionManager = SessionManager(application)

    /* Estados de UI */
    var email = mutableStateOf("")
        private set

    var password = mutableStateOf("")
        private set

    var rememberMe = mutableStateOf(false)
        private set

    var errorMessage = mutableStateOf<String?>(null)
        private set

    var isLoading = mutableStateOf(false)
        private set

    /* Eventos de UI */

    /**
     * Actualiza el valor del email introducido por el usuario.
     *
     * @param value Nuevo valor del email.
     */
    fun onEmailChange(value: String) {
        email.value = value
    }

    /**
     * Actualiza el valor de la contraseña introducida por el usuario.
     *
     * @param value Nuevo valor de la contraseña.
     */
    fun onPasswordChange(value: String) {
        password.value = value
    }

    /**
     * Actualiza el estado del checkbox "Recuérdame".
     *
     * @param value Valor booleano que indica si el usuario desea ser recordado.
     */
    fun onRememberMeChange(value: Boolean) {
        rememberMe.value = value
    }

    /**
     * Ejecuta el proceso de autenticación del usuario.
     *
     * Flujo:
     * - Valida que los campos no estén vacíos.
     * - Llama al repositorio para comprobar las credenciales.
     * - Guarda la sesión si el login es correcto.
     * - Aplica la preferencia "Recuérdame".
     * - Gestiona estados de carga y error.
     *
     * @param onSuccess Callback que se ejecuta cuando el login es exitoso.
     */
    fun login(onSuccess: () -> Unit) {
        isLoading.value = true
        errorMessage.value = null

        viewModelScope.launch {

            // Validación básica
            if (email.value.isBlank() || password.value.isBlank()) {
                errorMessage.value = "Completa todos los campos"
                isLoading.value = false
                return@launch
            }

            // Login real usando el repositorio
            when (val result = userRepository.login(email.value, password.value)) {

                is LoginResult.Success -> {

                    // SIEMPRE guardamos la sesión del usuario activo
                    sessionManager.saveSession(email.value)

                    // Si NO quiere ser recordado, la borramos al cerrar la app
                    sessionManager.setRememberMe(rememberMe.value)


                    isLoading.value = false
                    onSuccess()
                }

                is LoginResult.Error -> {
                    errorMessage.value = result.message
                    isLoading.value = false
                }
            }
        }
    }
}