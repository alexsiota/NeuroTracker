package com.neurotracker.viewmodel

/**
 * Modelo de datos para mostrar un test reciente en la interfaz de usuario.
 *
 * Este data class transforma los datos de la entidad [TestResultEntity]
 * en un formato optimizado para las pantallas de historial y main.
 *
 * @param id Identificador único del test en la base de datos (necesario para operaciones de borrado).
 * @param dateTime Fecha y hora del test formateada para mostrar al usuario.
 * @param testName Nombre legible del tipo de test realizado.
 * @param precisionPct Porcentaje de precisión del test (0-100).
 * @param eegAlpha Potencia de la banda Alpha (simulada) en el momento del test.
 * @param eegBeta Potencia de la banda Beta (simulada) en el momento del test.
 * @param eegGamma Potencia de la banda Gamma (simulada) en el momento del test.
 * @param eegTheta Potencia de la banda Theta (simulada) en el momento del test.
 */
data class RecentTestUi(
    val id: Int = 0,         // id de la BD — necesario para borrar
    val dateTime:     String,
    val testName:     String,
    val precisionPct: Int,
    val eegAlpha:     Float = 0f,
    val eegBeta:      Float = 0f,
    val eegGamma:     Float = 0f,
    val eegTheta:     Float = 0f
) {
    /**
     * Indica si el test dispone de datos EEG registrados.
     *
     * @return true si al menos una banda EEG tiene valor distinto de cero.
     */
    val hasEegData: Boolean
        get() = eegAlpha != 0f || eegBeta != 0f || eegGamma != 0f || eegTheta != 0f

    /**
     * Devuelve la banda EEG dominante en el momento del test.
     *
     * @return Nombre de la banda con mayor potencia ("Alpha", "Beta", "Gamma", "Theta")
     *         o "—" si no hay datos EEG.
     */
    val dominantBand: String
        get() {
            if (!hasEegData) return "—"
            return listOf(
                "Alpha" to eegAlpha,
                "Beta"  to eegBeta,
                "Gamma" to eegGamma,
                "Theta" to eegTheta
            ).maxByOrNull { it.second }?.first ?: "—"
        }
}