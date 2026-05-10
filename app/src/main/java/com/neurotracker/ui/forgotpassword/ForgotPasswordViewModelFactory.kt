package com.neurotracker.ui.forgotpassword

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para instanciar [ForgotPasswordViewModel].
 *
 * Necesaria porque [ForgotPasswordViewModel] extiende [AndroidViewModel]
 * y requiere [Application] como parámetro constructor, lo que impide
 * la creación automática por parte de Compose Navigation.
 *
 * Las tres pantallas del flujo de recuperación ([ForgotPasswordScreen],
 * [VerifyCodeScreen] y [ResetPasswordScreen]) comparten la misma instancia
 * del ViewModel usando el mismo [NavBackStackEntry] del grafo de navegación.
 *
 * @param application Instancia de [Application] pasada al ViewModel.
 */
class ForgotPasswordViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia de [ForgotPasswordViewModel].
     *
     * @param modelClass Clase del ViewModel solicitado.
     * @return Instancia tipada del ViewModel.
     * @throws IllegalArgumentException si [modelClass] no es [ForgotPasswordViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ForgotPasswordViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
