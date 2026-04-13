package com.neurotracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.neurotracker.data.entities.UserEntity

/**
 * Data Access Object (DAO) para la entidad [UserEntity].
 *
 * Define todas las operaciones de base de datos relacionadas con los usuarios
 * registrados en la aplicación.
 *
 * ─── Patrón Repository ───────────────────────────────────────────────────────
 *
 * Este DAO no se usa directamente desde ViewModels ni la UI.
 * Se accede exclusivamente a través de [UserRepository], que encapsula la
 * lógica de negocio (hashing de contraseñas, validación de credenciales, etc.)
 * y desacopla Room de la capa de presentación.
 *
 * ─── Seguridad ───────────────────────────────────────────────────────────────
 *
 * Las contraseñas NUNCA se almacenan en texto plano en la tabla `users`.
 * El campo `passwordHash` de [UserEntity] contiene el hash SHA-256 de la
 * contraseña. El hashing se realiza en [RegisterViewModel], [ProfileViewModel]
 * y [ForgotPasswordViewModel] antes de llamar a [insertUser] o [updateUser].
 *
 * ─── Tabla asociada ──────────────────────────────────────────────────────────
 *
 * Tabla: `users`
 * Entidad: [UserEntity]
 * Clave primaria: `email` (único e inmutable en teoría, pero actualizable
 *                 desde [ProfileViewModel] si el usuario cambia su email)
 *
 * ─── Unicidad del email ───────────────────────────────────────────────────────
 *
 * El email es la clave de identificación del usuario en toda la app.
 * [userExists] se usa en [RegisterViewModel] y [ForgotPasswordViewModel]
 * para garantizar que no se dupliquen cuentas y que se valide el email
 * antes de iniciar flujos críticos.
 */
@Dao
interface UserDao {

    /**
     * Inserta un nuevo usuario en la tabla `users`.
     *
     * Se llama una única vez por cuenta, desde [RegisterViewModel.register]
     * tras validar que el email no existe ya en la base de datos.
     * La contraseña ya viene hasheada con SHA-256 en [UserEntity.passwordHash].
     *
     * La operación es suspendida: debe llamarse desde una corrutina.
     *
     * @param user Entidad del usuario a insertar con todos sus campos:
     *             name, email, birthDate y passwordHash.
     */
    @Insert
    suspend fun insertUser(user: UserEntity)

    /**
     * Actualiza los datos de un usuario existente en la tabla `users`.
     *
     * Room identifica el registro a actualizar por la clave primaria del objeto
     * [user] recibido. Se llama desde:
     *  - [ProfileViewModel.submitChangePassword]: actualiza solo [UserEntity.passwordHash].
     *  - [ProfileViewModel.submitChangeEmail]   : actualiza [UserEntity.email].
     *  - [ForgotPasswordViewModel.saveNewPassword]: actualiza [UserEntity.passwordHash]
     *    tras la verificación del código de recuperación.
     *
     * Patrón típico de uso:
     * ```kotlin
     * val user = userDao.getUserByEmail(email) ?: return
     * userDao.updateUser(user.copy(passwordHash = newHash))
     * ```
     *
     * @param user Entidad con los datos actualizados. Room usa la clave primaria
     *             para localizar el registro a modificar.
     */
    @Update
    suspend fun updateUser(user: UserEntity)

    /**
     * Obtiene un usuario por su email.
     *
     * Consulta principal para recuperar datos de la cuenta activa.
     * Se usa en:
     *  - [LoginViewModel.login]            : verificar credenciales.
     *  - [ProfileViewModel.loadUser]       : cargar nombre y email en la UI.
     *  - [ProfileViewModel.submitChangePassword]: verificar contraseña actual.
     *  - [ForgotPasswordViewModel.saveNewPassword]: recuperar la entidad antes
     *    de actualizarla con la nueva contraseña.
     *
     * `LIMIT 1` garantiza eficiencia incluso si por algún error de migración
     * existieran duplicados (no debería ocurrir si se usa [userExists] antes).
     *
     * @param email Email del usuario a buscar.
     * @return [UserEntity] si existe una cuenta con ese email, null si no existe.
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    /**
     * Comprueba si existe un usuario con el email especificado.
     *
     * Más eficiente que [getUserByEmail] cuando solo se necesita saber si
     * la cuenta existe, sin cargar todos sus datos. Usa `COUNT(*)` que Room
     * convierte en un Boolean automáticamente (0 = false, ≥1 = true).
     *
     * Se usa en:
     *  - [RegisterViewModel.register]: evitar duplicar cuentas.
     *  - [ForgotPasswordViewModel.sendCode]: validar que el email pertenece
     *    a una cuenta antes de generar el código de verificación.
     *  - [ProfileViewModel.submitChangeEmail]: comprobar que el nuevo email
     *    no esté ya registrado por otra cuenta.
     *
     * @param email Email a verificar en la tabla `users`.
     * @return true si existe al menos un usuario con ese email, false si no existe.
     */
    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    suspend fun userExists(email: String): Boolean
}