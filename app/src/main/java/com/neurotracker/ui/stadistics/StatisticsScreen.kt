package com.neurotracker.ui.stadistics

import android.app.Application
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

private val AlphaColor = Color(0xFF2E7D32)
private val BetaColor  = Color(0xFF1565C0)
private val GammaColor = Color(0xFFE65100)
private val ThetaColor = Color(0xFF6A1B9A)

/**
 * Devuelve el indicador textual asociado a una precisión media.
 *
 * @param pct Porcentaje de precisión.
 * @return Indicador visual usado en resúmenes.
 */
private fun precisionEmoji(pct: Float) = when { pct >= 80 -> "✅"; pct >= 50 -> "⚠️"; else -> "❌" }

/**
 * Selecciona el color semántico de una precisión media.
 *
 * @param p Porcentaje de precisión.
 * @return Color para precisión alta, media o baja.
 */
private fun precisionColorFn(p: Float): Color = when { p >= 80 -> Color(0xFF2E7D32); p >= 50 -> Color(0xFFE65100); else -> Color(0xFFC62828) }

/**
 * Pantalla de estadísticas globales del usuario.
 *
 * Muestra resumen de actividad, evolución de precisión, rendimiento por
 * bloque cognitivo y medias de actividad EEG.
 *
 * @param navController Controlador usado para volver a la pantalla anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModelFactory(application))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás")
                    }
                }
            )
        }
    ) { padding ->

        if (viewModel.isLoading.value) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val avgInt = viewModel.avgGlobal.value.toInt()

            // ── Resumen: 3 cards con MISMO ALTO FIJO ─────────────────────────
            // Todas tienen height(100.dp) explícito para que sean iguales
            // independientemente del contenido de cada una.
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    label             = "Tests",
                    value             = "${viewModel.totalTests.value}",
                    valueColor        = MaterialTheme.colorScheme.primary,
                    accessibilityDesc = "Tests totales: ${viewModel.totalTests.value}",
                    modifier          = Modifier.weight(1f)
                )
                SummaryCard(
                    label             = "Precisión",
                    value             = "${precisionEmoji(viewModel.avgGlobal.value)} $avgInt%",
                    valueColor        = precisionColorFn(viewModel.avgGlobal.value),
                    accessibilityDesc = "Precisión media: $avgInt por ciento",
                    modifier          = Modifier.weight(1f)
                )
                SummaryCard(
                    label             = "Mejor bloque",
                    value             = viewModel.bestBlock.value.ifBlank { "—" },
                    valueColor        = MaterialTheme.colorScheme.primary,
                    accessibilityDesc = "Mejor bloque: ${viewModel.bestBlock.value.ifBlank { "sin datos" }}",
                    modifier          = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Evolución global ──────────────────────────────────────────────
            if (viewModel.globalEvolution.value.size >= 2) {
                SectionTitle("Evolución global — últimos tests")
                Spacer(Modifier.height(4.dp))
                Text("Cada punto es un test · Línea verde = 80%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                val primaryColor   = MaterialTheme.colorScheme.primary
                val gridColor      = MaterialTheme.colorScheme.surfaceVariant

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.width(30.dp).height(180.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                listOf("100","75","50","25","0").forEach { Text(it, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            LineChart(points = viewModel.globalEvolution.value.map { it.value }, lineColor = primaryColor, gridColor = gridColor, modifier = Modifier.weight(1f).height(180.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        val pts   = viewModel.globalEvolution.value
                        val trend = pts.last().value - pts.first().value
                        val (emoji, text, tColor) = when {
                            trend > 10f  -> Triple("📈", "Tu precisión ha mejorado +${trend.toInt()}%", Color(0xFF2E7D32))
                            trend < -10f -> Triple("📉", "Tu precisión ha bajado ${trend.toInt()}%",     Color(0xFFC62828))
                            else         -> Triple("➡️", "Tu precisión se mantiene estable",              MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("$emoji $text", style = MaterialTheme.typography.bodySmall, color = tColor)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // ── Bloques cognitivos ────────────────────────────────────────────
            SectionTitle("Rendimiento por bloque cognitivo")
            Spacer(Modifier.height(8.dp))

            val blocks = listOfNotNull(
                viewModel.attentionStats.value, viewModel.memoryStats.value,
                viewModel.speedStats.value,     viewModel.executiveStats.value,
                viewModel.coordStats.value,     viewModel.compositeStats.value
            )

            if (blocks.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Realiza tests para ver estadísticas", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                blocks.forEach { block -> BlockCard(block); Spacer(Modifier.height(12.dp)) }
            }

            Spacer(Modifier.height(20.dp))

            // ── EEG ──────────────────────────────────────────────────────────
            SectionTitle("Estado cerebral durante los tests")
            Spacer(Modifier.height(8.dp))

            if (!viewModel.hasEegData.value) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Realiza tests para ver tu actividad cerebral.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                viewModel.eegAverages.value?.let { eeg ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text("Potencia media por banda", style = MaterialTheme.typography.titleSmall)
                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text("Dominante: ${eeg.dominant}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            EegBandBar("α Alpha — Relajación",   eeg.alpha, AlphaColor)
                            Spacer(Modifier.height(8.dp))
                            EegBandBar("β Beta — Concentración", eeg.beta,  BetaColor)
                            Spacer(Modifier.height(8.dp))
                            EegBandBar("γ Gamma — Cognitivo",    eeg.gamma, GammaColor)
                            Spacer(Modifier.height(8.dp))
                            EegBandBar("θ Theta — Memoria",      eeg.theta, ThetaColor)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── COMPONENTES PRIVADOS ─────────────────────────────────────────────────────

/**
 * Tarjeta de resumen con altura FIJA de 100dp.
 *
 * La altura fija garantiza que los 3 bloques superiores siempre tengan
 * exactamente el mismo tamaño, independientemente de cuánto texto contengan.
 * El texto se ajusta con maxLines + overflow para no desbordarse.
 */
@Composable
private fun SummaryCard(
    label:             String,
    value:             String,
    valueColor:        Color,
    accessibilityDesc: String,
    modifier:          Modifier = Modifier
) {
    Card(
        modifier  = modifier
            .height(100.dp)   // ALTURA FIJA — igual en los 3 bloques siempre
            .semantics { contentDescription = accessibilityDesc },
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                value,
                color      = valueColor,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                maxLines   = 2,
                overflow   = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                style     = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines  = 2,
                overflow  = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Título de sección para bloques internos de estadísticas.
 *
 * @param text Texto que identifica la sección.
 */
@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

/**
 * Gráfico de línea para representar evolución porcentual.
 *
 * @param points Valores de precisión en orden temporal.
 * @param lineColor Color de la línea principal.
 * @param gridColor Color de la cuadrícula.
 * @param modifier Modificador aplicado al canvas.
 */
@Composable
private fun LineChart(points: List<Float>, lineColor: Color, gridColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val w = size.width; val h = size.height
        val stepX = w / (points.size - 1).toFloat()
        listOf(0f,0.25f,0.5f,0.75f,1.0f).forEach { l -> drawLine(gridColor, Offset(0f,h-l*h*0.9f-h*0.05f), Offset(w,h-l*h*0.9f-h*0.05f), 1f) }
        drawLine(Color(0xFF2E7D32).copy(alpha=0.5f), Offset(0f,h-0.8f*h*0.9f-h*0.05f), Offset(w,h-0.8f*h*0.9f-h*0.05f), 1.5f)
        val path = Path()
        points.forEachIndexed { i, v -> val x=i*stepX; val y=h-(v/100f)*h*0.9f-h*0.05f; if(i==0) path.moveTo(x,y) else path.lineTo(x,y) }
        val filled = Path().apply { addPath(path); lineTo((points.size-1)*stepX,h); lineTo(0f,h); close() }
        drawPath(filled, lineColor.copy(alpha=0.15f)); drawPath(path, lineColor, style=Stroke(3f))
        points.forEachIndexed { i, v -> val x=i*stepX; val y=h-(v/100f)*h*0.9f-h*0.05f; drawCircle(lineColor,5f,Offset(x,y)); drawCircle(Color.White,2f,Offset(x,y)) }
    }
}

/**
 * Gráfico de barras para puntos estadísticos de un bloque cognitivo.
 *
 * @param points Valores asociados a tests o categorías.
 * @param labelColor Color reservado para etiquetas del gráfico.
 * @param gridColor Color de la cuadrícula.
 * @param modifier Modificador aplicado al canvas.
 */
@Composable
private fun BarChart(points: List<StatPoint>, labelColor: Color, gridColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        val w=size.width; val h=size.height
        val gap=if(points.size>10) 2f else 4f
        val barW=(w-gap*(points.size-1))/points.size
        listOf(0f,0.25f,0.5f,0.75f,1.0f).forEach { l -> drawLine(gridColor,Offset(0f,h-l*h*0.9f),Offset(w,h-l*h*0.9f),1f) }
        drawLine(Color(0xFF2E7D32).copy(alpha=0.5f),Offset(0f,h-0.8f*h*0.9f),Offset(w,h-0.8f*h*0.9f),1.5f)
        points.forEachIndexed { i, p ->
            val x=i*(barW+gap); val barH=(p.value/100f)*h*0.9f
            val color=when { p.value>=80->Color(0xFF2E7D32); p.value>=50->Color(0xFFE65100); else->Color(0xFFC62828) }
            drawRect(color=color, topLeft=Offset(x,h-barH), size=Size(barW,barH))
        }
    }
}

/**
 * Tarjeta con el resumen de rendimiento de un bloque cognitivo.
 *
 * @param block Estadísticas agregadas del bloque.
 */
@Composable
private fun BlockCard(block: BlockStats) {
    val trendEmoji = when { block.trend>5f->"📈"; block.trend<-5f->"📉"; else->"➡️" }
    val trendText  = when { block.trend>5f->"Mejorando"; block.trend<-5f->"Bajando"; else->"Estable" }
    val trendColor = when { block.trend>5f->Color(0xFF2E7D32); block.trend<-5f->Color(0xFFC62828); else->Color(0xFFE65100) }
    val avgInt     = block.avgPrecision.toInt()

    Card(modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(16.dp)) {
        Column(modifier=Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(block.blockName, style=MaterialTheme.typography.titleMedium)
                Column(horizontalAlignment=Alignment.End) {
                    Text("${precisionEmoji(block.avgPrecision)} $avgInt%", style=MaterialTheme.typography.titleLarge, color=precisionColorFn(block.avgPrecision), fontWeight=FontWeight.Bold)
                    Text("$trendEmoji $trendText", color=trendColor, style=MaterialTheme.typography.labelSmall)
                }
            }
            if (block.points.size >= 2) {
                Spacer(Modifier.height(12.dp))
                val lc=MaterialTheme.colorScheme.onSurface; val gc=MaterialTheme.colorScheme.surfaceVariant
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.width(28.dp).height(100.dp), Arrangement.SpaceBetween) {
                        listOf("100","50","0").forEach { Text(it, style=MaterialTheme.typography.labelSmall, fontSize=9.sp, color=MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    BarChart(block.points, lc, gc, Modifier.weight(1f).height(100.dp))
                }
            } else {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress={block.avgPrecision/100f}, modifier=Modifier.fillMaxWidth().height(8.dp), color=precisionColorFn(block.avgPrecision), trackColor=MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("1 test — realiza más para ver evolución", color=MaterialTheme.colorScheme.onSurfaceVariant, style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * Barra horizontal para mostrar la potencia media de una banda EEG.
 *
 * @param label Nombre de la banda.
 * @param value Valor EEG original entre -1 y 1.
 * @param color Color asociado a la banda.
 */
@Composable
private fun EegBandBar(label: String, value: Float, color: Color) {
    val norm = ((value+1f)/2f).coerceIn(0f,1f)
    Row(verticalAlignment=Alignment.CenterVertically, modifier=Modifier.semantics { contentDescription="$label: ${(norm*100).toInt()} por ciento" }) {
        Text(label, color=color, style=MaterialTheme.typography.bodySmall, modifier=Modifier.width(140.dp))
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(progress={norm}, modifier=Modifier.weight(1f).height(8.dp), color=color, trackColor=MaterialTheme.colorScheme.surfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text("${(norm*100).toInt()}%", color=MaterialTheme.colorScheme.onSurfaceVariant, style=MaterialTheme.typography.bodySmall, modifier=Modifier.width(36.dp))
    }
}
