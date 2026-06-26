package com.neurotracker.ui.forgotpassword

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.navigation.Routes

/**
 * Pantalla de recuperación de contraseña — Paso 1: Introducir email.
 *
 * ─── Nota: navegación directa en onSuccess ────────────────────────────────────
 *
 * Problema original: la navegación se hacía con un LaunchedEffect observando
 * [codeSent]. Esto creaba una carrera entre la recomposición que actualizaba
 * [demoCode] y la navegación. Resultado: VerifyCodeScreen montaba sin demoCode.
 *
 * Solución: [sendCode] recibe el callback de navegación directamente.
 * El ViewModel establece [demoCode] ANTES de llamar [onSuccess], garantizando
 * que el código ya está disponible cuando VerifyCodeScreen monta.
 *
 * @param navController Controlador de navegación.
 * @param viewModel     ViewModel compartido (instancia desde el NavBackStackEntry padre).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: ForgotPasswordViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D1A), Color(0xFF1A1A2E), Color(0xFF0D0D1A))
                )
            )
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        // Botón retroceso
        IconButton(
            onClick  = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .semantics { contentDescription = "Volver atrás" }
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
        }

        Column(
            modifier            = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.LockReset, contentDescription = null, tint = Color(0xFF6C63FF), modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(16.dp))
            Text("¿Olvidaste tu contraseña?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Introduce tu email y te enviaremos\nun código de verificación.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))

            // Campo email
            OutlinedTextField(
                value           = viewModel.email.value,
                onValueChange   = viewModel::onEmailChange,
                label           = { Text("Email", color = Color.White.copy(alpha = 0.7f)) },
                leadingIcon     = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF6C63FF)) },
                modifier        = Modifier.fillMaxWidth().semantics { contentDescription = "Campo de email para recuperar contraseña" },
                shape           = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine      = true,
                colors          = fieldColors(),
                isError         = viewModel.errorStep1.value != null
            )

            Spacer(Modifier.height(16.dp))

            // Botón enviar
            Button(
                onClick  = {
                    // Nota: navegación en onSuccess, no en LaunchedEffect
                    viewModel.sendCode(onSuccess = {
                        navController.navigate(Routes.VERIFY_CODE)
                    })
                },
                enabled  = !viewModel.isLoadingStep1.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = "Enviar código de recuperación al email" },
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFF6C63FF),
                    disabledContainerColor = Color(0xFF6C63FF).copy(alpha = 0.5f)
                )
            ) {
                if (viewModel.isLoadingStep1.value) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text("Enviar código", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            // Error
            AnimatedVisibility(visible = viewModel.errorStep1.value != null, enter = fadeIn(tween(300)) + slideInVertically(), exit = fadeOut(tween(200))) {
                viewModel.errorStep1.value?.let { error ->
                    Spacer(Modifier.height(12.dp))
                    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                        Row(modifier = Modifier.padding(12.dp).semantics { contentDescription = "Error: $error" }, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(error, color = Color(0xFFC62828), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Colores compartidos por los campos del flujo de recuperación.
 *
 * @return Configuración visual para [OutlinedTextField].
 */
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Color(0xFF6C63FF),
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    errorBorderColor     = Color(0xFFC62828),
    focusedTextColor     = Color.White,
    unfocusedTextColor   = Color.White,
    cursorColor          = Color(0xFF6C63FF)
)
