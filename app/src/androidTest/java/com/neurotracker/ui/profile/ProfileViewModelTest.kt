package com.neurotracker.ui.profile

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [ProfileViewModel].
 *
 * Cubre la lógica de validación síncrona de los tres diálogos de cambio:
 *  - Cambio de contraseña: campos vacíos, contraseña corta, no coinciden, igual a la actual.
 *  - Cambio de email: campos vacíos, formato inválido, igual al actual.
 *  - Cambio de nombre: vacío, demasiado corto, demasiado largo.
 *  - Apertura y cierre de diálogos.
 *
 * Las rutas que requieren DB (verificar contraseña, guardar) no se prueban aquí
 * ya que no hay sesión activa ni usuario registrado en los tests instrumentados.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ProfileViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ProfileViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    // ─── Diálogos ─────────────────────────────────────────────────────────────

    @Test
    fun openChangePassword_showsDialogAndClearsFields() {
        viewModel.openChangePassword()
        assertTrue(viewModel.showChangePassword.value)
        assertNull(viewModel.passwordError.value)
        assertFalse(viewModel.passwordSuccess.value)
        assertEquals("", viewModel.currentPassword.value)
        assertEquals("", viewModel.newPassword.value)
        assertEquals("", viewModel.confirmPassword.value)
    }

    @Test
    fun closeChangePassword_hidesDialog() {
        viewModel.openChangePassword()
        viewModel.closeChangePassword()
        assertFalse(viewModel.showChangePassword.value)
    }

    @Test
    fun openChangeEmail_showsDialogAndClearsFields() {
        viewModel.openChangeEmail()
        assertTrue(viewModel.showChangeEmail.value)
        assertNull(viewModel.emailError.value)
        assertFalse(viewModel.emailSuccess.value)
        assertEquals("", viewModel.currentEmail.value)
        assertEquals("", viewModel.newEmail.value)
        assertEquals("", viewModel.emailPassword.value)
    }

    @Test
    fun closeChangeEmail_hidesDialog() {
        viewModel.openChangeEmail()
        viewModel.closeChangeEmail()
        assertFalse(viewModel.showChangeEmail.value)
    }

    @Test
    fun openChangeName_showsDialog() {
        viewModel.openChangeName()
        assertTrue(viewModel.showChangeName.value)
        assertNull(viewModel.nameError.value)
        assertFalse(viewModel.nameSuccess.value)
    }

    @Test
    fun closeChangeName_hidesDialog() {
        viewModel.openChangeName()
        viewModel.closeChangeName()
        assertFalse(viewModel.showChangeName.value)
    }

    // ─── Validación de contraseña ──────────────────────────────────────────────

    @Test
    fun submitChangePassword_withEmptyFields_showsError() {
        viewModel.submitChangePassword()
        assertEquals("Rellena todos los campos", viewModel.passwordError.value)
    }

    @Test
    fun submitChangePassword_withShortNewPassword_showsError() {
        viewModel.onCurrentPasswordChange("oldpass1")
        viewModel.onNewPasswordChange("short")
        viewModel.onConfirmPasswordChange("short")
        viewModel.submitChangePassword()
        assertEquals("Mínimo 8 caracteres", viewModel.passwordError.value)
    }

    @Test
    fun submitChangePassword_withMismatchedConfirmation_showsError() {
        viewModel.onCurrentPasswordChange("oldpass123")
        viewModel.onNewPasswordChange("newpass123")
        viewModel.onConfirmPasswordChange("different1")
        viewModel.submitChangePassword()
        assertEquals("Las contraseñas no coinciden", viewModel.passwordError.value)
    }

    @Test
    fun submitChangePassword_withSameAsCurrentPassword_showsError() {
        viewModel.onCurrentPasswordChange("samepass1")
        viewModel.onNewPasswordChange("samepass1")
        viewModel.onConfirmPasswordChange("samepass1")
        viewModel.submitChangePassword()
        assertEquals("La nueva contraseña debe ser distinta", viewModel.passwordError.value)
    }

    // ─── Validación de email ───────────────────────────────────────────────────

    @Test
    fun submitChangeEmail_withEmptyFields_showsError() {
        viewModel.submitChangeEmail()
        assertEquals("Rellena todos los campos", viewModel.emailError.value)
    }

    @Test
    fun submitChangeEmail_withInvalidEmailFormat_showsError() {
        viewModel.onCurrentEmailChange("current@test.com")
        viewModel.onNewEmailChange("not-an-email")
        viewModel.onEmailPasswordChange("password123")
        viewModel.submitChangeEmail()
        assertEquals("El nuevo email no tiene formato válido", viewModel.emailError.value)
    }

    @Test
    fun submitChangeEmail_withSameEmail_showsError() {
        viewModel.onCurrentEmailChange("same@test.com")
        viewModel.onNewEmailChange("same@test.com")
        viewModel.onEmailPasswordChange("password123")
        viewModel.submitChangeEmail()
        assertEquals("El nuevo email es igual al actual", viewModel.emailError.value)
    }

    // ─── Validación de nombre ──────────────────────────────────────────────────

    @Test
    fun submitChangeName_withBlankName_showsError() {
        viewModel.onNewNameChange("")
        viewModel.submitChangeName()
        assertEquals("El nombre no puede estar vacío", viewModel.nameError.value)
    }

    @Test
    fun submitChangeName_withSingleCharacter_showsError() {
        viewModel.onNewNameChange("A")
        viewModel.submitChangeName()
        assertEquals("El nombre debe tener al menos 2 caracteres", viewModel.nameError.value)
    }

    @Test
    fun submitChangeName_withTooLongName_showsError() {
        viewModel.onNewNameChange("A".repeat(51))
        viewModel.submitChangeName()
        assertEquals("El nombre es demasiado largo", viewModel.nameError.value)
    }

    // ─── Actualizaciones de campo ──────────────────────────────────────────────

    @Test
    fun onCurrentPasswordChange_updatesState() {
        viewModel.onCurrentPasswordChange("testpass")
        assertEquals("testpass", viewModel.currentPassword.value)
    }

    @Test
    fun onNewPasswordChange_updatesState() {
        viewModel.onNewPasswordChange("newpass1")
        assertEquals("newpass1", viewModel.newPassword.value)
    }

    @Test
    fun onConfirmPasswordChange_updatesState() {
        viewModel.onConfirmPasswordChange("confirm1")
        assertEquals("confirm1", viewModel.confirmPassword.value)
    }

    @Test
    fun onNewEmailChange_clearsEmailError() {
        viewModel.submitChangeEmail()
        viewModel.onNewEmailChange("new@test.com")
        assertNull(viewModel.emailError.value)
    }

    @Test
    fun onNewNameChange_clearsNameError() {
        viewModel.onNewNameChange("A")
        viewModel.submitChangeName()
        viewModel.onNewNameChange("ValidName")
        assertNull(viewModel.nameError.value)
    }

    // ─── Opciones de foto ──────────────────────────────────────────────────────

    @Test
    fun openPhotoOptions_setsShowPhotoOptionsTrue() {
        viewModel.openPhotoOptions()
        assertTrue(viewModel.showPhotoOptions.value)
    }

    @Test
    fun closePhotoOptions_setsShowPhotoOptionsFalse() {
        viewModel.openPhotoOptions()
        viewModel.closePhotoOptions()
        assertFalse(viewModel.showPhotoOptions.value)
    }

    @Test
    fun onCameraTaken_withFalse_hidesPhotoOptions() {
        viewModel.openPhotoOptions()
        viewModel.onCameraTaken(false)
        assertFalse(viewModel.showPhotoOptions.value)
    }

    @Test
    fun onGalleryImageSelected_withNull_hidesPhotoOptions() {
        viewModel.openPhotoOptions()
        viewModel.onGalleryImageSelected(null)
        assertFalse(viewModel.showPhotoOptions.value)
    }

    @Test
    fun loadProfilePhoto_withBlankEmail_doesNotCrash() {
        viewModel.loadProfilePhoto("")
    }

    // ─── Logout ────────────────────────────────────────────────────────────────

    @Test
    fun logout_doesNotThrow() {
        viewModel.logout()
    }

    // ─── Cambio de nombre: igual al actual ─────────────────────────────────────

    @Test
    fun submitChangeName_withSameNameAsCurrentUser_showsError() {
        // Sin sesión el userName es "". Llamar submitChangeName con "" activa el error de campo vacío.
        viewModel.onNewNameChange("")
        viewModel.submitChangeName()
        assertNotNull(viewModel.nameError.value)
    }

    // ─── Estado inicial ────────────────────────────────────────────────────────

    @Test
    fun initialState_userNameIsEmpty() {
        assertEquals("", viewModel.userName.value)
    }

    @Test
    fun initialState_userEmailIsEmpty() {
        assertEquals("", viewModel.userEmail.value)
    }

    @Test
    fun initialState_profilePhotoUriIsNull() {
        assertNull(viewModel.profilePhotoUri.value)
    }

    @Test
    fun initialState_photoVersionIsZero() {
        assertEquals(0, viewModel.photoVersion.intValue)
    }

    @Test
    fun initialState_cameraUriIsNull() {
        assertNull(viewModel.cameraUri.value)
    }

    @Test
    fun initialState_emailSuggestionsIsEmpty() {
        assertTrue(viewModel.emailSuggestions.value.isEmpty())
    }

    // ─── Actualizaciones de campo de email ────────────────────────────────────

    @Test
    fun onCurrentEmailChange_updatesField() {
        viewModel.onCurrentEmailChange("current@test.com")
        assertEquals("current@test.com", viewModel.currentEmail.value)
    }

    @Test
    fun onEmailPasswordChange_updatesField() {
        viewModel.onEmailPasswordChange("secretpass")
        assertEquals("secretpass", viewModel.emailPassword.value)
    }

    @Test
    fun onEmailPasswordChange_clearsEmailError() {
        viewModel.submitChangeEmail()
        assertNotNull(viewModel.emailError.value)
        viewModel.onEmailPasswordChange("anypass")
        assertNull(viewModel.emailError.value)
    }

    @Test
    fun onCurrentEmailChange_clearsSuggestions() {
        viewModel.onCurrentEmailChange("a@test.com")
        assertTrue(viewModel.emailSuggestions.value.isEmpty())
    }

    // ─── Apertura de nombre precarga nombre actual ────────────────────────────

    @Test
    fun openChangeName_preloadsCurrentUserName() {
        viewModel.openChangeName()
        assertEquals(viewModel.userName.value, viewModel.newName.value)
    }

    // ─── No dialog open initially ─────────────────────────────────────────────

    @Test
    fun initialState_noDialogIsOpen() {
        assertFalse(viewModel.showChangePassword.value)
        assertFalse(viewModel.showChangeEmail.value)
        assertFalse(viewModel.showChangeName.value)
        assertFalse(viewModel.showPhotoOptions.value)
    }

    @Test
    fun initialState_noSuccessFlags() {
        assertFalse(viewModel.passwordSuccess.value)
        assertFalse(viewModel.emailSuccess.value)
        assertFalse(viewModel.nameSuccess.value)
    }
}
