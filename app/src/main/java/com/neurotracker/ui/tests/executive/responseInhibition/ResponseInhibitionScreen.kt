package com.neurotracker.ui.tests.executive.responseInhibition

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.tests.components.*

/**
 * Pantalla del Test de Inhibición de Respuesta (Go / No-Go).
 *
 * Muestra estímulos GO (círculo verde) y NOGO (cuadrado rojo) de forma
 * alternada. El usuario debe pulsar solo cuando aparece el círculo verde.
 *
 * Sistema de puntuación visible en pantalla:
 *  - Aciertos  : pulsaciones sobre círculo verde.
 *  - Errores   : pulsaciones sobre cuadrado rojo.
 *  - Omisiones : círculos verdes que pasan sin pulsar.
 *
 * Accesibilidad y daltonismo:
 *  - Triple diferenciación: forma (círculo/cuadrado) + color + etiqueta de texto.
 *  - [contentDescription] dinámico para TalkBack.
 *  - Área de estímulo con altura fija para evitar saltos de layout.
 *
 * @param navController Controlador de navegación para el botón de retroceso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponseInhibitionScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ResponseInhibitionViewModel = viewModel(
        factory = ResponseInhibitionViewModelFactory(application)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inhibición de Respuesta") },
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
            // Leer testState directamente del ViewModel aquí para que el when
            // sea observado por Compose y recomponga al cambiar
            when (viewModel.testState.value) {

                // ── IDLE: instrucciones ───────────────────────────────────────
                ResponseInhibitionViewModel.TestState.IDLE -> IdleScreen(
                    title       = "Inhibición de Respuesta",
                    description = "Pulsa SIEMPRE que aparezca el CÍRCULO VERDE.\n\n" +
                            "● PULSA    → Círculo verde\n" +
                            "■ NO PULSES → Cuadrado rojo\n\n" +
                            "Son 25 rondas.",
                    onStart = { viewModel.startTest() }
                )

                // ── RUNNING: test en curso ────────────────────────────────────
                ResponseInhibitionViewModel.TestState.RUNNING -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        TestProgress(viewModel.currentRound.value, 25)
                        Spacer(Modifier.height(32.dp))

                        // Área de estímulo con altura fija para evitar saltos de layout
                        Box(
                            modifier         = Modifier.height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Leer currentType directamente aquí — cada vez que cambia,
                            // Compose recompone este bloque automáticamente
                            when (viewModel.currentType.value) {
                                ResponseInhibitionViewModel.StimulusType.GO -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable { viewModel.onPress() }
                                            .semantics { contentDescription = "Círculo verde. ¡Pulsa ahora!" }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(180.dp)
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
                                }
                                ResponseInhibitionViewModel.StimulusType.NOGO -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable { viewModel.onPress() }
                                            .semantics { contentDescription = "Cuadrado rojo. No pulses." }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(180.dp)
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
                                null -> {
                                    // Espacio vacío entre estímulos — altura garantizada por el Box padre
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        // Leer los contadores directamente del ViewModel dentro del árbol
                        // composable para que Compose detecte los cambios y recomponga
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ScoreChip(
                                label = "Aciertos",
                                value = viewModel.hits.value,
                                color = Color(0xFF2E7D32)
                            )
                            ScoreChip(
                                label = "Errores",
                                value = viewModel.commissions.value,
                                color = Color(0xFFC62828)
                            )
                            ScoreChip(
                                label = "Omisiones",
                                value = viewModel.omissions.value,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── FINISHED: resultados ──────────────────────────────────────
                ResponseInhibitionViewModel.TestState.FINISHED -> {
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
                            ResultRow("Aciertos",  "${viewModel.hits.value}",        Color(0xFF2E7D32))
                            ResultRow("Errores",   "${viewModel.commissions.value}", Color(0xFFC62828))
                            ResultRow("Omisiones", "${viewModel.omissions.value}",   MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ResultRow("Precisión", "${viewModel.precisionPercent()}%", MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(when {
                            viewModel.precisionPercent() >= 90 -> "🧠 Control inhibitorio excepcional"
                            viewModel.precisionPercent() >= 70 -> "✅ Buen rendimiento"
                            viewModel.precisionPercent() >= 50 -> "⚠️ Rendimiento mejorable"
                            else                               -> "❌ Dificultades de inhibición de respuesta"
                        })
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Inhibición de Respuesta",
                            precisionPct  = viewModel.precisionPercent(),
                            onRepeat      = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}