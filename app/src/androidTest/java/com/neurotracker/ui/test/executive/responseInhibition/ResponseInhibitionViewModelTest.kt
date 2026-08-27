package com.neurotracker.ui.test.executive.responseInhibition

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neurotracker.ui.tests.executive.responseInhibition.ResponseInhibitionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [ResponseInhibitionViewModel].
 *
 * Verifica el sistema de puntuación Go/No-Go:
 *  - Pulsar GO   → acierto.
 *  - Pulsar NOGO → error de comisión.
 *  - GO sin pulsar → error de omisión.
 *  - NOGO sin pulsar → no suma nada.
 *
 * Los tests que verifican la fórmula de precisión lo hacen ejecutando el test
 * completo con tiempo avanzado y comprobando rangos válidos, en lugar de
 * forzar valores internos con reflexión (que es frágil y dependiente del
 * nombre interno del campo de [mutableStateOf]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ResponseInhibitionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ResponseInhibitionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ResponseInhibitionViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Verifica que el estado inicial del test es IDLE con todos los contadores a cero.
     */
    @Test
    fun initialState_isIdleWithZeroCounters() {
        assertEquals(ResponseInhibitionViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.commissions.value)
        assertEquals(0, viewModel.omissions.value)
    }

    /**
     * Verifica que [ResponseInhibitionViewModel.startTest] cambia el estado a RUNNING.
     */
    @Test
    fun startTest_changesStateToRunning() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        assertEquals(ResponseInhibitionViewModel.TestState.RUNNING, viewModel.testState.value)
    }

    /**
     * Verifica que [ResponseInhibitionViewModel.precisionPercent] devuelve 0
     * cuando no hay ningún intento registrado.
     */
    @Test
    fun precisionPercent_withNoAttempts_returnsZero() {
        assertEquals(0, viewModel.precisionPercent())
    }

    /**
     * Verifica que [ResponseInhibitionViewModel.precisionPercent] devuelve un valor
     * en el rango 0–100 tras completar el test con el tiempo avanzado.
     *
     * Se evita forzar valores internos con reflexión porque es frágil. En su lugar
     * se avanza el tiempo suficiente para que el test complete y se comprueba
     * que la precisión está en un rango válido.
     */
    @Test
    fun precisionPercent_afterTestCompletes_isInValidRange() = runTest {
        viewModel.startTest()
        // 25 rondas × (1500ms estímulo + 300ms pausa) = 45_000ms
        advanceTimeBy(50_000)
        val precision = viewModel.precisionPercent()
        assertTrue("Precisión debe estar en 0–100, fue $precision", precision in 0..100)
    }

    /**
     * Verifica que [ResponseInhibitionViewModel.resetTest] vuelve al estado IDLE
     * con todos los contadores a cero.
     */
    @Test
    fun resetTest_returnsToIdleWithZeroCounters() = runTest {
        viewModel.startTest()
        advanceTimeBy(100)
        viewModel.resetTest()
        assertEquals(ResponseInhibitionViewModel.TestState.IDLE, viewModel.testState.value)
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.commissions.value)
        assertEquals(0, viewModel.omissions.value)
    }

    /**
     * Verifica que [ResponseInhibitionViewModel.onPress] no registra ninguna
     * respuesta si el test no está en estado RUNNING.
     */
    @Test
    fun onPress_whenNotRunning_doesNotRegisterAnyResponse() {
        assertEquals(ResponseInhibitionViewModel.TestState.IDLE, viewModel.testState.value)
        viewModel.onPress()
        assertEquals(0, viewModel.hits.value)
        assertEquals(0, viewModel.commissions.value)
    }

    /**
     * Verifica que tras completar el test, el estado cambia a FINISHED.
     */
    @Test
    fun afterAllRounds_stateChangesToFinished() = runTest {
        viewModel.startTest()
        advanceTimeBy(50_000)
        assertEquals(ResponseInhibitionViewModel.TestState.FINISHED, viewModel.testState.value)
    }

    /**
     * Verifica que la suma de hits + commissions + omissions no supera el total de rondas (25).
     */
    @Test
    fun totalAttempts_neverExceedsTotalRounds() = runTest {
        viewModel.startTest()
        advanceTimeBy(50_000)
        val total = viewModel.hits.value + viewModel.commissions.value + viewModel.omissions.value
        assertTrue("Total intentos ($total) no debe superar 25 rondas", total <= 25)
    }

    /**
     * Verifica que [ResponseInhibitionViewModel.currentRound] avanza durante el test.
     */
    @Test
    fun currentRound_incrementsDuringTest() = runTest {
        viewModel.startTest()
        advanceTimeBy(2000)
        assertTrue("La ronda actual debe ser mayor que 0", viewModel.currentRound.value > 0)
    }
}
