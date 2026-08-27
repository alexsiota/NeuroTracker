package com.neurotracker.data.repository

import com.neurotracker.data.dao.UserDao
import com.neurotracker.data.entities.UserEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest

/**
 * Tests unitarios puros del [UserRepository].
 *
 * Usa MockK para simular el [UserDao] y verificar que el repositorio
 * delega correctamente las operaciones, aplica el hash SHA-256 a las
 * contraseñas antes de compararlas y devuelve los resultados esperados.
 *
 * Se ejecuta en la JVM sin necesidad de contexto Android ni base de datos real.
 */
class UserRepositoryTest {

    private lateinit var userDao: UserDao
    private lateinit var repository: UserRepository

    private val testPassword = "password123"
    private val testHash     = hashPassword(testPassword)

    private val testUser = UserEntity(
        id           = 1,
        name         = "Álex Siota",
        birthDate    = 946684800000L,
        email        = "alex@neurotracker.com",
        passwordHash = testHash
    )

    @Before
    fun setUp() {
        userDao    = mockk()
        repository = UserRepository(userDao)
    }

    /**
     * Verifica que [UserRepository.insertUser] delega la llamada al [UserDao].
     */
    @Test
    fun insertUser_delegatesToDao() = runTest {
        coEvery { userDao.insertUser(testUser) } returns Unit
        repository.insertUser(testUser)
        coVerify(exactly = 1) { userDao.insertUser(testUser) }
    }

    /**
     * Verifica que [UserRepository.getUserByEmail] devuelve el usuario
     * cuando el DAO lo encuentra.
     */
    @Test
    fun getUserByEmail_whenUserExists_returnsUser() = runTest {
        coEvery { userDao.getUserByEmail(testUser.email) } returns testUser
        val result = repository.getUserByEmail(testUser.email)
        assertNotNull(result)
        assertEquals(testUser.email, result!!.email)
    }

    /**
     * Verifica que [UserRepository.getUserByEmail] devuelve null
     * cuando el DAO no encuentra ningún usuario con ese email.
     */
    @Test
    fun getUserByEmail_whenUserDoesNotExist_returnsNull() = runTest {
        coEvery { userDao.getUserByEmail(any()) } returns null
        val result = repository.getUserByEmail("noexiste@neurotracker.com")
        assertNull(result)
    }

    /**
     * Verifica que [UserRepository.userExists] devuelve true
     * cuando el DAO encuentra un usuario con ese email.
     */
    @Test
    fun userExists_whenUserFound_returnsTrue() = runTest {
        coEvery { userDao.getUserByEmail(testUser.email) } returns testUser
        assertTrue(repository.userExists(testUser.email))
    }

    /**
     * Verifica que [UserRepository.userExists] devuelve false
     * cuando el DAO no encuentra ningún usuario con ese email.
     */
    @Test
    fun userExists_whenUserNotFound_returnsFalse() = runTest {
        coEvery { userDao.getUserByEmail(any()) } returns null
        assertFalse(repository.userExists("fantasma@neurotracker.com"))
    }

    /**
     * Verifica que [UserRepository.login] devuelve [LoginResult.Success]
     * cuando el email existe y la contraseña en texto plano coincide
     * con el hash almacenado tras aplicar SHA-256.
     */
    @Test
    fun login_withCorrectCredentials_returnsSuccess() = runTest {
        coEvery { userDao.getUserByEmail(testUser.email) } returns testUser
        val result = repository.login(testUser.email, testPassword)
        assertTrue(result is LoginResult.Success)
        assertEquals(testUser, (result as LoginResult.Success).user)
    }

    /**
     * Verifica que [UserRepository.login] devuelve [LoginResult.Error]
     * cuando la contraseña introducida es incorrecta.
     */
    @Test
    fun login_withWrongPassword_returnsError() = runTest {
        coEvery { userDao.getUserByEmail(testUser.email) } returns testUser
        val result = repository.login(testUser.email, "contraseñaIncorrecta")
        assertTrue(result is LoginResult.Error)
    }

    /**
     * Verifica que [UserRepository.login] devuelve [LoginResult.Error]
     * cuando el email no está registrado en la base de datos.
     */
    @Test
    fun login_withUnknownEmail_returnsError() = runTest {
        coEvery { userDao.getUserByEmail(any()) } returns null
        val result = repository.login("noexiste@neurotracker.com", testPassword)
        assertTrue(result is LoginResult.Error)
        assertEquals("Usuario no encontrado", (result as LoginResult.Error).message)
    }

    /**
     * Verifica que el mensaje de error de login con email incorrecto
     * es exactamente "Usuario no encontrado".
     */
    @Test
    fun login_withUnknownEmail_errorMessageIsCorrect() = runTest {
        coEvery { userDao.getUserByEmail(any()) } returns null
        val result = repository.login("unknown@test.com", "cualquier") as LoginResult.Error
        assertEquals("Usuario no encontrado", result.message)
    }

    /**
     * Verifica que el mensaje de error de login con contraseña incorrecta
     * es exactamente "Contraseña incorrecta".
     */
    @Test
    fun login_withWrongPassword_errorMessageIsCorrect() = runTest {
        coEvery { userDao.getUserByEmail(testUser.email) } returns testUser
        val result = repository.login(testUser.email, "mal") as LoginResult.Error
        assertEquals("Contraseña incorrecta", result.message)
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
