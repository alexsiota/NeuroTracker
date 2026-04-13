package com.neurotracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Sistema tipográfico de NeuroTracker.
 *
 * Define los estilos de texto usados por [MaterialTheme.typography] en toda la app.
 * Se pasa a [NeuroTrackerTheme] como parámetro de [MaterialTheme].
 *
 * ─── Criterios de accesibilidad aplicados ────────────────────────────────────
 *
 * WCAG 1.4.4 — "Resize text":
 *  El texto debe poder escalarse hasta el 200% sin pérdida de contenido.
 *  Al usar `sp` en lugar de `dp`, el sistema respeta el tamaño de fuente
 *  configurado por el usuario en los ajustes de accesibilidad del dispositivo.
 *
 * Tamaños mínimos recomendados:
 *  - Cuerpo de texto (bodyLarge): 16sp — legible sin esfuerzo.
 *  - Etiquetas pequeñas (labelSmall): 11sp — solo para información secundaria.
 *  - Altura de línea (lineHeight): al menos 1.5× el tamaño de fuente.
 *
 * ─── Uso en la app ───────────────────────────────────────────────────────────
 *
 * Los estilos no declarados aquí usan los valores por defecto de Material3,
 * que ya cumplen las guías de accesibilidad de Google.
 *
 * Referencia de estilos usados en la app:
 *  - [Typography.displaySmall]  → resultados grandes (span máximo, score global).
 *  - [Typography.headlineMedium]→ títulos de estado FINISHED.
 *  - [Typography.titleLarge]    → cabeceras de bloque en ChooseTestScreen.
 *  - [Typography.titleMedium]   → nombres de test, reglas en TaskSwitching.
 *  - [Typography.bodyLarge]     → descripción de instrucciones en IdleScreen.
 *  - [Typography.bodyMedium]    → texto general, ResultRow.
 *  - [Typography.bodySmall]     → metadatos secundarios, fechas, etiquetas EEG.
 *  - [Typography.labelSmall]    → badges, contadores pequeños.
 */
val Typography = Typography(

    /**
     * Estilo de cuerpo principal.
     *
     * Usado en instrucciones de tests ([IdleScreen.description]),
     * mensajes de feedback ([FeedbackText]) y contenido principal.
     *
     * 16sp garantiza legibilidad sin esfuerzo para la mayoría de usuarios,
     * incluyendo personas con visión reducida sin necesidad de zoom.
     */
    bodyLarge = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,   // ratio 1.5 — recomendado WCAG
        letterSpacing = 0.5.sp
    )

    /*
     * Los demás estilos usan los valores por defecto de Material3.
     * Se pueden personalizar aquí si se añade una fuente personalizada.
     *
     * Ejemplos para referencia:
     *
     * titleLarge = TextStyle(
     *     fontFamily    = FontFamily.Default,
     *     fontWeight    = FontWeight.Normal,
     *     fontSize      = 22.sp,
     *     lineHeight    = 28.sp,
     *     letterSpacing = 0.sp
     * ),
     * labelSmall = TextStyle(
     *     fontFamily    = FontFamily.Default,
     *     fontWeight    = FontWeight.Medium,
     *     fontSize      = 11.sp,
     *     lineHeight    = 16.sp,
     *     letterSpacing = 0.5.sp
     * )
     */
)
