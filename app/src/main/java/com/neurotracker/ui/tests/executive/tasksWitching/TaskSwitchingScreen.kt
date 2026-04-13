package com.neurotracker.ui.tests.executive.tasksWitching

/**
 * Pantalla del Test de Cambio de Tarea (Task Switching).
 *
 * ─── Fix daltonismo ───────────────────────────────────────────────────────────
 *
 * Problema original: el estímulo era solo una figura de color sin etiqueta.
 * Para la regla COLOR, el usuario debía identificar el color visualmente.
 * Los colores usados (Color.Red, Color.Green, etc.) no son distinguibles
 * en deuteranopia (confusión rojo-verde).
 *
 * Solución:
 *  1. El ViewModel ahora usa colores oscuros de alto contraste.
 *  2. La pantalla muestra el nombre del color en texto bajo la figura,
 *     usando el mismo color del estímulo. Esto permite identificar el color
 *     incluso sin percepción cromática normal.
 *  3. Las opciones de respuesta (botones de texto) no dependen del color.
 *
 * @param navController Controlador de navegación para el botón de retroceso.
 */

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.tests.components.*
import com.neurotracker.ui.tests.components.TestResultActions

/**
 * Pantalla principal del Test de Cambio de Tarea.
 *
 * @param navController Controlador de navegación para volver atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSwitchingScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: TaskSwitchingViewModel = viewModel(
        factory = TaskSwitchingViewModelFactory(application)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cambio de Tarea") },
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
                TaskSwitchingViewModel.TestState.IDLE -> IdleScreen(
                    title       = "Cambio de Tarea",
                    description = "La regla cambia cada 4 rondas.\n\n" +
                        "COLOR → indica el color de la figura (Rojo, Azul, Verde, Naranja).\n" +
                        "FORMA → indica la forma (Círculo, Cuadrado).\n\n" +
                        "Son 20 rondas.",
                    onStart = { viewModel.startTest() }
                )

                // ── RUNNING ───────────────────────────────────────────────────
                TaskSwitchingViewModel.TestState.RUNNING -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        TestProgress(viewModel.currentRound.value, 20)
                        Spacer(Modifier.height(8.dp))

                        // Indicador de regla activa
                        val ruleColor = if (viewModel.currentRule.value == TaskSwitchingViewModel.Rule.COLOR)
                            MaterialTheme.colorScheme.primary else Color(0xFFE65100)
                        Surface(color = ruleColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                if (viewModel.currentRule.value == TaskSwitchingViewModel.Rule.COLOR)
                                    "REGLA: COLOR" else "REGLA: FORMA",
                                color    = ruleColor,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                style    = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        viewModel.currentStimulus.value?.let { stimulus ->
                            val shape = if (stimulus.shape == "Círculo") CircleShape
                                        else RoundedCornerShape(12.dp)

                            // Figura con nombre del color debajo (FIX daltonismo)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.semantics {
                                    contentDescription =
                                        "Figura: ${stimulus.shape}. Color: ${stimulus.colorName}"
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .background(stimulus.color, shape)
                                )
                                Spacer(Modifier.height(8.dp))
                                // FIX: nombre del color en texto para daltónicos
                                Text(
                                    stimulus.colorName,
                                    color      = stimulus.color,
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Opciones de respuesta
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            viewModel.options.value.forEach { option ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable(enabled = !viewModel.hasAnswered.value) {
                                            viewModel.onOptionSelected(option)
                                        }
                                        .semantics { contentDescription = "Opción: $option" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        option,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            ScoreChip("Aciertos", viewModel.hits.value,   Color(0xFF2E7D32))
                            ScoreChip("Errores",  viewModel.errors.value, Color(0xFFC62828))
                        }
                    }
                }

                // ── FINISHED ──────────────────────────────────────────────────
                TaskSwitchingViewModel.TestState.FINISHED -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        Text("Test completado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        ResultCard {
                            ResultRow("Aciertos", "${viewModel.hits.value} / 20", Color(0xFF2E7D32))
                            ResultRow("Errores",  "${viewModel.errors.value}",     Color(0xFFC62828))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ResultRow("Precisión", "${viewModel.precisionPercent()}%", MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(when {
                            viewModel.precisionPercent() >= 90 -> "🧠 Flexibilidad cognitiva excepcional"
                            viewModel.precisionPercent() >= 70 -> "✅ Buen rendimiento"
                            viewModel.precisionPercent() >= 50 -> "⚠️ Rendimiento mejorable"
                            else                               -> "❌ Dificultades de flexibilidad cognitiva"
                        })
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Cambio de Tarea",
                            precisionPct  = viewModel.precisionPercent(),
                            onRepeat      = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}
