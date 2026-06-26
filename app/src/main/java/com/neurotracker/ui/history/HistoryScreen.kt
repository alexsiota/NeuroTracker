package com.neurotracker.ui.history

/**
 * Pantalla de historial de tests.
 *
 * ─── Nota borrado ──────────────────────────────────────────────────────────────
 *
 * Problema 1: el swipe mostraba el fondo rojo en todos los ítems a la vez.
 * Causa: el `rememberSwipeToDismissBoxState` se instanciaba fuera del `forEach`,
 * compartiendo el mismo estado entre todos los ítems.
 * Solución: [SwipeToDeleteRow] usa `key(test.id)` para garantizar que cada
 * ítem tiene su propio estado de swipe aislado.
 *
 * Problema 2: el borrado no ejecutaba el delete.
 * Causa: `confirmValueChange` retornaba `false`, lo que impedía que
 * SwipeToDismissBox completara el dismiss. Se mostraba el fondo pero no se
 * confirmaba la acción.
 * Solución: el callback ahora llama directamente a `onDelete()` y retorna
 * `false` para evitar que el ítem desaparezca visualmente (el ViewModel
 * actualiza la lista y el ítem desaparece por recomposición).
 *
 * ─── Nota EEG "ver más" ───────────────────────────────────────────────────────
 *
 * Se añade un botón "Ver todos" que alterna entre mostrar los últimos 5
 * y mostrar todos los tests con datos EEG.
 *
 * ─── Precisión y tendencia ────────────────────────────────────────────────────
 *
 * Las gráficas de "Precisión por test" y "Tendencia" evalúan TODOS los tests
 * del filtro activo, no solo los 5 con datos EEG. Los 5 últimos son solo
 * para la sección de actividad cerebral EEG.
 */

import android.app.Application
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.viewmodel.RecentTestUi

private val AlphaColor = Color(0xFF2E7D32)
private val BetaColor  = Color(0xFF1565C0)
private val GammaColor = Color(0xFFE65100)
private val ThetaColor = Color(0xFF6A1B9A)

/**
 * Devuelve el indicador textual asociado al nivel de precisión.
 *
 * @param pct Porcentaje de precisión.
 * @return Indicador visual usado en los resúmenes.
 */
private fun precisionEmoji(pct: Int) = when { pct >= 80 -> "✅"; pct >= 50 -> "⚠️"; else -> "❌" }
/**
 * Obtiene el color semántico correspondiente a una precisión.
 *
 * @param pct Porcentaje de precisión.
 * @return Color para representar rendimiento alto, medio o bajo.
 */
private fun precisionColor(pct: Int) = when { pct >= 80 -> Color(0xFF2E7D32); pct >= 50 -> Color(0xFFE65100); else -> Color(0xFFC62828) }

/**
 * Pantalla principal del historial de tests.
 *
 * @param navController Controlador de navegación para volver atrás.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(application))

    var expanded       by remember { mutableStateOf(false) }
    var showChart      by remember { mutableStateOf(false) }
    var testToDelete   by remember { mutableStateOf<RecentTestUi?>(null) }
    // Nota: flag para mostrar todos los tests EEG o solo los últimos 5
    var showAllEeg     by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de tests") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás")
                    }
                },
                actions = {
                    IconButton(
                        onClick  = { showChart = !showChart },
                        modifier = Modifier.semantics {
                            contentDescription = if (showChart) "Ver lista" else "Ver gráfica"
                        }
                    ) {
                        Icon(
                            if (showChart) Icons.AutoMirrored.Filled.List else Icons.Default.BarChart,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())) {

            // Filtros
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = viewModel.activeFilter.value == filter,
                        onClick  = { viewModel.changeFilter(filter) },
                        label    = { Text(filter.label) },
                        modifier = Modifier.semantics {
                            contentDescription = "Filtro ${filter.label}. ${if (viewModel.activeFilter.value == filter) "Activo" else "Inactivo"}"
                        }
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                if (!showChart) {
                    // ── MODO LISTA ────────────────────────────────────────────
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            OutlinedButton(onClick = { expanded = true }) {
                                Text(if (viewModel.order.value == HistoryOrder.DESC) "Más recientes primero" else "Más antiguos primero")
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(text = { Text("Más recientes primero") }, onClick = { viewModel.changeOrder(HistoryOrder.DESC); expanded = false })
                                DropdownMenuItem(text = { Text("Más antiguos primero") },  onClick = { viewModel.changeOrder(HistoryOrder.ASC);  expanded = false })
                            }
                        }
                        Text("${viewModel.tests.value.size} resultados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("Desliza a la izquierda para eliminar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))

                    if (viewModel.tests.value.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (viewModel.activeFilter.value == HistoryFilter.ALL) "No hay tests registrados"
                                else "No hay tests de ${viewModel.activeFilter.value.label}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        viewModel.tests.value.forEach { test ->
                            // Nota: key(test.id) garantiza estado de swipe aislado por ítem
                            key(test.id) {
                                SwipeToDeleteRow(test = test, onDelete = { testToDelete = test })
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                } else {
                    // ── MODO GRÁFICA ──────────────────────────────────────────
                    // NOTA: usa TODOS los tests del filtro activo (no solo los 5 EEG)
                    val tests = viewModel.tests.value

                    if (tests.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (viewModel.activeFilter.value == HistoryFilter.ALL) "No hay datos suficientes"
                                else "No hay tests de ${viewModel.activeFilter.value.label}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val sorted = tests.sortedBy { it.dateTime }
                        val avg    = tests.map { it.precisionPct }.average().toInt()
                        val max    = tests.maxOf { it.precisionPct }
                        val min    = tests.minOf { it.precisionPct }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatSummaryCard("Media",  "${precisionEmoji(avg)}$avg%", precisionColor(avg),  Modifier.weight(1f), "Precisión media: $avg por ciento")
                            StatSummaryCard("Máximo", "${precisionEmoji(max)}$max%", precisionColor(max),  Modifier.weight(1f), "Máximo: $max por ciento")
                            StatSummaryCard("Mínimo", "${precisionEmoji(min)}$min%", precisionColor(min),  Modifier.weight(1f), "Mínimo: $min por ciento")
                        }

                        Spacer(Modifier.height(16.dp))

                        // Precisión por test — TODOS los tests del filtro
                        Text(
                            if (viewModel.activeFilter.value == HistoryFilter.ALL) "Precisión por test"
                            else "Precisión — ${viewModel.activeFilter.value.label}",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("✅ ≥80%  ·  ⚠️ ≥50%  ·  ❌ <50%  ·  Líneas: 80% y 50%", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))

                        val onSurface  = MaterialTheme.colorScheme.onSurface
                        val surfaceVar = MaterialTheme.colorScheme.surfaceVariant

                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.width(32.dp).height(220.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                        listOf("100","80","60","40","20","0").forEach { label ->
                                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                        }
                                    }
                                    ImprovedBarChart(tests = sorted, gridColor = surfaceVar, labelColor = onSurface, modifier = Modifier.weight(1f).height(220.dp))
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Total: ${tests.size} tests", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Tendencia — TODOS los tests del filtro
                        Text("Tendencia de precisión", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))

                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (sorted.size < 2) {
                                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                                        Text("Necesitas al menos 2 tests para ver la tendencia", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                } else {
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    ImprovedLineChart(tests = sorted, lineColor = primaryColor, gridColor = surfaceVar, modifier = Modifier.fillMaxWidth().height(160.dp))
                                    Spacer(Modifier.height(8.dp))
                                    val trend = sorted.last().precisionPct - sorted.first().precisionPct
                                    val (trendEmoji, trendText, trendColor) = when {
                                        trend > 10  -> Triple("📈", "Tu precisión ha mejorado +$trend%", Color(0xFF2E7D32))
                                        trend < -10 -> Triple("📉", "Tu precisión ha bajado ${trend}%",  Color(0xFFC62828))
                                        else        -> Triple("➡️", "Tu precisión se mantiene estable",   MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("$trendEmoji $trendText", style = MaterialTheme.typography.bodySmall, color = trendColor, modifier = Modifier.semantics { contentDescription = "Tendencia: $trendText" })
                                }
                            }
                        }

                        // ── EEG — con opción "Ver más" ────────────────────────
                        val testsWithEeg = sorted.filter { it.hasEegData }
                        if (testsWithEeg.isNotEmpty()) {
                            Spacer(Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Actividad cerebral EEG", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                // Nota: botón para alternar entre últimos 5 y todos
                                if (testsWithEeg.size > 5) {
                                    TextButton(onClick = { showAllEeg = !showAllEeg }) {
                                        Text(if (showAllEeg) "Ver menos" else "Ver todos (${testsWithEeg.size})", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Potencia media de cada banda por test", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                EegLegendItem("α Alpha", AlphaColor)
                                EegLegendItem("β Beta",  BetaColor)
                                EegLegendItem("γ Gamma", GammaColor)
                                EegLegendItem("θ Theta", ThetaColor)
                            }
                            Spacer(Modifier.height(8.dp))

                            // Nota: mostrar últimos 5 o todos según showAllEeg
                            val eegToShow = if (showAllEeg) testsWithEeg else testsWithEeg.takeLast(5)
                            eegToShow.forEach { test ->
                                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                            Text(test.testName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text(test.dateTime, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            EegBandBar("α", test.eegAlpha, AlphaColor, Modifier.weight(1f))
                                            EegBandBar("β", test.eegBeta,  BetaColor,  Modifier.weight(1f))
                                            EegBandBar("γ", test.eegGamma, GammaColor, Modifier.weight(1f))
                                            EegBandBar("θ", test.eegTheta, ThetaColor, Modifier.weight(1f))
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text("Dominante: ${test.dominantBand}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            if (!showAllEeg && testsWithEeg.size > 5) {
                                Text("Mostrando los últimos 5 de ${testsWithEeg.size} con datos EEG", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // Diálogo de confirmación de borrado
    testToDelete?.let { test ->
        AlertDialog(
            onDismissRequest = { testToDelete = null },
            icon  = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar resultado") },
            text  = { Text("¿Seguro que quieres eliminar \"${test.testName}\" del ${test.dateTime}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteTest(test.id); testToDelete = null },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { testToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

// ─── COMPONENTES PRIVADOS ─────────────────────────────────────────────────────

/**
 * Fila deslizable para eliminar un test.
 *
 * Nota: el estado de swipe es local a este composable y está aislado
 * por `key(test.id)` en el llamador. Esto evita que todos los ítems
 * muestren el fondo rojo a la vez.
 *
 * Nota: `confirmValueChange` ahora llama `onDelete()` correctamente.
 * Retorna `false` para que el ítem no desaparezca por animación —
 * desaparece por recomposición cuando el ViewModel actualiza la lista.
 *
 * @param test     Test a mostrar.
 * @param onDelete Callback que abre el diálogo de confirmación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(test: RecentTestUi, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()  // Nota: llamar onDelete correctamente
            }
            false           // Nota: no animar desaparición (el ViewModel lo hace)
        }
    )
    SwipeToDismissBox(
        state                      = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFE53935)
                    else                              -> MaterialTheme.colorScheme.surfaceVariant
                },
                label = "swipe_color"
            )
            val scale by animateFloatAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0.75f,
                label       = "swipe_scale"
            )
            Box(
                modifier         = Modifier.fillMaxSize().background(color, RoundedCornerShape(14.dp)).padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar ${test.testName}", tint = Color.White, modifier = Modifier.scale(scale))
            }
        }
    ) { TestRow(test) }
}

/**
 * Fila que resume un resultado de test dentro del historial.
 *
 * @param test Resultado ya adaptado para presentación.
 */
@Composable
private fun TestRow(test: RecentTestUi) {
    val emoji = precisionEmoji(test.precisionPct)
    Card(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${test.testName}. ${test.dateTime}. Precisión: ${test.precisionPct} por ciento." },
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(test.dateTime, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(test.testName, style = MaterialTheme.typography.bodyMedium)
                }
                Text("$emoji ${test.precisionPct}%", style = MaterialTheme.typography.titleMedium, color = precisionColor(test.precisionPct), fontWeight = FontWeight.Bold)
            }
            if (test.hasEegData) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Estado EEG", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Text("Dominante: ${test.dominantBand}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EegBandBar("α", test.eegAlpha, AlphaColor, Modifier.weight(1f))
                    EegBandBar("β", test.eegBeta,  BetaColor,  Modifier.weight(1f))
                    EegBandBar("γ", test.eegGamma, GammaColor, Modifier.weight(1f))
                    EegBandBar("θ", test.eegTheta, ThetaColor, Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Gráfico de barras para comparar la precisión de varios tests.
 *
 * @param tests Resultados que se dibujan en orden temporal.
 * @param gridColor Color de las líneas de referencia.
 * @param labelColor Color de las etiquetas internas.
 * @param modifier Modificador aplicado al canvas.
 */
@Composable
private fun ImprovedBarChart(tests: List<RecentTestUi>, gridColor: Color, labelColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (tests.isEmpty()) return@Canvas
        val w   = size.width; val h = size.height
        val gap = if (tests.size > 20) 1f else 4f
        val barW = (w - gap * (tests.size - 1)) / tests.size
        listOf(0f,0.2f,0.4f,0.6f,0.8f,1.0f).forEach { level -> drawLine(gridColor, Offset(0f, h - level*h*0.95f), Offset(w, h - level*h*0.95f), 1f) }
        drawLine(Color(0xFF2E7D32).copy(alpha=0.5f), Offset(0f,h-0.8f*h*0.95f), Offset(w,h-0.8f*h*0.95f), 2f)
        drawLine(Color(0xFFE65100).copy(alpha=0.5f), Offset(0f,h-0.5f*h*0.95f), Offset(w,h-0.5f*h*0.95f), 2f)
        tests.forEachIndexed { i, test ->
            val x = i*(barW+gap); val barH = (test.precisionPct/100f)*h*0.95f
            val color = when { test.precisionPct >= 80 -> Color(0xFF2E7D32); test.precisionPct >= 50 -> Color(0xFFE65100); else -> Color(0xFFC62828) }
            drawRect(color=color, topLeft=Offset(x,h-barH), size=Size(barW,barH))
            if (barW > 20f && tests.size <= 15) {
                drawContext.canvas.nativeCanvas.drawText("${test.precisionPct}", x+barW/2, h-barH-4f, android.graphics.Paint().apply { this.color=labelColor.toArgb(); textSize=24f; textAlign=android.graphics.Paint.Align.CENTER })
            }
        }
    }
}

/**
 * Gráfico de línea que muestra la evolución de precisión.
 *
 * @param tests Resultados ordenados temporalmente.
 * @param lineColor Color de la línea principal.
 * @param gridColor Color de la cuadrícula de referencia.
 * @param modifier Modificador aplicado al canvas.
 */
@Composable
private fun ImprovedLineChart(tests: List<RecentTestUi>, lineColor: Color, gridColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (tests.size < 2) return@Canvas
        val w=size.width; val h=size.height; val stepX=w/(tests.size-1).toFloat()
        listOf(0f,0.25f,0.5f,0.75f,1.0f).forEach { level -> drawLine(gridColor, Offset(0f,h-level*h*0.9f-h*0.05f), Offset(w,h-level*h*0.9f-h*0.05f), 1f) }
        drawLine(Color(0xFF2E7D32).copy(alpha=0.4f), Offset(0f,h-0.8f*h*0.9f-h*0.05f), Offset(w,h-0.8f*h*0.9f-h*0.05f), 1.5f)
        val path = Path()
        tests.forEachIndexed { i, test -> val x=i*stepX; val y=h-(test.precisionPct/100f)*h*0.9f-h*0.05f; if (i==0) path.moveTo(x,y) else path.lineTo(x,y) }
        val filled = Path().apply { addPath(path); lineTo((tests.size-1)*stepX,h); lineTo(0f,h); close() }
        drawPath(filled, lineColor.copy(alpha=0.15f)); drawPath(path, lineColor, style=Stroke(width=3f))
        tests.forEachIndexed { i, test -> val x=i*stepX; val y=h-(test.precisionPct/100f)*h*0.9f-h*0.05f; drawCircle(lineColor,5f,Offset(x,y)); drawCircle(Color.White,2f,Offset(x,y)) }
    }
}

/**
 * Barra compacta para representar la potencia normalizada de una banda EEG.
 *
 * @param label Etiqueta corta de la banda.
 * @param value Valor EEG original entre -1 y 1.
 * @param color Color asociado a la banda.
 * @param modifier Modificador aplicado al componente.
 */
@Composable
private fun EegBandBar(label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    val normalized = ((value+1f)/2f).coerceIn(0f,1f)
    Column(modifier=modifier.semantics { contentDescription="Banda $label: ${(normalized*100).toInt()} por ciento" }, horizontalAlignment=Alignment.CenterHorizontally) {
        Text(label, color=color, style=MaterialTheme.typography.labelSmall, fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress={normalized}, modifier=Modifier.fillMaxWidth().height(8.dp), color=color, trackColor=MaterialTheme.colorScheme.surface)
        Spacer(Modifier.height(2.dp))
        Text("${(normalized*100).toInt()}%", color=MaterialTheme.colorScheme.onSurfaceVariant, style=MaterialTheme.typography.labelSmall)
    }
}

/**
 * Elemento de leyenda para identificar una banda EEG.
 *
 * @param label Nombre de la banda.
 * @param color Color usado en barras y leyendas.
 */
@Composable
private fun EegLegendItem(label: String, color: Color) {
    Row(verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape)); Spacer(Modifier.width(4.dp))
        Text(label, color=color, style=MaterialTheme.typography.labelSmall)
    }
}

/**
 * Tarjeta de resumen para métricas agregadas del historial.
 *
 * @param label Texto descriptivo de la métrica.
 * @param value Valor principal mostrado.
 * @param color Color semántico del valor.
 * @param modifier Modificador aplicado a la tarjeta.
 * @param accessibilityDesc Descripción accesible completa.
 */
@Composable
private fun StatSummaryCard(label: String, value: String, color: Color, modifier: Modifier=Modifier, accessibilityDesc: String="$label: $value") {
    Card(modifier=modifier.semantics { contentDescription=accessibilityDesc }, shape=RoundedCornerShape(12.dp), elevation=CardDefaults.cardElevation(2.dp)) {
        Column(modifier=Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment=Alignment.CenterHorizontally) {
            Text(value, color=color, style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold, textAlign=androidx.compose.ui.text.style.TextAlign.Center, maxLines=2)
            Text(label, color=MaterialTheme.colorScheme.onSurfaceVariant, style=MaterialTheme.typography.bodySmall, textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
