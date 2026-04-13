package com.neurotracker.ui.navigation

/**
 * Constantes de rutas de navegación de la aplicación NeuroTracker.
 *
 * Define todos los destinos del grafo de navegación de Jetpack Compose Navigation.
 * Cada constante es una cadena única que identifica una pantalla en el [NavHost].
 */
object Routes {

    // ── Pantallas base ────────────────────────────────────────────────────────

    /** Pantalla de onboarding. Solo se muestra la primera vez que se instala la app. */
    const val ONBOARDING  = "onboarding"

    /** Pantalla de bienvenida. */
    const val WELCOME     = "welcome"

    /** Pantalla de inicio de sesión. */
    const val LOGIN       = "login"

    /** Pantalla de registro. */
    const val REGISTER    = "register"

    /** Pantalla principal (home). */
    const val MAIN        = "main"

    /** Pantalla de perfil del usuario. */
    const val PROFILE     = "profile"

    /** Pantalla de historial de tests. */
    const val HISTORY     = "history"

    /** Pantalla de selección de test. */
    const val CHOOSE_TEST = "choose_test"

    /** Pantalla de logros del usuario. */
    const val ACHIEVEMENTS = "achievements"

    // ── Recuperación de contraseña ────────────────────────────────────────────

    /**
     * Paso 1 — Introducir el email de la cuenta.
     *
     * Es también el owner del [ForgotPasswordViewModel] compartido.
     * VerifyCodeScreen y ResetPasswordScreen obtienen el ViewModel
     * usando getBackStackEntry(FORGOT_PASSWORD) como viewModelStoreOwner.
     */
    const val FORGOT_PASSWORD = "forgot_password"

    /** Paso 2 — Introducir el código de verificación de 7 caracteres. */
    const val VERIFY_CODE = "verify_code"

    /** Paso 3 — Establecer la nueva contraseña. */
    const val RESET_PASSWORD = "reset_password"

    // ── Bloque 1: Atención y concentración ───────────────────────────────────

    /** Test de Reacción Simple. */
    const val REACTION_SIMPLE     = "reaction_simple"

    /** Test de Atención Sostenida. */
    const val ATTENTION_SUSTAINED = "attention_sustained"

    /** Test de Atención Selectiva. */
    const val ATTENTION_SELECTIVE = "attention_selective"

    /** Test de Stroop. */
    const val STROOP              = "stroop"

    /** Test de Cancelación Visual. */
    const val VISUAL_CANCELLATION = "visual_cancellation"

    // ── Bloque 2: Memoria ─────────────────────────────────────────────────────

    /** Test de Memoria de Dígitos. */
    const val DIGIT_SPAN         = "digit_span"

    /** Test N-Back. */
    const val N_BACK             = "n_back"

    /** Test de Memoria Visual. */
    const val VISUAL_MEMORY      = "visual_memory"

    /** Test de Memoria de Objetos. */
    const val OBJECT_MEMORY      = "object_memory"

    /** Test de Memoria Asociativa. */
    const val ASSOCIATIVE_MEMORY = "associative_memory"

    // ── Bloque 3: Velocidad de procesamiento ─────────────────────────────────

    /** Test de Comparación Rápida. */
    const val QUICK_COMPARISON  = "quick_comparison"

    /** Test de Ordenar Secuencias. */
    const val SEQUENCE_ORDERING = "sequence_ordering"

    /** Test de Tiempo de Decisión. */
    const val DECISION_TIME     = "decision_time"

    // ── Bloque 4: Funciones ejecutivas ───────────────────────────────────────

    /** Test de Cambio de Tarea. */
    const val TASK_SWITCHING      = "task_switching"

    /** Test de Inhibición de Respuesta. */
    const val RESPONSE_INHIBITION = "response_inhibition"

    /** Test de Planificación. */
    const val PLANNING            = "planning"

    // ── Bloque 5: Coordinación y percepción ──────────────────────────────────

    /** Test de Seguimiento de Objetos (MOT). */
    const val OBJECT_TRACKING    = "object_tracking"

    /** Test de Coordinación Visomotora. */
    const val VISUOMOTOR         = "visuomotor"

    /** Test de Percepción Espacial. */
    const val SPATIAL_PERCEPTION = "spatial_perception"

    // ── Bloque 6: Tests compuestos ────────────────────────────────────────────

    /** Test Cognitivo Global. */
    const val GLOBAL_COGNITIVE  = "global_cognitive"

    /** Test de Fatiga Cognitiva. */
    const val COGNITIVE_FATIGUE = "cognitive_fatigue"

    /** Test de Doble Tarea. */
    const val DUAL_TASK         = "dual_task"

    // ── Pantallas secundarias ─────────────────────────────────────────────────

    /** Pantalla de simulación EEG. */
    const val EEG        = "eeg"

    /** Pantalla de estadísticas y evolución cognitiva. */
    const val STATISTICS = "statistics"
}
