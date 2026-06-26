package com.neurotracker.ui.tests.composite.dualTask

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
 * Pantalla del Test de Doble Tarea.
 *
 * Combina una tarea N-Back y una tarea de reacción durante una misma sesión,
 * mostrando instrucciones, ejecución en tiempo real y resultados finales.
 *
 * @param navController Controlador usado para volver o finalizar la prueba.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualTaskScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: DualTaskViewModel = viewModel(factory = DualTaskViewModelFactory(application))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Doble Tarea") },
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

                DualTaskViewModel.TestState.IDLE -> IdleScreen(
                    title       = "Test de Doble Tarea",
                    description = "Realizarás DOS tareas al mismo tiempo durante 60 segundos:\n\n" +
                        "🔵 Parte superior — N-Back\n" +
                        "Responde ✓ SÍ / ✗ NO si el símbolo es igual al anterior.\n\n" +
                        "🟢 Parte inferior — Reacción\n" +
                        "Pulsa el CÍRCULO VERDE cuando aparezca.\n\n" +
                        "Intenta hacerlo bien en ambas tareas.",
                    onStart = { viewModel.startTest() }
                )

                DualTaskViewModel.TestState.RUNNING -> {
                    // Leer todos los estados al inicio para garantizar recomposición
                    val nbackSymbol   = viewModel.nbackSymbol.value
                    val nbackIndex    = viewModel.nbackIndex.value
                    val nbackAnswered = viewModel.nbackAnswered.value
                    val nbackHits     = viewModel.nbackHits.value
                    val nbackErrors   = viewModel.nbackErrors.value
                    val reactionTarget = viewModel.reactionTarget.value
                    val reactionHits   = viewModel.reactionHits.value
                    val reactionErrors = viewModel.reactionErrors.value

                    Column(
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Temporizador
                        Text(
                            "⏱ ${viewModel.remainingSec.value}s",
                            style    = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .semantics { contentDescription = "${viewModel.remainingSec.value} segundos restantes" }
                        )

                        // ── Tarea 1: N-Back ───────────────────────────────────
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(16.dp),
                            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier            = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("N-Back · estímulo $nbackIndex", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(8.dp))

                                // Símbolo con altura fija
                                Box(
                                    modifier         = Modifier
                                        .size(72.dp)
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        nbackSymbol ?: " ",
                                        fontSize   = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick  = { viewModel.onNBackYes() },
                                        enabled  = !nbackAnswered && nbackIndex > 1,
                                        colors   = ButtonDefaults.buttonColors(
                                            containerColor         = Color(0xFF1B5E20),
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) { Text("✓ SÍ", color = Color.White) }

                                    Button(
                                        onClick  = { viewModel.onNBackNo() },
                                        enabled  = !nbackAnswered && nbackIndex > 1,
                                        colors   = ButtonDefaults.buttonColors(
                                            containerColor         = Color(0xFF7F0000),
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) { Text("✗ NO", color = Color.White) }
                                }

                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    Text("✓ $nbackHits",   color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
                                    Text("✗ $nbackErrors", color = Color(0xFFC62828), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        // ── Tarea 2: Reacción ─────────────────────────────────
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(16.dp),
                            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier            = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Reacción", color = Color(0xFF2E7D32), style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(12.dp))

                                // Círculo con altura fija total (círculo + espacio para etiqueta)
                                // El Box reserva siempre el mismo espacio para que "● PULSA"
                                // aparezca y desaparezca sin mover nada
                                Box(
                                    modifier         = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp),  // altura fija: cubre círculo + etiqueta
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable { viewModel.onReactionPress() }
                                            .semantics {
                                                contentDescription = if (reactionTarget)
                                                    "Círculo verde activo. ¡Pulsa ahora!"
                                                else
                                                    "Esperando estímulo"
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .background(
                                                    if (reactionTarget) Color(0xFF2E7D32)
                                                    else MaterialTheme.colorScheme.surface,
                                                    CircleShape
                                                )
                                        )
                                        // Espacio reservado fijo para la etiqueta (24dp de alto)
                                        // La etiqueta aparece/desaparece pero el espacio no cambia
                                        Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.Center) {
                                            if (reactionTarget) {
                                                Text(
                                                    "● PULSA",
                                                    color      = Color(0xFF2E7D32),
                                                    style      = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    Text("✓ $reactionHits",   color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
                                    Text("✗ $reactionErrors", color = Color(0xFFC62828), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                DualTaskViewModel.TestState.FINISHED -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(24.dp)
                    ) {
                        Text("Test completado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Score global: ${viewModel.globalScore()}%",
                            color      = MaterialTheme.colorScheme.primary,
                            style      = MaterialTheme.typography.headlineSmall,
                            modifier   = Modifier.semantics { contentDescription = "Score global: ${viewModel.globalScore()} por ciento" }
                        )
                        Spacer(Modifier.height(24.dp))
                        ResultCard {
                            Text("Tarea N-Back", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
                            ResultRow("Aciertos",  "${viewModel.nbackHits.value}",   Color(0xFF2E7D32))
                            ResultRow("Errores",   "${viewModel.nbackErrors.value}", Color(0xFFC62828))
                            ResultRow("Precisión", "${viewModel.nbackPrecision()}%", MaterialTheme.colorScheme.onSurface)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Tarea Reacción", color = Color(0xFF2E7D32), style = MaterialTheme.typography.titleSmall)
                            ResultRow("Aciertos",  "${viewModel.reactionHits.value}",   Color(0xFF2E7D32))
                            ResultRow("Errores",   "${viewModel.reactionErrors.value}", Color(0xFFC62828))
                            ResultRow("Precisión", "${viewModel.reactionPrecision()}%", MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(when {
                            viewModel.globalScore() >= 80 -> "🧠 Excelente capacidad de doble tarea"
                            viewModel.globalScore() >= 60 -> "✅ Buen rendimiento simultáneo"
                            viewModel.globalScore() >= 40 -> "⚠️ Dificultad para mantener ambas tareas"
                            else                          -> "❌ La doble tarea supone un reto alto"
                        })
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Doble Tarea",
                            precisionPct  = viewModel.precisionPercent(),
                            onRepeat      = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}
