package com.neurotracker.ui.tests.executive.planning

/**
 * Pantalla del Test de Planificación (Torres de Hanoi).
 *
 * Evalúa la capacidad de planificación, resolución de problemas
 * y control ejecutivo del usuario mediante la clásica tarea
 * de Torres de Hanoi.
 *
 * Objetivo del test:
 *  - Mover todos los discos desde la torre A hasta la torre C.
 *
 * Reglas:
 *  - Solo se puede mover un disco cada vez.
 *  - No se puede colocar un disco grande sobre uno más pequeño.
 *
 * Estados del test:
 *  - IDLE     → Instrucciones iniciales.
 *  - RUNNING  → Interacción con las torres (selección y movimiento).
 *  - FINISHED → Resultados y evaluación del rendimiento.
 *
 * Métricas:
 *  - Número de movimientos realizados.
 *  - Eficiencia respecto al mínimo teórico (7 movimientos).
 *
 * Accesibilidad:
 *  - Texto explicativo claro en cada estado.
 *  - Feedback textual en errores de movimiento.
 *  - Indicadores visuales reforzados con texto (no solo color).
 *
 * @param navController Controlador de navegación.
 */

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.tests.components.*
import com.neurotracker.ui.tests.components.TestResultActions

/**
 * Pantalla del Test de Planificación basado en Torres de Hanoi.
 *
 * Presenta las instrucciones, permite mover discos entre torres y muestra
 * el resultado final con métricas de eficiencia.
 *
 * @param navController Controlador usado para volver o finalizar la prueba.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(navController: NavController) {

    /**
     * Obtención del ViewModel con factory personalizada.
     * Se requiere Application para persistencia y lógica interna.
     */
    val application = LocalContext.current.applicationContext as Application
    val viewModel: PlanningViewModel = viewModel(factory = PlanningViewModelFactory(application))

    /**
     * Configuración visual de los discos:
     *  - Colores diferenciados.
     *  - Tamaños proporcionales (grande → pequeño).
     *  - Etiquetas de torres.
     */
    val discColors = listOf(Color(0xFF1565C0), Color(0xFF43A047), Color(0xFFC62828))
    val discWidths = listOf(110.dp, 74.dp, 44.dp)
    val labels     = listOf("A", "B", "C")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Torres de Hanoi") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás")
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

                // ─────────────────────────────────────────────
                // ESTADO INICIAL
                // ─────────────────────────────────────────────
                PlanningViewModel.TestState.IDLE -> IdleScreen(
                    title = "Torres de Hanoi",
                    description =
                        "Mueve todos los discos de la pila A a la pila C.\n\n" +
                                "• Solo puedes mover un disco a la vez.\n" +
                                "• No puedes colocar un disco grande sobre uno pequeño.\n\n" +
                                "Pulsa una pila para seleccionarla, luego pulsa el destino.\n\n" +
                                "Mínimo de movimientos: 7",
                    onStart = { viewModel.startTest() }
                )

                // ─────────────────────────────────────────────
                // ESTADO EN EJECUCIÓN
                // ─────────────────────────────────────────────
                PlanningViewModel.TestState.RUNNING -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {

                        // Contador de movimientos
                        Text(
                            "Movimientos: ${viewModel.moveCount.value}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(4.dp))

                        /**
                         * Mensajes dinámicos:
                         *  - Error de movimiento (si existe).
                         *  - Instrucciones contextuales.
                         */
                        viewModel.errorMessage.value?.let {
                            Text(
                                it,
                                color = Color(0xFFC62828),
                                style = MaterialTheme.typography.bodySmall
                            )
                        } ?: Text(
                            if (viewModel.selectedTower.value == null)
                                "Selecciona la pila de origen"
                            else
                                "Seleccionada: ${labels[viewModel.selectedTower.value!!]} — Pulsa el destino",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(20.dp))

                        /**
                         * Renderizado de las tres torres.
                         * Cada torre:
                         *  - Es interactiva (clickable).
                         *  - Muestra discos en orden vertical.
                         *  - Indica selección mediante color/borde.
                         */
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            viewModel.towers.value.forEachIndexed { towerIndex, discs ->

                                val isSelected = viewModel.selectedTower.value == towerIndex

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(110.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            if (isSelected) 2.dp else 1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.onTowerTapped(towerIndex) }
                                        .padding(8.dp)
                                ) {

                                    // Zona de discos
                                    Box(
                                        modifier = Modifier.height(160.dp),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Bottom,
                                            modifier = Modifier.fillMaxHeight()
                                        ) {
                                            discs.forEach { disc ->
                                                Box(
                                                    modifier = Modifier
                                                        .width(discWidths[disc - 1])
                                                        .height(32.dp)
                                                        .background(
                                                            discColors[disc - 1],
                                                            RoundedCornerShape(6.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        disc.toString(),
                                                        color = Color.White,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                                Spacer(Modifier.height(3.dp))
                                            }
                                        }
                                    }

                                    // Base de la torre
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .background(
                                                MaterialTheme.colorScheme.onSurface,
                                                RoundedCornerShape(2.dp)
                                            )
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    // Etiqueta (A, B, C)
                                    Text(
                                        labels[towerIndex],
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "Objetivo: mover todos los discos a la pila C",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )

                        /**
                         * Botón de finalización manual:
                         * Solo disponible cuando el estado permite finalizar.
                         */
                        if (viewModel.canFinish.value) {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.onConfirmFinish() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                modifier = Modifier.fillMaxWidth(0.7f)
                            ) {
                                Text("✓ Completar test", color = Color.White)
                            }
                        }
                    }
                }

                // ─────────────────────────────────────────────
                // RESULTADOS
                // ─────────────────────────────────────────────
                PlanningViewModel.TestState.FINISHED -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {

                        Text(
                            "¡Completado!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(24.dp))

                        ResultCard {
                            ResultRow(
                                "Movimientos usados",
                                "${viewModel.moveCount.value}",
                                MaterialTheme.colorScheme.onSurface
                            )
                            ResultRow(
                                "Mínimo posible",
                                "7",
                                MaterialTheme.colorScheme.onSurface
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            ResultRow(
                                "Eficiencia",
                                "${viewModel.efficiencyPercent()}%",
                                MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        /**
                         * Feedback interpretativo del rendimiento.
                         */
                        FeedbackText(
                            when {
                                viewModel.efficiencyPercent() >= 100 -> "Solución óptima"
                                viewModel.efficiencyPercent() >= 70  -> "Buena planificación"
                                viewModel.efficiencyPercent() >= 50  -> "Rendimiento mejorable"
                                else                                 -> "Planificación insuficiente"
                            }
                        )

                        Spacer(Modifier.height(24.dp))

                        TestResultActions(
                            navController = navController,
                            testName      = "Planificación",
                            precisionPct  = viewModel.precisionPercent(),
                            onRepeat      = { viewModel.resetTest() }
                        )
                    }
                }
            }
        }
    }
}
