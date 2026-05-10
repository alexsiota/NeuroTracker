package com.neurotracker.ui.main

import android.app.Application
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.neurotracker.R
import com.neurotracker.data.profile.ProfilePhotoManager
import com.neurotracker.data.session.SessionEvents
import com.neurotracker.data.session.SessionManager
import com.neurotracker.ui.navigation.Routes
import com.neurotracker.viewmodel.BadgeUi
import com.neurotracker.viewmodel.RecentTestUi

/**
 * Devuelve el indicador textual asociado al porcentaje de precisión.
 *
 * @param pct Porcentaje de precisión.
 * @return Indicador visual usado en tarjetas y resúmenes.
 */
private fun precisionEmoji(pct: Int) = when { pct >= 80 -> "✅"; pct >= 50 -> "⚠️"; else -> "❌" }

/**
 * Pantalla principal de la aplicación (Home/Dashboard).
 *
 * Hub central de NeuroTracker. Muestra el estado actual del usuario
 * y proporciona acceso directo a todas las funcionalidades.
 *
 * La recarga de datos tras un cambio de email se gestiona de forma
 * instantánea en [MainViewModel] a través de [SessionEvents.emailChanged].
 *
 * La foto de perfil del TopBar se actualiza al instante escuchando
 * [SessionEvents.photoChanged], que [com.neurotracker.ui.profile.ProfileViewModel]
 * emite cada vez que el usuario guarda o elimina su foto. Para forzar que
 * Coil descarte la caché y lea el archivo actualizado del disco, se usa
 * un [ImageRequest] con [ImageRequest.Builder.memoryCacheKey] único basado
 * en un contador que se incrementa con cada evento recibido.
 *
 * @param navController Controlador de navegación para acceder a otras pantallas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val context     = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(application))

    val avgPrecision = viewModel.avgPrecisionAll.value
    val tests        = viewModel.recentTests.value

    val sessionManager = remember { SessionManager(context) }
    val photoUri       = remember { mutableStateOf<Uri?>(null) }
    val photoCacheKey  = remember { mutableStateOf(0) }

    // Carga inicial de la foto
    LaunchedEffect(Unit) {
        val email = sessionManager.getUserEmail()
        if (!email.isNullOrBlank()) {
            photoUri.value = ProfilePhotoManager.getPhotoUri(context, email)
        }
    }

    // Actualiza la foto al instante cuando ProfileViewModel emite notifyPhotoChanged()
    LaunchedEffect(Unit) {
        SessionEvents.photoChanged.collect {
            val email = sessionManager.getUserEmail()
            if (!email.isNullOrBlank()) {
                photoUri.value     = ProfilePhotoManager.getPhotoUri(context, email)
                photoCacheKey.value++
            }
        }
    }

    var selectedBadge by remember { mutableStateOf<BadgeUi?>(null) }
    val activity = context as? android.app.Activity
    BackHandler { activity?.finishAffinity() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painterResource(id = R.drawable.ic_neurotracker_launcher_logo_pagprincipal),
                            contentDescription = "Logo de NeuroTracker",
                            modifier           = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("NeuroTracker", style = MaterialTheme.typography.titleMedium)
                            Text("Hola, ${viewModel.userName.value.ifBlank { "Usuario" }}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp).size(36.dp).clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { navController.navigate(Routes.PROFILE) }
                            .semantics { contentDescription = "Ir a mi perfil" },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri.value != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(photoUri.value)
                                    .memoryCacheKey("profile_${photoCacheKey.value}")
                                    .diskCacheKey("profile_${photoCacheKey.value}")
                                    .build(),
                                contentDescription = "Foto de perfil",
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Box(
                                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(modifier = Modifier.weight(1f).height(90.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text("Precisión media", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (viewModel.totalTests.value > 0) "${precisionEmoji(avgPrecision)} $avgPrecision%" else "—",
                                style      = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    viewModel.totalTests.value == 0 -> MaterialTheme.colorScheme.onSurface
                                    avgPrecision >= 80              -> Color(0xFF2E7D32)
                                    avgPrecision >= 50              -> Color(0xFFE65100)
                                    else                            -> Color(0xFFC62828)
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = if (viewModel.totalTests.value > 0)
                                        "Precisión media: $avgPrecision por ciento"
                                    else "Sin datos de precisión todavía"
                                }
                            )
                        }
                    }
                    Card(modifier = Modifier.weight(1f).height(90.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text("Racha actual", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics { contentDescription = "Racha actual: ${viewModel.streakDays.value} días" }) {
                                Text(if (viewModel.streakDays.value > 0) "🔥" else "💤", fontSize = 20.sp)
                                Spacer(Modifier.width(6.dp))
                                Text("${viewModel.streakDays.value} días", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (viewModel.streakDays.value >= 3) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                ProgressCard("Progreso diario",  viewModel.dailyTests.value,  viewModel.dailyGoal,  viewModel.dailyCountdown.value,  "🌙 Se reinicia en:", "¡Empieza tu primera sesión del día!",      "✅ ¡Objetivo diario completado!",  { r -> "Te faltan $r tests para el objetivo de hoy" })
                Spacer(Modifier.height(12.dp))
                ProgressCard("Progreso semanal", viewModel.weeklyTests.value, viewModel.weeklyGoal, viewModel.weeklyCountdown.value, "🔄 Se reinicia en:", "¡Empieza tu primera sesión de la semana!", "✅ ¡Objetivo semanal completado!", { r -> "Te faltan $r tests para el objetivo" })
                Spacer(Modifier.height(12.dp))

                if (viewModel.recommendation.value.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Text("💡", fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Recomendación del día", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                Spacer(Modifier.height(2.dp))
                                Text(viewModel.recommendation.value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Card(
                    modifier = Modifier.fillMaxWidth().height(90.dp).semantics { contentDescription = "Nuevo Test. Pulsa para elegir un test cognitivo." },
                    shape    = RoundedCornerShape(18.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    onClick  = { navController.navigate(Routes.CHOOSE_TEST) }
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Nuevo Test",              style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                            Text("Elige el test cognitivo", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Tests recientes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text("Ver todos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { navController.navigate(Routes.HISTORY) }.semantics { contentDescription = "Ver historial completo" })
                        }
                        Spacer(Modifier.height(12.dp))
                        if (tests.isEmpty()) {
                            Text("Aún no has realizado ningún test", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            tests.forEach { test -> RecentTestRow(test); Spacer(Modifier.height(8.dp)) }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureCard("Historial",    "Mis resultados",         Icons.Default.DateRange, Modifier.weight(1f)) { navController.navigate(Routes.HISTORY) }
                    FeatureCard("Estadísticas", "Mi evolución cognitiva", Icons.Default.BarChart,  Modifier.weight(1f)) { navController.navigate(Routes.STATISTICS) }
                }
                Spacer(Modifier.height(12.dp))
                FeatureCardWide("Simulación EEG", "Visualiza tus ondas cerebrales en tiempo real", Icons.Default.Timeline) { navController.navigate(Routes.EEG) }

                Spacer(Modifier.height(12.dp))

                val unlockedCount = viewModel.badges.value.count { it.unlocked }
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("🏆 Logros", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Text("$unlockedCount/${viewModel.badges.value.size} desbloqueados", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Pulsa cualquier logro para ver cómo conseguirlo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        viewModel.badges.value.chunked(4).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { badge -> BadgeItem(badge, Modifier.weight(1f)) { selectedBadge = badge } }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    selectedBadge?.let { badge ->
        AlertDialog(
            onDismissRequest = { selectedBadge = null },
            icon  = { Text(badge.emoji, fontSize = 40.sp) },
            title = { Text(badge.title, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) },
            text  = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(shape = RoundedCornerShape(8.dp), color = if (badge.unlocked) Color(0xFF1B5E20) else MaterialTheme.colorScheme.surfaceVariant) {
                        Text(if (badge.unlocked) "✅ Logro desbloqueado" else "🔒 Bloqueado", color = if (badge.unlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(badge.description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    if (!badge.unlocked && badge.progressText.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Progreso", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(badge.progressText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(progress = { badge.progressFraction }, modifier = Modifier.fillMaxWidth().height(8.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    }
                    if (badge.unlocked) {
                        Spacer(Modifier.height(12.dp))
                        Text("¡Enhorabuena! Ya has conseguido este logro.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedBadge = null }) { Text("Cerrar") } }
        )
    }

    viewModel.newlyUnlockedBadge.value?.let { badge ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissBadgeNotification() },
            icon  = { Text(badge.emoji, fontSize = 48.sp) },
            title = { Text("🎉 ¡Logro desbloqueado!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text  = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(badge.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(badge.description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            },
            confirmButton = { Button(onClick = { viewModel.dismissBadgeNotification() }, modifier = Modifier.fillMaxWidth()) { Text("¡Genial!") } }
        )
    }
}

/**
 * Tarjeta de progreso reutilizable para objetivos diarios y semanales.
 *
 * @param title           Título de la tarjeta.
 * @param current         Número de tests realizados actualmente.
 * @param goal            Objetivo total de tests.
 * @param countdown       Tiempo restante hasta el reinicio formateado.
 * @param countdownLabel  Etiqueta del contador de reinicio.
 * @param messageEmpty    Mensaje cuando no hay tests realizados.
 * @param messageComplete Mensaje cuando se alcanza el objetivo.
 * @param messagePending  Función que genera el mensaje cuando falta completar.
 */
@Composable
private fun ProgressCard(title: String, current: Int, goal: Int, countdown: String, countdownLabel: String, messageEmpty: String, messageComplete: String, messagePending: (Int) -> String) {
    val msg = when { current == 0 -> messageEmpty; current >= goal -> messageComplete; else -> messagePending(goal - current) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("$current/$goal tests", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { (current.toFloat() / goal).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp), color = if (current >= goal) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(countdownLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(countdown, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }
    }
}

/**
 * Tarjeta de acceso rápido con icono, título y subtítulo.
 *
 * @param title    Título de la función.
 * @param subtitle Subtítulo descriptivo.
 * @param icon     Icono representativo.
 * @param modifier Modifier para tamaño y posición.
 * @param onClick  Callback al pulsar la tarjeta.
 */
@Composable
private fun FeatureCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(100.dp).semantics { contentDescription = "$title. $subtitle." }, shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp), onClick = onClick) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Column { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/**
 * Tarjeta de acceso rápido ancha con icono, título y subtítulo.
 *
 * @param title    Título de la función.
 * @param subtitle Subtítulo descriptivo.
 * @param icon     Icono representativo.
 * @param onClick  Callback al pulsar la tarjeta.
 */
@Composable
private fun FeatureCardWide(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().height(80.dp).semantics { contentDescription = "$title. $subtitle." }, shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp), onClick = onClick) {
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Ítem individual de logro con emoji y título.
 *
 * @param badge    Datos del logro.
 * @param modifier Modifier para tamaño y posición.
 * @param onClick  Callback al pulsar el logro.
 */
@Composable
private fun BadgeItem(badge: BadgeUi, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable { onClick() }.semantics { contentDescription = if (badge.unlocked) "Logro desbloqueado: ${badge.title}" else "Logro bloqueado: ${badge.title}. ${badge.progressText}" }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(56.dp).background(if (badge.unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Text(if (badge.unlocked) badge.emoji else "🔒", fontSize = if (badge.unlocked) 26.sp else 20.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(badge.title, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = if (badge.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}

/**
 * Fila que muestra un test reciente en formato tarjeta compacta.
 *
 * @param test Datos del test a mostrar.
 */
@Composable
private fun RecentTestRow(test: RecentTestUi) {
    val emoji = precisionEmoji(test.precisionPct)
    Card(modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${test.testName}. ${test.dateTime}. Precisión: ${test.precisionPct} por ciento." }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(test.testName, style = MaterialTheme.typography.bodyMedium)
                Text(test.dateTime, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$emoji ${test.precisionPct}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = when { test.precisionPct >= 80 -> Color(0xFF2E7D32); test.precisionPct >= 50 -> Color(0xFFE65100); else -> Color(0xFFC62828) })
        }
    }
}
