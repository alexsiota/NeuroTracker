package com.neurotracker.ui.accessibility

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Utilidades de accesibilidad y soporte para daltonismo compartidas en toda la app.
 *
 * PRINCIPIO WCAG 1.4.1 — "Use of Color":
 * El color NUNCA puede ser el único medio para transmitir información.
 * Siempre se combina con icono, emoji o texto descriptivo.
 *
 * Tipos de daltonismo contemplados:
 *  - Deuteranopia  → dificultad para distinguir rojo/verde (el más común, ~6% hombres)
 *  - Protanopia    → dificultad para percibir el rojo
 *  - Tritanopia    → dificultad para distinguir azul/amarillo (poco común)
 *
 * Estrategia aplicada:
 *  - Emoji como indicador primario (universal, no depende del color)
 *  - Colores de mayor contraste que los estándar Material (≥4.5:1 sobre fondo claro/oscuro)
 *  - Verde oscuro  #2E7D32 en lugar de #4CAF50
 *  - Naranja oscuro #E65100 en lugar de amarillo (seguro para tritanopia)
 *  - Rojo oscuro   #C62828 en lugar de #E53935
 */
object AccessibilityUtils {

    /**
     * Devuelve el emoji semántico correspondiente al porcentaje de precisión.
     *
     * Para usuarios con daltonismo, el emoji actúa como indicador primario
     * e independiente del color de pantalla.
     *
     * @param pct Porcentaje de precisión (0–100).
     * @return Emoji representativo del nivel de rendimiento.
     */
    fun precisionEmoji(pct: Int): String = when {
        pct >= 80 -> "✅"   // Bueno
        pct >= 50 -> "⚠️"  // Mejorable
        else      -> "❌"   // Bajo
    }

    /**
     * Devuelve una descripción verbal de la precisión, apta para TalkBack.
     *
     * @param pct Porcentaje de precisión (0–100).
     * @return Cadena descriptiva legible por lectores de pantalla.
     */
    fun precisionLabel(pct: Int): String = when {
        pct >= 80 -> "Buena precisión: $pct%"
        pct >= 50 -> "Precisión mejorable: $pct%"
        else      -> "Precisión baja: $pct%"
    }
}

/**
 * Devuelve el color semántico asociado a un porcentaje de precisión.
 *
 * Uso: refuerzo visual. NUNCA debe ser el único indicador de estado.
 * Combinar siempre con [AccessibilityUtils.precisionEmoji].
 *
 * Colores escogidos por contraste WCAG AA (≥4.5:1 sobre blanco y sobre fondos oscuros):
 *  - ≥80% → Verde oscuro  (#2E7D32)
 *  - ≥50% → Naranja oscuro (#E65100) — evita amarillo por tritanopia
 *  - <50% → Rojo oscuro   (#C62828)
 *
 * @param pct Porcentaje de precisión (0–100).
 * @return Color semántico de refuerzo.
 */
@Composable
fun precisionColor(pct: Int): Color = when {
    pct >= 80 -> Color(0xFF2E7D32)
    pct >= 50 -> Color(0xFFE65100)
    else      -> Color(0xFFC62828)
}

/**
 * Variante de [precisionColor] que usa el sistema de colores de MaterialTheme.
 *
 * Útil cuando se prefiere coherencia total con el tema de la app
 * en lugar de colores semánticos fijos.
 *
 * @param pct Porcentaje de precisión (0–100).
 * @return Color del esquema de tema actual.
 */
@Composable
fun precisionColorMaterial(pct: Int): Color = when {
    pct >= 80 -> MaterialTheme.colorScheme.primary
    pct >= 50 -> MaterialTheme.colorScheme.secondary
    else      -> MaterialTheme.colorScheme.error
}