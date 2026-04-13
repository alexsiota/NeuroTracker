package com.neurotracker.ui.login

/**
 * Pantalla de inicio de sesión.
 *
 * Permite al usuario autenticarse con email y contraseña.
 * La contraseña se hashea con SHA-256 antes de compararse con la BD.
 *
 * Funcionalidades:
 *  - Campo de email con validación de formato.
 *  - Campo de contraseña con toggle de visibilidad.
 *  - Checkbox "Recuérdame" para persistir la sesión entre reinicios.
 *  - Mensaje de error contextual cuando las credenciales son incorrectas.
 *  - Enlace de navegación a [RegisterScreen].
 *
 * Usabilidad:
 *  - Animación de entrada suave con offset vertical.
 *  - Botón deshabilitado durante la carga para evitar doble envío.
 *  - Feedback de error con Card roja accesible.
 */

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.neurotracker.ui.navigation.Routes

/**
 * Composable principal de la pantalla de login.
 *
 * Responsabilidades:
 * - Renderizar la interfaz de inicio de sesión.
 * - Gestionar el estado visual de los campos (email, contraseña, visibilidad).
 * - Interactuar con el ViewModel para autenticación.
 * - Navegar entre pantallas (welcome, register, main).
 * - Mostrar feedback visual (loading, errores).
 *
 * @param navController Controlador de navegación para cambiar de pantalla.
 * @param viewModel ViewModel asociado que gestiona la lógica de autenticación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel()
) {
    var passwordVisible by remember { mutableStateOf(false) }

    val offsetY = remember { Animatable(60f) }
    LaunchedEffect(Unit) {
        offsetY.animateTo(0f, animationSpec = tween(500, easing = EaseOutCubic))
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

        // Botón volver
        IconButton(
            onClick  = { navController.navigate("welcome") { popUpTo("welcome") { inclusive = true } } },
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
        }

        // Formulario centrado
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .offset(y = offsetY.value.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(Icons.Default.Psychology, null, tint = Color(0xFF6C63FF), modifier = Modifier.size(48.dp))

            Spacer(Modifier.height(12.dp))

            Text("Bienvenido de nuevo", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Spacer(Modifier.height(6.dp))

            Text("Inicia sesión para continuar", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))

            Spacer(Modifier.height(32.dp))

            // Email
            OutlinedTextField(
                value           = viewModel.email.value,
                onValueChange   = viewModel::onEmailChange,
                label           = { Text("Email", color = Color.White.copy(alpha = 0.7f)) },
                leadingIcon     = { Icon(Icons.Default.Email, null, tint = Color(0xFF6C63FF)) },
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors          = fieldColors()
            )

            Spacer(Modifier.height(14.dp))

            // Contraseña
            OutlinedTextField(
                value         = viewModel.password.value,
                onValueChange = viewModel::onPasswordChange,
                label         = { Text("Contraseña", color = Color.White.copy(alpha = 0.7f)) },
                leadingIcon   = { Icon(Icons.Default.Lock, null, tint = Color(0xFF6C63FF)) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon  = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            null, tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = fieldColors()
            )

            Spacer(Modifier.height(8.dp))

            // Fila: Recuérdame + ¿Olvidaste tu contraseña?
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked         = viewModel.rememberMe.value,
                        onCheckedChange = viewModel::onRememberMeChange,
                        colors          = CheckboxDefaults.colors(checkedColor = Color(0xFF6C63FF))
                    )
                    Text("Recuérdame", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "¿Olvidaste tu contraseña?",
                    color  = Color(0xFF6C63FF),
                    style  = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { navController.navigate(Routes.FORGOT_PASSWORD) }
                )
            }

            Spacer(Modifier.height(24.dp))

            /**
             * Botón de inicio de sesión.
             *
             * - Ejecuta la función login del ViewModel.
             * - Se deshabilita mientras la operación está en curso.
             * - Muestra un indicador de carga durante el proceso.
             */
            Button(
                onClick  = { viewModel.login { navController.navigate("main") } },
                enabled  = !viewModel.isLoading.value,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFF6C63FF),
                    disabledContainerColor = Color(0xFF6C63FF).copy(alpha = 0.5f)
                )
            ) {
                if (viewModel.isLoading.value) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text("Entrar", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            /**
             * Componente de visualización de errores.
             *
             * - Se muestra únicamente cuando existe un mensaje de error.
             * - Incluye animaciones de entrada y salida.
             * - Presenta un mensaje accesible para el usuario.
             */
            AnimatedVisibility(
                visible = viewModel.errorMessage.value != null,
                enter   = fadeIn() + slideInVertically(),
                exit    = fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        shape  = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "El email y/o contraseña introducidos son incorrectos",
                                color = Color(0xFFC62828),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Link a registro — funcional
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "¿No tienes cuenta? ",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Regístrate",
                    color      = Color(0xFF6C63FF),
                    fontWeight = FontWeight.SemiBold,
                    style      = MaterialTheme.typography.bodyMedium,
                    modifier   = Modifier.clickable {
                        navController.navigate("register") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Configuración de colores para los campos de texto del formulario.
 *
 * Define los colores de:
 * - Borde (activo/inactivo)
 * - Texto (activo/inactivo)
 * - Cursor
 *
 * @return Configuración de colores para OutlinedTextField.
 */
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Color(0xFF6C63FF),
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    focusedTextColor     = Color.White,
    unfocusedTextColor   = Color.White,
    cursorColor          = Color(0xFF6C63FF)
)