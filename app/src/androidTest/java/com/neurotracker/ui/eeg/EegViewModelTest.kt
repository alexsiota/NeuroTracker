package com.neurotracker.ui.eeg

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del [EegViewModel].
 *
 * Verifica la gestión del stream de datos EEG, el modo de visualización
 * ([EegViewModel.ViewMode]) y el ciclo de vida del stream (start/stop).
 *
 *
 * Se ejecuta en dispositivo o emulador porque [EegViewModel]
 * extiende [AndroidViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class EegViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: EegViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = EegViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    @After
    fun tearDown() {
        viewModel.stopStream()
        Dispatchers.resetMain()
    }

    /**
     * Verifica que el stream se inicia automáticamente en el [init] del ViewModel
     * y [EegViewModel.isRunning] es true desde el primer momento.
     */
    @Test
    fun initialState_streamStartsAutomatically() {
        assertTrue(viewModel.isRunning.value)
    }

    /**
     * Verifica que el modo de visualización inicial es [EegViewModel.ViewMode.COMPACT].
     */
    @Test
    fun initialState_viewModeIsCompact() {
        assertEquals(EegViewModel.ViewMode.COMPACT, viewModel.viewMode.value)
    }

    /**
     * Verifica que [EegViewModel.stopStream] detiene el stream
     * y [EegViewModel.isRunning] pasa a false.
     */
    @Test
    fun stopStream_setsIsRunningToFalse() {
        viewModel.stopStream()
        assertFalse(viewModel.isRunning.value)
    }

    /**
     * Verifica que [EegViewModel.startStream] cuando el stream ya está activo
     * no lanza un segundo stream (es idempotente).
     */
    @Test
    fun startStream_whenAlreadyRunning_remainsRunning() {
        assertTrue(viewModel.isRunning.value)
        viewModel.startStream()
        assertTrue(viewModel.isRunning.value)
    }

    /**
     * Verifica que [EegViewModel.toggleViewMode] cambia de [EegViewModel.ViewMode.COMPACT]
     * a [EegViewModel.ViewMode.DETAIL].
     */
    @Test
    fun toggleViewMode_fromCompact_switchesToDetail() {
        assertEquals(EegViewModel.ViewMode.COMPACT, viewModel.viewMode.value)
        viewModel.toggleViewMode()
        assertEquals(EegViewModel.ViewMode.DETAIL, viewModel.viewMode.value)
    }

    /**
     * Verifica que un segundo [toggleViewMode] vuelve de DETAIL a COMPACT.
     */
    @Test
    fun toggleViewMode_fromDetail_switchesBackToCompact() {
        viewModel.toggleViewMode()
        viewModel.toggleViewMode()
        assertEquals(EegViewModel.ViewMode.COMPACT, viewModel.viewMode.value)
    }

    /**
     * Verifica que el stream puede detenerse y reiniciarse correctamente.
     */
    @Test
    fun stopAndRestart_streamCycleWorksCorrectly() {
        viewModel.stopStream()
        assertFalse(viewModel.isRunning.value)
        viewModel.startStream()
        assertTrue(viewModel.isRunning.value)
    }

    /**
     * Verifica que [EegViewModel.ViewMode] tiene exactamente 2 valores: COMPACT y DETAIL.
     */
    @Test
    fun viewMode_hasExactlyTwoValues() {
        assertEquals(2, EegViewModel.ViewMode.values().size)
    }
}
