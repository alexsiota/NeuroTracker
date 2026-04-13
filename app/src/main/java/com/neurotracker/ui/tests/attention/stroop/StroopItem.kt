package com.neurotracker.ui.tests.attention.stroop

import androidx.compose.ui.graphics.Color

/**
 * Representa un estímulo del test de Stroop.
 *
 * Contiene la información necesaria para mostrar un elemento al usuario
 * y determinar si la respuesta esperada es correcta o no.
 *
 * @property text Texto mostrado (nombre del color).
 * @property color Color visual con el que se renderiza el texto.
 * @property isCorrect Indica si el estímulo es congruente (texto coincide con el color).
 */
data class StroopItem(
    val text: String,
    val color: Color,
    val isCorrect: Boolean
)