package com.neurotracker.ui.eeg

/**
 * Pantalla de Simulación EEG.
 *
 * ─── Fix: layout fijo para la card de estado ─────────────────────────────────
 *
 * Problema original: el texto "Relajado · Atención pasiva" (banda dominante)
 * cambiaba de longitud al actualizarse, haciendo que la card de estado se
 * redimensionara y el resto de la UI "saltara" hacia arriba o abajo.
 * Lo mismo ocurría en la vista Detalle cuando las tarjetas de cada banda
 * se recalculaban.
 *
 * Solución:
 *  - La card de estado tiene [Modifier.height(100.dp)] fija para que
 *    nunca cambie de tamaño independientemente del texto.
 *  - [cognitiveState] y "Banda dominante detectada" tienen [maxLines] y
 *    [softWrap] para que el texto no desborde si es largo.
 *  - En la vista Detalle, cada [WaveCard] tiene altura fija [Modifier.height(160.dp)].
 *
 * @param navController Controlador de navegación.
 */

import android.app.Application
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

// Colores EEG unificados en toda la app
private val AlphaColor = Color(0xFF2E7D32)  // verde oscuro
private val BetaColor  = Color(0xFF1565C0)  // azul oscuro
private val GammaColor = Color(0xFFE65100)  // naranja oscuro
private val ThetaColor = Color(0xFF6A1B9A)  // morado oscuro

/**
 * Pantalla principal de simulación EEG.
 *
 * @param navController Controlador de navegación para volver atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EegScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: EegViewModel = viewModel(factory = EegViewModelFactory(application))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Simulación EEG") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.toggleViewMode() }) {
                        Text(
                            if (viewModel.viewMode.value == EegViewModel.ViewMode.COMPACT) "Detalle" else "Compacto",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // FIX: card de estado con altura FIJA para que no salte al cambiar el texto
            Card(
                shape    = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)  // altura fija — evita saltos de layout
            ) {
                Column(
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (viewModel.isRunning.value) Color(0xFF2E7D32)
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    CircleShape
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (viewModel.isRunning.value) "Señal activa" else "Sin señal",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    // FIX: maxLines + overflow para que el texto no redimensione la card
                    Text(
                        viewModel.cognitiveState.value,
                        style    = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Banda dominante detectada",
                        color  = MaterialTheme.colorScheme.onSurfaceVariant,
                        style  = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (viewModel.viewMode.value == EegViewModel.ViewMode.COMPACT) {
                CompactEegView(viewModel)
            } else {
                DetailEegView(viewModel)
            }

            Spacer(Modifier.height(16.dp))
            BandPowerPanel(viewModel)
            Spacer(Modifier.height(16.dp))

            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("ℹ️", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Datos simulados. En producción esta pantalla recibirá señal de un dispositivo EEG externo vía Bluetooth.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Vista compacta: todas las bandas superpuestas con leyenda.
 *
 * @param viewModel ViewModel con los buffers de señal.
 */
@Composable
private fun CompactEegView(viewModel: EegViewModel) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Señal EEG combinada", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            // Altura fija para el canvas de ondas
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                EegWaveCanvas(points = viewModel.alphaBuffer.value, color = AlphaColor, strokeWidth = 2f)
                EegWaveCanvas(points = viewModel.betaBuffer.value,  color = BetaColor,  strokeWidth = 2f)
                EegWaveCanvas(points = viewModel.gammaBuffer.value, color = GammaColor, strokeWidth = 1.5f)
                EegWaveCanvas(points = viewModel.thetaBuffer.value, color = ThetaColor, strokeWidth = 2f)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                LegendItem("α Alpha", AlphaColor)
                LegendItem("β Beta",  BetaColor)
                LegendItem("γ Gamma", GammaColor)
                LegendItem("θ Theta", ThetaColor)
            }
        }
    }
}

/**
 * Vista detallada: una tarjeta por banda con altura fija.
 *
 * FIX: cada [WaveCard] tiene altura fija para evitar saltos de layout
 * cuando los valores de potencia cambian.
 *
 * @param viewModel ViewModel con los buffers y potencias.
 */
@Composable
private fun DetailEegView(viewModel: EegViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WaveCard("α Alpha — 8–13 Hz",   "Relajación · Atención pasiva", viewModel.alphaBuffer.value, AlphaColor, viewModel.alphaPower.value)
        WaveCard("β Beta — 13–30 Hz",   "Concentración · Alerta",       viewModel.betaBuffer.value,  BetaColor,  viewModel.betaPower.value)
        WaveCard("γ Gamma — 30–100 Hz", "Procesamiento cognitivo alto", viewModel.gammaBuffer.value, GammaColor, viewModel.gammaPower.value)
        WaveCard("θ Theta — 4–8 Hz",    "Memoria · Creatividad",        viewModel.thetaBuffer.value, ThetaColor, viewModel.thetaPower.value)
    }
}

/**
 * Tarjeta de banda EEG con gráfica de altura fija.
 *
 * FIX: [Modifier.height(160.dp)] fijo para que el canvas no se redimensione
 * al actualizar los puntos de señal.
 *
 * @param title    Nombre y rango de frecuencia.
 * @param subtitle Descripción funcional.
 * @param points   Datos de la señal.
 * @param color    Color identificativo.
 * @param power    Potencia actual (0–100).
 */
@Composable
private fun WaveCard(title: String, subtitle: String, points: List<Float>, color: Color, power: Float) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title,    color = color,                                      style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,  maxLines = 1)
                }
                // FIX: ancho fijo para el porcentaje, evita que cambie el layout al variar dígitos
                Text(
                    "${power.toInt()}%",
                    color    = color,
                    style    = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.End,
                    maxLines  = 1
                )
            }
            Spacer(Modifier.height(8.dp))
            // FIX: altura fija para el canvas de ondas — no salta al actualizarse
            Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                EegWaveCanvas(points = points, color = color, strokeWidth = 2.5f, filled = true)
            }
        }
    }
}

/**
 * Panel de barras de potencia por banda.
 *
 * @param viewModel ViewModel con los valores de potencia.
 */
@Composable
private fun BandPowerPanel(viewModel: EegViewModel) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Potencia por banda", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            BandBar("α Alpha", viewModel.alphaPower.value, AlphaColor)
            Spacer(Modifier.height(8.dp))
            BandBar("β Beta",  viewModel.betaPower.value,  BetaColor)
            Spacer(Modifier.height(8.dp))
            BandBar("γ Gamma", viewModel.gammaPower.value, GammaColor)
            Spacer(Modifier.height(8.dp))
            BandBar("θ Theta", viewModel.thetaPower.value, ThetaColor)
        }
    }
}

/**
 * Barra de potencia de una banda EEG.
 *
 * @param label Nombre de la banda.
 * @param power Potencia (0–100).
 * @param color Color de la banda.
 */
@Composable
private fun BandBar(label: String, power: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // FIX: ancho fijo para la etiqueta
        Text(label, color = color, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(60.dp), maxLines = 1)
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(
            progress   = { (power / 100f).coerceIn(0f, 1f) },
            modifier   = Modifier.weight(1f).height(8.dp),
            color      = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        // FIX: ancho fijo para el porcentaje
        Text("${power.toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(36.dp), maxLines = 1)
    }
}

/**
 * Elemento de leyenda EEG.
 *
 * @param label Nombre con letra griega.
 * @param color Color de la banda.
 */
@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, color = color, style = MaterialTheme.typography.bodySmall)
    }
}
