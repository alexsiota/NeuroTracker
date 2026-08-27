package com.neurotracker.ui.welcome

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.neurotracker.R

/**
 * Pantalla de bienvenida de NeuroTracker.
 *
 * Es la primera pantalla que ve el usuario en visitas posteriores al onboarding
 * (el onboarding se muestra solo la primera vez). Presenta la propuesta de valor
 * de la app y ofrece acceso a inicio de sesión y registro.
 *
 * Estructura visual:
 *  - Fondo degradado vertical oscuro (azul marino profundo).
 *  - Logo con animación de entrada tipo spring bounce.
 *  - Título y subtítulo de la app.
 *  - 4 chips de características principales organizados en 2 filas.
 *  - Botones de acción inferiores (Iniciar sesión / Crear cuenta).
 *  - Pie de página con atribución del proyecto.
 *
 * Usabilidad:
 *  - Botones de altura 52dp (supera el mínimo de 48dp de Material Design).
 *  - Padding de barras del sistema mediante [WindowInsets.systemBars] para
 *    evitar solapamiento con la barra de estado y la de navegación.
 *  - Jerarquía visual clara: logo → título → características → acciones.
 *
 * Accesibilidad:
 *  - Logo con contentDescription "Logo NeuroTracker".
 *  - Cada [FeatureChip] tiene contentDescription descriptivo para TalkBack.
 *  - Botones con texto descriptivo (no solo iconos).
 *  - Contraste de texto: blanco sobre fondo muy oscuro (≥7:1, WCAG AAA).
 *
 * Daltonismo:
 *  - Esta pantalla no usa color para transmitir información funcional.
 *  - El color púrpura (#6C63FF) del botón es decorativo; la función la
 *    indica el texto "Iniciar sesión".
 *  - Los chips de características usan icono + texto, nunca solo color.
 *
 * Navegación:
 *  - "Iniciar sesión" → [LoginScreen] (ruta "login").
 *  - "Crear cuenta"   → [RegisterScreen] (ruta "register").
 *
 * @param navController Controlador de navegación de Jetpack Compose.
 */
@Composable
fun WelcomeScreen(navController: NavController) {

    // Animación de entrada del logo: escala de 0.6 a 1.0 con efecto rebote
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue    = 1f,
            animationSpec  = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D1A),
                        Color(0xFF1A1A2E),
                        Color(0xFF0D0D1A)
                    )
                )
            )
            .padding(WindowInsets.systemBars.asPaddingValues())
            .padding(horizontal = 24.dp)
    ) {

        // Contenido central
        Column(
            modifier            = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Logo con animación de escala
            Image(
                painter            = painterResource(id = R.drawable.ic_neurotracker_logo),
                contentDescription = "Logo NeuroTracker",
                modifier           = Modifier
                    .size(100.dp)
                    .scale(scale.value)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text       = "NeuroTracker",
                fontSize   = 32.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text      = "Mide, entrena y mejora\ntu rendimiento cognitivo",
                style     = MaterialTheme.typography.bodyLarge,
                color     = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Fila 1 de características
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureChip(
                    icon     = Icons.Default.Psychology,
                    label    = "Tests cognitivos",
                    modifier = Modifier.weight(1f)
                )
                FeatureChip(
                    icon     = Icons.AutoMirrored.Filled.ShowChart,
                    label    = "Estadísticas reales",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Fila 2 de características
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureChip(
                    icon     = Icons.Default.Waves,
                    label    = "Simulación EEG",
                    modifier = Modifier.weight(1f)
                )
                FeatureChip(
                    icon     = Icons.Default.Security,
                    label    = "Datos seguros",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Botones inferiores
        Column(
            modifier            = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Botón principal: Iniciar sesión
            Button(
                onClick  = { navController.navigate("login") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = "Iniciar sesión en NeuroTracker" },
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
            ) {
                Text("Iniciar sesión", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            // Botón secundario: Crear cuenta
            OutlinedButton(
                onClick  = { navController.navigate("register") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = "Crear una cuenta nueva en NeuroTracker" },
                shape    = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text       = "Crear cuenta",
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 16.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            // Pie de página: atribución del proyecto
            Text(
                text  = "Proyecto educativo · TFC Álex Siota · 2026",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * Chip de característica de la pantalla de bienvenida.
 *
 * Componente privado que muestra un icono y una etiqueta de texto
 * representando una funcionalidad clave de la app.
 *
 * Accesibilidad:
 *  - El composable completo tiene contentDescription para TalkBack,
 *    describiendo la característica sin depender del icono.
 *
 * Daltonismo:
 *  - La información se transmite mediante icono + texto, nunca solo color.
 *  - El fondo semitransparente es decorativo.
 *
 * @param icon Icono representativo de la característica.
 * @param label Texto descriptivo de la característica.
 * @param modifier Modifier para controlar el tamaño y distribución en la fila.
 */
@Composable
private fun FeatureChip(
    icon:     ImageVector,
    label:    String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.semantics { contentDescription = label },
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f))
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,  // La semántica está en el Card padre
                tint               = Color(0xFF6C63FF),
                modifier           = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text  = label,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}