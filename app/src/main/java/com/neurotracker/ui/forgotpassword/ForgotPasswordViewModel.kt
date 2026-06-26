package com.neurotracker.ui.forgotpassword

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * ViewModel del flujo de recuperación de contraseña.
 *
 * Mantiene el estado de los tres pasos del proceso: validación del email,
 * verificación del código temporal y guardado de la nueva contraseña.
 *
 * @param application Contexto de aplicación para acceder a Room.
 */
class ForgotPasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val db             = NeuroTrackerDatabase.getDatabase(application)
    private val userRepository = UserRepository(db.userDao())

    private var verifiedEmail = ""
    private var generatedCode = ""

    // Pantalla 1
    var email          = mutableStateOf(""); private set
    var isLoadingStep1 = mutableStateOf(false); private set
    var errorStep1     = mutableStateOf<String?>(null); private set
    var codeSent       = mutableStateOf(false); private set
    var demoCode       = mutableStateOf(""); private set

    // Pantalla 2
    var enteredCode  = mutableStateOf(""); private set
    var errorStep2   = mutableStateOf<String?>(null); private set
    var codeVerified = mutableStateOf(false); private set

    // Pantalla 3
    var newPassword     = mutableStateOf(""); private set
    var confirmPassword = mutableStateOf(""); private set
    var isLoadingStep3  = mutableStateOf(false); private set
    var errorStep3      = mutableStateOf<String?>(null); private set
    var passwordChanged = mutableStateOf(false); private set

    /**
     * Actualiza el email introducido y limpia el error del primer paso.
     *
     * @param value Nuevo valor del campo email.
     */
    fun onEmailChange(value: String) { email.value = value; errorStep1.value = null }

    /**
     * Valida el email y genera un código temporal si la cuenta existe.
     *
     * @param onSuccess Acción ejecutada cuando el código queda preparado.
     */
    fun sendCode(onSuccess: () -> Unit) {
        errorStep1.value = null
        if (email.value.isBlank()) { errorStep1.value = "Introduce tu email"; return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.value).matches()) { errorStep1.value = "Email no válido"; return }
        isLoadingStep1.value = true
        viewModelScope.launch {
            val exists = userRepository.userExists(email.value)
            if (!exists) { errorStep1.value = "No existe ninguna cuenta con ese email"; isLoadingStep1.value = false; return@launch }
            verifiedEmail        = email.value
            val code             = generateCode()
            generatedCode        = code
            demoCode.value       = code
            isLoadingStep1.value = false
            codeSent.value       = true
            onSuccess()
        }
    }

    // Código de 6 caracteres (reducido de 7 para que los cuadros entren mejor)
    /**
     * Actualiza el código escrito por el usuario.
     *
     * @param value Código parcial o completo introducido desde la UI.
     */
    fun onCodeChange(value: String) {
        if (value.length <= 6) { enteredCode.value = value.uppercase(); errorStep2.value = null }
    }

    /**
     * Comprueba el código introducido contra el código generado.
     *
     * @param onSuccess Acción ejecutada cuando el código es correcto.
     */
    fun verifyCode(onSuccess: () -> Unit) {
        errorStep2.value = null
        if (enteredCode.value.length < 6) { errorStep2.value = "El código tiene 6 caracteres"; return }
        if (enteredCode.value.uppercase() == generatedCode.uppercase()) { codeVerified.value = true; onSuccess() }
        else errorStep2.value = "Código incorrecto"
    }

    // Se llama SIEMPRE al entrar en VerifyCodeScreen (LaunchedEffect(Unit))
    // Genera nuevo código y limpia los cuadros
    /**
     * Genera un nuevo código de verificación y reinicia el estado del segundo paso.
     */
    fun resendCode() {
        val code       = generateCode()
        generatedCode  = code
        demoCode.value = code
        enteredCode.value  = ""
        errorStep2.value   = null
        codeVerified.value = false
    }

    /**
     * Actualiza la nueva contraseña introducida.
     *
     * @param value Nuevo valor del campo.
     */
    fun onNewPasswordChange(value: String)     { newPassword.value = value; errorStep3.value = null }

    /**
     * Actualiza la confirmación de la nueva contraseña.
     *
     * @param value Nuevo valor del campo.
     */
    fun onConfirmPasswordChange(value: String) { confirmPassword.value = value; errorStep3.value = null }

    /**
     * Valida y guarda la nueva contraseña del usuario verificado.
     *
     * @param onSuccess Acción ejecutada cuando la contraseña se actualiza.
     */
    fun saveNewPassword(onSuccess: () -> Unit) {
        errorStep3.value = null
        when {
            newPassword.value.isBlank() || confirmPassword.value.isBlank() -> { errorStep3.value = "Rellena todos los campos"; return }
            newPassword.value.length < 8   -> { errorStep3.value = "Mínimo 8 caracteres"; return }
            newPassword.value.length > 32  -> { errorStep3.value = "Máximo 32 caracteres"; return }
            newPassword.value != confirmPassword.value -> { errorStep3.value = "Las contraseñas no coinciden"; return }
        }
        if (verifiedEmail.isBlank()) { errorStep3.value = "Sesión expirada. Vuelve a introducir tu email."; return }
        isLoadingStep3.value = true
        viewModelScope.launch {
            val user = userRepository.getUserByEmail(verifiedEmail)
            if (user == null) { errorStep3.value = "Error al encontrar la cuenta. Inténtalo de nuevo."; isLoadingStep3.value = false; return@launch }
            db.userDao().updateUser(user.copy(passwordHash = hash(newPassword.value)))
            isLoadingStep3.value  = false
            passwordChanged.value = true
            onSuccess()
        }
    }

    /**
     * Genera un código alfanumérico temporal de seis caracteres.
     *
     * @return Código en mayúsculas para verificar la recuperación.
     */
    private fun generateCode(): String {
        val pool = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { pool.random() }.joinToString("")
    }

    /**
     * Calcula el hash SHA-256 de una contraseña.
     *
     * @param password Contraseña en texto plano.
     * @return Hash hexadecimal de la contraseña.
     */
    private fun hash(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
