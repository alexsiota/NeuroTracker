package com.neurotracker.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa un resultado de test cognitivo en la base de datos Room.
 *
 * @param id Identificador único autogenerado.
 * @param userEmail Email del usuario (relación lógica con [UserEntity]).
 * @param testType Identificador del test (ej: "reaction_simple", "stroop", etc.).
 * @param score Valor principal del test (ej: tiempo de reacción en ms, span máximo, etc.).
 * @param timestamp Marca de tiempo en milisegundos desde el epoch.
 * @param precisionPct Precisión real del test en porcentaje (0-100).
 *                     Calculada en cada ViewModel con precisionPercent().
 * @param eegAlpha Potencia de la banda Alpha (8–13 Hz) — relajación, atención pasiva.
 * @param eegBeta Potencia de la banda Beta (13–30 Hz) — concentración, alerta.
 * @param eegGamma Potencia de la banda Gamma (30–100 Hz) — procesamiento cognitivo.
 * @param eegTheta Potencia de la banda Theta (4–8 Hz) — memoria, creatividad.
 */
@Entity(tableName = "test_results")
data class TestResultEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userEmail: String,

    val testType: String,

    val score: Long,

    val timestamp: Long = System.currentTimeMillis(),

    val precisionPct: Int = 0,

    /*
     * Datos EEG registrados al finalizar el test.
     * Valor por defecto 0f para compatibilidad con registros anteriores
     * que no tienen datos EEG.
     * En el futuro se pueden obtener de hardware real.
     */
    val eegAlpha: Float = 0f,
    val eegBeta:  Float = 0f,
    val eegGamma: Float = 0f,
    val eegTheta: Float = 0f,
)