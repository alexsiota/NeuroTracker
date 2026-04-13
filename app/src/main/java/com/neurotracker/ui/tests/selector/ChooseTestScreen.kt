package com.neurotracker.ui.tests.selector

/**
 * Pantalla de selección de test cognitivo.
 *
 * Muestra los 22 tests organizados en 6 bloques cognitivos mediante
 * [ExpandableTestBlock]. El usuario expande el bloque deseado y pulsa
 * el test para navegar a su pantalla correspondiente.
 *
 * Bloques disponibles:
 *  1. Atención y concentración (5 tests)
 *  2. Memoria (5 tests)
 *  3. Velocidad de procesamiento (3 tests)
 *  4. Funciones ejecutivas (3 tests)
 *  5. Coordinación y percepción (3 tests)
 *  6. Tests compuestos (3 tests)
 *
 * Navegación: cada [TestItem.id] corresponde a una ruta en [Routes].
 * Accesibilidad: cabeceras de bloque con contentDescription dinámico.
 */

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.neurotracker.ui.navigation.Routes
import com.neurotracker.ui.tests.components.ExpandableTestBlock
import com.neurotracker.ui.tests.components.TestItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseTestScreen(navController: NavController) {

    // ---- Bloque 1: Atención y concentración ----
    val attentionTests = listOf(
        TestItem(
            id          = Routes.REACTION_SIMPLE,
            title       = "Test de Reacción Simple",
            description = "Pulsa cuando aparezca el círculo. Mide tu tiempo de reacción."
        ),
        TestItem(
            id          = Routes.ATTENTION_SUSTAINED,
            title       = "Atención Sostenida",
            description = "Mantén la concentración durante 60 segundos pulsando solo el estímulo correcto."
        ),
        TestItem(
            id          = Routes.ATTENTION_SELECTIVE,
            title       = "Atención Selectiva",
            description = "Identifica el estímulo objetivo ignorando los distractores."
        ),
        TestItem(
            id          = Routes.STROOP,
            title       = "Test de Stroop",
            description = "¿El color y la palabra coinciden? Mide tu control inhibitorio."
        ),
        TestItem(
            id          = Routes.VISUAL_CANCELLATION,
            title       = "Cancelación Visual",
            description = "Pulsa solo los círculos en la cuadrícula. Evita los cuadrados."
        )
    )

    // ---- Bloque 2: Memoria ----
    val memoryTests = listOf(
        TestItem(
            id          = Routes.DIGIT_SPAN,
            title       = "Memoria de Dígitos",
            description = "Repite secuencias numéricas en orden directo. La secuencia crece con cada acierto."
        ),
        TestItem(
            id          = Routes.N_BACK,
            title       = "N-Back",
            description = "Recuerda si el estímulo actual coincide con el de N posiciones atrás."
        ),
        TestItem(
            id          = Routes.VISUAL_MEMORY,
            title       = "Memoria Visual",
            description = "Recuerda las posiciones de una cuadrícula. Similar al Simon Says."
        ),
        TestItem(
            id          = Routes.OBJECT_MEMORY,
            title       = "Memoria de Objetos",
            description = "Memoriza objetos y recuerda cuáles estaban tras ocultarlos."
        ),
        TestItem(
            id          = Routes.ASSOCIATIVE_MEMORY,
            title       = "Memoria Asociativa",
            description = "Encuentra parejas de imágenes o palabras relacionadas."
        )
    )

    // ---- Bloque 3: Velocidad de procesamiento ----
    val speedTests = listOf(
        TestItem(
            id          = Routes.QUICK_COMPARISON,
            title       = "Comparación Rápida",
            description = "¿Son iguales o diferentes? Tiempo + precisión."
        ),
        TestItem(
            id          = Routes.SEQUENCE_ORDERING,
            title       = "Ordenar Secuencias",
            description = "Ordena números o letras lo más rápido posible."
        ),
        TestItem(
            id          = Routes.DECISION_TIME,
            title       = "Tiempo de Decisión",
            description = "Elige entre varias opciones bajo presión de tiempo."
        )
    )

    // ---- Bloque 4: Funciones ejecutivas ----
    val executiveTests = listOf(
        TestItem(
            id          = Routes.TASK_SWITCHING,
            title       = "Cambio de Tarea",
            description = "Alterna entre reglas distintas. Mide tu flexibilidad cognitiva."
        ),
        TestItem(
            id          = Routes.RESPONSE_INHIBITION,
            title       = "Inhibición de Respuesta",
            description = "Pulsa siempre excepto cuando aparezca el estímulo prohibido."
        ),
        TestItem(
            id          = Routes.PLANNING,
            title       = "Planificación",
            description = "Resuelve un pequeño puzzle en el mínimo de movimientos."
        )
    )

    // ---- Bloque 5: Coordinación y percepción ----
    val coordinationTests = listOf(
        TestItem(
            id          = Routes.OBJECT_TRACKING,
            title       = "Seguimiento de Objetos",
            description = "Sigue 2–4 objetos en movimiento simultáneamente."
        ),
        TestItem(
            id          = Routes.VISUOMOTOR,
            title       = "Coordinación Visomotora",
            description = "Toca objetivos móviles con la mayor precisión posible."
        ),
        TestItem(
            id          = Routes.SPATIAL_PERCEPTION,
            title       = "Percepción Espacial",
            description = "Rotación mental de figuras geométricas."
        )
    )

    // ---- Bloque 6: Tests compuestos ----
    val compositeTests = listOf(
        TestItem(
            id          = Routes.GLOBAL_COGNITIVE,
            title       = "Test Cognitivo Global",
            description = "Mini-pruebas encadenadas que generan un score cognitivo global."
        ),
        TestItem(
            id          = Routes.COGNITIVE_FATIGUE,
            title       = "Test de Fatiga Cognitiva",
            description = "Compara tu rendimiento al inicio y al final de la sesión."
        ),
        TestItem(
            id          = Routes.DUAL_TASK,
            title       = "Test de Doble Tarea",
            description = "Memoria + reacción simultánea."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar Test") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            ExpandableTestBlock(
                title       = "🧠 Atención y concentración",
                tests       = attentionTests,
                onTestClick = { test -> navController.navigate(test.id) }
            )
            ExpandableTestBlock(
                title       = "🧩 Memoria",
                tests       = memoryTests,
                onTestClick = { test -> navController.navigate(test.id) }
            )
            ExpandableTestBlock(
                title       = "⚡ Velocidad de procesamiento",
                tests       = speedTests,
                onTestClick = { test -> navController.navigate(test.id) }
            )
            ExpandableTestBlock(
                title       = "🔄 Funciones ejecutivas",
                tests       = executiveTests,
                onTestClick = { test -> navController.navigate(test.id) }
            )
            ExpandableTestBlock(
                title       = "🎮 Coordinación y percepción",
                tests       = coordinationTests,
                onTestClick = { test -> navController.navigate(test.id) }
            )
            ExpandableTestBlock(
                title       = "🧪 Tests compuestos",
                tests       = compositeTests,
                onTestClick = { test -> navController.navigate(test.id) }
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}