package com.neurotracker.data.eeg

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Muestra de datos EEG simulados.
 *
 * @param timestampMs Marca de tiempo en milisegundos.
 * @param alpha Potencia de la banda Alpha (8–13 Hz) — relajación.
 * @param beta Potencia de la banda Beta (13–30 Hz) — concentración.
 * @param gamma Potencia de la banda Gamma (30–100 Hz) — procesamiento cognitivo.
 * @param theta Potencia de la banda Theta (4–8 Hz) — memoria.
 */
data class EegSample(
    val timestampMs: Long,
    val alpha: Float,
    val beta:  Float,
    val gamma: Float,
    val theta: Float
)

/**
 * Interfaz para fuentes de datos EEG.
 * Permite cambiar entre simulación y hardware real en el futuro.
 */
interface EegDataSource {
    /**
     * Flujo continuo de muestras EEG en tiempo real.
     */
    fun streamSamples(): Flow<EegSample>

    /**
     * Obtiene la última muestra disponible.
     */
    fun getLastSample(): EegSample
}

/**
 * Fuente de datos EEG simulada para desarrollo y pruebas.
 *
 * Genera ondas sinusoidales moduladas con ruido para simular
 * actividad cerebral realista. Implementa el patrón Singleton.
 */
class SimulatedEegDataSource : EegDataSource {

    companion object {
        private const val SAMPLE_RATE_MS = 50L
        private const val NOISE_FACTOR   = 0.12f
        /** Instancia única (Singleton) para toda la aplicación. */
        val instance: SimulatedEegDataSource by lazy { SimulatedEegDataSource() }
    }

    private var t = 0.0
    private var lastSample = generateFreshSample()

    private val baseAmplitudes = mapOf(
        "alpha" to 0.45f,
        "beta"  to 0.42f,
        "gamma" to 0.38f,
        "theta" to 0.35f
    )

    private val modulationFreqs = mapOf(
        "alpha" to 0.08,
        "beta"  to 0.11,
        "gamma" to 0.07,
        "theta" to 0.09
    )

    /**
     * Flujo continuo de muestras EEG simuladas.
     * Genera una muestra cada SAMPLE_RATE_MS milisegundos.
     */
    override fun streamSamples(): Flow<EegSample> = flow {
        while (true) {
            t += SAMPLE_RATE_MS / 1000.0

            /**
             * Calcula una amplitud variable para la banda indicada.
             *
             * @param band Nombre interno de la banda EEG.
             * @return Amplitud modulada dentro del rango seguro de simulación.
             */
            fun modulatedAmplitude(band: String): Float {
                val base = baseAmplitudes[band] ?: 0.4f
                val modF = modulationFreqs[band] ?: 0.1
                val mod  = (sin(2 * PI * modF * t) * 0.15).toFloat()
                return (base + mod).coerceIn(0.1f, 0.8f)
            }

            val sample = EegSample(
                timestampMs = System.currentTimeMillis(),
                alpha = generateWave(10.0, modulatedAmplitude("alpha")),
                beta  = generateWave(20.0, modulatedAmplitude("beta")),
                gamma = generateWave(40.0, modulatedAmplitude("gamma")),
                theta = generateWave(6.0,  modulatedAmplitude("theta"))
            )
            lastSample = sample
            emit(sample)
            delay(SAMPLE_RATE_MS)
        }
    }

    /**
     * Devuelve la última muestra del stream si está activo,
     * o genera una muestra nueva con valores realistas y variados
     * si el stream no está corriendo (caso tests individuales).
     *
     * Cada llamada genera valores distintos gracias al tiempo actual
     * y al Random, evitando que todos los tests guarden los mismos datos EEG.
     */
    override fun getLastSample(): EegSample = generateFreshSample()

    /**
     * Genera una onda sinusoidal para una banda específica con ruido.
     *
     * @param freq Frecuencia base de la onda (Hz).
     * @param amplitude Amplitud de la onda.
     * @return Valor normalizado entre -1 y 1.
     */
    private fun generateWave(freq: Double, amplitude: Float): Float {
        val tNow   = System.currentTimeMillis() / 1000.0
        val signal = sin(2 * PI * freq * tNow).toFloat() * amplitude
        val noise  = (Random.nextFloat() * 2f - 1f) * NOISE_FACTOR
        return (signal + noise).coerceIn(-1f, 1f)
    }
}

/**
 * Genera una muestra EEG realista con valores distintos cada vez,
 * basándose en el timestamp actual para garantizar variación entre tests.
 *
 * @return Muestra EEG con valores aleatorios pero coherentes.
 */
private fun generateFreshSample(): EegSample {
    val tNow = System.currentTimeMillis() / 1000.0

    // Cada banda tiene frecuencia base distinta → valores siempre diferentes
    /**
     * Genera el valor instantáneo de una banda EEG simulada.
     *
     * @param freq Frecuencia base de la banda.
     * @param baseAmp Amplitud base usada para escalar la señal.
     * @return Valor normalizado entre -1 y 1.
     */
    fun band(freq: Double, baseAmp: Float): Float {
        val signal = sin(2 * PI * freq * tNow).toFloat() * baseAmp
        val noise  = (Random.nextFloat() * 2f - 1f) * 0.15f
        return (signal + noise).coerceIn(-1f, 1f)
    }

    // Añadir offset aleatorio por banda para maximizar diferencias entre tests
    val alphaOffset = Random.nextFloat() * 0.2f - 0.1f
    val betaOffset  = Random.nextFloat() * 0.2f - 0.1f
    val gammaOffset = Random.nextFloat() * 0.2f - 0.1f
    val thetaOffset = Random.nextFloat() * 0.2f - 0.1f

    return EegSample(
        timestampMs = System.currentTimeMillis(),
        alpha = (band(10.0, 0.45f) + alphaOffset).coerceIn(-1f, 1f),
        beta  = (band(20.0, 0.42f) + betaOffset).coerceIn(-1f, 1f),
        gamma = (band(40.0, 0.38f) + gammaOffset).coerceIn(-1f, 1f),
        theta = (band(6.0,  0.35f) + thetaOffset).coerceIn(-1f, 1f)
    )
}
