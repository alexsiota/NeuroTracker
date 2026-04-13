package com.neurotracker.ui.tests.attention.stroop

import StroopViewModelFactory
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.tests.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StroopScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: StroopViewModel = viewModel(factory = StroopViewModelFactory(application))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test de Stroop") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (viewModel.testState.value) {

                StroopViewModel.TestState.IDLE -> IdleScreen(
                    title       = "Test de Stroop",
                    description = "Verás una palabra escrita en un color.\n\n" +
                        "Pulsa ✓ SÍ si el color de la tinta coincide con el significado de la palabra.\n" +
                        "Pulsa ✗ NO si no coinciden.\n\n" +
                        "El nombre del color de la tinta aparece debajo de la palabra.\n\n" +
                        "Son 25 rondas.",
                    onStart = { viewModel.startTest() }
                )

                StroopViewModel.TestState.RUNNING -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        TestProgress(viewModel.currentRound.value, 25)
                        Spacer(Modifier.height(48.dp))

                        viewModel.currentItem.value?.let { item ->
                            // Nombre del color de la tinta para accesibilidad y daltonismo
                            val colorName = when (item.color) {
                                Color(0xFFC62828) -> "Rojo"
                                Color(0xFF2E7D32) -> "Verde"
                                Color(0xFF1565C0) -> "Azul"
                                Color(0xFFE65100) -> "Naranja"
                                else              -> ""
                            }

                            // Palabra en el color de la tinta
                            Text(
                                text       = item.text,
                                color      = item.color,
                                fontSize   = 64.sp,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier.semantics {
                                    contentDescription = "Palabra: ${item.text}. Color de la tinta: $colorName"
                                }
                            )

                            // Nombre del color de la tinta debajo
                            Text(
                                text  = "(Tinta: $colorName)",
                                color = item.color,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(Modifier.height(8.dp))
                            Text(
                                "¿Coinciden el color de la tinta y la palabra?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } ?: Text(" ", fontSize = 64.sp)

                        Spacer(Modifier.height(40.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick  = { viewModel.onUserAnswer(true) },
                                enabled  = !viewModel.answered.value,
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor         = Color(0xFF1B5E20),
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f).height(64.dp)
                                    .semantics { contentDescription = "Sí, coinciden" }
                            ) { Text("✓ SÍ", color = Color.White, fontSize = 22.sp) }

                            Button(
                                onClick  = { viewModel.onUserAnswer(false) },
                                enabled  = !viewModel.answered.value,
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor         = Color(0xFF7F0000),
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f).height(64.dp)
                                    .semantics { contentDescription = "No, no coinciden" }
                            ) { Text("✗ NO", color = Color.White, fontSize = 22.sp) }
                        }

                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            ScoreChip("Aciertos",  viewModel.hits.value,      Color(0xFF2E7D32))
                            ScoreChip("Errores",   viewModel.errors.value,    Color(0xFFC62828))
                            ScoreChip("Omisiones", viewModel.omissions.value, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                StroopViewModel.TestState.FINISHED -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        Text("Test completado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        ResultCard {
                            ResultRow("Aciertos",     "${viewModel.hits.value} / 25",       Color(0xFF2E7D32))
                            ResultRow("Errores",      "${viewModel.errors.value}",            Color(0xFFC62828))
                            ResultRow("Omisiones",    "${viewModel.omissions.value}",         MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ResultRow("Tiempo medio", "${viewModel.avgReactionMs.value} ms", MaterialTheme.colorScheme.onSurface)
                            ResultRow("Precisión",    "${viewModel.precisionPercent()}%",    MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(when {
                            viewModel.precisionPercent() >= 90 -> "🧠 Control inhibitorio excepcional"
                            viewModel.precisionPercent() >= 70 -> "✅ Buen rendimiento"
                            viewModel.precisionPercent() >= 50 -> "⚠️ Rendimiento mejorable"
                            else                               -> "❌ Dificultades de control inhibitorio"
                        })
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Test de Stroop",
                            precisionPct  = viewModel.precisionPercent(),
                            onRepeat      = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}
