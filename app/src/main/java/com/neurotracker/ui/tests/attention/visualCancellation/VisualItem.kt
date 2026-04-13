package com.neurotracker.ui.tests.attention.visualCancellation

/**
 * Representa un elemento del test de cancelación visual.
 *
 * Cada elemento puede ser un objetivo o un distractor y puede ser marcado
 * como pulsado por el usuario durante el test.
 *
 * @property id Identificador único del elemento.
 * @property isTarget Indica si el elemento es un objetivo que debe ser seleccionado.
 * @property tapped Indica si el usuario ya ha interactuado con este elemento.
 */
data class VisualItem(
    val id: Int,
    val isTarget: Boolean,
    var tapped: Boolean = false
)