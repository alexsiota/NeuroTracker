package com.neurotracker.ui.eeg

/**
 * Componente visual reutilizable para la representación de señales EEG.
 *
 * Este composable utiliza un Canvas de Jetpack Compose para dibujar una onda
 * continua a partir de una serie de valores normalizados.
 *
 * Funcionalidad:
 * - Renderiza una curva que representa la señal EEG en tiempo real.
 * - Permite configurar el color y el grosor de la línea.
 * - Opcionalmente, rellena el área bajo la curva para mejorar la legibilidad.
 * - Incluye una línea central de referencia para facilitar la interpretación visual.
 *
 * Requisitos:
 * - Los valores recibidos en [points] deben estar normalizados en el rango [-1, 1].
 *
 * Reutilización:
 * - Puede emplearse en distintas pantallas de la aplicación donde sea necesario
 *   representar actividad EEG de forma gráfica.
 *
 * @param points Lista de valores de la señal EEG normalizados.
 * @param color Color principal utilizado para dibujar la onda.
 * @param modifier Modificador de Compose para ajustar tamaño y disposición.
 * @param strokeWidth Grosor de la línea de la señal.
 * @param filled Indica si debe rellenarse el área bajo la curva.
 */

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Dibuja una señal EEG en un Canvas de Jetpack Compose.
 *
 * Esta función transforma una colección de puntos normalizados en una curva continua,
 * adaptándola automáticamente al tamaño disponible del contenedor.
 *
 * Además, permite representar la señal en dos modos:
 * - Solo línea, para una vista más limpia.
 * - Línea con relleno, para una representación más visual e intuitiva.
 *
 * También añade una línea horizontal central que actúa como referencia del eje base.
 *
 * @param points Lista de muestras de la señal EEG normalizadas en el rango [-1, 1].
 * @param color Color utilizado para la línea y el relleno.
 * @param modifier Modificador de Compose aplicado al Canvas.
 * @param strokeWidth Grosor de la línea principal.
 * @param filled Si es true, dibuja una zona rellena bajo la curva.
 */
@Composable
fun EegWaveCanvas(
    points: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 3f,
    filled: Boolean = false
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (points.size < 2) return@Canvas

        val w = size.width
        val h = size.height
        val midY = h / 2f
        val stepX = w / (points.size - 1).coerceAtLeast(1).toFloat()

        val path = Path()
        points.forEachIndexed { index, value ->
            val x = index * stepX
            // Conversión de un valor normalizado [-1, 1] a coordenada vertical del Canvas.
            val y = midY - value * (midY * 0.85f)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth)
        )

        if (filled) {
            val filledPath = Path().apply {
                addPath(path)
                lineTo(w, midY)
                lineTo(0f, midY)
                close()
            }
            drawPath(
                path = filledPath,
                color = color.copy(alpha = 0.12f)
            )
        }

        drawLine(
            color = color.copy(alpha = 0.2f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1f
        )
    }
}