package com.neurotracker.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
/**
 * Modelo persistente del usuario para Room.
 *
 * @param id Identificador autogenerado por Room.
 * @param name Nombre visible del usuario.
 * @param birthDate Fecha de nacimiento almacenada como timestamp.
 * @param email Correo electrónico único del usuario.
 * @param passwordHash Contraseña almacenada como hash SHA-256.
 * @param createdAt Fecha de creación de la cuenta en milisegundos.
 */
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val birthDate: Long,
    val email: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis()
)
