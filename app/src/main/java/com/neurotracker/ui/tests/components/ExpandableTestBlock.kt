package com.neurotracker.ui.tests.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Modelo de datos UI para representar un test en el selector de tests.
 *
 * No es una entidad de base de datos, solo se usa en la capa de presentación
 * para construir la lista de tests en [ExpandableTestBlock].
 *
 * @param id          Identificador de ruta de navegación. Corresponde a una constante de [Routes].
 * @param title       Nombre visible del test en la UI.
 * @param description Descripción breve de qué capacidad cognitiva mide el test.
 */
data class TestItem(
    val id: String,
    val title: String,
    val description: String
)

/**
 * Bloque desplegable que agrupa tests cognitivos por categoría.
 *
 * Cada bloque tiene un título de categoría y una lista de [TestItem] que
 * se muestra u oculta al pulsar la cabecera. Se usa en [ChooseTestScreen]
 * para organizar los 22 tests en 6 bloques cognitivos.
 *
 * Accesibilidad:
 *  - La cabecera completa tiene contentDescription dinámico que indica
 *    si el bloque está expandido o contraído para TalkBack.
 *  - El icono de flecha tiene contentDescription ("Expandir" / "Contraer").
 *  - Altura mínima de la cabecera: 48dp (mínimo Material Design para zonas de toque).
 *
 * Usabilidad:
 *  - Animación suave de aparición/desaparición del contenido.
 *  - Badge "Próximamente" visible cuando [comingSoon] = true.
 *  - [HorizontalDivider] separa visualmente los bloques.
 *
 * @param title       Título del bloque cognitivo (ej: "🧠 Atención y concentración").
 * @param tests       Lista de [TestItem] pertenecientes a este bloque.
 * @param onTestClick Callback invocado al seleccionar un test habilitado.
 * @param comingSoon  Si true, los tests se muestran deshabilitados con badge "Próximamente".
 */
@Composable
fun ExpandableTestBlock(
    title: String,
    tests: List<TestItem>,
    onTestClick: (TestItem) -> Unit,
    comingSoon: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Cabecera del bloque ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable { expanded = !expanded }
                .semantics {
                    contentDescription = if (expanded)
                        "Contraer bloque $title"
                    else
                        "Expandir bloque $title"
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text     = title,
                style    = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )

            // Badge "Próximamente" — solo en bloques no implementados
            if (comingSoon) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text     = "Próximamente",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }

            Icon(
                imageVector        = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Contraer" else "Expandir"
            )
        }

        // ── Lista de tests desplegable ───────────────────────────────────
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tests.forEach { test ->
                    TestCard(
                        title       = test.title,
                        description = test.description,
                        onClick     = { if (!comingSoon) onTestClick(test) },
                        enabled     = !comingSoon
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}
