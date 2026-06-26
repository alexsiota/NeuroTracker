package com.neurotracker.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurotracker.data.session.SessionEvents

/**
 * Diálogo global de logro desbloqueado.
 *
 * Observa [SessionEvents.badgeUnlocked] y muestra un [AlertDialog] en el momento
 * exacto en que se detecta el desbloqueo, independientemente de la pantalla activa.
 * Se coloca una sola vez en [com.neurotracker.MainActivity], fuera del NavHost,
 * por lo que es visible en cualquier pantalla sin modificar cada composable.
 *
 * El diálogo desaparece al pulsar "¡Genial!" o al tocar fuera de él.
 * Ambas acciones llaman a [SessionEvents.clearBadgeUnlocked] para resetear el estado.
 */
@Composable
fun BadgeUnlockOverlay() {
    val currentBadge = SessionEvents.badgeUnlocked.collectAsState().value ?: return

    AlertDialog(
        onDismissRequest = { SessionEvents.clearBadgeUnlocked() },
        icon  = { Text(currentBadge.emoji, fontSize = 48.sp) },
        title = {
            Text(
                "🎉 ¡Logro desbloqueado!",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        currentBadge.title,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    currentBadge.description,
                    style     = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { SessionEvents.clearBadgeUnlocked() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("¡Genial!")
            }
        }
    )
}
