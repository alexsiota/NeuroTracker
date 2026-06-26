package com.neurotracker.ui.achievements

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

/**
 * Pantalla que muestra el progreso de logros del usuario autenticado.
 *
 * Agrupa los logros por categoría, muestra el avance global y permite volver
 * a la pantalla anterior mediante el controlador de navegación.
 *
 * @param navController Controlador usado para gestionar la navegación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: AchievementsViewModel = viewModel(
        factory = AchievementsViewModelFactory(application)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logros") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás")
                    }
                }
            )
        }
    ) { padding ->

        if (viewModel.isLoading.value) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val allAchievements = viewModel.achievements.value
            val unlocked        = allAchievements.count { it.unlocked }
            val total           = allAchievements.size

            // Resumen
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier          = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏆", fontSize = 40.sp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "$unlocked de $total logros desbloqueados",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress   = { if (total == 0) 0f else unlocked.toFloat() / total },
                            modifier   = Modifier.fillMaxWidth().height(8.dp),
                            color      = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${if (total == 0) 0 else unlocked * 100 / total}% completado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Logros agrupados por categoría
            val byCategory = allAchievements.groupBy { it.category }
            byCategory.forEach { (category, list) ->
                Text(
                    category.uppercase(),
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
                list.forEach { achievement ->
                    AchievementCard(achievement)
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Tarjeta visual de un logro individual.
 *
 * Refleja si el logro está bloqueado o desbloqueado y expone una descripción
 * accesible para lectores de pantalla.
 *
 * @param achievement Logro que se debe representar.
 */
@Composable
private fun AchievementCard(achievement: Achievement) {
    val containerColor = if (achievement.unlocked)
        achievement.unlockedColor
    else
        MaterialTheme.colorScheme.surfaceVariant

    val titleColor = if (achievement.unlocked) Color.White
                     else MaterialTheme.colorScheme.onSurfaceVariant

    val descColor = if (achievement.unlocked) Color.White.copy(alpha = 0.92f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    val accessDesc = if (achievement.unlocked)
        "Logro desbloqueado: ${achievement.title}. ${achievement.description}"
    else
        "Logro bloqueado: ${achievement.title}. ${achievement.description}"

    Card(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = accessDesc },
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Emoji como indicador primario (independiente del color)
            Text(
                text     = if (achievement.unlocked) achievement.emoji else "🔒",
                fontSize = 36.sp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    achievement.title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = titleColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = descColor
                )
                Spacer(Modifier.height(8.dp))
                // Badge de estado — nunca solo color como indicador
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (achievement.unlocked)
                        Color.Black.copy(alpha = 0.25f)
                    else
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Text(
                        text     = if (achievement.unlocked) "✓ Desbloqueado" else "🔒 Bloqueado",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = if (achievement.unlocked) Color.White
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
