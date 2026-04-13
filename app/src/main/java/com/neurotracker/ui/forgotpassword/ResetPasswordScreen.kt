package com.neurotracker.ui.forgotpassword

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.neurotracker.ui.navigation.Routes

/**
 * Pantalla de recuperación de contraseña — Paso 3: Nueva contraseña.
 *
 * ─── Fix: recibe viewModel como parámetro ────────────────────────────────────
 *
 * Problema original: creaba su propio ViewModel con `viewModel(factory)`,
 * obteniendo una instancia distinta a la de ForgotPasswordScreen.
 * Resultado: [verifiedEmail] estaba vacío → "error al encontrar la cuenta".
 *
 * Solución: recibe el ViewModel compartido como parámetro, igual que
 * ForgotPasswordScreen y VerifyCodeScreen. Ver instrucción de integración
 * en [ForgotPasswordViewModel].
 *
 * @param navController Controlador de navegación.
 * @param viewModel     ViewModel compartido (instancia desde el NavBackStackEntry padre).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    navController: NavController,
    viewModel: ForgotPasswordViewModel
) {
    var newPasswordVisible     by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showSuccessDialog      by remember { mutableStateOf(false) }

    // Mostrar diálogo cuando passwordChanged se activa
    LaunchedEffect(viewModel.passwordChanged.value) {
        if (viewModel.passwordChanged.value) showSuccessDialog = true
    }

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
            Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFF6C63FF), modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(16.dp))
            Text("Nueva contraseña", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Elige una contraseña segura\nde entre 8 y 32 caracteres.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))

            // Campo nueva contraseña
            OutlinedTextField(
                value                = viewModel.newPassword.value,
                onValueChange        = viewModel::onNewPasswordChange,
                label                = { Text("Nueva contraseña", color = Color.White.copy(alpha = 0.7f)) },
                leadingIcon          = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF6C63FF)) },
                trailingIcon         = {
                    IconButton(
                        onClick  = { newPasswordVisible = !newPasswordVisible },
                        modifier = Modifier.semantics { contentDescription = if (newPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña" }
                    ) {
                        Icon(if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                    }
                },
                modifier             = Modifier.fillMaxWidth().semantics { contentDescription = "Campo para introducir la nueva contraseña" },
                shape                = RoundedCornerShape(14.dp),
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine           = true,
                isError              = viewModel.errorStep3.value != null,
                colors               = fieldColors()
            )

            Spacer(Modifier.height(14.dp))

            // Campo confirmar contraseña
            OutlinedTextField(
                value                = viewModel.confirmPassword.value,
                onValueChange        = viewModel::onConfirmPasswordChange,
                label                = { Text("Repetir contraseña", color = Color.White.copy(alpha = 0.7f)) },
                leadingIcon          = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF6C63FF)) },
                trailingIcon         = {
                    IconButton(
                        onClick  = { confirmPasswordVisible = !confirmPasswordVisible },
                        modifier = Modifier.semantics { contentDescription = if (confirmPasswordVisible) "Ocultar confirmación" else "Mostrar confirmación" }
                    ) {
                        Icon(if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                    }
                },
                modifier             = Modifier.fillMaxWidth().semantics { contentDescription = "Campo para confirmar la nueva contraseña" },
                shape                = RoundedCornerShape(14.dp),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine           = true,
                isError              = viewModel.errorStep3.value != null,
                colors               = fieldColors()
            )

            Spacer(Modifier.height(20.dp))

            // Botón guardar
            Button(
                onClick  = {
                    viewModel.saveNewPassword(onSuccess = {})
                },
                enabled  = !viewModel.isLoadingStep3.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = "Guardar la nueva contraseña" },
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFF6C63FF),
                    disabledContainerColor = Color(0xFF6C63FF).copy(alpha = 0.5f)
                )
            ) {
                if (viewModel.isLoadingStep3.value) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text("Guardar contraseña", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            // Error
            AnimatedVisibility(visible = viewModel.errorStep3.value != null, enter = fadeIn(tween(300)) + slideInVertically(), exit = fadeOut(tween(200))) {
                viewModel.errorStep3.value?.let { error ->
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

    // Diálogo de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            icon             = { Text("✅", fontSize = 40.sp) },
            title            = { Text("¡Contraseña cambiada!", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) },
            text             = { Text("Tu contraseña se ha actualizado correctamente.\nYa puedes iniciar sesión con tu nueva contraseña.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium) },
            confirmButton    = {
                Button(
                    onClick  = {
                        showSuccessDialog = false
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Ir a iniciar sesión" },
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Text("✓ Iniciar sesión", color = Color.White, fontWeight = FontWeight.SemiBold) }
            }
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Color(0xFF6C63FF),
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    errorBorderColor     = Color(0xFFC62828),
    focusedTextColor     = Color.White,
    unfocusedTextColor   = Color.White,
    cursorColor          = Color(0xFF6C63FF)
)
