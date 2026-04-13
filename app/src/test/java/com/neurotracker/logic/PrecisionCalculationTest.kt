package com.neurotracker.logic

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests unitarios puros de las fórmulas de precisión usadas en los ViewModels.
 *
 * Estas fórmulas se usan de forma idéntica en:
 *  - [ResponseInhibitionViewModel.precisionPercent]
 *  - [CognitiveFatigueViewModel.precisionPercent]
 *  - [GlobalCognitiveViewModel.ratio]
 *  - [GlobalCognitiveViewModel.precisionPercent]
 *
 * Se testean aquí de forma aislada en la JVM sin necesidad de contexto Android,
 * para verificar la corrección matemática independientemente del ViewModel que las use.
 *
 * Fórmula general: hits / (hits + errors) × 100.
 */
class PrecisionCalculationTest {

    /**
     * Calcula la precisión porcentual entera dado un número de aciertos y errores.
     * Equivalente a [ResponseInhibitionViewModel.precisionPercent].
     */
    private fun precision(hits: Int, errors: Int): Int {
        val total = hits + errors
        return if (total == 0) 0 else ((hits.toFloat() / total) * 100).toInt()
    }

    /**
     * Calcula la precisión de Go/NoGo incluyendo omisiones en el denominador.
     * Equivalente a la fórmula de [ResponseInhibitionViewModel]:
     * hits / (hits + commissions + omissions) × 100.
     */
    private fun precisionGoNoGo(hits: Int, commissions: Int, omissions: Int): Int {
        val total = hits + commissions + omissions
        return if (total == 0) 0 else ((hits.toFloat() / total) * 100).toInt()
    }

    // ─── Tests de fórmula básica ──────────────────────────────────────────────

    /**
     * Verifica que la precisión es 0 cuando no hay intentos.
     */
    @Test
    fun precision_withNoAttempts_returnsZero() {
        assertEquals(0, precision(0, 0))
    }

    /**
     * Verifica que la precisión es 100 cuando todos los intentos son aciertos.
     */
    @Test
    fun precision_withAllHits_returns100() {
        assertEquals(100, precision(25, 0))
    }

    /**
     * Verifica que la precisión es 0 cuando todos los intentos son errores.
     */
    @Test
    fun precision_withAllErrors_returnsZero() {
        assertEquals(0, precision(0, 25))
    }

    /**
     * Verifica la precisión con 3 aciertos y 1 error → 75%.
     */
    @Test
    fun precision_with3HitsAnd1Error_returns75() {
        assertEquals(75, precision(3, 1))
    }

    /**
     * Verifica la precisión con 1 acierto y 1 error → 50%.
     */
    @Test
    fun precision_with1HitAnd1Error_returns50() {
        assertEquals(50, precision(1, 1))
    }

    /**
     * Verifica la precisión con 19 aciertos y 1 error sobre 20 intentos → 95%.
     */
    @Test
    fun precision_with19HitsAnd1ErrorOf20_returns95() {
        assertEquals(95, precision(19, 1))
    }

    // ─── Tests de fórmula Go/NoGo con omisiones ───────────────────────────────

    /**
     * Verifica que la precisión Go/NoGo es 0 cuando no hay ningún intento.
     */
    @Test
    fun precisionGoNoGo_withNoAttempts_returnsZero() {
        assertEquals(0, precisionGoNoGo(0, 0, 0))
    }

    /**
     * Verifica que la precisión Go/NoGo es 100 con solo aciertos.
     */
    @Test
    fun precisionGoNoGo_withAllHits_returns100() {
        assertEquals(100, precisionGoNoGo(25, 0, 0))
    }

    /**
     * Verifica que pulsar el rojo (comisión) reduce la precisión.
     * Con 8 hits, 1 comisión y 1 omisión sobre 10 → 80%.
     */
    @Test
    fun precisionGoNoGo_with8Hits1Commission1Omission_returns80() {
        assertEquals(80, precisionGoNoGo(8, 1, 1))
    }

    /**
     * Verifica que omitir un GO cuenta igual que cometer un error de comisión
     * a efectos del denominador.
     * Con 9 hits y 1 omisión (sin comisiones) → 90%.
     */
    @Test
    fun precisionGoNoGo_omissionCountsInDenominator() {
        assertEquals(90, precisionGoNoGo(9, 0, 1))
    }

    /**
     * Verifica que no pulsar un NOGO (inhibición correcta) no aparece en la fórmula,
     * es decir, el denominador es solo hits + commissions + omissions, sin NOGO correctos.
     * Con 5 hits, 0 comisiones, 0 omisiones → 100% aunque haya habido NOGO correctos.
     */
    @Test
    fun precisionGoNoGo_correctNogoDoesNotAffectDenominator() {
        assertEquals(100, precisionGoNoGo(5, 0, 0))
    }

    // ─── Tests de casos límite ────────────────────────────────────────────────

    /**
     * Verifica que la precisión nunca supera 100.
     */
    @Test
    fun precision_cannotExceed100() {
        val result = precision(100, 0)
        assertEquals(100, result)
        assert(result <= 100)
    }

    /**
     * Verifica que la precisión nunca es negativa.
     */
    @Test
    fun precision_cannotBeNegative() {
        val result = precision(0, 100)
        assertEquals(0, result)
        assert(result >= 0)
    }

    /**
     * Verifica la truncación entera en lugar de redondeo.
     * 2 / 3 = 0.666… → 66 (no 67).
     */
    @Test
    fun precision_truncatesIntegerInsteadOfRounding() {
        assertEquals(66, precision(2, 1))
    }
}
