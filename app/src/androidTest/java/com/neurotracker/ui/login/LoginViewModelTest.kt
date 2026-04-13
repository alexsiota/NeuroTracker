package com.neurotracker.ui.login

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
 * Tests instrumentados del [LoginViewModel].
 *
 * Verifica la lógica de validación del formulario de login y la gestión
 * del estado de la UI:
 *  - Estado inicial limpio.
 *  - Actualización de campos de entrada.
 *  - Validación de campos vacíos.
 *  - Error ante credenciales inexistentes en Room.
 *  - [isLoading] vuelve a false tras completar el intento de login.
 *
 * Se ejecuta en dispositivo o emulador porque [LoginViewModel]
 * extiende [AndroidViewModel] y accede a Room y [SessionManager].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LoginViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica que el estado inicial tiene todos los campos vacíos,
     * sin errores, sin carga y con "Recuérdame" desactivado.
     */
    @Test
    fun initialState_allFieldsEmptyAndNoError() {
        assertTrue(viewModel.email.value.isBlank())
        assertTrue(viewModel.password.value.isBlank())
        assertFalse(viewModel.rememberMe.value)
        assertNull(viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    /**
     * Verifica que [onEmailChange] actualiza el email correctamente.
     */
    @Test
    fun onEmailChange_updatesEmail() {
        viewModel.onEmailChange("alex@neurotracker.com")
        assertEquals("alex@neurotracker.com", viewModel.email.value)
    }

    /**
     * Verifica que [onPasswordChange] actualiza la contraseña correctamente.
     */
    @Test
    fun onPasswordChange_updatesPassword() {
        viewModel.onPasswordChange("password123")
        assertEquals("password123", viewModel.password.value)
    }

    /**
     * Verifica que [onRememberMeChange] alterna correctamente el estado
     * del checkbox "Recuérdame".
     */
    @Test
    fun onRememberMeChange_togglesBehaviorCorrectly() {
        viewModel.onRememberMeChange(true)
        assertTrue(viewModel.rememberMe.value)
        viewModel.onRememberMeChange(false)
        assertFalse(viewModel.rememberMe.value)
    }

    /**
     * Verifica que intentar login con ambos campos vacíos establece el error
     * "Completa todos los campos" y no navega.
     */
    @Test
    fun login_withEmptyFields_setsCompleteAllFieldsError() = runTest {
        var navigated = false
        viewModel.login { navigated = true }
        advanceUntilIdle()
        assertFalse(navigated)
        assertEquals("Completa todos los campos", viewModel.errorMessage.value)
    }

    /**
     * Verifica que intentar login con email correcto pero contraseña vacía
     * establece el error "Completa todos los campos".
     */
    @Test
    fun login_withEmailButNoPassword_setsError() = runTest {
        viewModel.onEmailChange("alex@neurotracker.com")
        var navigated = false
        viewModel.login { navigated = true }
        advanceUntilIdle()
        assertFalse(navigated)
        assertEquals("Completa todos los campos", viewModel.errorMessage.value)
    }

    /**
     * Verifica que intentar login con credenciales que no existen en Room
     * establece un mensaje de error y no navega.
     */
    @Test
    fun login_withNonExistentUser_setsErrorMessage() = runTest {
        viewModel.onEmailChange("noexiste@neurotracker.com")
        viewModel.onPasswordChange("password123")
        var navigated = false
        viewModel.login { navigated = true }
        advanceUntilIdle()
        assertFalse(navigated)
        assertNotNull(viewModel.errorMessage.value)
        assertEquals("Usuario no encontrado", viewModel.errorMessage.value)
    }

    /**
     * Verifica que [isLoading] vuelve a false después de completar el intento
     * de login, independientemente del resultado (éxito o error).
     */
    @Test
    fun login_afterCompletion_isLoadingIsFalse() = runTest {
        viewModel.onEmailChange("test@neurotracker.com")
        viewModel.onPasswordChange("password123")
        viewModel.login { }
        advanceUntilIdle()
        assertFalse(viewModel.isLoading.value)
    }
}
