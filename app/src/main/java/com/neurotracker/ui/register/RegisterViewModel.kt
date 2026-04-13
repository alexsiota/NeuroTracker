package com.neurotracker.ui.register

import android.app.Application
import android.util.Patterns
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.entities.UserEntity
import com.neurotracker.data.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel encargado de la lógica de registro de usuarios.
 *
 * Responsabilidades:
 * - Gestionar los campos de entrada del formulario.
 * - Validar los datos introducidos por el usuario.
 * - Crear un nuevo usuario en la base de datos.
 * - Controlar estados de UI (errores, carga, éxito).
 * - Gestionar la cuenta atrás tras el registro exitoso.
 *
 * @param application Contexto de la aplicación necesario para inicializar dependencias.
 */
class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val database       = NeuroTrackerDatabase.getDatabase(application)
    private val userRepository = UserRepository(database.userDao())

    var name           = mutableStateOf(""); private set
    var email          = mutableStateOf(""); private set
    var password       = mutableStateOf(""); private set
    var repeatPassword = mutableStateOf(""); private set
    var birthDateMillis = mutableStateOf<Long?>(null); private set
    var birthDateText   = mutableStateOf(""); private set

    var errorMessage     = mutableStateOf<String?>(null); private set
    var showSuccessModal = mutableStateOf(false); private set
    var successCountdown = mutableStateOf(5); private set
    var isLoading        = mutableStateOf(false); private set

    /**
     * Actualiza el nombre introducido por el usuario.
     *
     * @param value Nuevo valor.
     */
    fun onNameChange(value: String)           { name.value = value }

    /**
     * Actualiza el email introducido por el usuario.
     *
     * @param value Nuevo valor.
     */
    fun onEmailChange(value: String)          { email.value = value }

    /**
     * Actualiza la contraseña introducida.
     *
     * @param value Nuevo valor.
     */
    fun onPasswordChange(value: String)       { password.value = value }

    /**
     * Actualiza la repetición de la contraseña.
     *
     * @param value Nuevo valor.
     */
    fun onRepeatPasswordChange(value: String) { repeatPassword.value = value }

    /**
     * Gestiona la selección de la fecha de nacimiento.
     *
     * - Guarda el timestamp.
     * - Formatea la fecha para mostrar en la UI.
     * - Limpia errores previos.
     *
     * @param timestamp Fecha seleccionada en milisegundos.
     */
    fun onBirthDateSelected(timestamp: Long) {
        birthDateMillis.value = timestamp
        birthDateText.value   = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
        errorMessage.value    = null
    }

    /**
     * Ejecuta el proceso de registro de usuario.
     *
     * Flujo:
     * - Valida los campos.
     * - Comprueba si el usuario ya existe.
     * - Inserta el nuevo usuario en la base de datos.
     * - Muestra modal de éxito.
     * - Inicia cuenta atrás para navegación.
     *
     * @param onNavigateToLogin Callback para navegar a la pantalla de login.
     */
    fun register(onNavigateToLogin: () -> Unit) {
        errorMessage.value = null

        when {
            name.value.isBlank() || email.value.isBlank() || password.value.isBlank()
                    || repeatPassword.value.isBlank() || birthDateMillis.value == null ->
            { errorMessage.value = "No se puede registrar con campos vacíos"; return }
            !Patterns.EMAIL_ADDRESS.matcher(email.value).matches() ->
            { errorMessage.value = "El email no tiene un formato válido"; return }
            password.value.length < 8 ->
            { errorMessage.value = "La contraseña debe tener al menos 8 caracteres"; return }
            password.value.length > 32 ->
            { errorMessage.value = "La contraseña es demasiado larga, máximo 32 caracteres"; return }
            password.value != repeatPassword.value ->
            { errorMessage.value = "Las contraseñas no coinciden"; return }
        }

        viewModelScope.launch {
            isLoading.value = true
            val exists = userRepository.userExists(email.value)
            if (exists) { errorMessage.value = "Este email ya está registrado"; isLoading.value = false; return@launch }

            userRepository.insertUser(
                UserEntity(
                    name         = name.value,
                    email        = email.value,
                    birthDate    = birthDateMillis.value!!,
                    passwordHash = hashPassword(password.value)
                )
            )
            isLoading.value        = false
            showSuccessModal.value = true
            startCountdown(onNavigateToLogin)
        }
    }

    /**
     * Inicia una cuenta atrás antes de redirigir al usuario al login.
     *
     * @param onNavigate Acción a ejecutar al finalizar la cuenta atrás.
     */
    private fun startCountdown(onNavigate: () -> Unit) {
        viewModelScope.launch {
            successCountdown.value = 5
            while (successCountdown.value > 0) {
                delay(1000)
                successCountdown.value--
            }
            showSuccessModal.value = false
            onNavigate()
        }
    }

    /**
     * Genera el hash SHA-256 de la contraseña.
     *
     * @param password Contraseña en texto plano.
     * @return Hash en formato hexadecimal.
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}