package com.neurotracker.ui.tests.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Componentes reutilizables para pantallas de tests cognitivos.
 *
 * Este archivo centraliza elementos visuales comunes utilizados en todos los tests,
 * garantizando consistencia en diseño, accesibilidad y usabilidad.
 *
 * Principios aplicados:
 *  - Accesibilidad: todos los elementos relevantes incluyen contentDescription.
 *  - Daltonismo: el color nunca es el único indicador de estado.
 *  - Usabilidad: tamaños adecuados de interacción y jerarquía visual clara.
 *  - Reutilización: evita duplicación de código en cada pantalla de test.
 *
 * Patrón de uso:
 *  - IdleScreen → estado inicial del test
 *  - ScoreChip → métricas en tiempo real
 *  - ResultCard + ResultRow → resultados finales
 *  - FeedbackText → interpretación cualitativa
 *  - TestProgress → avance del test
 */

/**
 * Pantalla inicial de un test cognitivo.
 *
 * Muestra:
 *  - Título del test
 *  - Descripción/instrucciones
 *  - Botón de inicio
 *
 * @param title Nombre del test.
 * @param description Instrucciones claras y comprensibles.
 * @param onStart Callback al pulsar el botón de inicio.
 */
@Composable
fun IdleScreen(
    title: String,
    description: String,
    onStart: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.padding(24.dp)
    ) {
        Text(
            text       = title,
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text      = description,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style     = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick  = onStart,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(52.dp)
                .semantics { contentDescription = "Comenzar el test $title" }
        ) {
            Text("Comenzar", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// RESULTADOS
// ─────────────────────────────────────────────────────────────

/**
 * Contenedor visual para resultados de test.
 *
 * Proporciona:
 *  - Fondo diferenciado
 *  - Padding consistente
 *  - Espaciado uniforme entre elementos
 *
 * Uso:
 *  - Agrupar múltiples filas de resultados (ResultRow)
 *
 * @param content Contenido interno del card.
 */
@Composable
fun ResultCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier            = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content             = content
        )
    }
}

/**
 * Fila individual de resultado.
 *
 * Muestra una métrica con su valor asociado.
 *
 * Accesibilidad:
 *  - Se expone como "label: value" para lectores de pantalla.
 *
 * @param label Descripción de la métrica.
 * @param value Valor mostrado.
 * @param color Color semántico del valor.
 */
@Composable
fun ResultRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label: $value" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text       = value,
            color      = color,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Indicador de puntuación en tiempo real.
 *
 * Representa métricas dinámicas durante el test (ej: aciertos, errores).
 *
 * Características:
 *  - Valor destacado visualmente
 *  - Etiqueta descriptiva debajo
 *
 * Accesibilidad:
 *  - Lectura completa: "label: value"
 *
 * @param label Nombre de la métrica.
 * @param value Valor numérico.
 * @param color Color de refuerzo visual.
 */
@Composable
fun ScoreChip(
    label: String,
    value: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.semantics { contentDescription = "$label: $value" }
    ) {
        Text(
            text       = value.toString(),
            color      = color,
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Tarjeta de feedback cualitativo.
 *
 * Muestra la interpretación del resultado del test.
 *
 * Características:
 *  - Fondo diferenciado para destacar el mensaje
 *  - Texto centrado y legible
 *
 * Accesibilidad:
 *  - Prefijo "Resultado:" para claridad en lectores de pantalla
 *
 * @param text Mensaje de feedback.
 */
@Composable
fun FeedbackText(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Text(
            text      = text,
            color     = MaterialTheme.colorScheme.onSecondaryContainer,
            style     = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier  = Modifier
                .padding(16.dp)
                .semantics { contentDescription = "Resultado: $text" }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// PROGRESO
// ─────────────────────────────────────────────────────────────

/**
 * Indicador de progreso del test.
 *
 * Muestra:
 *  - Texto de progreso (ej: "3 / 10")
 *  - Barra de progreso visual
 *
 * Accesibilidad:
 *  - Lectura completa: "Ronda X de Y"
 *
 * @param current Ronda actual.
 * @param total Total de rondas.
 */
@Composable
fun TestProgress(current: Int, total: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.semantics { contentDescription = "Ronda $current de $total" }
    ) {
        Text(
            text  = "$current / $total",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress   = { current.toFloat() / total },
            modifier   = Modifier.fillMaxWidth(),
            color      = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}