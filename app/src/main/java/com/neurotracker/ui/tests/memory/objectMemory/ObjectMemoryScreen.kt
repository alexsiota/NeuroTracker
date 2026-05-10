package com.neurotracker.ui.tests.memory.objectMemory

/**
 * Pantalla del Test de Memoria de Objetos.
 * Evalúa la memoria visual episódica mediante el reconocimiento de objetos.
 * El usuario memoriza 6 objetos durante 5 segundos, luego debe identificarlos
 * entre 12 objetos (6 originales + 6 distractores).
 * Estados: IDLE → MEMORIZE → RECALL → FINISHED.
 */

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.tests.components.*
import com.neurotracker.ui.tests.components.TestResultActions

/**
 * Pantalla principal del Test de Memoria de Objetos.
 *
 * @param navController Controlador de navegación para volver atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectMemoryScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ObjectMemoryViewModel = viewModel(factory = ObjectMemoryViewModelFactory(
        application
    )
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Memoria de Objetos") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás") } }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (viewModel.testState.value) {

                // Estado IDLE: pantalla de instrucciones
                ObjectMemoryViewModel.TestState.IDLE -> IdleScreen(
                    title = "Memoria de Objetos",
                    description = "Se mostrarán 6 objetos durante 5 segundos.\n\nLuego verás 12 objetos y deberás marcar cuáles eran los originales.",
                    onStart = { viewModel.startTest() }
                )

                // Estado MEMORIZE: fase de memorización con temporizador
                ObjectMemoryViewModel.TestState.MEMORIZE -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("Memoriza estos objetos", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Tiempo: ${viewModel.remainingMemorizeSec.value}s", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(32.dp))
                        LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(220.dp)) {
                            items(viewModel.targetObjects.value) { obj -> ObjectTile(emoji = obj, selected = false, interactive = false, onClick = {}) }
                        }
                    }
                }

                // Estado RECALL: fase de reconocimiento con selección de objetos
                ObjectMemoryViewModel.TestState.RECALL -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        Text("¿Cuáles estaban en la lista?", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Seleccionados: ${viewModel.selectedObjects.value.size} / 6", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(24.dp))
                        LazyVerticalGrid(columns = GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            items(viewModel.recallObjects.value) { obj -> ObjectTile(emoji = obj, selected = viewModel.selectedObjects.value.contains(obj), interactive = true, onClick = { viewModel.onObjectToggled(obj) }) }
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { viewModel.onConfirm() }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Confirmar selección") }
                    }
                }

                // Estado FINISHED: resultados del test
                ObjectMemoryViewModel.TestState.FINISHED -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("Test completado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        ResultCard {
                            ResultRow("Aciertos",       "${viewModel.hits.value} / 6",    Color(0xFF2E7D32))
                            ResultRow("Omisiones",      "${viewModel.misses.value}",       MaterialTheme.colorScheme.onSurfaceVariant)
                            ResultRow("Falsas alarmas", "${viewModel.falseAlarms.value}", Color(0xFFC62828))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ResultRow("Precisión", "${viewModel.precisionPercent()}%", MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        FeedbackText(when {
                            viewModel.precisionPercent() >= 90 -> "🧠 Memoria de objetos excepcional"
                            viewModel.precisionPercent() >= 70 -> "✅ Buen rendimiento"
                            viewModel.precisionPercent() >= 50 -> "⚠️ Rendimiento mejorable"
                            else                               -> "❌ Dificultades de memoria visual"
                        })
                        Spacer(Modifier.height(24.dp))
                        TestResultActions(
                            navController = navController,
                            testName      = "Memoria de Objetos",
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
 * Componente que representa un objeto (emoji) en una cuadrícula seleccionable.
 *
 * @param emoji Emoji del objeto a mostrar.
 * @param selected Indica si el objeto está seleccionado.
 * @param interactive Indica si el objeto es clickable.
 * @param onClick Callback al pulsar el objeto.
 */
@Composable
private fun ObjectTile(emoji: String, selected: Boolean, interactive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(72.dp)
            .background(if (selected) Color(0xFF1B5E20) else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(2.dp, if (selected) Color(0xFF2E7D32) else Color.Transparent, RoundedCornerShape(12.dp))
            .then(if (interactive) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) { Text(emoji, fontSize = 32.sp) }
}