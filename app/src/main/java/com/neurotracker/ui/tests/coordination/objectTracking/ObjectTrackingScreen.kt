package com.neurotracker.ui.tests.coordination.objectTracking

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.neurotracker.ui.tests.components.*
import com.neurotracker.ui.tests.components.TestResultActions

/**
 * Pantalla del Test de Seguimiento de Objetos.
 *
 * Evalúa la capacidad de seguimiento visual múltiple (Multiple Object Tracking).
 *
 * Flujo del test:
 *  - IDLE: instrucciones iniciales.
 *  - MEMORIZE: se iluminan los objetos objetivo (color primario).
 *  - MOVING: todos los objetos se mueven por la pantalla.
 *  - RESPONSE: el usuario selecciona los objetivos recordados.
 *  - FINISHED: se muestran resultados.
 *
 * Características:
 *  - 4 rondas.
 *  - 2 objetivos por ronda.
 *  - Movimiento continuo de objetos.
 *
 * Accesibilidad:
 *  - Indicadores de ronda visibles y comprensibles.
 *  - Colores combinados con selección (borde) para daltonismo.
 *  - Zonas táctiles amplias en objetos (36dp).
 *
 * @param navController Controlador de navegación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectTrackingScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ObjectTrackingViewModel = viewModel(
        factory = ObjectTrackingViewModelFactory(application)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguimiento de Objetos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver atrás"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (viewModel.testState.value) {

                ObjectTrackingViewModel.TestState.IDLE -> IdleScreen(
                    title = "Seguimiento de Objetos",
                    description = "Se iluminarán 2 objetos objetivo.\n\nMemorízalos, luego todos se moverán.\n\nAl parar, pulsa cuáles eran los 2 objetivos y confirma.\n\nSon 4 rondas.",
                    onStart = { viewModel.startTest() }
                )

                ObjectTrackingViewModel.TestState.MEMORIZE -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        RoundIndicator(
                            current = viewModel.currentRound.value,
                            total = viewModel.totalRounds
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Memoriza los objetivos — ${viewModel.remainingMemorizeSec.value}s",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        TrackingCanvas(viewModel = viewModel, interactive = false)
                    }
                }

                ObjectTrackingViewModel.TestState.MOVING -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        RoundIndicator(
                            current = viewModel.currentRound.value,
                            total = viewModel.totalRounds
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Sigue los objetos — ${viewModel.remainingTrackingSec.value}s",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        TrackingCanvas(viewModel = viewModel, interactive = false)
                    }
                }

                ObjectTrackingViewModel.TestState.RESPONSE -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        RoundIndicator(
                            current = viewModel.currentRound.value,
                            total = viewModel.totalRounds
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "¿Cuáles eran los 2 objetivos?",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Seleccionados: ${viewModel.userSelection.value.size} / 2",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(16.dp))
                        TrackingCanvas(viewModel = viewModel, interactive = true)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.onConfirmSelection() },
                            enabled = viewModel.userSelection.value.size == 2,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            Text("Confirmar selección")
                        }
                    }
                }

                ObjectTrackingViewModel.TestState.FINISHED -> {
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

                        ResultCard {
                            ResultRow(
                                "Aciertos",
                                "${viewModel.hits.value} / ${viewModel.totalPossibleHits}",
                                Color(0xFF2E7D32)
                            )
                            ResultRow(
                                "Errores",
                                "${viewModel.errors.value}",
                                Color(0xFFC62828)
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            ResultRow(
                                "Precisión",
                                "${viewModel.precisionPercent()}%",
                                MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        FeedbackText(
                            when {
                                viewModel.precisionPercent() >= 90 -> "🧠 Seguimiento visual excepcional"
                                viewModel.precisionPercent() >= 70 -> "Buen rendimiento"
                                viewModel.precisionPercent() >= 50 -> "Rendimiento mejorable"
                                else -> "Dificultades de seguimiento visual"
                            }
                        )

                        Spacer(Modifier.height(24.dp))

                        TestResultActions(
                            navController = navController,
                            testName = "Seguimiento de Objetos",
                            precisionPct = viewModel.precisionPercent(),
                            onRepeat = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Indicador visual de ronda actual.
 *
 * Representa el progreso mediante puntos y texto.
 *
 * @param current Ronda actual.
 * @param total Número total de rondas.
 */
@Composable
private fun RoundIndicator(current: Int, total: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..total).forEach { i ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (i <= current)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            "Ronda $current / $total",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Canvas de renderizado de objetos en movimiento.
 *
 * Muestra los objetos en posiciones dinámicas dentro del área de juego.
 * Permite interacción opcional para selección de objetivos.
 *
 * Accesibilidad:
 *  - Diferenciación por color y borde.
 *  - Tamaño táctil adecuado.
 *
 * @param viewModel ViewModel que contiene el estado de los objetos.
 * @param interactive Indica si los objetos son clicables.
 */
@Composable
private fun TrackingCanvas(
    viewModel: ObjectTrackingViewModel,
    interactive: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(16.dp)
            )
    ) {
        viewModel.objects.value.forEach { obj ->

            val isTarget   = obj.isTarget && viewModel.revealTargets.value
            val isSelected = viewModel.userSelection.value.contains(obj.id)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.TopStart)
                    .offset(
                        x = (obj.x * 300).dp,
                        y = (obj.y * 280).dp
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            when {
                                isTarget   -> MaterialTheme.colorScheme.primary
                                isSelected -> Color(0xFF2E7D32)
                                else       -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            CircleShape
                        )
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onSurface
                            else Color.Transparent,
                            shape = CircleShape
                        )
                        .then(
                            if (interactive)
                                Modifier.clickable { viewModel.onObjectSelected(obj.id) }
                            else Modifier
                        )
                )
            }
        }
    }
}