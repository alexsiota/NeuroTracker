package com.neurotracker.ui.tests.memory.visualMemory

/**
 * Pantalla del Test de Memoria Visual.
 * Evalúa la memoria visual a corto plazo mediante el juego de secuencias.
 * El usuario debe memorizar una secuencia de celdas que se iluminan y luego repetirla.
 * La secuencia crece con cada acierto (longitud +1).
 * Estados: IDLE → SHOWING → INPUT → FEEDBACK → FINISHED.
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.tests.components.*
import com.neurotracker.ui.tests.components.TestResultActions

/**
 * Pantalla principal del Test de Memoria Visual.
 *
 * @param navController Controlador de navegación para volver atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualMemoryScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: VisualMemoryViewModel = viewModel(factory = VisualMemoryViewModelFactory(
        application
    )
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Memoria Visual") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás") } }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (viewModel.testState.value) {

                // Estado IDLE: pantalla de instrucciones
                VisualMemoryViewModel.TestState.IDLE -> IdleScreen(
                    title = "Memoria Visual",
                    description = "Observa qué celdas se iluminan y en qué orden.\n\nLuego repite la secuencia pulsando las mismas celdas en el mismo orden.\n\nLa secuencia crece con cada acierto.",
                    onStart = { viewModel.startTest() }
                )

                // Estado SHOWING: muestra la secuencia iluminando celdas
                VisualMemoryViewModel.TestState.SHOWING -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("Memoriza la secuencia", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("${viewModel.sequence.value.size} celdas", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(32.dp))
                        MemoryGrid(highlightedCell = viewModel.highlightedCell.value, userInputCells = emptyList(), interactive = false, onCellPressed = {})
                    }
                }

                // Estado INPUT: usuario introduce la secuencia
                VisualMemoryViewModel.TestState.INPUT -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("Tu turno", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("${viewModel.userInput.value.size} / ${viewModel.sequence.value.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(32.dp))
                        MemoryGrid(highlightedCell = -1, userInputCells = viewModel.userInput.value, interactive = true, onCellPressed = { viewModel.onCellPressed(it) })
                    }
                }

                // Estado FEEDBACK: retroalimentación visual del resultado
                VisualMemoryViewModel.TestState.FEEDBACK -> {
                    val correct = viewModel.feedbackCorrect.value == true
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (correct) "✓ Correcto" else "✗ Incorrecto", color = if (correct) Color(0xFF2E7D32) else Color(0xFFC62828), style = MaterialTheme.typography.headlineMedium)
                        if (correct) {
                            Spacer(Modifier.height(8.dp))
                            Text("Siguiente ronda: ${viewModel.currentLength.value + 1} celdas", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Estado FINISHED: resultados del test
                VisualMemoryViewModel.TestState.FINISHED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("Test completado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        ResultCard {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("Secuencia máxima recordada", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))
                                Text("${viewModel.maxLength.value} celdas", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(when {
                            viewModel.maxLength.value >= 8 -> "🧠 Memoria visual excepcional"
                            viewModel.maxLength.value >= 6 -> "✅ Memoria visual normal"
                            viewModel.maxLength.value >= 4 -> "⚠️ Por debajo de la media"
                            else                           -> "❌ Dificultades de memoria visual"
                        })
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Memoria Visual",
                            precisionPct  = viewModel.precisionPercent(),
                            onRepeat      = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cuadrícula 3x3 para el test de memoria visual.
 *
 * @param highlightedCell Índice de la celda actualmente iluminada (-1 si ninguna).
 * @param userInputCells Lista de celdas ya seleccionadas por el usuario.
 * @param interactive Indica si la cuadrícula es interactiva (fase INPUT).
 * @param onCellPressed Callback al pulsar una celda.
 */
@Composable
private fun MemoryGrid(highlightedCell: Int, userInputCells: List<Int>, interactive: Boolean, onCellPressed: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        (0 until 3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (0 until 3).forEach { col ->
                    val idx           = row * 3 + col
                    val isHighlighted = idx == highlightedCell
                    val isSelected    = userInputCells.contains(idx)
                    Box(
                        modifier = Modifier.size(90.dp)
                            .background(when {
                                isHighlighted -> MaterialTheme.colorScheme.primary
                                isSelected -> Color(0xFF2E7D32)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }, RoundedCornerShape(14.dp))
                            .border(2.dp, if (isHighlighted) MaterialTheme.colorScheme.onPrimary else Color.Transparent, RoundedCornerShape(14.dp))
                            .then(if (interactive) Modifier.clickable { onCellPressed(idx) } else Modifier)
                    )
                }
            }
        }
    }
}