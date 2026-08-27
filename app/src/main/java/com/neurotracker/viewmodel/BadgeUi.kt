package com.neurotracker.viewmodel

/**
 * Modelo UI para representar un logro/insignia en la pantalla principal.
 *
 * @param emoji       Emoji visual del logro
 * @param title       Nombre corto del logro
 * @param description Descripción de cómo desbloquearlo
 * @param unlocked    true si el usuario ya lo ha conseguido
 * @param progressText Texto de progreso actual, ej: "3 / 10 tests"
 * @param progressFraction Fracción de progreso entre 0f y 1f para la barra
 */
data class BadgeUi(
    val emoji: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    val progressText: String = "",
    val progressFraction: Float = 0f
)
