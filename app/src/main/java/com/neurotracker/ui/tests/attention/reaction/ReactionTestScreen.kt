package com.neurotracker.ui.tests.attention.reaction

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.tests.components.*

/**
 * Pantalla del Test de Reacción Simple.
 *
 * Mide el tiempo de reacción del usuario ante estímulos visuales.
 * El usuario debe pulsar la pantalla lo más rápido posible cuando
 * aparece el círculo verde. Si pulsa antes de que aparezca, cuenta como error.
 *
 * Flujo de estados:
 *  - [ReactionTestViewModel.TestState.IDLE]     → Instrucciones + botón de inicio.
 *  - [ReactionTestViewModel.TestState.WAITING]  → Pantalla en espera (sin estímulo visible).
 *  - [ReactionTestViewModel.TestState.STIMULUS] → Círculo verde visible, espera pulsación.
 *  - [ReactionTestViewModel.TestState.FINISHED] → Resultados y botones de acción.
 *
 * Accesibilidad:
 *  - El área de respuesta tiene contentDescription dinámico según el estado del estímulo.
 *  - El icono de retroceso tiene contentDescription "Volver atrás".
 *  - El círculo verde incluye etiqueta "● PULSA AHORA" debajo (para daltonismo).
 *
 * Daltonismo:
 *  - Indicador GO: círculo con color verde oscuro (#2E7D32) + etiqueta texto.
 *  - Los ScoreChips usan colores de alto contraste de [TestColors].
 *
 * @param navController Controlador de navegación para el botón de retroceso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionTestScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ReactionTestViewModel = viewModel(
        factory = ReactionTestViewModelFactory(application)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test de Reacción") },
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

                // ── IDLE: instrucciones ──────────────────────────────────
                ReactionTestViewModel.TestState.IDLE -> IdleScreen(
                    title       = "Test de Reacción",
                    description = "Pulsa la pantalla lo más rápido posible cuando aparezca el círculo verde.\n\n" +
                            "Si pulsas antes de que aparezca contará como error.\n\nSon 15 rondas en total.",
                    onStart     = { viewModel.startTest() }
                )

                // ── WAITING / STIMULUS: área de respuesta ────────────────
                ReactionTestViewModel.TestState.WAITING,
                ReactionTestViewModel.TestState.STIMULUS -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { viewModel.onPress() }
                            .semantics {
                                contentDescription = if (viewModel.showTarget.value)
                                    "¡Círculo verde! Pulsa ahora."
                                else
                                    "Espera el círculo verde."
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier            = Modifier.padding(24.dp)
                        ) {
                            // Indicador de ronda y progreso
                            Text(
                                "Ronda ${viewModel.currentRound.value} / 15",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress   = { viewModel.currentRound.value / 15f },
                                modifier   = Modifier.fillMaxWidth(0.7f),
                                color      = Color(0xFF2E7D32),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(Modifier.height(64.dp))

                            // Estímulo — daltonismo: círculo + etiqueta de texto
                            if (viewModel.showTarget.value) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(200.dp)
                                            .background(Color(0xFF2E7D32), CircleShape)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "● PULSA AHORA",
                                        color      = Color(0xFF2E7D32),
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Text(
                                    "Espera...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }

                            Spacer(Modifier.height(64.dp))

                            // Marcador en tiempo real
                            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                                ScoreChip("Aciertos",  viewModel.hits.value,      Color(0xFF2E7D32))
                                ScoreChip("Errores",   viewModel.errors.value,    Color(0xFFC62828))
                                ScoreChip("Omisiones", viewModel.omissions.value, MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // ── FINISHED: resultados ─────────────────────────────────
                ReactionTestViewModel.TestState.FINISHED -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        Text(
                            "Test completado",
                            style      = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(24.dp))
                        ResultCard {
                            ResultRow("Aciertos",     "${viewModel.hits.value} / 15",       Color(0xFF2E7D32))
                            ResultRow("Errores",      "${viewModel.errors.value}",           Color(0xFFC62828))
                            ResultRow("Omisiones",    "${viewModel.omissions.value}",        MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ResultRow("Tiempo medio", "${viewModel.avgReactionMs.value} ms", MaterialTheme.colorScheme.onSurface)
                            ResultRow("Precisión",    "${viewModel.precisionPercent()}%",    MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(
                            when {
                                viewModel.avgReactionMs.value in 1..250   -> "🧠 Tiempo de reacción excepcional"
                                viewModel.avgReactionMs.value in 251..400 -> "✅ Buen tiempo de reacción"
                                viewModel.avgReactionMs.value in 401..600 -> "⚠️ Tiempo mejorable"
                                else                                       -> "❌ Tiempo de reacción lento"
                            }
                        )
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Reacción Simple",
                            precisionPct  = viewModel.precisionPercent(),
                            onRepeat      = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}