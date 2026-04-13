package com.neurotracker.data.repository

import com.neurotracker.data.dao.UserDao
import com.neurotracker.data.entities.UserEntity
import java.security.MessageDigest

/**
 * Repositorio de usuarios.
 *
 * Actúa como intermediario entre:
 * - La base de datos (DAO)
 * - La lógica de la aplicación (ViewModel)
 *
 * Centraliza toda la lógica de acceso a datos relacionada con usuarios.
 * El ViewModel nunca debe acceder directamente al DAO.
 *
 * Aquí NO hay UI, solo lógica de datos.
 *
 * @param userDao Data Access Object para operaciones con la tabla de usuarios.
 */
class UserRepository(
    private val userDao: UserDao
) {

    /**
     * Inserta un nuevo usuario en la base de datos.
     * Se asume que la validación previa ya se ha hecho en el ViewModel.
     *
     * @param user Entidad [UserEntity] con los datos del usuario.
     */
    suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    /**
     * Obtiene un usuario por su email.
     *
     * @param email Email del usuario a buscar.
     * @return El usuario si existe, null en caso contrario.
     */
    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }

    /**
     * Comprueba si existe un usuario con ese email.
     *
     * @param email Email a verificar.
     * @return true si el usuario existe, false en caso contrario.
     */
    suspend fun userExists(email: String): Boolean {
        return userDao.getUserByEmail(email) != null
    }

    /**
     * Login real del usuario.
     *
     * 1. Busca el usuario por email.
     * 2. Si no existe, devuelve error.
     * 3. Hashea la contraseña introducida.
     * 4. Compara con el hash almacenado.
     *
     * @param email Email del usuario.
     * @param password Contraseña en texto plano.
     * @return [LoginResult] indicando éxito con el usuario o error con mensaje.
     */
    suspend fun login(email: String, password: String): LoginResult {
        val user = userDao.getUserByEmail(email)
            ?: return LoginResult.Error("Usuario no encontrado")

        val hashedPassword = hashPassword(password)

        return if (user.passwordHash == hashedPassword) {
            LoginResult.Success(user)
        } else {
            LoginResult.Error("Contraseña incorrecta")
        }
    }

    /**
     * Cifrado SHA-256 de la contraseña.
     * Se usa tanto en registro como en login.
     *
     * @param password Contraseña en texto plano.
     * @return Hash SHA-256 de la contraseña como string hexadecimal.
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray())

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }
}

/**
 * Resultado del login.
 */
sealed class LoginResult {
    /** Login exitoso con el usuario autenticado. */
    data class Success(val user: UserEntity) : LoginResult()
    /** Login fallido con mensaje de error. */
    data class Error(val message: String) : LoginResult()
}