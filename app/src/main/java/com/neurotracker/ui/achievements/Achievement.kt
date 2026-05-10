package com.neurotracker.ui.achievements

import androidx.compose.ui.graphics.Color

/**
 * Modelo de un logro individual.
 *
 * @param id            Identificador único del logro.
 * @param emoji         Emoji representativo (indicador primario, independiente del color).
 * @param title         Título corto del logro.
 * @param description   Descripción de qué hay que hacer para desbloquearlo.
 * @param category      Categoría del logro para agrupar en la UI.
 * @param unlocked      true si el usuario ha completado el requisito.
 * @param unlockedColor Color de fondo cuando está desbloqueado. Debe ser oscuro (≥4.5:1 con blanco).
 */
data class Achievement(
    val id:            String,
    val emoji:         String,
    val title:         String,
    val description:   String,
    val category:      String,
    val unlocked:      Boolean,
    val unlockedColor: Color = Color(0xFF2E7D32)
)
