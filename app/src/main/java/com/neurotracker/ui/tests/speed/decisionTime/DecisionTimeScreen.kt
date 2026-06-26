package com.neurotracker.ui.tests.speed.decisionTime

/**
 * Pantalla del Test de Tiempo de Decisión.
 *
 * ─── Nota: usar [DecisionTimeViewModel.answeredUi] para deshabilitar botones ──
 *
 * El ViewModel ahora expone [answeredUi] (observable) en lugar de [hasAnswered]
 * (flag @Volatile no observable). La pantalla usa [answeredUi] para deshabilitar
 * los botones, lo que garantiza consistencia en la UI.
 *
 * @param navController Controlador de navegación para volver atrás.
 */

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * Pantalla principal del Test de Tiempo de Decisión.
 *
 * @param navController Controlador de navegación para volver atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionTimeScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: DecisionTimeViewModel = viewModel(
        factory = DecisionTimeViewModelFactory(application)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tiempo de Decisión") },
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
                DecisionTimeViewModel.TestState.IDLE -> IdleScreen(
                    title       = "Tiempo de Decisión",
                    description = "Verás 4 números y una regla.\n\n" +
                        "Elige el número correcto según la regla lo más rápido posible.\n\n" +
                        "Reglas posibles: Mayor · Menor · Par · Impar\n\n" +
                        "Tienes 3 segundos por ronda. Son 15 rondas.",
                    onStart = { viewModel.startTest() }
                )

                // ── RUNNING ───────────────────────────────────────────────────
                DecisionTimeViewModel.TestState.RUNNING -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        TestProgress(viewModel.currentRound.value, 15)
                        Spacer(Modifier.height(24.dp))

                        viewModel.currentItem.value?.let { item ->
                            Text(
                                "Elige el: ${item.rule}",
                                color    = MaterialTheme.colorScheme.primary,
                                style    = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.semantics {
                                    contentDescription = "Regla: ${item.rule}. Elige el número correcto."
                                }
                            )
                            Spacer(Modifier.height(24.dp))

                            // Opciones en cuadrícula 2×2
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item.options.chunked(2).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        row.forEach { option ->
                                            val globalIndex = item.options.indexOf(option)
                                            val isSelected  = viewModel.selectedIndex.value == globalIndex
                                            // Nota: usar answeredUi (observable) en lugar de hasAnswered (@Volatile)
                                            val isDisabled  = viewModel.answeredUi.value

                                            Box(
                                                modifier = Modifier
                                                    .size(120.dp)
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                        else            MaterialTheme.colorScheme.surfaceVariant,
                                                        RoundedCornerShape(16.dp)
                                                    )
                                                    .border(
                                                        2.dp,
                                                        if (isSelected) MaterialTheme.colorScheme.primary
                                                        else            Color.Transparent,
                                                        RoundedCornerShape(16.dp)
                                                    )
                                                    .clickable(enabled = !isDisabled) {
                                                        viewModel.onOptionSelected(globalIndex)
                                                    }
                                                    .semantics {
                                                        contentDescription = "Opción: $option"
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    option,
                                                    color    = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 48.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            ScoreChip("Aciertos", viewModel.hits.value,   Color(0xFF2E7D32))
                            ScoreChip("Errores",  viewModel.errors.value, Color(0xFFC62828))
                        }
                    }
                }

                // ── FINISHED ──────────────────────────────────────────────────
                DecisionTimeViewModel.TestState.FINISHED -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        Text("Test completado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        ResultCard {
                            ResultRow("Aciertos", "${viewModel.hits.value} / 15", Color(0xFF2E7D32))
                            ResultRow("Errores",  "${viewModel.errors.value}",     Color(0xFFC62828))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ResultRow("Precisión", "${viewModel.precisionPercent()}%", MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(when {
                            viewModel.precisionPercent() >= 90 -> "🧠 Toma de decisiones excepcional"
                            viewModel.precisionPercent() >= 70 -> "✅ Buen rendimiento"
                            viewModel.precisionPercent() >= 50 -> "⚠️ Rendimiento mejorable"
                            else                               -> "❌ Dificultades en la toma de decisiones"
                        })
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Tiempo de Decisión",
                            precisionPct  = viewModel.precisionPercent(),
                            onRepeat      = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}
