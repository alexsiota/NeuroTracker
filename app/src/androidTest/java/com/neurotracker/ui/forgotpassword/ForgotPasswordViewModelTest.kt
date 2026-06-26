package com.neurotracker.ui.forgotpassword

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
 * Tests instrumentados del [ForgotPasswordViewModel].
 *
 * Verifica el flujo completo de recuperación de contraseña en 3 pasos:
 *  1. Pantalla 1 — Validación del email ([sendCode]).
 *  2. Pantalla 2 — Verificación del código de 6 caracteres ([verifyCode]).
 *  3. Pantalla 3 — Cambio de contraseña con validaciones ([saveNewPassword]).
 *
 * También verifica:
 *  - [resendCode] genera un nuevo código de 6 caracteres y limpia los campos.
 *  - [onCodeChange] convierte a mayúsculas y limita a 6 caracteres.
 *  - Mensajes de error exactos para cada validación.
 *
 * Se ejecuta en dispositivo o emulador porque [ForgotPasswordViewModel]
 * extiende [AndroidViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ForgotPasswordViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ForgotPasswordViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ForgotPasswordViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica el estado inicial: todos los campos vacíos, sin errores
     * y todos los flags de flujo a false.
     */
    @Test
    fun initialState_allFieldsEmptyAndNoErrors() {
        assertTrue(viewModel.email.value.isBlank())
        assertNull(viewModel.errorStep1.value)
        assertFalse(viewModel.codeSent.value)
        assertFalse(viewModel.codeVerified.value)
        assertFalse(viewModel.passwordChanged.value)
        assertTrue(viewModel.enteredCode.value.isEmpty())
        assertTrue(viewModel.newPassword.value.isEmpty())
        assertTrue(viewModel.confirmPassword.value.isEmpty())
    }

    /**
     * Verifica que [onEmailChange] actualiza el email y limpia el error del paso 1.
     */
    @Test
    fun onEmailChange_updatesEmailAndClearsError() {
        viewModel.onEmailChange("test@neurotracker.com")
        assertEquals("test@neurotracker.com", viewModel.email.value)
        assertNull(viewModel.errorStep1.value)
    }

    /**
     * Verifica que [sendCode] con email vacío establece el error "Introduce tu email"
     * y no navega a la siguiente pantalla.
     */
    @Test
    fun sendCode_withBlankEmail_setsIntroduceEmailError() = runTest {
        var navigated = false
        viewModel.sendCode { navigated = true }
        advanceUntilIdle()
        assertFalse(navigated)
        assertEquals("Introduce tu email", viewModel.errorStep1.value)
    }

    /**
     * Verifica que [sendCode] con formato de email inválido establece
     * el error "Email no válido" y no navega.
     */
    @Test
    fun sendCode_withInvalidEmailFormat_setsEmailNotValidError() = runTest {
        viewModel.onEmailChange("noesuncorreo")
        var navigated = false
        viewModel.sendCode { navigated = true }
        advanceUntilIdle()
        assertFalse(navigated)
        assertEquals("Email no válido", viewModel.errorStep1.value)
    }

    /**
     * Verifica que [sendCode] con un email que no existe en Room establece
     * el error "No existe ninguna cuenta con ese email".
     */
    @Test
    fun sendCode_withNonExistentEmail_setsAccountNotFoundError() = runTest {
        viewModel.onEmailChange("noexiste@neurotracker.com")
        var navigated = false
        viewModel.sendCode { navigated = true }
        advanceUntilIdle()
        assertFalse(navigated)
        assertEquals("No existe ninguna cuenta con ese email", viewModel.errorStep1.value)
    }

    /**
     * Verifica que [onCodeChange] convierte la entrada a mayúsculas automáticamente.
     */
    @Test
    fun onCodeChange_convertsToUppercase() {
        viewModel.onCodeChange("abcdef")
        assertEquals("ABCDEF", viewModel.enteredCode.value)
    }

    /**
     * Verifica que [onCodeChange] no acepta más de 6 caracteres.
     */
    @Test
    fun onCodeChange_doesNotExceedSixCharacters() {
        viewModel.onCodeChange("ABCDEFGH")
        assertTrue(viewModel.enteredCode.value.length <= 6)
    }

    /**
     * Verifica que [verifyCode] con código de menos de 6 caracteres establece
     * el error "El código tiene 6 caracteres".
     */
    @Test
    fun verifyCode_withShortCode_setsLengthError() = runTest {
        viewModel.onCodeChange("ABC")
        var navigated = false
        viewModel.verifyCode { navigated = true }
        assertFalse(navigated)
        assertEquals("El código tiene 6 caracteres", viewModel.errorStep2.value)
    }

    /**
     * Verifica que [verifyCode] con un código de 6 caracteres incorrecto
     * establece el error "Código incorrecto".
     */
    @Test
    fun verifyCode_withWrongCode_setsIncorrectCodeError() = runTest {
        viewModel.onCodeChange("XXXXXX")
        var navigated = false
        viewModel.verifyCode { navigated = true }
        assertFalse(navigated)
        assertEquals("Código incorrecto", viewModel.errorStep2.value)
    }

    /**
     * Verifica que [resendCode] genera un nuevo código de exactamente 6 caracteres,
     * limpia el código introducido y restablece el estado de verificación.
     */
    @Test
    fun resendCode_generatesSixCharCodeAndClearsState() {
        viewModel.resendCode()
        assertEquals(6, viewModel.demoCode.value.length)
        assertTrue(viewModel.enteredCode.value.isEmpty())
        assertNull(viewModel.errorStep2.value)
        assertFalse(viewModel.codeVerified.value)
    }

    /**
     * Verifica que el código generado por [resendCode] solo contiene
     * letras mayúsculas (A-Z) y dígitos (0-9).
     */
    @Test
    fun resendCode_codeContainsOnlyValidCharacters() {
        viewModel.resendCode()
        val code = viewModel.demoCode.value
        assertTrue(
            "El código '$code' contiene caracteres inválidos",
            code.all { it.isUpperCase() || it.isDigit() }
        )
    }

    /**
     * Verifica que [saveNewPassword] con campos vacíos establece
     * el error "Rellena todos los campos".
     */
    @Test
    fun saveNewPassword_withBlankFields_setsError() = runTest {
        var navigated = false
        viewModel.saveNewPassword { navigated = true }
        assertFalse(navigated)
        assertEquals("Rellena todos los campos", viewModel.errorStep3.value)
    }

    /**
     * Verifica que [saveNewPassword] con contraseña de menos de 8 caracteres
     * establece el error "Mínimo 8 caracteres".
     */
    @Test
    fun saveNewPassword_withShortPassword_setsMinLengthError() = runTest {
        viewModel.onNewPasswordChange("corta")
        viewModel.onConfirmPasswordChange("corta")
        var navigated = false
        viewModel.saveNewPassword { navigated = true }
        assertFalse(navigated)
        assertEquals("Mínimo 8 caracteres", viewModel.errorStep3.value)
    }

    /**
     * Verifica que [saveNewPassword] cuando las contraseñas no coinciden
     * establece el error "Las contraseñas no coinciden".
     */
    @Test
    fun saveNewPassword_withMismatchedPasswords_setsMismatchError() = runTest {
        viewModel.onNewPasswordChange("password123")
        viewModel.onConfirmPasswordChange("diferente123")
        var navigated = false
        viewModel.saveNewPassword { navigated = true }
        assertFalse(navigated)
        assertEquals("Las contraseñas no coinciden", viewModel.errorStep3.value)
    }

    @Test
    fun saveNewPassword_withTooLongPassword_setsMaxLengthError() = runTest {
        val long = "a".repeat(33)
        viewModel.onNewPasswordChange(long)
        viewModel.onConfirmPasswordChange(long)
        var navigated = false
        viewModel.saveNewPassword { navigated = true }
        assertFalse(navigated)
        assertEquals("Máximo 32 caracteres", viewModel.errorStep3.value)
    }

    @Test
    fun saveNewPassword_withValidPasswordButNoVerifiedEmail_setsSessionExpiredError() = runTest {
        viewModel.onNewPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        var navigated = false
        viewModel.saveNewPassword { navigated = true }
        assertFalse(navigated)
        assertEquals("Sesión expirada. Vuelve a introducir tu email.", viewModel.errorStep3.value)
    }

    @Test
    fun verifyCode_withCorrectCode_setsCodeVerifiedAndCallsOnSuccess() = runTest {
        viewModel.resendCode()
        val code = viewModel.demoCode.value
        viewModel.onCodeChange(code)
        var navigated = false
        viewModel.verifyCode { navigated = true }
        assertTrue(viewModel.codeVerified.value)
        assertTrue(navigated)
        assertNull(viewModel.errorStep2.value)
    }

    @Test
    fun onNewPasswordChange_clearsErrorStep3() = runTest {
        viewModel.saveNewPassword { }
        assertNotNull(viewModel.errorStep3.value)
        viewModel.onNewPasswordChange("newvalue")
        assertNull(viewModel.errorStep3.value)
    }

    @Test
    fun onConfirmPasswordChange_clearsErrorStep3() = runTest {
        viewModel.saveNewPassword { }
        assertNotNull(viewModel.errorStep3.value)
        viewModel.onConfirmPasswordChange("newvalue")
        assertNull(viewModel.errorStep3.value)
    }

    @Test
    fun onNewPasswordChange_updatesField() {
        viewModel.onNewPasswordChange("mypassword")
        assertEquals("mypassword", viewModel.newPassword.value)
    }

    @Test
    fun onConfirmPasswordChange_updatesField() {
        viewModel.onConfirmPasswordChange("myconfirm")
        assertEquals("myconfirm", viewModel.confirmPassword.value)
    }
}
