package com.neurotracker.ui.tests.coordination.visuoMotor

/**
 * Pantalla del Test de Coordinación Visomotora.
 *
 * Evalúa la capacidad de coordinar la visión con el movimiento de la mano.
 * El usuario debe pulsar círculos que aparecen en posiciones aleatorias
 * de la pantalla dentro de un límite de 30 segundos.
 *
 * Reglas:
 *  - Acierto: pulsar dentro del círculo.
 *  - Error: pulsar fuera del círculo o no pulsar nada antes de que desaparezca.
 *  - Solo se cuenta un error por círculo.
 *
 * Accesibilidad:
 *  - El área de toque tiene contentDescription dinámico.
 *  - Los ScoreChip usan colores de alto contraste.
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.tests.components.FeedbackText
import com.neurotracker.ui.tests.components.IdleScreen
import com.neurotracker.ui.tests.components.ResultCard
import com.neurotracker.ui.tests.components.ResultRow
import com.neurotracker.ui.tests.components.ScoreChip
import com.neurotracker.ui.tests.components.TestResultActions

/**
 * Pantalla principal del Test de Coordinación Visomotora.
 *
 * @param navController Controlador de navegación para volver atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisuomotorScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: VisuomotorViewModel = viewModel(factory = VisuomotorViewModelFactory(application))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coordinación Visomotora") },
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
                VisuomotorViewModel.TestState.IDLE -> IdleScreen(
                    title       = "Coordinación Visomotora",
                    description = "Pulsa el círculo cuando aparezca.\n\n" +
                            "✅ Acierto: pulsas dentro del círculo.\n" +
                            "❌ Error: pulsas fuera del círculo, o no pulsas nada.\n\n" +
                            "Solo se cuenta un error por círculo.\nTienes 30 segundos.",
                    onStart     = { viewModel.startTest() }
                )

                // Estado RUNNING: test en curso con temporizador y objetivos móviles
                VisuomotorViewModel.TestState.RUNNING -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("⏱ ${viewModel.remainingSec.value}s", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                ScoreChip("Aciertos", viewModel.hits.value,   Color(0xFF2E7D32))
                                ScoreChip("Errores",  viewModel.errors.value, Color(0xFFC62828))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                                .clickable { viewModel.onBackgroundTapped() }
                        ) {
                            viewModel.currentTarget.value?.let { target ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .wrapContentSize(Alignment.TopStart)
                                        .offset(x = (target.x * 280).dp, y = (target.y * 400).dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            .clickable { viewModel.onTargetTapped(target.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Estado FINISHED: resultados del test
                VisuomotorViewModel.TestState.FINISHED -> {
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
                            ResultRow("Aciertos", "${viewModel.hits.value}",   Color(0xFF2E7D32))
                            ResultRow("Errores",  "${viewModel.errors.value}", Color(0xFFC62828))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ResultRow("Precisión", "${viewModel.precisionPercent()}%", MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(
                            when {
                                viewModel.precisionPercent() >= 90 -> "🧠 Coordinación visomotora excepcional"
                                viewModel.precisionPercent() >= 70 -> "✅ Buen rendimiento"
                                viewModel.precisionPercent() >= 50 -> "⚠️ Rendimiento mejorable"
                                else                               -> "❌ Dificultades de coordinación"
                            }
                        )
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Coordinación Visomotora",
                            precisionPct  = viewModel.precisionPercent(),
                            onRepeat      = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}