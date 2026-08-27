package com.neurotracker.ui.tests.speed.sequenceOrdering

/**
 * Pantalla del Test de Ordenación de Secuencias.
 *
 * ─── Nota: muestra cuenta atrás de la ronda ───────────────────────────────────
 *
 * Se añade el temporizador [SequenceOrderingViewModel.remainingMs] visible
 * en la UI para que el usuario sepa cuánto tiempo le queda por ronda.
 * Si no completa la secuencia antes del timeout, la ronda avanza
 * automáticamente marcándose como error.
 *
 * @param navController Controlador de navegación para volver atrás.
 */

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.neurotracker.ui.tests.components.TestResultActions

/**
 * Pantalla principal del Test de Ordenación de Secuencias.
 *
 * @param navController Controlador de navegación para volver atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequenceOrderingScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: SequenceOrderingViewModel = viewModel(
        factory = SequenceOrderingViewModelFactory(application)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ordenar Secuencias") },
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

                // ── IDLE ──────────────────────────────────────────────────────
                SequenceOrderingViewModel.TestState.IDLE -> IdleScreen(
                    title       = "Ordenar Secuencias",
                    description = "Verás 5 números desordenados.\n\n" +
                        "Púlsalos en orden ascendente (del menor al mayor) " +
                        "lo más rápido posible.\n\n" +
                        "Tienes 10 segundos por ronda.\n\n" +
                        "Son 10 rondas.",
                    onStart = { viewModel.startTest() }
                )

                // ── RUNNING ───────────────────────────────────────────────────
                SequenceOrderingViewModel.TestState.RUNNING -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        TestProgress(viewModel.currentRound.value, 10)
                        Spacer(Modifier.height(12.dp))

                        // Cuenta atrás de la ronda (Nota: nuevo)
                        val secondsLeft = (viewModel.remainingMs.value / 1000L).toInt() + 1
                        val timerColor  = when {
                            secondsLeft > 5  -> MaterialTheme.colorScheme.onSurfaceVariant
                            secondsLeft > 2  -> Color(0xFFE65100)
                            else             -> Color(0xFFC62828)
                        }
                        Text(
                            "⏱ ${secondsLeft}s",
                            color    = timerColor,
                            style    = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics {
                                contentDescription = "$secondsLeft segundos restantes"
                            }
                        )

                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Orden ascendente ↑",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(24.dp))

                        // Números de la secuencia
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            viewModel.currentSequence.value.forEach { number ->
                                val tapped = viewModel.userOrder.value.contains(number)
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            if (tapped) MaterialTheme.colorScheme.surfaceVariant
                                            else        MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable(enabled = !tapped) {
                                            viewModel.onNumberTapped(number)
                                        }
                                        .semantics {
                                            contentDescription =
                                                "Número $number. ${if (tapped) "Ya seleccionado" else "Pulsa para seleccionar"}"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (tapped) "✓" else number.toString(),
                                        color    = if (tapped) MaterialTheme.colorScheme.onSurfaceVariant
                                                   else        MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 22.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            ScoreChip("Aciertos", viewModel.hits.value,   Color(0xFF2E7D32))
                            ScoreChip("Errores",  viewModel.errors.value, Color(0xFFC62828))
                        }
                    }
                }

                // ── FINISHED ──────────────────────────────────────────────────
                SequenceOrderingViewModel.TestState.FINISHED -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        Text("Test completado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        ResultCard {
                            ResultRow("Aciertos", "${viewModel.hits.value} / 10",      Color(0xFF2E7D32))
                            ResultRow("Errores",  "${viewModel.errors.value}",          Color(0xFFC62828))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ResultRow("Tiempo medio / ronda", "${viewModel.avgRoundTimeMs.value} ms", MaterialTheme.colorScheme.onSurface)
                            ResultRow("Precisión", "${viewModel.precisionPercent()}%", MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(when {
                            viewModel.precisionPercent() >= 90 -> "🧠 Velocidad de ordenación excepcional"
                            viewModel.precisionPercent() >= 70 -> "✅ Buen rendimiento"
                            viewModel.precisionPercent() >= 50 -> "⚠️ Rendimiento mejorable"
                            else                               -> "❌ Dificultades de ordenación secuencial"
                        })
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Ordenar Secuencias",
                            precisionPct  = viewModel.precisionPercent(),
                            onRepeat      = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}
