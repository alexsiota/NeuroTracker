package com.neurotracker.ui.tests.composite.globalCognitive

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

/**
 * Pantalla del Test Cognitivo Global.
 *
 * Muestra las cuatro fases del test encadenadas, cada una precedida por una
 * pantalla de transición con instrucciones. Gestiona el renderizado de
 * estímulos, controles de respuesta y marcadores en tiempo real.
 *
 * Accesibilidad y daltonismo:
 *  - Fase 1: triple diferenciación (forma + color + etiqueta) para GO/NOGO.
 *  - Fase 2: nombre del color de la tinta siempre visible bajo la palabra.
 *            Feedback visual inmediato (✅/❌) tras cada respuesta.
 *  - Fase 3: botones ✓ SÍ / ✗ NO con prefijos como indicadores primarios.
 *  - Fase 4: celdas con número de orden al seleccionarlas.
 *
 * @param navController Controlador de navegación para el botón de retroceso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalCognitiveScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: GlobalCognitiveViewModel = viewModel(
        factory = GlobalCognitiveViewModelFactory(application)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Cognitivo Global") },
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

                // ── IDLE: instrucciones generales ─────────────────────────────
                GlobalCognitiveViewModel.TestState.IDLE -> IdleScreen(
                    title       = "Test Cognitivo Global",
                    description = "Evaluación de 4 capacidades cognitivas:\n\n" +
                            "1️⃣ Reacción simple — 30s\n" +
                            "2️⃣ Test de Stroop — 30s\n" +
                            "3️⃣ N-Back — 20 estímulos\n" +
                            "4️⃣ Memoria Visual — 5 rondas\n\n" +
                            "El resultado es la media de las 4 fases.",
                    onStart = { viewModel.startTest() }
                )

                // ── TRANSITION: pantalla entre fases ──────────────────────────
                GlobalCognitiveViewModel.TestState.TRANSITION -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        PhaseIndicator(viewModel.currentPhase.value, viewModel.totalPhases)
                        Spacer(Modifier.height(24.dp))
                        Text(
                            viewModel.transitionMessage.value,
                            style      = MaterialTheme.typography.headlineSmall,
                            textAlign  = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            viewModel.phaseDescription.value,
                            style     = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        CircularProgressIndicator()
                    }
                }

                // ── FASE 1: Reacción ──────────────────────────────────────────
                GlobalCognitiveViewModel.TestState.PHASE_REACTION -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        PhaseIndicator(viewModel.currentPhase.value, viewModel.totalPhases)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Fase 1 — Reacción · ${viewModel.reactionSec.value}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))

                        // Área de estímulo con altura fija para evitar saltos de layout
                        Box(modifier = Modifier.height(220.dp), contentAlignment = Alignment.Center) {
                            when {
                                viewModel.reactionStimulus.value?.isTarget == true -> {
                                    // GO — círculo rojo + etiqueta (triple diferenciación)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable { viewModel.onReactionPress() }
                                            .semantics { contentDescription = "Círculo rojo. ¡Pulsa ahora!" }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(160.dp)
                                                .background(Color(0xFFC62828), CircleShape)
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            "● PULSA",
                                            color      = Color(0xFFC62828),
                                            fontSize   = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                viewModel.reactionStimulus.value?.isTarget == false -> {
                                    // NOGO — cuadrado azul + etiqueta (triple diferenciación)
                                    // También tiene clickable para registrar el error de comisión
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable { viewModel.onReactionPress() }
                                            .semantics { contentDescription = "Cuadrado azul. No pulses." }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(160.dp)
                                                .background(Color(0xFF1565C0), RoundedCornerShape(12.dp))
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            "■ NO PULSES",
                                            color      = Color(0xFF1565C0),
                                            fontSize   = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                else -> {
                                    Text(
                                        "Preparado...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            ScoreChip("Aciertos", viewModel.reactionHits.value,   Color(0xFF2E7D32))
                            ScoreChip("Errores",  viewModel.reactionErrors.value, Color(0xFFC62828))
                        }
                    }
                }

                // ── FASE 2: Stroop ────────────────────────────────────────────
                GlobalCognitiveViewModel.TestState.PHASE_STROOP -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        PhaseIndicator(viewModel.currentPhase.value, viewModel.totalPhases)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Fase 2 — Stroop · ${viewModel.stroopSec.value}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(32.dp))

                        viewModel.stroopItem.value?.let { item ->
                            // Nombre del color de la tinta usando los mismos valores
                            // de color que define el ViewModel en stroopColorPairs
                            val colorName = when (item.color) {
                                Color(0xFFC62828) -> "Rojo"
                                Color(0xFF2E7D32) -> "Verde"
                                Color(0xFF1565C0) -> "Azul"
                                Color(0xFFE65100) -> "Naranja"
                                else              -> ""
                            }

                            Text(
                                item.text,
                                color      = item.color,
                                fontSize   = 56.sp,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier.semantics {
                                    contentDescription = "Palabra: ${item.text}. Color de la tinta: $colorName"
                                }
                            )

                            // Nombre del color de la tinta visible para daltonismo
                            Text(
                                "(Tinta: $colorName)",
                                color = item.color,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(Modifier.height(8.dp))

                            // Feedback visual inmediato tras responder
                            viewModel.stroopLastCorrect.value?.let { correct ->
                                Text(
                                    if (correct) "✅ Correcto" else "❌ Error",
                                    color      = if (correct) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            } ?: Text(" ", style = MaterialTheme.typography.bodyMedium)

                        } ?: Text(" ", fontSize = 56.sp)

                        Spacer(Modifier.height(24.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick  = { viewModel.onStroopAnswer(true) },
                                enabled  = !viewModel.stroopAnswered.value,
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor         = Color(0xFF1B5E20),
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f).height(56.dp)
                                    .semantics { contentDescription = "Sí, la palabra y el color coinciden" }
                            ) { Text("✓ SÍ", color = Color.White, fontSize = 18.sp) }

                            Button(
                                onClick  = { viewModel.onStroopAnswer(false) },
                                enabled  = !viewModel.stroopAnswered.value,
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor         = Color(0xFF7F0000),
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f).height(56.dp)
                                    .semantics { contentDescription = "No, la palabra y el color no coinciden" }
                            ) { Text("✗ NO", color = Color.White, fontSize = 18.sp) }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            ScoreChip("Aciertos", viewModel.stroopHits.value,   Color(0xFF2E7D32))
                            ScoreChip("Errores",  viewModel.stroopErrors.value, Color(0xFFC62828))
                        }
                    }
                }

                // ── FASE 3: N-Back ────────────────────────────────────────────
                GlobalCognitiveViewModel.TestState.PHASE_NBACK -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        PhaseIndicator(viewModel.currentPhase.value, viewModel.totalPhases)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Fase 3 — N-Back · estímulo ${viewModel.nbackIndex.value} / 20",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(32.dp))

                        Box(
                            modifier         = Modifier
                                .size(100.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                viewModel.nbackSymbol.value ?: " ",
                                fontSize   = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (viewModel.nbackIndex.value <= 1) "Memoriza el primer símbolo"
                            else "¿Igual al anterior?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick  = { viewModel.onNBackYes() },
                                enabled  = !viewModel.nbackAnswered.value && viewModel.nbackIndex.value > 1,
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor         = Color(0xFF1B5E20),
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f).height(52.dp)
                                    .semantics { contentDescription = "Sí, es igual al anterior" }
                            ) { Text("✓ SÍ", color = Color.White) }

                            Button(
                                onClick  = { viewModel.onNBackNo() },
                                enabled  = !viewModel.nbackAnswered.value && viewModel.nbackIndex.value > 1,
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor         = Color(0xFF7F0000),
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f).height(52.dp)
                                    .semantics { contentDescription = "No, es distinto al anterior" }
                            ) { Text("✗ NO", color = Color.White) }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            ScoreChip("Aciertos", viewModel.nbackHits.value,   Color(0xFF2E7D32))
                            ScoreChip("Errores",  viewModel.nbackErrors.value, Color(0xFFC62828))
                        }
                    }
                }

                // ── FASE 4: Memoria Visual ────────────────────────────────────
                GlobalCognitiveViewModel.TestState.PHASE_VISUAL_MEMORY -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        PhaseIndicator(viewModel.currentPhase.value, viewModel.totalPhases)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Fase 4 — Memoria Visual · ronda ${viewModel.vmRound.value} / ${viewModel.vmTotalRounds}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (viewModel.vmPhase.value == "showing") "Memoriza la secuencia"
                            else "Repite la secuencia",
                            style      = MaterialTheme.typography.bodyMedium,
                            color      = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(16.dp))

                        // Cuadrícula 3×3
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            (0..2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    (0..2).forEach { col ->
                                        val cell     = row * 3 + col
                                        val isLit    = viewModel.vmHighlight.value == cell
                                        val isPicked = viewModel.vmUserInput.value.contains(cell)
                                        val pickIdx  = viewModel.vmUserInput.value.indexOf(cell).takeIf { it >= 0 }

                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .background(
                                                    when {
                                                        isLit    -> Color(0xFF1565C0)
                                                        isPicked -> Color(0xFF2E7D32)
                                                        else     -> MaterialTheme.colorScheme.surfaceVariant
                                                    },
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable(enabled = viewModel.vmPhase.value == "input" && !isPicked) {
                                                    viewModel.onVmCellPressed(cell)
                                                }
                                                .semantics {
                                                    contentDescription = "Celda ${cell + 1}. ${
                                                        if (isPicked) "Seleccionada en posición ${(pickIdx ?: 0) + 1}"
                                                        else "Sin seleccionar"
                                                    }"
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isPicked && pickIdx != null) {
                                                Text(
                                                    "${pickIdx + 1}",
                                                    color      = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize   = 20.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(
                            "${viewModel.vmUserInput.value.size} / ${viewModel.vmSequence.value.size} celdas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            ScoreChip("Aciertos", viewModel.vmHits.value,   Color(0xFF2E7D32))
                            ScoreChip("Errores",  viewModel.vmErrors.value, Color(0xFFC62828))
                        }
                    }
                }

                // ── FINISHED: resultados ──────────────────────────────────────
                GlobalCognitiveViewModel.TestState.FINISHED -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Test completado",
                            style      = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Score global: ${viewModel.globalScore.value}%",
                            style      = MaterialTheme.typography.headlineSmall,
                            color      = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(24.dp))
                        ResultCard {
                            ResultRow("Reacción Simple", "${viewModel.reactionScore.value}%", MaterialTheme.colorScheme.onSurface)
                            ResultRow("Stroop",          "${viewModel.stroopScore.value}%",   MaterialTheme.colorScheme.onSurface)
                            ResultRow("N-Back",          "${viewModel.nbackScore.value}%",    MaterialTheme.colorScheme.onSurface)
                            ResultRow("Memoria Visual",  "${viewModel.visualScore.value}%",   MaterialTheme.colorScheme.onSurface)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ResultRow("Score global", "${viewModel.globalScore.value}%", MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(when {
                            viewModel.globalScore.value >= 80 -> "🧠 Rendimiento cognitivo excepcional"
                            viewModel.globalScore.value >= 60 -> "✅ Buen rendimiento global"
                            viewModel.globalScore.value >= 40 -> "⚠️ Rendimiento mejorable"
                            else                              -> "❌ Áreas de mejora significativas"
                        })
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Test Cognitivo Global",
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
 * Indicador visual del progreso entre fases.
 *
 * Muestra [total] puntos horizontales, coloreando con el color primario
 * los que corresponden a fases ya iniciadas o en curso ([current] o menor).
 * Las fases pendientes se muestran en surfaceVariant.
 *
 * @param current Fase actual (1-indexed).
 * @param total   Número total de fases del test.
 */
@Composable
private fun PhaseIndicator(current: Int, total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..total).forEach { i ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (i <= current) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Fase $current de $total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}