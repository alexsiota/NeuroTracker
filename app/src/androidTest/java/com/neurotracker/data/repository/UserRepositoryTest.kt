package com.neurotracker.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.entities.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

/**
 * Tests instrumentados del [UserRepository].
 *
 * Usa base de datos Room en memoria para garantizar aislamiento entre tests
 * y ejercitar todos los caminos del repositorio:
 *  - insertUser / getUserByEmail.
 *  - userExists con usuario registrado y no registrado.
 *  - login: éxito, contraseña incorrecta, usuario no encontrado.
 *  - Coherencia entre la lógica de hashing del repositorio y SHA-256.
 */
@RunWith(AndroidJUnit4::class)
class UserRepositoryTest {

    private lateinit var database: NeuroTrackerDatabase
    private lateinit var repository: UserRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NeuroTrackerDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = UserRepository(database.userDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sha256(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun makeUser(
        email: String = "test@example.com",
        password: String = "password123",
        name: String = "Test User"
    ) = UserEntity(
        name         = name,
        birthDate    = 946684800000L,
        email        = email,
        passwordHash = sha256(password)
    )

    // ─── insertUser / getUserByEmail ──────────────────────────────────────────

    @Test
    fun insertUser_andGetByEmail_returnsCorrectUser() = runTest {
        repository.insertUser(makeUser())
        val retrieved = repository.getUserByEmail("test@example.com")
        assertNotNull(retrieved)
        assertEquals("test@example.com", retrieved!!.email)
        assertEquals("Test User", retrieved.name)
    }

    @Test
    fun getUserByEmail_withNonExistentEmail_returnsNull() = runTest {
        assertNull(repository.getUserByEmail("ghost@example.com"))
    }

    // ─── userExists ────────────────────────────────────────────────────────────

    @Test
    fun userExists_withRegisteredUser_returnsTrue() = runTest {
        repository.insertUser(makeUser())
        assertTrue(repository.userExists("test@example.com"))
    }

    @Test
    fun userExists_withNonExistentUser_returnsFalse() = runTest {
        assertFalse(repository.userExists("ghost@example.com"))
    }

    // ─── login: success ────────────────────────────────────────────────────────

    @Test
    fun login_withCorrectCredentials_returnsSuccess() = runTest {
        repository.insertUser(makeUser(password = "correctpass"))
        val result = repository.login("test@example.com", "correctpass")
        assertTrue(result is LoginResult.Success)
    }

    @Test
    fun login_withCorrectCredentials_returnsUserWithCorrectEmail() = runTest {
        repository.insertUser(makeUser(password = "correctpass"))
        val result = repository.login("test@example.com", "correctpass") as LoginResult.Success
        assertEquals("test@example.com", result.user.email)
    }

    @Test
    fun login_withCorrectCredentials_returnsUserWithCorrectName() = runTest {
        repository.insertUser(makeUser(name = "Álex Siota", password = "securepass"))
        val result = repository.login("test@example.com", "securepass") as LoginResult.Success
        assertEquals("Álex Siota", result.user.name)
    }

    // ─── login: wrong password ─────────────────────────────────────────────────

    @Test
    fun login_withWrongPassword_returnsError() = runTest {
        repository.insertUser(makeUser(password = "correctpass"))
        val result = repository.login("test@example.com", "wrongpass")
        assertTrue(result is LoginResult.Error)
    }

    @Test
    fun login_withWrongPassword_returnsIncorrectPasswordMessage() = runTest {
        repository.insertUser(makeUser(password = "correctpass"))
        val result = repository.login("test@example.com", "wrongpass") as LoginResult.Error
        assertEquals("Contraseña incorrecta", result.message)
    }

    @Test
    fun login_withEmptyPassword_returnsIncorrectPasswordError() = runTest {
        repository.insertUser(makeUser(password = "realpassword"))
        val result = repository.login("test@example.com", "")
        assertTrue(result is LoginResult.Error)
        assertEquals("Contraseña incorrecta", (result as LoginResult.Error).message)
    }

    // ─── login: user not found ─────────────────────────────────────────────────

    @Test
    fun login_withNonExistentEmail_returnsError() = runTest {
        val result = repository.login("ghost@example.com", "anypass")
        assertTrue(result is LoginResult.Error)
    }

    @Test
    fun login_withNonExistentEmail_returnsNotFoundMessage() = runTest {
        val result = repository.login("ghost@example.com", "anypass") as LoginResult.Error
        assertEquals("Usuario no encontrado", result.message)
    }

    // ─── multiple users ────────────────────────────────────────────────────────

    @Test
    fun login_withMultipleUsers_authenticatesEachCorrectly() = runTest {
        repository.insertUser(makeUser(email = "user1@example.com", password = "pass1"))
        repository.insertUser(makeUser(email = "user2@example.com", password = "pass2"))

        assertTrue(repository.login("user1@example.com", "pass1") is LoginResult.Success)
        assertTrue(repository.login("user2@example.com", "pass2") is LoginResult.Success)
        assertTrue(repository.login("user1@example.com", "pass2") is LoginResult.Error)
        assertTrue(repository.login("user2@example.com", "pass1") is LoginResult.Error)
    }

    @Test
    fun userExists_afterInsertingMultipleUsers_eachReturnsTrue() = runTest {
        repository.insertUser(makeUser(email = "a@example.com"))
        repository.insertUser(makeUser(email = "b@example.com"))
        assertTrue(repository.userExists("a@example.com"))
        assertTrue(repository.userExists("b@example.com"))
        assertFalse(repository.userExists("c@example.com"))
    }
}
