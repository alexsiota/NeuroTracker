package com.neurotracker.ui.tests.memory.digitspan

/**
 * Pantalla del Test de Memoria de Dígitos (Digit Span).
 * Evalúa la memoria a corto plazo y la capacidad de retención de secuencias numéricas.
 * El usuario debe memorizar una secuencia de dígitos que crece con cada acierto.
 * Estados: IDLE → SHOWING → INPUT → FEEDBACK → FINISHED.
 */

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
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
import com.neurotracker.ui.tests.components.FeedbackText
import com.neurotracker.ui.tests.components.IdleScreen
import com.neurotracker.ui.tests.components.ResultCard
import com.neurotracker.ui.tests.components.TestResultActions

/**
 * Pantalla principal del Test de Memoria de Dígitos.
 *
 * @param navController Controlador de navegación para volver atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitSpanScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: DigitSpanViewModel = viewModel(factory = DigitSpanViewModelFactory(application))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memoria de Dígitos") },
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

                // Estado IDLE: pantalla de instrucciones
                DigitSpanViewModel.TestState.IDLE -> IdleScreen(
                    title       = "Memoria de Dígitos",
                    description = "Memoriza la secuencia de números.\n\nIntrodúcela en el mismo orden usando el teclado.\n\nLa secuencia crece con cada acierto.",
                    onStart     = { viewModel.startTest() }
                )

                // Estado SHOWING: muestra la secuencia dígito por dígito
                DigitSpanViewModel.TestState.SHOWING -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Memoriza la secuencia",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${viewModel.currentSpan.value} dígitos",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(48.dp))
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    if (viewModel.currentDigit.value != null) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            viewModel.currentDigit.value?.let {
                                Text(it.toString(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 64.sp)
                            }
                        }
                    }
                }

                // Estado INPUT: usuario introduce la secuencia mediante teclado numérico
                DigitSpanViewModel.TestState.INPUT -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text("Introduce la secuencia", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(24.dp))

                        // Cuadros adaptativos: máx 5 por fila
                        val sequence   = viewModel.sequence.value
                        val userInput  = viewModel.userInput.value
                        val chunkSize  = if (sequence.size <= 5) sequence.size else 5

                        sequence.chunked(chunkSize).forEachIndexed { rowIndex, rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier              = Modifier.padding(bottom = 6.dp)
                            ) {
                                rowItems.indices.forEach { colIndex ->
                                    val globalIndex = rowIndex * chunkSize + colIndex
                                    val digit       = userInput.getOrNull(globalIndex)
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(
                                                if (digit != null) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                                RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            digit?.toString() ?: "",
                                            color = if (digit != null) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                        NumericKeypad(
                            onDigit     = { viewModel.onDigitPressed(it) },
                            onBackspace = { viewModel.onBackspace() }
                        )
                    }
                }

                // Estado FEEDBACK: retroalimentación visual del resultado
                DigitSpanViewModel.TestState.FEEDBACK -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val correct = viewModel.feedbackCorrect.value == true
                        Text(
                            if (correct) "✓ Correcto" else "✗ Incorrecto",
                            color = if (correct) Color(0xFF2E7D32) else Color(0xFFC62828),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        if (!correct) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "La secuencia era: ${viewModel.sequence.value.joinToString(" ")}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Estado FINISHED: resultados finales del test
                DigitSpanViewModel.TestState.FINISHED -> {
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier            = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Span máximo alcanzado",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${viewModel.maxSpan.value} dígitos",
                                    style      = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(
                            when {
                                viewModel.maxSpan.value >= 8 -> "🧠 Memoria excepcional"
                                viewModel.maxSpan.value >= 6 -> "✅ Memoria normal"
                                viewModel.maxSpan.value >= 4 -> "⚠️ Por debajo de la media"
                                else                         -> "❌ Dificultades de memoria"
                            }
                        )
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Memoria de Dígitos",
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
 * Teclado numérico para introducir dígitos.
 *
 * @param onDigit Callback al pulsar un dígito (0-9).
 * @param onBackspace Callback al pulsar la tecla de borrar.
 */
@Composable
private fun NumericKeypad(onDigit: (Int) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { digit -> KeypadButton(digit.toString()) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(modifier = Modifier.size(72.dp))
            KeypadButton("0") { onDigit(0) }
            IconButton(onClick = onBackspace, modifier = Modifier.size(72.dp)) {
                Icon(Icons.Default.Backspace, null)
            }
        }
    }
}

/**
 * Botón individual del teclado numérico.
 *
 * @param label Texto del botón.
 * @param onClick Callback al pulsar el botón.
 */
@Composable
private fun KeypadButton(label: String, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        modifier = Modifier.size(72.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp)
    }
}