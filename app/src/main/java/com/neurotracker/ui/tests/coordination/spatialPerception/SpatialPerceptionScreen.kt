package com.neurotracker.ui.tests.coordination.spatialPerception

/**
 * Pantalla del Test de Percepción Espacial.
 *
 * Evalúa la capacidad de reconocer figuras geométricas rotadas en el espacio.
 * El usuario debe identificar cuál de las 4 opciones coincide con la figura
 * de referencia, teniendo en cuenta la rotación.
 *
 * Características:
 *  - 12 rondas totales
 *  - Cada figura de referencia puede estar rotada
 *  - Las opciones incluyen la figura correcta (rotada) y 3 distractores (formas diferentes)
 *
 * Daltonismo:
 *  - Feedback visual con colores de alto contraste (verde para acierto, rojo para error)
 *  - No se depende exclusivamente del color para transmitir el resultado
 *
 * Accesibilidad:
 *  - El icono de retroceso tiene contentDescription "Volver atrás"
 *  - Todas las opciones son clickables con área mínima de 48dp
 *
 * @param navController Controlador de navegación para el botón de retroceso.
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
import androidx.compose.ui.draw.rotate
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
 * Pantalla principal del Test de Percepción Espacial.
 *
 * @param navController Controlador de navegación para volver atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpatialPerceptionScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: SpatialPerceptionViewModel = viewModel(factory = SpatialPerceptionViewModelFactory(
        application
    )
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Percepción Espacial") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás") } }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (viewModel.testState.value) {

                // Estado IDLE: pantalla de instrucciones
                SpatialPerceptionViewModel.TestState.IDLE -> IdleScreen(
                    title = "Percepción Espacial",
                    description = "Verás una figura de referencia.\n\nDebajo hay 4 opciones. Una es la misma figura (aunque girada).\nLas otras 3 son figuras distintas.\n\nElige la que coincide con la referencia.\n\nSon 12 rondas.",
                    onStart = { viewModel.startTest() }
                )

                // Estado RUNNING: test en curso con figura de referencia y opciones
                SpatialPerceptionViewModel.TestState.RUNNING -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        TestProgress(viewModel.currentRound.value, 12)
                        Spacer(Modifier.height(24.dp))

                        viewModel.currentItem.value?.let { item ->
                            Text("Referencia", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier.size(110.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp))
                                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(item.referenceShape, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 64.sp, modifier = Modifier.rotate(item.referenceRotation))
                            }

                            Spacer(Modifier.height(20.dp))
                            Text("¿Cuál es la misma figura?", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(16.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item.options.chunked(2).forEachIndexed { rowIdx, row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        row.forEachIndexed { colIdx, option ->
                                            val globalIndex = rowIdx * 2 + colIdx
                                            val isSelected  = viewModel.selectedIndex.value == globalIndex
                                            val isCorrect   = isSelected && viewModel.lastAnswerCorrect.value == true
                                            val isWrong     = isSelected && viewModel.lastAnswerCorrect.value == false
                                            Box(
                                                modifier = Modifier.size(110.dp)
                                                    .background(when { isCorrect -> Color(0xFF1B5E20); isWrong -> Color(0xFF7F0000); else -> MaterialTheme.colorScheme.surfaceVariant }, RoundedCornerShape(14.dp))
                                                    .border(2.dp, when { isCorrect -> Color(0xFF2E7D32); isWrong -> Color(0xFFC62828); else -> Color.Transparent }, RoundedCornerShape(14.dp))
                                                    .then(if (!viewModel.hasAnswered.value) Modifier.clickable { viewModel.onOptionSelected(globalIndex) } else Modifier),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(option.shape, color = MaterialTheme.colorScheme.onSurface, fontSize = 56.sp, modifier = Modifier.rotate(option.rotation))
                                            }
                                        }
                                    }
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

                // Estado FINISHED: resultados del test
                SpatialPerceptionViewModel.TestState.FINISHED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("Test completado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        ResultCard {
                            ResultRow("Aciertos", "${viewModel.hits.value} / 12", Color(0xFF2E7D32))
                            ResultRow("Errores",  "${viewModel.errors.value}",     Color(0xFFC62828))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ResultRow("Precisión", "${viewModel.precisionPercent()}%", MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(when {
                            viewModel.precisionPercent() >= 85 -> "🧠 Percepción espacial excepcional"
                            viewModel.precisionPercent() >= 65 -> "✅ Buen rendimiento"
                            viewModel.precisionPercent() >= 50 -> "⚠️ Rendimiento mejorable"
                            else                               -> "❌ Practica la identificación de formas"
                        })
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Percepción Espacial",
                            precisionPct  = viewModel.precisionPercent(),
                            onRepeat      = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}