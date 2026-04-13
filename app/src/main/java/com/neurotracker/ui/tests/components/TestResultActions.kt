package com.neurotracker.ui.tests.components

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.neurotracker.ui.navigation.Routes

/**
 * Conjunto de botones de acción al finalizar un test cognitivo.
 *
 * Se muestra en el estado FINISHED de todos los tests y proporciona:
 *  - Repetir el test desde cero.
 *  - Volver a la pantalla principal.
 *  - Compartir el resultado por cualquier app del dispositivo.
 *
 * Accesibilidad:
 *  - Todos los botones tienen [contentDescription] explícito para TalkBack.
 *  - Los iconos tienen contentDescription = null (la semántica está en el botón).
 *  - Altura mínima 52dp (supera el mínimo de 48dp de Material Design).
 *
 * Daltonismo:
 *  - Los botones usan texto + icono, nunca solo color para transmitir su función.
 *  - El mensaje de compartir incluye emoji para indicar el nivel de resultado.
 *
 * @param navController Controlador de navegación para volver al inicio.
 * @param testName      Nombre del test, usado en el mensaje de compartir.
 * @param precisionPct  Precisión obtenida (0–100), usada en el mensaje de compartir.
 * @param onRepeat      Callback invocado al pulsar "Repetir test".
 */
@Composable
fun TestResultActions(
    navController: NavController,
    testName: String,
    precisionPct: Int,
    onRepeat: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Botón principal: Repetir ─────────────────────────────────────
        Button(
            onClick  = onRepeat,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .semantics { contentDescription = "Repetir el test de $testName" },
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Repetir test")
        }

        Spacer(Modifier.height(10.dp))

        // ── Fila secundaria: Inicio + Compartir ──────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // Botón: Volver al inicio
            OutlinedButton(
                onClick  = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.MAIN) { inclusive = false }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .semantics { contentDescription = "Volver a la pantalla de inicio" },
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Inicio")
            }

            // Botón: Compartir resultado
            OutlinedButton(
                onClick  = {
                    val resultLabel = when {
                        precisionPct >= 90 -> "🏆 ¡Resultado excepcional!"
                        precisionPct >= 80 -> "✅ ¡Muy buen resultado!"
                        precisionPct >= 60 -> "👍 Buen resultado"
                        else               -> "💪 Seguiré mejorando"
                    }
                    val shareText = buildString {
                        append("🧠 NeuroTracker\n\n")
                        append("Acabo de completar el test de $testName\n")
                        append("Precisión: $precisionPct%\n\n")
                        append(resultLabel)
                        append("\n\n#NeuroTracker #EntrenamientoCognitivo")
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir resultado"))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .semantics { contentDescription = "Compartir mi resultado de $precisionPct por ciento en $testName" },
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Compartir")
            }
        }
    }
}
