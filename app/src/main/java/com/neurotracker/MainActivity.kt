package com.neurotracker

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neurotracker.data.database.NeuroTrackerDatabase
import com.neurotracker.data.session.OnboardingManager
import com.neurotracker.data.session.SessionManager
import com.neurotracker.data.session.ThemeManager
import com.neurotracker.ui.achievements.AchievementsScreen
import com.neurotracker.ui.achievements.AchievementsViewModelFactory
import com.neurotracker.ui.eeg.EegScreen
import com.neurotracker.ui.forgotpassword.ForgotPasswordScreen
import com.neurotracker.ui.forgotpassword.ForgotPasswordViewModel
import com.neurotracker.ui.forgotpassword.ForgotPasswordViewModelFactory
import com.neurotracker.ui.forgotpassword.ResetPasswordScreen
import com.neurotracker.ui.forgotpassword.VerifyCodeScreen
import com.neurotracker.ui.history.HistoryScreen
import com.neurotracker.ui.login.LoginScreen
import com.neurotracker.ui.main.BadgeUnlockOverlay
import com.neurotracker.ui.main.MainScreen
import com.neurotracker.ui.navigation.Routes
import com.neurotracker.ui.onboarding.OnboardingScreen
import com.neurotracker.ui.profile.ProfileScreen
import com.neurotracker.ui.register.RegisterScreen
import com.neurotracker.ui.stadistics.StatisticsScreen
import com.neurotracker.ui.tests.selector.ChooseTestScreen
import com.neurotracker.ui.tests.attention.visualCancellation.VisualCancellationScreen
import com.neurotracker.ui.tests.attention.reaction.ReactionTestScreen
import com.neurotracker.ui.tests.attention.selectiveAttention.SelectiveAttentionScreen
import com.neurotracker.ui.tests.attention.sustainedAttention.SustainedAttentionScreen
import com.neurotracker.ui.tests.composite.cognitiveFatigue.CognitiveFatigueScreen
import com.neurotracker.ui.tests.composite.dualTask.DualTaskScreen
import com.neurotracker.ui.tests.composite.globalCognitive.GlobalCognitiveScreen
import com.neurotracker.ui.tests.coordination.objectTracking.ObjectTrackingScreen
import com.neurotracker.ui.tests.coordination.spatialPerception.SpatialPerceptionScreen
import com.neurotracker.ui.tests.coordination.visuoMotor.VisuomotorScreen
import com.neurotracker.ui.tests.executive.planning.PlanningScreen
import com.neurotracker.ui.tests.executive.responseInhibition.ResponseInhibitionScreen
import com.neurotracker.ui.tests.attention.stroop.StroopScreen
import com.neurotracker.ui.tests.executive.tasksWitching.TaskSwitchingScreen
import com.neurotracker.ui.tests.memory.associativeMemory.AssociativeMemoryScreen
import com.neurotracker.ui.tests.memory.digitspan.DigitSpanScreen
import com.neurotracker.ui.tests.memory.nback.NBackScreen
import com.neurotracker.ui.tests.memory.objectMemory.ObjectMemoryScreen
import com.neurotracker.ui.tests.memory.visualMemory.VisualMemoryScreen
import com.neurotracker.ui.tests.speed.decisionTime.DecisionTimeScreen
import com.neurotracker.ui.tests.speed.quickComparison.QuickComparisonScreen
import com.neurotracker.ui.tests.speed.sequenceOrdering.SequenceOrderingScreen
import com.neurotracker.ui.theme.NeuroTrackerTheme
import com.neurotracker.ui.welcome.WelcomeScreen
import kotlinx.coroutines.launch

/**
 * Actividad principal de la aplicación NeuroTracker.
 *
 * Punto de entrada de la app. Gestiona:
 *  - Configuración del sistema (edge-to-edge, barras de estado y navegación).
 *  - Validación de la sesión del usuario contra Room.
 *  - Tema oscuro/claro persistente mediante [ThemeManager].
 *  - Grafo de navegación completo de la aplicación.
 *  - Destino de inicio dinámico según onboarding y sesión.
 *
 * ─── Nota: ViewModel compartido para el flujo de recuperación ─────────────────
 *
 * Las tres pantallas del flujo de recuperación de contraseña
 * (ForgotPasswordScreen, VerifyCodeScreen, ResetPasswordScreen) deben
 * compartir la MISMA instancia de [ForgotPasswordViewModel] para que
 * [verifiedEmail] y [generatedCode] persistan entre pantallas.
 *
 * Solución: en los tres composable() del flujo se obtiene el ViewModel
 * usando `getBackStackEntry(Routes.FORGOT_PASSWORD)` como owner, lo que
 * garantiza que las tres pantallas comparten exactamente la misma instancia.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager    = SessionManager(this)
        val themeManager      = ThemeManager(this)
        val onboardingManager = OnboardingManager(this)

        // Validación de sesión contra Room
        lifecycleScope.launch {
            if (sessionManager.hasSession()) {
                val email = sessionManager.getUserEmail()
                if (email != null) {
                    val db   = NeuroTrackerDatabase.getDatabase(applicationContext)
                    val user = db.userDao().getUserByEmail(email)
                    if (user == null) sessionManager.clearSession()
                } else {
                    sessionManager.clearSession()
                }
            }
        }

        setContent {
            val isDarkMode = remember { mutableStateOf(themeManager.isDarkMode()) }

            enableEdgeToEdge(
                statusBarStyle = if (isDarkMode.value)
                    SystemBarStyle.dark(Color.Transparent.toArgb())
                else
                    SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
                navigationBarStyle = if (isDarkMode.value)
                    SystemBarStyle.dark(Color.Transparent.toArgb())
                else
                    SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
            )

            NeuroTrackerTheme(darkTheme = isDarkMode.value, dynamicColor = false) {

                val navController   = rememberNavController()
                val application     = LocalContext.current.applicationContext as Application

                val startDestination = when {
                    !onboardingManager.hasSeenOnboarding() -> Routes.ONBOARDING
                    sessionManager.hasSession()            -> Routes.MAIN
                    else                                   -> Routes.WELCOME
                }

                NavHost(navController = navController, startDestination = startDestination) {

                    // ── Onboarding ────────────────────────────────────────────
                    composable(Routes.ONBOARDING) {
                        onboardingManager.markOnboardingAsSeen()
                        OnboardingScreen(navController)
                    }

                    // ── Pantallas base ────────────────────────────────────────
                    composable(Routes.WELCOME)     { WelcomeScreen(navController) }
                    composable(Routes.LOGIN)       { LoginScreen(navController) }
                    composable(Routes.REGISTER)    { RegisterScreen(navController) }
                    composable(Routes.MAIN)        { MainScreen(navController) }
                    composable(Routes.HISTORY)     { HistoryScreen(navController) }
                    composable(Routes.CHOOSE_TEST) { ChooseTestScreen(navController) }
                    composable(Routes.EEG)         { EegScreen(navController) }
                    composable(Routes.STATISTICS)  { StatisticsScreen(navController) }

                    composable(Routes.PROFILE) {
                        ProfileScreen(
                            navController = navController,
                            isDarkMode    = isDarkMode.value,
                            onToggleTheme = { dark ->
                                isDarkMode.value = dark
                                themeManager.setDarkMode(dark)
                            }
                        )
                    }

                    // ── Logros ────────────────────────────────────────────────
                    composable(Routes.ACHIEVEMENTS) {
                        AchievementsScreen(navController)
                    }

                    // ── Flujo recuperación contraseña (ViewModel compartido) ──
                    //
                    // Nota: las 3 pantallas usan getBackStackEntry(FORGOT_PASSWORD)
                    // como viewModelStoreOwner para compartir la misma instancia
                    // de ForgotPasswordViewModel. Sin esto, verifiedEmail se pierde
                    // entre pantallas y ResetPasswordScreen da "error al encontrar cuenta".

                    composable(Routes.FORGOT_PASSWORD) { entry ->
                        val forgotVm: ForgotPasswordViewModel = viewModel(
                            viewModelStoreOwner = entry,
                            factory             = ForgotPasswordViewModelFactory(application)
                        )
                        ForgotPasswordScreen(navController = navController, viewModel = forgotVm)
                    }

                    composable(Routes.VERIFY_CODE) { entry ->
                        val parentEntry = remember(entry) {
                            navController.getBackStackEntry(Routes.FORGOT_PASSWORD)
                        }
                        val forgotVm: ForgotPasswordViewModel = viewModel(
                            viewModelStoreOwner = parentEntry,
                            factory             = ForgotPasswordViewModelFactory(application)
                        )
                        VerifyCodeScreen(navController = navController, viewModel = forgotVm)
                    }

                    composable(Routes.RESET_PASSWORD) { entry ->
                        val parentEntry = remember(entry) {
                            navController.getBackStackEntry(Routes.FORGOT_PASSWORD)
                        }
                        val forgotVm: ForgotPasswordViewModel = viewModel(
                            viewModelStoreOwner = parentEntry,
                            factory             = ForgotPasswordViewModelFactory(application)
                        )
                        ResetPasswordScreen(navController = navController, viewModel = forgotVm)
                    }

                    // ── Bloque 1 — Atención ───────────────────────────────────
                    composable(Routes.REACTION_SIMPLE)     { ReactionTestScreen(navController) }
                    composable(Routes.ATTENTION_SUSTAINED) { SustainedAttentionScreen(navController) }
                    composable(Routes.ATTENTION_SELECTIVE) { SelectiveAttentionScreen(navController) }
                    composable(Routes.STROOP)              { StroopScreen(navController) }
                    composable(Routes.VISUAL_CANCELLATION) { VisualCancellationScreen(navController) }

                    // ── Bloque 2 — Memoria ────────────────────────────────────
                    composable(Routes.DIGIT_SPAN)         { DigitSpanScreen(navController) }
                    composable(Routes.N_BACK)             { NBackScreen(navController) }
                    composable(Routes.VISUAL_MEMORY)      { VisualMemoryScreen(navController) }
                    composable(Routes.OBJECT_MEMORY)      { ObjectMemoryScreen(navController) }
                    composable(Routes.ASSOCIATIVE_MEMORY) { AssociativeMemoryScreen(navController) }

                    // ── Bloque 3 — Velocidad ──────────────────────────────────
                    composable(Routes.QUICK_COMPARISON)  { QuickComparisonScreen(navController) }
                    composable(Routes.SEQUENCE_ORDERING) { SequenceOrderingScreen(navController) }
                    composable(Routes.DECISION_TIME)     { DecisionTimeScreen(navController) }

                    // ── Bloque 4 — Ejecutivo ──────────────────────────────────
                    composable(Routes.TASK_SWITCHING)      { TaskSwitchingScreen(navController) }
                    composable(Routes.RESPONSE_INHIBITION) { ResponseInhibitionScreen(navController) }
                    composable(Routes.PLANNING)            { PlanningScreen(navController) }

                    // ── Bloque 5 — Coordinación ───────────────────────────────
                    composable(Routes.OBJECT_TRACKING)    { ObjectTrackingScreen(navController) }
                    composable(Routes.VISUOMOTOR)         { VisuomotorScreen(navController) }
                    composable(Routes.SPATIAL_PERCEPTION) { SpatialPerceptionScreen(navController) }

                    // ── Bloque 6 — Compuestos ─────────────────────────────────
                    composable(Routes.GLOBAL_COGNITIVE)  { GlobalCognitiveScreen(navController) }
                    composable(Routes.COGNITIVE_FATIGUE) { CognitiveFatigueScreen(navController) }
                    composable(Routes.DUAL_TASK)         { DualTaskScreen(navController) }
                }

                // Diálogo global de logro desbloqueado. Se coloca fuera del NavHost
                // para que sea visible desde cualquier pantalla en el momento exacto
                // del desbloqueo, sin necesidad de modificar cada pantalla de test.
                BadgeUnlockOverlay()
            }
        }
    }
}
