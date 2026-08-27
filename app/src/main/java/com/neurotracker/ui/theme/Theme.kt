package com.neurotracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Esquema de color para modo oscuro.
 *
 * Usado cuando el sistema opera en modo oscuro y el color dinámico
 * no está disponible (Android < 12). Usa los colores "80" de [Color.kt],
 * diseñados para alto contraste sobre fondos oscuros.
 */
private val DarkColorScheme = darkColorScheme(
    primary   = Purple80,
    secondary = PurpleGrey80,
    tertiary  = Pink80
)

/**
 * Esquema de color para modo claro.
 *
 * Usado cuando el sistema opera en modo claro y el color dinámico
 * no está disponible (Android < 12). Usa los colores "40" de [Color.kt],
 * diseñados para alto contraste sobre fondos claros.
 */
private val LightColorScheme = lightColorScheme(
    primary   = Purple40,
    secondary = PurpleGrey40,
    tertiary  = Pink40
)

/**
 * Tema principal de la aplicación NeuroTracker.
 *
 * Envuelve todo el árbol de composición de la app y aplica:
 *  - El esquema de colores adecuado (dinámico, oscuro o claro).
 *  - La tipografía definida en [Typography].
 *  - Las formas y elevaciones por defecto de Material3.
 *
 * Se aplica en [MainActivity] como composable raíz, envolviendo
 * el [NavHost] y todas las pantallas.
 *
 * ─── Estrategia de color ─────────────────────────────────────────────────────
 *
 * 1. Color dinámico (Android 12+, [dynamicColor] = true):
 *    Material You genera automáticamente un esquema de color personalizado
 *    a partir del fondo de pantalla del usuario. Esto garantiza contraste
 *    óptimo con el entorno visual del dispositivo.
 *
 * 2. Esquema estático oscuro ([DarkColorScheme]):
 *    Fallback para Android < 12 en modo oscuro. Púrpura claro sobre fondos
 *    oscuros, contraste WCAG AA garantizado.
 *
 * 3. Esquema estático claro ([LightColorScheme]):
 *    Fallback para Android < 12 en modo claro. Púrpura oscuro sobre fondos
 *    claros, contraste WCAG AA garantizado.
 *
 * ─── Accesibilidad ───────────────────────────────────────────────────────────
 *
 * Modo oscuro (WCAG 1.4.3 — Contrast):
 *  - El modo oscuro reduce la fatiga visual en entornos con poca luz.
 *  - Es especialmente beneficioso para usuarios con fotofobia o migraña.
 *  - NeuroTracker respeta la preferencia del sistema mediante [isSystemInDarkTheme()].
 *
 * Preferencia del usuario:
 *  - La app permite cambiar manualmente entre modo claro y oscuro desde
 *    [ProfileScreen] mediante [ThemeManager], que persiste la preferencia
 *    en SharedPreferences y la aplica en [MainActivity].
 *
 * Color dinámico y daltonismo:
 *  - El color dinámico de Material You puede generar colores que el usuario
 *    ya ha validado como accesibles para sí mismo (son sus propios colores).
 *  - Los colores de feedback semántico (acierto/error/aviso) se definen
 *    independientemente del tema en [TestColors] y [AccessibilityUtils],
 *    garantizando consistencia sin depender del esquema dinámico.
 *
 * ─── Parámetros ──────────────────────────────────────────────────────────────
 *
 * @param darkTheme    Si true, aplica el esquema oscuro. Por defecto sigue
 *                     la preferencia del sistema ([isSystemInDarkTheme()]).
 *                     [MainActivity] puede sobreescribir este valor desde
 *                     [ThemeManager] para aplicar la preferencia del usuario.
 * @param dynamicColor Si true (por defecto), usa Material You en Android 12+.
 *                     En versiones anteriores se ignora y se usa el esquema estático.
 * @param content      Árbol de composición que recibirá el tema aplicado.
 */
@Composable
fun NeuroTrackerTheme(
    darkTheme:    Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content:      @Composable () -> Unit
) {
    val colorScheme = when {
        // Material You: esquema dinámico generado por el dispositivo (Android 12+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Fallback estático: esquema oscuro o claro según preferencia
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
