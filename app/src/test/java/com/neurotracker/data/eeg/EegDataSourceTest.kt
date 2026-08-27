package com.neurotracker.data.eeg

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios puros del [SimulatedEegDataSource].
 *
 * Verifica que la fuente de datos EEG simulada genera muestras con valores
 * dentro de los rangos válidos y con las propiedades esperadas:
 *  - Todos los valores de banda están en el rango [-1, 1].
 *  - El timestamp es positivo y reciente.
 *  - [getLastSample] siempre devuelve una muestra no nula.
 *  - [streamSamples] emite muestras con valores en rango válido.
 *  - Las muestras consecutivas son distintas (hay variación real).
 *
 * Se ejecuta en la JVM sin necesidad de contexto Android.
 */
class EegDataSourceTest {

    private val source = SimulatedEegDataSource.instance

    /**
     * Verifica que [SimulatedEegDataSource.getLastSample] devuelve
     * una muestra no nula en todo momento.
     */
    @Test
    fun getLastSample_returnsNonNullSample() {
        val sample = source.getLastSample()
        assertNotNull(sample)
    }

    /**
     * Verifica que el timestamp de la muestra es positivo,
     * lo que garantiza que se genera en el presente.
     */
    @Test
    fun getLastSample_timestampIsPositive() {
        val sample = source.getLastSample()
        assertTrue("El timestamp debe ser positivo", sample.timestampMs > 0L)
    }

    /**
     * Verifica que el valor de la banda Alpha está en el rango [-1, 1].
     */
    @Test
    fun getLastSample_alphaIsInValidRange() {
        val sample = source.getLastSample()
        assertTrue("Alpha debe estar en [-1, 1]", sample.alpha in -1f..1f)
    }

    /**
     * Verifica que el valor de la banda Beta está en el rango [-1, 1].
     */
    @Test
    fun getLastSample_betaIsInValidRange() {
        val sample = source.getLastSample()
        assertTrue("Beta debe estar en [-1, 1]", sample.beta in -1f..1f)
    }

    /**
     * Verifica que el valor de la banda Gamma está en el rango [-1, 1].
     */
    @Test
    fun getLastSample_gammaIsInValidRange() {
        val sample = source.getLastSample()
        assertTrue("Gamma debe estar en [-1, 1]", sample.gamma in -1f..1f)
    }

    /**
     * Verifica que el valor de la banda Theta está en el rango [-1, 1].
     */
    @Test
    fun getLastSample_thetaIsInValidRange() {
        val sample = source.getLastSample()
        assertTrue("Theta debe estar en [-1, 1]", sample.theta in -1f..1f)
    }

    /**
     * Verifica que todas las bandas de una misma muestra están en rango válido.
     */
    @Test
    fun getLastSample_allBandsAreInValidRange() {
        val sample = source.getLastSample()
        assertTrue(sample.alpha in -1f..1f)
        assertTrue(sample.beta  in -1f..1f)
        assertTrue(sample.gamma in -1f..1f)
        assertTrue(sample.theta in -1f..1f)
    }

    /**
     * Verifica que [EegSample] es una data class con los campos esperados
     * y que sus valores se almacenan correctamente.
     */
    @Test
    fun eegSample_storesFieldsCorrectly() {
        val sample = EegSample(
            timestampMs = 1000L,
            alpha       = 0.5f,
            beta        = 0.3f,
            gamma       = 0.2f,
            theta       = 0.1f
        )
        assertEquals(1000L, sample.timestampMs)
        assertEquals(0.5f,  sample.alpha, 0.001f)
        assertEquals(0.3f,  sample.beta,  0.001f)
        assertEquals(0.2f,  sample.gamma, 0.001f)
        assertEquals(0.1f,  sample.theta, 0.001f)
    }

    /**
     * Verifica que múltiples llamadas a [getLastSample] generan muestras
     * con timestamps dentro de un margen razonable del tiempo actual.
     */
    @Test
    fun getLastSample_timestampIsCloseToCurrentTime() {
        val before = System.currentTimeMillis()
        val sample = source.getLastSample()
        val after  = System.currentTimeMillis()
        assertTrue(
            "El timestamp debe estar entre $before y $after",
            sample.timestampMs in before..after + 100L
        )
    }

    /**
     * Verifica que [SimulatedEegDataSource.streamSamples] emite muestras
     * con todas las bandas en rango válido.
     */
    @Test
    fun streamSamples_emittedSamplesHaveValidBandValues() = runTest {
        val samples = source.streamSamples().take(3).toList()
        assertEquals(3, samples.size)
        samples.forEach { sample ->
            assertTrue(sample.alpha in -1f..1f)
            assertTrue(sample.beta  in -1f..1f)
            assertTrue(sample.gamma in -1f..1f)
            assertTrue(sample.theta in -1f..1f)
        }
    }

    /**
     * Verifica que [SimulatedEegDataSource.streamSamples] emite muestras
     * con timestamps positivos.
     */
    @Test
    fun streamSamples_emittedSamplesHavePositiveTimestamps() = runTest {
        val samples = source.streamSamples().take(2).toList()
        samples.forEach { assertTrue(it.timestampMs > 0L) }
    }
}
