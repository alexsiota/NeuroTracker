package com.neurotracker.ui.profile

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.profile.ProfilePhotoManager
import com.neurotracker.data.repository.LoginResult
import com.neurotracker.data.repository.UserRepository
import com.neurotracker.data.session.SessionEvents
import com.neurotracker.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * ViewModel de la pantalla de perfil del usuario.
 *
 * Gestiona los datos del usuario autenticado y las operaciones de edición:
 *  - Cambio de contraseña con verificación de la actual.
 *  - Cambio de email con verificación de identidad, migración de tests
 *    y notificación instantánea al dashboard mediante [SessionEvents.notifyEmailChanged].
 *  - Cambio de nombre.
 *  - Gestión de la foto de perfil (cámara, galería, eliminación) con
 *    notificación instantánea al dashboard mediante [SessionEvents.notifyPhotoChanged].
 *  - Cierre de sesión.
 *
 * @param application Contexto de aplicación para Room, SessionManager y FileProvider.
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val db             = NeuroTrackerDatabase.getDatabase(application)
    private val userRepository = UserRepository(db.userDao())
    private val sessionManager = SessionManager(application)

    var userName         = mutableStateOf(""); private set
    var userEmail        = mutableStateOf(""); private set
    var profilePhotoUri  = mutableStateOf<Uri?>(null); private set
    var photoVersion     = mutableIntStateOf(0); private set
    var showPhotoOptions = mutableStateOf(false); private set
    var cameraUri        = mutableStateOf<Uri?>(null); private set

    var showChangePassword = mutableStateOf(false); private set
    var showChangeEmail    = mutableStateOf(false); private set
    var showChangeName     = mutableStateOf(false); private set

    var currentPassword = mutableStateOf(""); private set
    var newPassword     = mutableStateOf(""); private set
    var confirmPassword = mutableStateOf(""); private set
    var passwordError   = mutableStateOf<String?>(null); private set
    var passwordSuccess = mutableStateOf(false); private set

    var currentEmail     = mutableStateOf(""); private set
    var newEmail         = mutableStateOf(""); private set
    var emailPassword    = mutableStateOf(""); private set
    var emailError       = mutableStateOf<String?>(null); private set
    var emailSuccess     = mutableStateOf(false); private set
    var emailSuggestions = mutableStateOf<List<String>>(emptyList()); private set

    var newName     = mutableStateOf(""); private set
    var nameError   = mutableStateOf<String?>(null); private set
    var nameSuccess = mutableStateOf(false); private set

    init { loadUser() }

    /**
     * Carga la URI de la foto de perfil desde el almacenamiento local.
     *
     * @param email Email del usuario cuya foto se quiere cargar.
     */
    fun loadProfilePhoto(email: String = userEmail.value) {
        if (email.isBlank()) return
        val context = getApplication<Application>()
        profilePhotoUri.value = ProfilePhotoManager.getPhotoUri(context, email)
    }

    /**
     * Prepara una URI temporal en caché para que la cámara escriba la foto.
     *
     * @return URI donde la cámara escribirá la foto capturada.
     */
    fun prepareCameraUri(): Uri {
        val context   = getApplication<Application>()
        val cacheFile = java.io.File(context.cacheDir, "camera_temp.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )
        cameraUri.value = uri
        return uri
    }

    /**
     * Procesa la foto capturada con la cámara, la guarda como foto de perfil
     * y notifica el cambio al dashboard mediante [SessionEvents.notifyPhotoChanged].
     *
     * @param success true si la cámara completó la captura correctamente.
     */
    fun onCameraTaken(success: Boolean) {
        if (!success) { showPhotoOptions.value = false; return }
        val tempUri = cameraUri.value ?: return
        val email   = userEmail.value.ifBlank { return }
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val stream  = context.contentResolver.openInputStream(tempUri)
            val bitmap  = BitmapFactory.decodeStream(stream)
            stream?.close()
            if (bitmap != null) {
                val dest = ProfilePhotoManager.getPhotoFile(context, email)
                FileOutputStream(dest).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                withContext(Dispatchers.Main) {
                    profilePhotoUri.value = null
                    profilePhotoUri.value = ProfilePhotoManager.getPhotoUri(context, email)
                    photoVersion.intValue++
                    SessionEvents.notifyPhotoChanged()
                }
            }
        }
        showPhotoOptions.value = false
    }

    /**
     * Procesa una imagen seleccionada desde la galería, la guarda como foto de perfil
     * y notifica el cambio al dashboard mediante [SessionEvents.notifyPhotoChanged].
     *
     * @param uri URI de contenido de la imagen seleccionada. null si se canceló.
     */
    fun onGalleryImageSelected(uri: Uri?) {
        if (uri == null) { showPhotoOptions.value = false; return }
        val email = userEmail.value.ifBlank { return }
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val stream  = context.contentResolver.openInputStream(uri)
            val bitmap  = BitmapFactory.decodeStream(stream)
            stream?.close()
            if (bitmap != null) {
                val dest = ProfilePhotoManager.getPhotoFile(context, email)
                FileOutputStream(dest).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                withContext(Dispatchers.Main) {
                    profilePhotoUri.value = null
                    profilePhotoUri.value = ProfilePhotoManager.getPhotoUri(context, email)
                    photoVersion.intValue++
                    SessionEvents.notifyPhotoChanged()
                }
            }
        }
        showPhotoOptions.value = false
    }

    /**
     * Elimina la foto de perfil, restaura el avatar por defecto
     * y notifica el cambio al dashboard mediante [SessionEvents.notifyPhotoChanged].
     */
    fun removeProfilePhoto() {
        val email = userEmail.value.ifBlank { return }
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            ProfilePhotoManager.deletePhoto(context, email)
            withContext(Dispatchers.Main) {
                profilePhotoUri.value  = null
                photoVersion.intValue++
                showPhotoOptions.value = false
                SessionEvents.notifyPhotoChanged()
            }
        }
    }

    /**
     * Abre el menú de opciones de foto de perfil.
     */
    fun openPhotoOptions() { showPhotoOptions.value = true }

    /**
     * Cierra el menú de opciones de foto de perfil.
     */
    fun closePhotoOptions() { showPhotoOptions.value = false }

    /**
     * Abre el diálogo de cambio de contraseña reiniciando su estado.
     */
    fun openChangePassword() {
        passwordError.value = null; passwordSuccess.value = false
        currentPassword.value = ""; newPassword.value = ""; confirmPassword.value = ""
        showChangePassword.value = true
    }

    /**
     * Cierra el diálogo de cambio de contraseña.
     */
    fun closeChangePassword() { showChangePassword.value = false }

    /**
     * Actualiza el campo de contraseña actual en el estado de la UI.
     *
     * @param v Nuevo valor del campo.
     */
    fun onCurrentPasswordChange(v: String) { currentPassword.value = v }

    /**
     * Actualiza el campo de nueva contraseña en el estado de la UI.
     *
     * @param v Nuevo valor del campo.
     */
    fun onNewPasswordChange(v: String) { newPassword.value = v }

    /**
     * Actualiza el campo de confirmación de contraseña en el estado de la UI.
     *
     * @param v Nuevo valor del campo.
     */
    fun onConfirmPasswordChange(v: String) { confirmPassword.value = v }

    /**
     * Valida y ejecuta el cambio de contraseña.
     *
     * Verifica que la contraseña actual es correcta antes de actualizar.
     * Muestra errores específicos para cada caso de validación fallida.
     */
    fun submitChangePassword() {
        passwordError.value = null
        when {
            currentPassword.value.isBlank() || newPassword.value.isBlank() || confirmPassword.value.isBlank() ->
            { passwordError.value = "Rellena todos los campos"; return }
            newPassword.value.length < 8 ->
            { passwordError.value = "Mínimo 8 caracteres"; return }
            newPassword.value != confirmPassword.value ->
            { passwordError.value = "Las contraseñas no coinciden"; return }
            newPassword.value == currentPassword.value ->
            { passwordError.value = "La nueva contraseña debe ser distinta"; return }
        }
        viewModelScope.launch {
            val email  = sessionManager.getUserEmail() ?: return@launch
            val result = userRepository.login(email, currentPassword.value)
            if (result is LoginResult.Error) { passwordError.value = "Contraseña actual incorrecta"; return@launch }
            val user = userRepository.getUserByEmail(email) ?: return@launch
            db.userDao().updateUser(user.copy(passwordHash = hashPassword(newPassword.value)))
            passwordSuccess.value = true
        }
    }

    /**
     * Abre el diálogo de cambio de email reiniciando su estado.
     */
    fun openChangeEmail() {
        emailError.value = null; emailSuccess.value = false
        currentEmail.value = ""; newEmail.value = ""; emailPassword.value = ""
        emailSuggestions.value = emptyList()
        showChangeEmail.value = true
    }

    /**
     * Cierra el diálogo de cambio de email.
     */
    fun closeChangeEmail() { showChangeEmail.value = false }

    /**
     * Actualiza el campo de email actual en el estado de la UI.
     *
     * @param v Nuevo valor del campo.
     */
    fun onCurrentEmailChange(v: String) { currentEmail.value = v; emailError.value = null; emailSuggestions.value = emptyList() }

    /**
     * Actualiza el campo de nuevo email en el estado de la UI.
     *
     * @param v Nuevo valor del campo.
     */
    fun onNewEmailChange(v: String) { newEmail.value = v; emailError.value = null; emailSuggestions.value = emptyList() }

    /**
     * Actualiza el campo de contraseña para confirmar el cambio de email.
     *
     * @param v Nuevo valor del campo.
     */
    fun onEmailPasswordChange(v: String) { emailPassword.value = v; emailError.value = null }

    /**
     * Valida y ejecuta el cambio de email.
     *
     * Verifica identidad (email actual + contraseña), comprueba disponibilidad
     * del nuevo email, actualiza Room, migra los resultados de tests al nuevo
     * email y notifica al dashboard mediante [SessionEvents.notifyEmailChanged]
     * para que la UI se actualice al instante.
     */
    fun submitChangeEmail() {
        emailError.value       = null
        emailSuggestions.value = emptyList()
        when {
            currentEmail.value.isBlank() || newEmail.value.isBlank() || emailPassword.value.isBlank() ->
            { emailError.value = "Rellena todos los campos"; return }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail.value).matches() ->
            { emailError.value = "El nuevo email no tiene formato válido"; return }
            newEmail.value == currentEmail.value ->
            { emailError.value = "El nuevo email es igual al actual"; return }
        }
        viewModelScope.launch {
            val sessionEmail = sessionManager.getUserEmail() ?: return@launch
            if (currentEmail.value.trim().lowercase() != sessionEmail.trim().lowercase()) {
                emailError.value = "El email actual no coincide con tu cuenta"; return@launch
            }
            val result = userRepository.login(sessionEmail, emailPassword.value)
            if (result is LoginResult.Error) { emailError.value = "Contraseña incorrecta"; return@launch }
            if (userRepository.userExists(newEmail.value)) {
                emailSuggestions.value = generateEmailSuggestions(newEmail.value)
                emailError.value = "Ese email ya está registrado. Prueba una de estas sugerencias:"
                return@launch
            }
            val user = userRepository.getUserByEmail(sessionEmail) ?: return@launch
            db.userDao().updateUser(user.copy(email = newEmail.value))
            migrateTestResults(oldEmail = sessionEmail, newEmail = newEmail.value)
            sessionManager.saveSession(newEmail.value)
            userEmail.value    = newEmail.value
            emailSuccess.value = true
            SessionEvents.notifyEmailChanged(newEmail.value)
        }
    }

    /**
     * Genera hasta 3 sugerencias de email alternativo cuando el solicitado ya existe.
     *
     * @param email Email que ya está ocupado.
     * @return Lista de hasta 3 sugerencias disponibles no registradas en Room.
     */
    private suspend fun generateEmailSuggestions(email: String): List<String> {
        val parts  = email.split("@")
        if (parts.size != 2) return emptyList()
        val user   = parts[0]
        val domain = parts[1]
        val year   = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val candidates = listOf(
            "${user}${year}@${domain}",
            "${user}_2@${domain}",
            "${user}${(10..99).random()}@${domain}"
        )
        return candidates.filter { !userRepository.userExists(it) }.take(3)
    }

    /**
     * Migra todos los resultados de tests del email antiguo al nuevo.
     *
     * @param oldEmail Email anterior del usuario.
     * @param newEmail Nuevo email del usuario.
     */
    private suspend fun migrateTestResults(oldEmail: String, newEmail: String) {
        withContext(Dispatchers.IO) {
            db.testResultDao().updateUserEmail(oldEmail, newEmail)
        }
    }

    /**
     * Abre el diálogo de cambio de nombre reiniciando su estado.
     * Precarga el nombre actual como valor inicial del campo.
     */
    fun openChangeName() {
        nameError.value      = null
        nameSuccess.value    = false
        newName.value        = userName.value
        showChangeName.value = true
    }

    /**
     * Cierra el diálogo de cambio de nombre.
     */
    fun closeChangeName() { showChangeName.value = false }

    /**
     * Actualiza el campo de nuevo nombre en el estado de la UI.
     *
     * @param v Nuevo valor del campo.
     */
    fun onNewNameChange(v: String) { newName.value = v; nameError.value = null }

    /**
     * Valida y ejecuta el cambio de nombre.
     *
     * Solo actualiza el campo name en Room. No modifica email, contraseña,
     * resultados de tests ni foto de perfil.
     */
    fun submitChangeName() {
        nameError.value = null
        when {
            newName.value.isBlank()         -> { nameError.value = "El nombre no puede estar vacío"; return }
            newName.value.length < 2        -> { nameError.value = "El nombre debe tener al menos 2 caracteres"; return }
            newName.value.length > 50       -> { nameError.value = "El nombre es demasiado largo"; return }
            newName.value == userName.value -> { nameError.value = "El nombre es igual al actual"; return }
        }
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val user  = userRepository.getUserByEmail(email) ?: return@launch
            db.userDao().updateUser(user.copy(name = newName.value))
            userName.value    = newName.value
            nameSuccess.value = true
        }
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    fun logout() { sessionManager.clearSession() }

    /**
     * Carga los datos del usuario autenticado desde Room.
     */
    private fun loadUser() {
        viewModelScope.launch {
            val email = sessionManager.getUserEmail() ?: return@launch
            val user  = userRepository.getUserByEmail(email) ?: return@launch
            userName.value  = user.name
            userEmail.value = user.email
            loadProfilePhoto(email)
        }
    }

    /**
     * Hashea una contraseña con SHA-256.
     *
     * @param password Contraseña en texto plano.
     * @return Hash hexadecimal de 64 caracteres.
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}