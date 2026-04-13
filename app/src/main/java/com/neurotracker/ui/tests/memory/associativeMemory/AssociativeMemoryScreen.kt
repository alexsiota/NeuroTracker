package com.neurotracker.ui.tests.memory.associativeMemory

/**
 * Pantalla del Test de Memoria Asociativa.
 * Evalúa la capacidad de recordar pares de elementos relacionados.
 * El usuario memoriza 5 pares de emojis en 6 segundos, luego debe identificar la pareja correcta de cada clave.
 * Estados: IDLE → MEMORIZE → RECALL → FEEDBACK → FINISHED.
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.tests.components.*
import com.neurotracker.ui.tests.components.TestResultActions

/**
 * Pantalla principal del Test de Memoria Asociativa.
 *
 * @param navController Controlador de navegación para volver atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssociativeMemoryScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: AssociativeMemoryViewModel = viewModel(factory = AssociativeMemoryViewModelFactory(
        application
    )
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Memoria Asociativa") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás") } }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (viewModel.testState.value) {

                // Estado IDLE: pantalla de instrucciones
                AssociativeMemoryViewModel.TestState.IDLE -> IdleScreen(
                    title = "Memoria Asociativa",
                    description = "Verás 5 pares de objetos durante 6 segundos.\n\nLuego tendrás que recordar qué objeto iba con cuál.",
                    onStart = { viewModel.startTest() }
                )

                // Estado MEMORIZE: fase de memorización con temporizador
                AssociativeMemoryViewModel.TestState.MEMORIZE -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("Memoriza los pares", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("${viewModel.remainingMemorizeSec.value}s", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(32.dp))
                        viewModel.pairs.value.forEach { pair ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                EmojiBox(pair.first)
                                Spacer(Modifier.width(16.dp))
                                Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 24.sp)
                                Spacer(Modifier.width(16.dp))
                                EmojiBox(pair.second)
                            }
                        }
                    }
                }

                // Estado RECALL: fase de preguntas de opción múltiple
                AssociativeMemoryViewModel.TestState.RECALL -> {
                    val question = viewModel.currentQuestion.value
                    if (question != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Text("${viewModel.currentQuestionIndex.value + 1} / ${viewModel.totalQuestions()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(24.dp))
                            Text("¿Qué iba con...?", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(16.dp))
                            EmojiBox(question.cue, size = 96.dp)
                            Spacer(Modifier.height(32.dp))
                            question.options.forEach { option ->
                                val isSelected = viewModel.selectedAnswer.value == option
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                                        .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(14.dp))
                                        .clickable { viewModel.onAnswerSelected(option) }
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text(option, fontSize = 36.sp) }
                            }
                        }
                    }
                }

                // Estado FEEDBACK: retroalimentación visual de la respuesta
                AssociativeMemoryViewModel.TestState.FEEDBACK -> {
                    val correct = viewModel.feedbackCorrect.value == true
                    Text(if (correct) "✓ Correcto" else "✗ Incorrecto", color = if (correct) Color(0xFF2E7D32) else Color(0xFFC62828), style = MaterialTheme.typography.headlineMedium)
                }

                // Estado FINISHED: resultados del test
                AssociativeMemoryViewModel.TestState.FINISHED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("Test completado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        ResultCard {
                            ResultRow("Aciertos", "${viewModel.hits.value} / ${viewModel.totalQuestions()}", Color(0xFF2E7D32))
                            ResultRow("Errores",  "${viewModel.errors.value}",                               Color(0xFFC62828))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ResultRow("Precisión", "${viewModel.precisionPercent()}%", MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(when {
                            viewModel.precisionPercent() >= 90 -> "🧠 Memoria asociativa excepcional"
                            viewModel.precisionPercent() >= 70 -> "✅ Buen rendimiento"
                            viewModel.precisionPercent() >= 50 -> "⚠️ Rendimiento mejorable"
                            else                               -> "❌ Dificultades de memoria asociativa"
                        })
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Memoria Asociativa",
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
 * Componente que muestra un emoji dentro de una caja con fondo.
 *
 * @param emoji Emoji a mostrar.
 * @param size Tamaño de la caja en dp.
 */
@Composable
private fun EmojiBox(emoji: String, size: Dp = 72.dp) {
    Box(modifier = Modifier.size(size).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = (size.value * 0.5f).sp)
    }
}