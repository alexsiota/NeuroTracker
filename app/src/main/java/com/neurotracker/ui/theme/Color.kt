package com.neurotracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de colores del tema NeuroTracker.
 *
 * Se usa en [Theme.kt] para construir los esquemas de color
 * claro y oscuro de MaterialTheme.
 *
 * ─── Modo oscuro ────────────────────────────────────────────────────────────
 *
 * Los colores "80" tienen alta luminosidad y están diseñados para usarse
 * sobre fondos oscuros, garantizando contraste WCAG AA (≥4.5:1).
 *
 * ─── Modo claro ─────────────────────────────────────────────────────────────
 *
 * Los colores "40" tienen baja luminosidad y están diseñados para usarse
 * sobre fondos claros, garantizando contraste WCAG AA (≥4.5:1).
 *
 * ─── Daltonismo ─────────────────────────────────────────────────────────────
 *
 * El sistema de color del tema usa púrpura/violeta como color primario,
 * que es seguro para todos los tipos de daltonismo:
 *  - Deuteranopia (rojo-verde): el púrpura no se confunde con verde ni rojo.
 *  - Protanopia (rojo): el púrpura no se confunde con verde.
 *  - Tritanopia (azul-amarillo): el púrpura tiene suficiente diferenciación
 *    de tono frente a los colores secundarios.
 *
 * Nota: los colores de feedback semántico (acierto/error/aviso) se definen
 * en [TestColors] de forma centralizada y son independientes del tema.
 */

// ── Modo oscuro (sobre fondos oscuros) ──────────────────────────────────────

/** Color primario modo oscuro. Púrpura claro, alto contraste sobre fondos oscuros. */
val Purple80 = Color(0xFFD0BCFF)

/** Color secundario modo oscuro. Lila grisáceo, contraste suave. */
val PurpleGrey80 = Color(0xFFCCC2DC)

/** Color terciario modo oscuro. Rosa suave para acentos. */
val Pink80 = Color(0xFFEFB8C8)

// ── Modo claro (sobre fondos claros) ────────────────────────────────────────

/** Color primario modo claro. Púrpura oscuro, alto contraste sobre fondos claros. */
val Purple40 = Color(0xFF6650A4)

/** Color secundario modo claro. Lila oscuro, contraste adecuado. */
val PurpleGrey40 = Color(0xFF625B71)

/** Color terciario modo claro. Rosa oscuro para acentos. */
val Pink40 = Color(0xFF7D5260)
