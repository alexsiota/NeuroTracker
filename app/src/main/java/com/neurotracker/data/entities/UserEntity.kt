package com.neurotracker.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad que representa un usuario en la base de datos Room.
 *
 * @param id Identificador único autogenerado.
 * @param name Nombre completo del usuario.
 * @param birthDate Fecha de nacimiento en timestamp (milisegundos desde epoch).
 * @param email Correo electrónico del usuario (debe ser único).
 * @param passwordHash Hash SHA-256 de la contraseña.
 * @param createdAt Marca de tiempo de creación de la cuenta.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val birthDate: Long,
    val email: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis()
)
