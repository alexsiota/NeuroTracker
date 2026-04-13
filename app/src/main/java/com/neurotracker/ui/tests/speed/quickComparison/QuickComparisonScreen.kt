package com.neurotracker.ui.tests.speed.quickComparison

/**
 * Pantalla del Test de Comparación Rápida.
 * Evalúa la velocidad de procesamiento visual mediante comparación de símbolos.
 * El usuario debe indicar si dos símbolos son iguales o diferentes lo más rápido posible.
 * Son 20 rondas.
 * Estados: IDLE → RUNNING → FINISHED.
 */

import android.app.Application
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.tests.components.*
import com.neurotracker.ui.tests.components.TestResultActions

/**
 * Pantalla principal del Test de Comparación Rápida.
 *
 * @param navController Controlador de navegación para volver atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickComparisonScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: QuickComparisonViewModel = viewModel(factory = QuickComparisonViewModelFactory(
        application
    )
    )
    Scaffold(
        topBar = { TopAppBar(title = { Text("Comparación Rápida") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás") } }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (viewModel.testState.value) {

                // Estado IDLE: pantalla de instrucciones
                QuickComparisonViewModel.TestState.IDLE -> IdleScreen(
                    title = "Comparación Rápida",
                    description = "Verás dos símbolos.\n\nPulsa IGUAL si son el mismo símbolo\no DISTINTO si son diferentes.\n\nSon 20 rondas.",
                    onStart = { viewModel.startTest() }
                )

                // Estado RUNNING: test en curso
                QuickComparisonViewModel.TestState.RUNNING -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        TestProgress(viewModel.currentRound.value, 20)
                        Spacer(Modifier.height(48.dp))
                        viewModel.currentItem.value?.let { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(item.left,  color = MaterialTheme.colorScheme.onSurface, fontSize = 64.sp)
                                Text(item.right, color = MaterialTheme.colorScheme.onSurface, fontSize = 64.sp)
                            }
                        }
                        Spacer(Modifier.height(48.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { viewModel.onAnswer(true) }, enabled = !viewModel.hasAnswered.value, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)), modifier = Modifier.weight(1f).height(56.dp)) { Text("IGUAL",   color = Color.White) }
                            Button(onClick = { viewModel.onAnswer(false) }, enabled = !viewModel.hasAnswered.value, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F0000)), modifier = Modifier.weight(1f).height(56.dp)) { Text("DISTINTO", color = Color.White) }
                        }
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            ScoreChip("Aciertos", viewModel.hits.value,   Color(0xFF2E7D32))
                            ScoreChip("Errores",  viewModel.errors.value, Color(0xFFC62828))
                        }
                    }
                }

                // Estado FINISHED: resultados del test
                QuickComparisonViewModel.TestState.FINISHED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
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
                            viewModel.precisionPercent() >= 90 -> "🧠 Velocidad de procesamiento excepcional"
                            viewModel.precisionPercent() >= 70 -> "✅ Buen rendimiento"
                            viewModel.precisionPercent() >= 50 -> "⚠️ Rendimiento mejorable"
                            else                               -> "❌ Dificultades de comparación visual"
                        })
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Comparación Rápida",
                            precisionPct  = viewModel.precisionPercent(),
                            onRepeat      = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}