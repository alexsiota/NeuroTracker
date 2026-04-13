package com.neurotracker.ui.tests.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Tarjeta interactiva que representa un test individual en el selector de tests.
 *
 * Usada dentro de [ExpandableTestBlock] para listar los tests disponibles
 * en cada bloque cognitivo. Puede aparecer en estado habilitado (test disponible)
 * o deshabilitado (próximamente).
 *
 * Accesibilidad:
 *  - contentDescription completo leído por TalkBack con nombre, descripción y estado.
 *  - Altura fija de 100dp, muy por encima del mínimo de toque de 48dp.
 *  - Clickable solo cuando [enabled] = true.
 *  - Estado deshabilitado indicado semánticamente ("No disponible todavía").
 *
 * Usabilidad:
 *  - Reducción visual de opacidad al 40% cuando está deshabilitada
 *    (sin depender solo del color para indicar el estado).
 *  - Fondo diferenciado entre habilitado [surface] y deshabilitado [surfaceVariant].
 *
 * @param title       Nombre del test (ej: "Test de Reacción Simple").
 * @param description Descripción breve de qué mide el test.
 * @param onClick     Callback invocado al pulsar la tarjeta (solo si [enabled] = true).
 * @param enabled     Si false, la tarjeta aparece deshabilitada visualmente y semánticamente.
 */
@Composable
fun TestCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val alpha = if (enabled) 1f else 0.4f

    val accessibilityDesc = if (enabled)
        "$title. $description. Pulsa para iniciar."
    else
        "$title. No disponible todavía."

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(enabled = enabled) { onClick() }
            .semantics { contentDescription = accessibilityDesc },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier          = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
        }
    }
}
