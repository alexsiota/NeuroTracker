package com.neurotracker.ui.register

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
 * Tests instrumentados del [RegisterViewModel].
 *
 * Verifica la lógica de validación del formulario de registro con los mensajes
 * de error exactos definidos en el ViewModel:
 *  - Campos vacíos.
 *  - Formato de email inválido.
 *  - Contraseña demasiado corta (< 8 caracteres).
 *  - Contraseña demasiado larga (> 32 caracteres).
 *  - Contraseñas que no coinciden.
 *  - Selección de fecha de nacimiento.
 *
 * Se ejecuta en dispositivo o emulador porque [RegisterViewModel]
 * extiende [AndroidViewModel] y accede a Room.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RegisterViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RegisterViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * Verifica que el estado inicial tiene todos los campos vacíos,
     * sin errores, sin carga y sin modal de éxito.
     */
    @Test
    fun initialState_allFieldsEmptyAndNoError() {
        assertTrue(viewModel.name.value.isBlank())
        assertTrue(viewModel.email.value.isBlank())
        assertTrue(viewModel.password.value.isBlank())
        assertNull(viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.showSuccessModal.value)
    }

    /**
     * Verifica que los métodos de actualización de campos funcionan correctamente
     * para todos los campos del formulario.
     */
    @Test
    fun fieldUpdates_workCorrectlyForAllFields() {
        viewModel.onNameChange("Álex")
        viewModel.onEmailChange("alex@neurotracker.com")
        viewModel.onPasswordChange("password123")
        viewModel.onRepeatPasswordChange("password123")
        assertEquals("Álex", viewModel.name.value)
        assertEquals("alex@neurotracker.com", viewModel.email.value)
        assertEquals("password123", viewModel.password.value)
        assertEquals("password123", viewModel.repeatPassword.value)
    }

    /**
     * Verifica que intentar registrarse con campos vacíos establece el error
     * "No se puede registrar con campos vacíos".
     */
    @Test
    fun register_withEmptyFields_setsEmptyFieldsError() = runTest {
        var navigated = false
        viewModel.register { navigated = true }
        assertFalse(navigated)
        assertEquals("No se puede registrar con campos vacíos", viewModel.errorMessage.value)
    }

    /**
     * Verifica que un email con formato inválido establece el error
     * "El email no tiene un formato válido".
     */
    @Test
    fun register_withInvalidEmail_setsFormatError() = runTest {
        viewModel.onNameChange("Álex")
        viewModel.onEmailChange("noesuncorreo")
        viewModel.onPasswordChange("password123")
        viewModel.onRepeatPasswordChange("password123")
        viewModel.onBirthDateSelected(946684800000L)
        var navigated = false
        viewModel.register { navigated = true }
        assertFalse(navigated)
        assertEquals("El email no tiene un formato válido", viewModel.errorMessage.value)
    }

    /**
     * Verifica que una contraseña de menos de 8 caracteres establece el error
     * "La contraseña debe tener al menos 8 caracteres".
     */
    @Test
    fun register_withShortPassword_setsMinLengthError() = runTest {
        viewModel.onNameChange("Álex")
        viewModel.onEmailChange("alex@neurotracker.com")
        viewModel.onPasswordChange("corta")
        viewModel.onRepeatPasswordChange("corta")
        viewModel.onBirthDateSelected(946684800000L)
        var navigated = false
        viewModel.register { navigated = true }
        assertFalse(navigated)
        assertEquals("La contraseña debe tener al menos 8 caracteres", viewModel.errorMessage.value)
    }

    /**
     * Verifica que una contraseña de más de 32 caracteres establece el error
     * "La contraseña es demasiado larga, máximo 32 caracteres".
     */
    @Test
    fun register_withTooLongPassword_setsMaxLengthError() = runTest {
        val longPassword = "a".repeat(33)
        viewModel.onNameChange("Álex")
        viewModel.onEmailChange("alex@neurotracker.com")
        viewModel.onPasswordChange(longPassword)
        viewModel.onRepeatPasswordChange(longPassword)
        viewModel.onBirthDateSelected(946684800000L)
        var navigated = false
        viewModel.register { navigated = true }
        assertFalse(navigated)
        assertEquals(
            "La contraseña es demasiado larga, máximo 32 caracteres",
            viewModel.errorMessage.value
        )
    }

    /**
     * Verifica que contraseñas que no coinciden establecen el error
     * "Las contraseñas no coinciden".
     */
    @Test
    fun register_withMismatchedPasswords_setsMismatchError() = runTest {
        viewModel.onNameChange("Álex")
        viewModel.onEmailChange("alex@neurotracker.com")
        viewModel.onPasswordChange("password123")
        viewModel.onRepeatPasswordChange("diferente123")
        viewModel.onBirthDateSelected(946684800000L)
        var navigated = false
        viewModel.register { navigated = true }
        assertFalse(navigated)
        assertEquals("Las contraseñas no coinciden", viewModel.errorMessage.value)
    }

    /**
     * Verifica que [onBirthDateSelected] actualiza el timestamp,
     * genera texto de fecha formateado y limpia el error previo.
     */
    @Test
    fun onBirthDateSelected_updatesTimestampFormatsDateAndClearsError() {
        viewModel.onBirthDateSelected(946684800000L)
        assertNotNull(viewModel.birthDateMillis.value)
        assertTrue(viewModel.birthDateText.value.isNotBlank())
        assertNull(viewModel.errorMessage.value)
    }
}
