package com.neurotracker.ui.tests.attention.sustainedAttention

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.tests.components.*

/**
 * Pantalla del Test de Atención Sostenida.
 *
 * Evalúa la capacidad de mantener la concentración durante un período prolongado.
 * El usuario debe responder solo ante el estímulo objetivo (círculo verde)
 * e ignorar el distractor (cuadrado azul). Son 30 rondas en total.
 *
 * Flujo de estados:
 *  - [SustainedAttentionViewModel.TestState.IDLE]    → Instrucciones con leyenda visual.
 *  - [SustainedAttentionViewModel.TestState.RUNNING] → Secuencia de estímulos.
 *  - [SustainedAttentionViewModel.TestState.FINISHED]→ Resultados y acciones.
 *
 * Daltonismo (punto crítico de este test):
 *  El test GO/NOGO es especialmente sensible al daltonismo porque diferencia
 *  dos estímulos por color. Se aplica la estrategia triple:
 *   - FORMA diferente  → Círculo (GO) vs Cuadrado (NOGO)
 *   - COLOR diferente  → Verde oscuro vs Azul oscuro
 *   - ETIQUETA de texto→ "● PULSA" vs "■ NO PULSES"
 *
 * Accesibilidad:
 *  - El área de toque tiene contentDescription dinámico según el estímulo activo.
 *  - Cada estímulo tiene su propio contentDescription en el Column.
 *  - El icono de retroceso tiene contentDescription "Volver atrás".
 *
 * @param navController Controlador de navegación para el botón de retroceso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SustainedAttentionScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: SustainedAttentionViewModel = viewModel(
        factory = SustainedAttentionViewModelFactory(application)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atención Sostenida") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier         = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (viewModel.testState.value) {

                // ── IDLE: instrucciones con leyenda accesible ────────────
                SustainedAttentionViewModel.TestState.IDLE -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        // Leyenda visual — daltonismo: forma + color + texto
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier            = Modifier.semantics { contentDescription = "Círculo verde: pulsa" }
                            ) {
                                Box(modifier = Modifier.size(40.dp).background(Color(0xFF2E7D32), CircleShape))
                                Spacer(Modifier.height(6.dp))
                                Text("● PULSA", color = Color(0xFF2E7D32), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier            = Modifier.semantics { contentDescription = "Cuadrado azul: no pulses" }
                            ) {
                                Box(modifier = Modifier.size(40.dp).background(Color(0xFF1565C0), RoundedCornerShape(8.dp)))
                                Spacer(Modifier.height(6.dp))
                                Text("■ NO PULSES", color = Color(0xFF1565C0), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        IdleScreen(
                            title       = "Atención Sostenida",
                            description = "Aparecerán estímulos uno a uno.\n\n" +
                                    "🟢 CÍRCULO VERDE → Pulsa\n" +
                                    "🔵 CUADRADO AZUL → No pulses\n\n" +
                                    "La forma Y el color te indican qué hacer.\nSon 30 rondas en total.",
                            onStart     = { viewModel.startTest() }
                        )
                    }
                }

                // ── RUNNING: secuencia de estímulos ─────────────────────
                SustainedAttentionViewModel.TestState.RUNNING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { viewModel.onPress() }
                            .semantics {
                                contentDescription = when (viewModel.currentStimulus.value) {
                                    SustainedAttentionViewModel.StimulusType.TARGET    -> "¡Círculo verde! Pulsa ahora."
                                    SustainedAttentionViewModel.StimulusType.DISTRACTOR -> "Cuadrado azul. No pulses."
                                    else -> "Preparado. Espera el estímulo."
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier            = Modifier.padding(24.dp)
                        ) {
                            TestProgress(viewModel.currentRound.value, 30)
                            Spacer(Modifier.height(48.dp))

                            when (viewModel.currentStimulus.value) {
                                // GO — círculo verde + etiqueta
                                SustainedAttentionViewModel.StimulusType.TARGET -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier            = Modifier.semantics { contentDescription = "Círculo verde. ¡Pulsa!" }
                                    ) {
                                        Box(modifier = Modifier.size(180.dp).background(Color(0xFF2E7D32), CircleShape))
                                        Spacer(Modifier.height(12.dp))
                                        Text("● PULSA", color = Color(0xFF2E7D32), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                // NOGO — cuadrado azul + etiqueta
                                SustainedAttentionViewModel.StimulusType.DISTRACTOR -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier            = Modifier.semantics { contentDescription = "Cuadrado azul. No pulses." }
                                    ) {
                                        Box(modifier = Modifier.size(180.dp).background(Color(0xFF1565C0), RoundedCornerShape(16.dp)))
                                        Spacer(Modifier.height(12.dp))
                                        Text("■ NO PULSES", color = Color(0xFF1565C0), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                // Sin estímulo — espacio en blanco
                                SustainedAttentionViewModel.StimulusType.NONE ->
                                    Box(modifier = Modifier.size(180.dp))
                            }

                            Spacer(Modifier.height(48.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                                ScoreChip("Aciertos",  viewModel.hits.value,      Color(0xFF2E7D32))
                                ScoreChip("Errores",   viewModel.errors.value,    Color(0xFFC62828))
                                ScoreChip("Omisiones", viewModel.omissions.value, MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // ── FINISHED: resultados ─────────────────────────────────
                SustainedAttentionViewModel.TestState.FINISHED -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        Text("Test completado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        ResultCard {
                            ResultRow("Aciertos",     "${viewModel.hits.value} / 30",       Color(0xFF2E7D32))
                            ResultRow("Errores",      "${viewModel.errors.value}",           Color(0xFFC62828))
                            ResultRow("Omisiones",    "${viewModel.omissions.value}",        MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ResultRow("Tiempo medio", "${viewModel.avgReactionMs.value} ms", MaterialTheme.colorScheme.onSurface)
                            ResultRow("Precisión",    "${viewModel.precisionPercent()}%",    MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(
                            when {
                                viewModel.precisionPercent() >= 90 -> "🧠 Atención sostenida excepcional"
                                viewModel.precisionPercent() >= 70 -> "✅ Buen rendimiento"
                                viewModel.precisionPercent() >= 50 -> "⚠️ Rendimiento mejorable"
                                else                               -> "❌ Dificultades de atención sostenida"
                            }
                        )
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Atención Sostenida",
                            precisionPct  = viewModel.precisionPercent(),
                            onRepeat      = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}