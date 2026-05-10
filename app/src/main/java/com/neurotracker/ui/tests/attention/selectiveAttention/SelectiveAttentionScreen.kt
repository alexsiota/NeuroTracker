package com.neurotracker.ui.tests.attention.selectiveAttention

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

/**
 * Pantalla del Test de Atención Selectiva.
 *
 * Evalúa la capacidad de identificar un estímulo objetivo entre distractores.
 * En cada ronda aparecen 4 símbolos; el usuario debe pulsar solo el que coincide
 * con el objetivo mostrado. Son 20 rondas con 2.5 segundos por ronda.
 *
 * Flujo de estados:
 *  - IDLE    → Instrucciones.
 *  - RUNNING → Cuadrícula 2×2 de símbolos con el objetivo indicado.
 *  - FINISHED→ Resultados y acciones.
 *
 * Accesibilidad:
 *  - Cada celda de estímulo tiene contentDescription que indica el símbolo
 *    y si es el objetivo o un distractor.
 *  - El indicador de objetivo tiene contentDescription "Busca el símbolo: X".
 *
 * Responsabilidades:
 * - Mostrar la UI del test en función del estado.
 * - Gestionar la interacción del usuario con los estímulos.
 * - Mostrar resultados y feedback final.
 *
 * @param navController Controlador de navegación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectiveAttentionScreen(navController: NavController) {

    val application = LocalContext.current.applicationContext as Application

    /**
     * ViewModel asociado al test de atención selectiva.
     */
    val viewModel: SelectiveAttentionViewModel = viewModel(
        factory = SelectiveAttentionViewModelFactory(application)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atención Selectiva") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás")
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {

            when (viewModel.testState.value) {

                /**
                 * Estado inicial del test.
                 *
                 * Muestra instrucciones y botón de inicio.
                 */
                SelectiveAttentionViewModel.TestState.IDLE -> IdleScreen(
                    title       = "Atención Selectiva",
                    description = "Verás 4 símbolos. Uno de ellos es el objetivo.\n\nPulsa solo el símbolo objetivo antes de que desaparezca.\n\nSon 20 rondas.",
                    onStart     = { viewModel.startTest() }
                )

                /**
                 * Estado de ejecución del test.
                 *
                 * Muestra:
                 * - Progreso
                 * - Objetivo actual
                 * - Cuadrícula de estímulos
                 * - Métricas en tiempo real
                 */
                SelectiveAttentionViewModel.TestState.RUNNING -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {

                        /**
                         * Indicador de progreso del test.
                         */
                        TestProgress(viewModel.currentRound.value, 20)

                        Spacer(Modifier.height(16.dp))

                        /**
                         * Indicador del símbolo objetivo.
                         */
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Busca: ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text     = viewModel.targetLabel.value,
                                color    = MaterialTheme.colorScheme.primary,
                                fontSize = 36.sp,
                                modifier = Modifier.semantics {
                                    contentDescription = "Busca el símbolo: ${viewModel.targetLabel.value}"
                                }
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        /**
                         * Cuadrícula de estímulos (2x2).
                         */
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            viewModel.stimuli.value.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { stimulus ->

                                        /**
                                         * Celda de estímulo individual.
                                         *
                                         * @param stimulus Objeto con label e indicador de objetivo.
                                         */
                                        Box(
                                            modifier = Modifier
                                                .size(130.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .border(
                                                    2.dp,
                                                    MaterialTheme.colorScheme.outline,
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .clickable { viewModel.onStimulusPressed(stimulus) }
                                                .semantics {
                                                    contentDescription =
                                                        "Símbolo ${stimulus.label}. " +
                                                                if (stimulus.isTarget)
                                                                    "Este es el objetivo."
                                                                else
                                                                    "Distractor."
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                stimulus.label,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 52.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        /**
                         * Métricas en tiempo real del test.
                         */
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            ScoreChip("Aciertos",  viewModel.hits.value,      Color(0xFF2E7D32))
                            ScoreChip("Errores",   viewModel.errors.value,    Color(0xFFC62828))
                            ScoreChip("Omisiones", viewModel.omissions.value, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                /**
                 * Estado final del test.
                 *
                 * Muestra resultados, feedback y acciones.
                 */
                SelectiveAttentionViewModel.TestState.FINISHED -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {

                        Text(
                            "Test completado",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(24.dp))

                        /**
                         * Tarjeta de resultados.
                         */
                        ResultCard {
                            ResultRow("Aciertos",     "${viewModel.hits.value} / 20",       Color(0xFF2E7D32))
                            ResultRow("Errores",      "${viewModel.errors.value}",           Color(0xFFC62828))
                            ResultRow("Omisiones",    "${viewModel.omissions.value}",        MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ResultRow("Tiempo medio", "${viewModel.avgReactionMs.value} ms", MaterialTheme.colorScheme.onSurface)
                            ResultRow("Precisión",    "${viewModel.precisionPercent()}%",    MaterialTheme.colorScheme.primary)
                        }

                        Spacer(Modifier.height(12.dp))

                        /**
                         * Feedback interpretativo según rendimiento.
                         */
                        FeedbackText(
                            when {
                                viewModel.precisionPercent() >= 90 -> "🧠 Atención selectiva excepcional"
                                viewModel.precisionPercent() >= 70 -> "✅ Buen rendimiento"
                                viewModel.precisionPercent() >= 50 -> "⚠️ Rendimiento mejorable"
                                else                               -> "❌ Dificultades de atención selectiva"
                            }
                        )

                        Spacer(Modifier.height(24.dp))

                        /**
                         * Acciones finales: repetir test o salir.
                         */
                        TestResultActions(
                            navController = navController,
                            testName = "Atención Selectiva",
                            precisionPct = viewModel.precisionPercent(),
                            onRepeat = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}