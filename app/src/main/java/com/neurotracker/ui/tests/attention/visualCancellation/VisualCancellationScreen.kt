package com.neurotracker.ui.tests.attention.visualCancellation

import android.app.Application
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.tests.components.*
import com.neurotracker.ui.tests.components.TestResultActions

/**
 * Pantalla principal del Test de Cancelación Visual.
 *
 * Este test evalúa la capacidad de atención selectiva y búsqueda visual
 * mediante la identificación de estímulos objetivo entre distractores.
 *
 * Dinámica del test:
 *  - Se presenta una cuadrícula de 6x6 con diferentes formas:
 *      • Círculos (objetivo)
 *      • Cuadrados (distractores)
 *      • Triángulos (distractores)
 *  - El usuario debe pulsar únicamente los círculos verdes.
 *  - Duración total: 45 segundos.
 *
 * Estados del test:
 *  IDLE:
 *      - Se muestran instrucciones y una leyenda visual de estímulos.
 *
 *  RUNNING:
 *      - Cuadrícula interactiva.
 *      - Temporizador visible.
 *      - Contadores de aciertos y errores en tiempo real.
 *
 *  FINISHED:
 *      - Resultados detallados del rendimiento.
 *      - Feedback cualitativo.
 *
 * Accesibilidad:
 *  - Diferenciación de estímulos por forma además de color.
 *  - Uso de texto y símbolos para reforzar significado.
 *  - Interacción directa con elementos de tamaño adecuado.
 *
 * Daltonismo:
 *  - No se depende únicamente del color:
 *      • Círculo = objetivo
 *      • Cuadrado y triángulo = distractores
 *
 * Métricas evaluadas:
 *  - Aciertos (detección correcta)
 *  - Errores (falsos positivos)
 *  - Omisiones (objetivos no detectados)
 *  - Tiempo medio por acierto
 *  - Precisión (%)
 *
 * @param navController Controlador de navegación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualCancellationScreen(navController: NavController) {

    val application = LocalContext.current.applicationContext as Application

    /**
     * ViewModel asociado al test.
     */
    val viewModel: VisualCancellationViewModel = viewModel(
        factory = VisualCancellationViewModelFactory(application)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cancelación Visual") },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {

            when (viewModel.testState.value) {

                /**
                 * Estado inicial:
                 * Se muestra una leyenda visual + instrucciones.
                 */
                VisualCancellationViewModel.TestState.IDLE -> {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {

                        /**
                         * Leyenda visual de estímulos.
                         */
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // Objetivo
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF2E7D32), CircleShape)
                                )
                                Text("Pulsa", style = MaterialTheme.typography.bodySmall)
                            }

                            // Distractor 1
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFFC62828), RoundedCornerShape(4.dp))
                                )
                                Text("Ignora", style = MaterialTheme.typography.bodySmall)
                            }

                            // Distractor 2
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                TriangleShape(color = Color(0xFF1565C0), size = 32)
                                Text("Ignora", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        /**
                         * Pantalla genérica de inicio reutilizable.
                         */
                        IdleScreen(
                            title = "Cancelación Visual",
                            description = "Pulsa solo los círculos verdes.\nIgnora cuadrados y triángulos.\nTienes 45 segundos.",
                            onStart = { viewModel.startTest() }
                        )
                    }
                }

                /**
                 * Estado en ejecución:
                 * Cuadrícula interactiva + métricas en tiempo real.
                 */
                VisualCancellationViewModel.TestState.RUNNING -> {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {

                        /**
                         * Cabecera: tiempo restante + puntuación.
                         */
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("⏱ ${viewModel.remainingSec.value}s")
                            Text("✓ ${viewModel.hits.value}  ✗ ${viewModel.errors.value}")
                        }

                        Spacer(Modifier.height(12.dp))

                        /**
                         * Cuadrícula 6x6 de estímulos.
                         */
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            viewModel.gridItems.value.chunked(6).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                                    row.forEach { item ->

                                        /**
                                         * Color base según tipo y estado.
                                         */
                                        val baseColor = when {
                                            item.cancelled -> MaterialTheme.colorScheme.surfaceVariant
                                            item.shape == VisualCancellationViewModel.ShapeType.CIRCLE -> Color(0xFF2E7D32)
                                            item.shape == VisualCancellationViewModel.ShapeType.SQUARE -> Color(0xFFC62828)
                                            else -> Color(0xFF1565C0)
                                        }

                                        /**
                                         * Renderizado según forma.
                                         */
                                        when (item.shape) {

                                            VisualCancellationViewModel.ShapeType.CIRCLE ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .background(baseColor, CircleShape)
                                                        .clickable { viewModel.onItemClicked(item.id) }
                                                )

                                            VisualCancellationViewModel.ShapeType.SQUARE ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .background(baseColor, RoundedCornerShape(4.dp))
                                                        .clickable { viewModel.onItemClicked(item.id) }
                                                )

                                            VisualCancellationViewModel.ShapeType.TRIANGLE ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clickable { viewModel.onItemClicked(item.id) }
                                                ) {
                                                    TriangleShape(color = baseColor, size = 46)
                                                }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        /**
                         * Barra de progreso basada en aciertos.
                         */
                        LinearProgressIndicator(
                            progress = { viewModel.hits.value / 10f },
                            modifier = Modifier.fillMaxWidth(0.8f),
                            color = Color(0xFF2E7D32),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Text(
                            "${viewModel.hits.value} / 10 círculos encontrados",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                /**
                 * Estado final:
                 * Se muestran resultados y análisis del rendimiento.
                 */
                VisualCancellationViewModel.TestState.FINISHED -> {

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
                                "Círculos encontrados",
                                "${viewModel.hits.value} / 10",
                                Color(0xFF2E7D32)
                            )

                            ResultRow(
                                "Errores (falsos positivos)",
                                "${viewModel.errors.value}",
                                Color(0xFFC62828)
                            )

                            ResultRow(
                                "Omisiones (no encontrados)",
                                "${10 - viewModel.hits.value}",
                                MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            ResultRow(
                                "Tiempo por acierto",
                                "${viewModel.avgTimePerHitMs.value} ms",
                                MaterialTheme.colorScheme.onSurface
                            )

                            ResultRow(
                                "Precisión",
                                "${viewModel.precisionPercent()}%",
                                MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        /**
                         * Interpretación cualitativa del rendimiento.
                         */
                        FeedbackText(
                            when {
                                viewModel.precisionPercent() >= 90 -> "Búsqueda visual excepcional"
                                viewModel.precisionPercent() >= 70 -> "Buen rendimiento"
                                viewModel.precisionPercent() >= 50 -> "Rendimiento mejorable"
                                else -> "Dificultades de búsqueda visual"
                            }
                        )

                        Spacer(Modifier.height(24.dp))

                        /**
                         * Acciones finales (repetir / navegación).
                         */
                        TestResultActions(
                            navController = navController,
                            testName = "Cancelación Visual",
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
 * Componente que dibuja un triángulo relleno.
 *
 * Se utiliza como uno de los distractores visuales.
 *
 * @param color Color del triángulo.
 * @param size Tamaño en dp.
 */
@Composable
private fun TriangleShape(color: Color, size: Int) {

    Canvas(modifier = Modifier.size(size.dp)) {

        val path = Path().apply {
            moveTo(this@Canvas.size.width / 2f, 0f)
            lineTo(this@Canvas.size.width, this@Canvas.size.height)
            lineTo(0f, this@Canvas.size.height)
            close()
        }

        drawPath(path, color = color)
    }
}