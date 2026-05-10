package com.neurotracker.ui.tests.composite.cognitiveFatigue

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.tests.components.*
import com.neurotracker.ui.tests.components.TestResultActions

/**
 * Pantalla del Test de Fatiga Cognitiva.
 *
 * Muestra 3 bloques consecutivos de una tarea Go/No-Go. Cada bloque dura
 * 30 segundos y va precedido de una pantalla de transición. Al terminar
 * muestra el análisis de fatiga comparando la precisión de los bloques.
 *
 * Accesibilidad y daltonismo:
 *  - GO   : círculo verde + etiqueta "● PULSA" (forma + color + texto).
 *  - NOGO : cuadrado rojo + etiqueta "■ NO PULSES" (forma + color + texto).
 *  - Área de estímulo con altura fija para evitar saltos de layout.
 *  - Ambos estímulos tienen [Modifier.clickable] para registrar la pulsación.
 *
 * @param navController Controlador de navegación para el botón de retroceso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CognitiveFatigueScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: CognitiveFatigueViewModel = viewModel(
        factory = CognitiveFatigueViewModelFactory(application)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fatiga Cognitiva") },
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

                // ── IDLE ──────────────────────────────────────────────────────
                CognitiveFatigueViewModel.TestState.IDLE -> IdleScreen(
                    title       = "Test de Fatiga Cognitiva",
                    description = "Realizarás 3 bloques de reacción GO/NOGO:\n\n" +
                            "● Bloque 1 — Inicio\n" +
                            "● Bloque 2 — Intermedio\n" +
                            "● Bloque 3 — Final\n\n" +
                            "Cada bloque dura 30 segundos.\n\n" +
                            "● PULSA el CÍRCULO VERDE.\n" +
                            "■ NO PULSES el CUADRADO ROJO.\n\n" +
                            "Al final verás cómo ha cambiado tu rendimiento.",
                    onStart = { viewModel.startTest() }
                )

                // ── TRANSITION ────────────────────────────────────────────────
                CognitiveFatigueViewModel.TestState.TRANSITION -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        BlockIndicator(current = viewModel.currentBlock.value, total = viewModel.totalBlocks)
                        Spacer(Modifier.height(32.dp))
                        Text(
                            viewModel.transitionMessage.value,
                            style     = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                // ── BLOCK_RUNNING ─────────────────────────────────────────────
                CognitiveFatigueViewModel.TestState.BLOCK_RUNNING -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        BlockIndicator(current = viewModel.currentBlock.value, total = viewModel.totalBlocks)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tiempo restante: ${viewModel.remainingSec.value}s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))

                        // Área de estímulo con altura fija para evitar saltos de layout
                        Box(modifier = Modifier.height(220.dp), contentAlignment = Alignment.Center) {
                            if (viewModel.stimulus.value) {
                                // GO — círculo verde + etiqueta
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { viewModel.onPress() }
                                        .semantics { contentDescription = "Círculo verde. ¡Pulsa ahora!" }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(160.dp)
                                            .background(Color(0xFF2E7D32), CircleShape)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "● PULSA",
                                        color      = Color(0xFF2E7D32),
                                        fontSize   = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                // NOGO — cuadrado rojo + etiqueta
                                // También tiene clickable para registrar el error de comisión
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { viewModel.onPress() }
                                        .semantics { contentDescription = "Cuadrado rojo. No pulses." }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(160.dp)
                                            .background(Color(0xFFC62828), RoundedCornerShape(12.dp))
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "■ NO PULSES",
                                        color      = Color(0xFFC62828),
                                        fontSize   = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            ScoreChip("Aciertos", viewModel.blockHits.value,   Color(0xFF2E7D32))
                            ScoreChip("Errores",  viewModel.blockErrors.value, Color(0xFFC62828))
                        }

                        if (viewModel.blockResults.value.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            viewModel.blockResults.value.forEach { result ->
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Bloque ${result.block} — ${result.label}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "${result.precision}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when {
                                            result.precision >= 70 -> Color(0xFF2E7D32)
                                            result.precision >= 50 -> Color(0xFFE65100)
                                            else                   -> Color(0xFFC62828)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── FINISHED ──────────────────────────────────────────────────
                CognitiveFatigueViewModel.TestState.FINISHED -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        Text(
                            "Análisis de Fatiga",
                            style      = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(24.dp))
                        ResultCard {
                            viewModel.blockResults.value.forEach { result ->
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Bloque ${result.block} — ${result.label}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            "${result.avgReactionMs} ms promedio",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    val emoji = when {
                                        result.precision >= 70 -> "✅"
                                        result.precision >= 50 -> "⚠️"
                                        else                   -> "❌"
                                    }
                                    Text(
                                        "$emoji ${result.precision}%",
                                        color      = when {
                                            result.precision >= 70 -> Color(0xFF2E7D32)
                                            result.precision >= 50 -> Color(0xFFE65100)
                                            else                   -> Color(0xFFC62828)
                                        },
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (result.block < viewModel.totalBlocks) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        val fatigue = viewModel.fatigueIndex()
                        FeedbackText(when {
                            fatigue >= 5  -> "📈 El rendimiento mejoró durante la sesión"
                            fatigue <= -5 -> "📉 Se detecta fatiga cognitiva"
                            else          -> "➡️ Rendimiento estable durante la sesión"
                        })
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Fatiga Cognitiva",
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
 * Indicador visual del bloque actual.
 *
 * Muestra [total] puntos coloreados con verde, naranja y rojo según el bloque.
 * Los bloques anteriores al actual se muestran con su color asignado,
 * los pendientes en surfaceVariant.
 *
 * @param current Bloque actual (1-indexed).
 * @param total   Número total de bloques.
 */
@Composable
private fun BlockIndicator(current: Int, total: Int) {
    val blockColors = listOf(Color(0xFF2E7D32), Color(0xFFE65100), Color(0xFFC62828))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics { contentDescription = "Bloque $current de $total" }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            (1..total).forEach { i ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            if (i <= current) blockColors.getOrElse(i - 1) { Color(0xFF2E7D32) }
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Bloque $current de $total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}