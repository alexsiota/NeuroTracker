package com.neurotracker.ui.tests.memory.nback

/**
 * Pantalla del Test N-Back (1-Back).
 * Evalúa la memoria de trabajo y la capacidad de actualización constante.
 * El usuario debe indicar si el estímulo actual es igual al anterior.
 * El primer estímulo no tiene respuesta correcta.
 */

import android.app.Application
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.tests.components.*
import com.neurotracker.ui.tests.components.TestResultActions

/**
 * Pantalla principal del Test N-Back.
 *
 * @param navController Controlador de navegación para volver atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NBackScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: NBackViewModel = viewModel(factory = NBackViewModelFactory(application))
    Scaffold(
        topBar = { TopAppBar(title = { Text("N-Back · 1-Back") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás") } }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (viewModel.testState.value) {

                // Estado IDLE: pantalla de instrucciones
                NBackViewModel.TestState.IDLE -> IdleScreen(
                    title = "N-Back · 1-Back",
                    description = "Verás una secuencia de letras y números.\n\nPulsa SÍ si el estímulo actual es igual al anterior.\nPulsa NO si es distinto.\n\nPara el primer estímulo no hay respuesta correcta.",
                    onStart = { viewModel.startTest() }
                )

                // Estado RUNNING: test en curso
                NBackViewModel.TestState.RUNNING -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        TestProgress(viewModel.currentIndex.value, 20)
                        Spacer(Modifier.height(48.dp))
                        Box(modifier = Modifier.size(140.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                            viewModel.currentStimulus.value?.let { Text(it, color = MaterialTheme.colorScheme.onSurface, fontSize = 72.sp) }
                        }
                        Spacer(Modifier.height(32.dp))
                        Text(
                            when {
                                viewModel.currentIndex.value <= 1 -> "Primer estímulo — sin respuesta"
                                viewModel.hasAnswered.value -> "✓ Respondido"
                                else -> "¿Igual al anterior?"
                            },
                            color = when {
                                viewModel.currentIndex.value <= 1 -> MaterialTheme.colorScheme.onSurfaceVariant
                                viewModel.hasAnswered.value -> Color(0xFF2E7D32)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = { viewModel.onYesPressed() },
                                enabled = !viewModel.hasAnswered.value && viewModel.currentIndex.value > 1,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20), disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.weight(1f).height(64.dp)
                            ) { Text("SÍ", fontSize = 22.sp, color = Color.White) }
                            Button(
                                onClick = { viewModel.onNoPressed() },
                                enabled = !viewModel.hasAnswered.value && viewModel.currentIndex.value > 1,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F0000), disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.weight(1f).height(64.dp)
                            ) { Text("NO", fontSize = 22.sp, color = Color.White) }
                        }
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            ScoreChip("Aciertos", viewModel.hits.value, Color(0xFF2E7D32))
                            ScoreChip("Errores", viewModel.errors.value, Color(0xFFC62828))
                            ScoreChip("Omisiones", viewModel.omissions.value, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Estado FINISHED: resultados del test
                NBackViewModel.TestState.FINISHED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("Test completado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        ResultCard {
                            ResultRow("Aciertos", "${viewModel.hits.value}", Color(0xFF2E7D32))
                            ResultRow("Errores", "${viewModel.errors.value}", Color(0xFFC62828))
                            ResultRow("Omisiones", "${viewModel.omissions.value}", MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ResultRow("Precisión", "${viewModel.precisionPercent()}%", MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(
                            when {
                                viewModel.precisionPercent() >= 85 -> "🧠 Memoria de trabajo excelente"
                                viewModel.precisionPercent() >= 70 -> "✅ Buen rendimiento"
                                viewModel.precisionPercent() >= 50 -> "⚠️ Rendimiento mejorable"
                                else -> "❌ Dificultades de memoria de trabajo"
                            }
                        )
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "N-Back",
                            precisionPct  = viewModel.precisionPercent(),
                            onRepeat      = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}