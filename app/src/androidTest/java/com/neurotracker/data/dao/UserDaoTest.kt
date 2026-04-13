package com.neurotracker.data.dao

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

/**
 * Tests instrumentados del [UserDao].
 *
 * Usa una base de datos Room en memoria para garantizar que cada test
 * parte de un estado limpio y no afecta al almacenamiento real del dispositivo.
 *
 * Cubre las operaciones fundamentales del ciclo de vida de un usuario:
 *  - Inserción y recuperación por email.
 *  - Verificación de existencia.
 *  - Actualización de campos (contraseña y email).
 *  - Comportamiento ante emails inexistentes.
 *
 * Se ejecuta en el dispositivo o emulador porque Room requiere el
 * contexto Android para instanciar la base de datos.
 */
@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var database: NeuroTrackerDatabase
    private lateinit var userDao: UserDao

    private val testUser = UserEntity(
        name         = "Álex Siota",
        birthDate    = 946684800000L,
        email        = "alex@neurotracker.com",
        passwordHash = "hashed_password_sha256"
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NeuroTrackerDatabase::class.java
        ).allowMainThreadQueries().build()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Verifica que un usuario insertado se puede recuperar por su email.
     */
    @Test
    fun insertUser_andRetrieveByEmail_returnsCorrectUser() = runTest {
        userDao.insertUser(testUser)
        val retrieved = userDao.getUserByEmail(testUser.email)
        assertNotNull(retrieved)
        assertEquals(testUser.name, retrieved!!.name)
        assertEquals(testUser.email, retrieved.email)
        assertEquals(testUser.passwordHash, retrieved.passwordHash)
    }

    /**
     * Verifica que [UserDao.getUserByEmail] devuelve null para un email no registrado.
     */
    @Test
    fun getUserByEmail_withNonExistentEmail_returnsNull() = runTest {
        val result = userDao.getUserByEmail("noexiste@neurotracker.com")
        assertNull(result)
    }

    /**
     * Verifica que [UserDao.userExists] devuelve true para un email registrado.
     */
    @Test
    fun userExists_withRegisteredEmail_returnsTrue() = runTest {
        userDao.insertUser(testUser)
        assertTrue(userDao.userExists(testUser.email))
    }

    /**
     * Verifica que [UserDao.userExists] devuelve false para un email no registrado.
     */
    @Test
    fun userExists_withUnregisteredEmail_returnsFalse() = runTest {
        assertFalse(userDao.userExists("fantasma@neurotracker.com"))
    }

    /**
     * Verifica que [UserDao.updateUser] actualiza correctamente el hash de la contraseña.
     */
    @Test
    fun updateUser_passwordHash_isUpdatedCorrectly() = runTest {
        userDao.insertUser(testUser)
        val inserted       = userDao.getUserByEmail(testUser.email)!!
        val newHash        = "nuevo_hash_sha256"
        userDao.updateUser(inserted.copy(passwordHash = newHash))
        val updated = userDao.getUserByEmail(testUser.email)
        assertEquals(newHash, updated!!.passwordHash)
    }

    /**
     * Verifica que [UserDao.updateUser] actualiza correctamente el email del usuario
     * y que el antiguo email ya no devuelve resultados.
     */
    @Test
    fun updateUser_email_oldEmailNoLongerExists() = runTest {
        userDao.insertUser(testUser)
        val inserted = userDao.getUserByEmail(testUser.email)!!
        val newEmail = "nuevo@neurotracker.com"
        userDao.updateUser(inserted.copy(email = newEmail))
        assertNull(userDao.getUserByEmail(testUser.email))
        assertNotNull(userDao.getUserByEmail(newEmail))
    }

    /**
     * Verifica que la base de datos puede contener múltiples usuarios distintos
     * y que cada uno se recupera de forma independiente por su email.
     */
    @Test
    fun insertMultipleUsers_eachRetrievableByEmail() = runTest {
        val second = UserEntity(
            name         = "Usuario Dos",
            birthDate    = 978307200000L,
            email        = "dos@neurotracker.com",
            passwordHash = "hash_dos"
        )
        userDao.insertUser(testUser)
        userDao.insertUser(second)
        assertNotNull(userDao.getUserByEmail(testUser.email))
        assertNotNull(userDao.getUserByEmail(second.email))
    }
}
