package com.neurotracker.ui.tests.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Colores semánticos para las pantallas de test cognitivo.
 *
 * Diseñados con accesibilidad y daltonismo en mente:
 *  - Nunca usados como único indicador de estado.
 *  - Combinados siempre con emoji, texto o forma diferenciadora.
 *  - Contraste mínimo 4.5:1 sobre fondos claros y oscuros (WCAG AA).
 *
 * Estos colores son constantes fijas que no cambian con el tema claro/oscuro,
 * para garantizar siempre la legibilidad semántica en contextos de test.
 */
object TestColors {

    // ── Feedback de respuesta ───────────────────────────────────────────────

    /** Verde oscuro: acierto / respuesta correcta. */
    val hit = Color(0xFF2E7D32)

    /** Rojo oscuro: error / respuesta incorrecta. */
    val error = Color(0xFFC62828)

    /** Naranja oscuro: advertencia / rendimiento mejorable. Seguro para tritanopia. */
    val warning = Color(0xFFE65100)

    /** Azul oscuro: información neutral / banda EEG beta. */
    val info = Color(0xFF1565C0)

    /** Gris: omisión / elemento neutro. */
    val neutral = Color(0xFF757575)

    // ── Colores de estímulos GO/NOGO ────────────────────────────────────────

    /**
     * Verde oscuro para estímulo GO (pulsar).
     * Siempre combinado con forma circular y etiqueta "● PULSA".
     */
    val goGreen = Color(0xFF2E7D32)

    /**
     * Azul para estímulo NOGO (no pulsar) en tests de atención sostenida.
     * Combinado con forma cuadrada y etiqueta "■ NO PULSES".
     * Se usa azul en lugar de rojo para diferenciar del NOGO de inhibición.
     */
    val nogoBlue = Color(0xFF1565C0)

    /**
     * Rojo oscuro para estímulo NOGO (no pulsar) en tests de inhibición.
     * Combinado con forma cuadrada y etiqueta "■ NO PULSES".
     */
    val nogoRed = Color(0xFFC62828)

    // ── Botones de respuesta SÍ/NO ──────────────────────────────────────────

    /**
     * Verde muy oscuro para botón de afirmación (✓ SÍ).
     * El prefijo ✓ garantiza la lectura sin dependencia del color.
     */
    val btnYes = Color(0xFF1B5E20)

    /**
     * Rojo muy oscuro para botón de negación (✗ NO).
     * El prefijo ✗ garantiza la lectura sin dependencia del color.
     */
    val btnNo = Color(0xFF7F0000)

    // ── Colores EEG ─────────────────────────────────────────────────────────

    /** Verde oscuro: onda Alpha (8–13 Hz). Diferenciado de BetaColor por luminosidad. */
    val alphaEeg = Color(0xFF2E7D32)

    /** Azul oscuro: onda Beta (13–30 Hz). */
    val betaEeg = Color(0xFF1565C0)

    /** Naranja oscuro: onda Gamma (30–100 Hz). Evita amarillo para tritanopia. */
    val gammaEeg = Color(0xFFE65100)

    /** Morado oscuro: onda Theta (4–8 Hz). */
    val thetaEeg = Color(0xFF6A1B9A)
}

/**
 * Devuelve el color de fondo de una tarjeta de test usando el tema actual.
 *
 * @return Color [MaterialTheme.colorScheme.surfaceVariant] del tema activo.
 */
@Composable
fun testCardColor() = MaterialTheme.colorScheme.surfaceVariant

/**
 * Devuelve el color de texto principal dentro de un test usando el tema actual.
 *
 * @return Color [MaterialTheme.colorScheme.onSurfaceVariant] del tema activo.
 */
@Composable
fun testOnCard() = MaterialTheme.colorScheme.onSurfaceVariant
