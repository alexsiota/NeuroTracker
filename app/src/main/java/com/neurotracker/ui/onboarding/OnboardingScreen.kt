package com.neurotracker.ui.onboarding

/**
 * Pantalla de onboarding de primer uso.
 *
 * Se muestra una única vez al instalar la app, antes de llegar a la pantalla
 * de bienvenida. Presenta las funcionalidades principales de NeuroTracker
 * en 4 diapositivas animadas con navegación por botones.
 *
 * Diapositivas:
 *  1. Bienvenida y propósito de la app.
 *  2. Descripción de los tests cognitivos.
 *  3. Funcionalidad de simulación EEG.
 *  4. Llamada a la acción (crear cuenta / iniciar sesión).
 *
 * Gestión de primer uso: [OnboardingManager] persiste en SharedPreferences
 * si el onboarding ya fue visto, evitando que se muestre de nuevo.
 *
 * Usabilidad:
 *  - windowInsetsPadding(navigationBars) evita solapamiento con la barra del sistema.
 *  - Botón "Omitir" visible en todos los slides excepto el último.
 */

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.neurotracker.ui.navigation.Routes

/**
 * Modelo de datos que representa una página del onboarding.
 *
 * @property emoji Icono visual representativo de la página.
 * @property title Título principal de la diapositiva.
 * @property description Descripción detallada del contenido.
 * @property highlight Texto destacado que resume la idea principal.
 */
data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String,
    val highlight: String
)

/**
 * Composable principal de la pantalla de onboarding.
 *
 * Responsabilidades:
 * - Mostrar las diferentes páginas del onboarding.
 * - Gestionar la navegación entre páginas.
 * - Controlar la navegación a otras pantallas (welcome, login, register).
 * - Renderizar animaciones de transición entre diapositivas.
 *
 * @param navController Controlador de navegación para cambiar de pantalla.
 */
@Composable
fun OnboardingScreen(navController: NavController) {

    val pages = listOf(
        OnboardingPage(
            emoji       = "🧠",
            title       = "Bienvenido a NeuroTracker",
            description = "Tu entrenador cognitivo personal. Mide, entrena y mejora tus capacidades mentales con tests científicamente validados.",
            highlight   = "Conoce cómo funciona tu mente."
        ),
        OnboardingPage(
            emoji       = "🎯",
            title       = "Tests cognitivos",
            description = "Organizados en 6 bloques: Atención, Memoria, Velocidad, Funciones Ejecutivas, Coordinación y Tests Compuestos.",
            highlight   = "Cada test mide una capacidad diferente."
        ),
        OnboardingPage(
            emoji       = "📡",
            title       = "Simulación EEG",
            description = "Visualiza tu actividad cerebral simulada durante los tests. Ondas Alpha, Beta, Gamma y Theta reflejan tu estado cognitivo.",
            highlight   = "Preparado para hardware EEG real."
        ),
        OnboardingPage(
            emoji       = "🚀",
            title       = "¡Empieza a entrenar!",
            description = "Crea tu cuenta, realiza tu primer test y observa cómo evolucionan tus capacidades cognitivas a lo largo del tiempo.",
            highlight   = "Tu cerebro mejora con la práctica."
        )
    )

    var currentPage by remember { mutableStateOf(0) }
    val isLastPage  = currentPage == pages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.navigationBars) // ← respeta barra inferior del teléfono
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 32.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ---- Skip ----
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!isLastPage) {
                    TextButton(
                        onClick = {
                            navController.navigate(Routes.WELCOME) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    ) {
                        Text("Omitir", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(Modifier.height(40.dp))
                }
            }

            // ---- Contenido animado ----
            /**
             * Contenido dinámico del onboarding.
             *
             * - Cambia según la página actual.
             * - Aplica animaciones de entrada y salida.
             */
            AnimatedContent(
                targetState    = currentPage,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                label          = "onboarding_page"
            ) { page ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(pages[page].emoji, fontSize = 58.sp)
                    }

                    Spacer(Modifier.height(32.dp))

                    Text(
                        pages[page].title,
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center,
                        color      = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        pages[page].description,
                        style      = MaterialTheme.typography.bodyLarge,
                        textAlign  = TextAlign.Center,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )

                    Spacer(Modifier.height(20.dp))

                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            pages[page].highlight,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.primary,
                            textAlign  = TextAlign.Center,
                            modifier   = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // ---- Dots + Botones ----
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pages.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentPage) 24.dp else 8.dp, 8.dp)
                                .background(
                                    if (index == currentPage) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                if (isLastPage) {
                    Button(
                        onClick  = { navController.navigate(Routes.REGISTER) { popUpTo(Routes.ONBOARDING) { inclusive = true } } },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(16.dp)
                    ) { Text("Crear cuenta", style = MaterialTheme.typography.titleMedium) }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick  = { navController.navigate(Routes.LOGIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } } },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(16.dp)
                    ) { Text("Ya tengo cuenta", style = MaterialTheme.typography.titleMedium) }
                } else {
                    /**
                     * Botón para avanzar a la siguiente página del onboarding.
                     */
                    Button(
                        onClick  = { currentPage++ },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(16.dp)
                    ) { Text("Siguiente", style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
    }
}