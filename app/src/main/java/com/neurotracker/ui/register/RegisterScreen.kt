package com.neurotracker.ui.register

/**
 * Pantalla de registro de nuevo usuario.
 *
 * Permite crear una cuenta con nombre, email, fecha de nacimiento y contraseña.
 * La contraseña se hashea con SHA-256 antes de guardarse en Room.
 *
 * Validaciones aplicadas:
 *  - Campos no vacíos.
 *  - Formato de email válido (android.util.Patterns).
 *  - Longitud de contraseña: 8–32 caracteres.
 *  - Coincidencia de contraseña y su repetición.
 *  - Email no existente en la base de datos.
 *
 * Flujo de éxito:
 *  - Modal de confirmación con cuenta atrás de 5 segundos.
 *  - Redirección automática a [LoginScreen] al finalizar.
 */

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.util.Calendar

/** Edad mínima requerida según RGPD en España (16 años). */
private const val MIN_AGE = 16

/**
 * Pantalla de registro de nuevo usuario.
 *
 * @param navController Controlador de navegación para redirigir a login tras éxito.
 * @param viewModel ViewModel que gestiona la lógica de registro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = viewModel()
) {
    val context     = LocalContext.current
    val scrollState = rememberScrollState()

    var passwordVisible       by remember { mutableStateOf(false) }
    var repeatPasswordVisible by remember { mutableStateOf(false) }
    var acceptedPrivacy       by remember { mutableStateOf(false) }
    var acceptedAge           by remember { mutableStateOf(false) }
    var showPrivacyDialog     by remember { mutableStateOf(false) }
    var showTermsDialog       by remember { mutableStateOf(false) }

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
            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
        }

        // Formulario
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp, bottom = 120.dp)
                .padding(horizontal = 28.dp)
                .verticalScroll(scrollState)
                .offset(y = offsetY.value.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(Icons.Default.PersonAdd, null, tint = Color(0xFF6C63FF), modifier = Modifier.size(44.dp))

            Spacer(Modifier.height(10.dp))

            Text("Crea tu cuenta", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Spacer(Modifier.height(4.dp))

            Text("Completa los datos para empezar", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))

            Spacer(Modifier.height(28.dp))

            // Nombre
            RegisterField(viewModel.name.value, viewModel::onNameChange, "Nombre completo", Icons.Default.Person)

            Spacer(Modifier.height(14.dp))

            // Fecha de nacimiento — limitada a mayores de MIN_AGE años
            OutlinedTextField(
                value         = viewModel.birthDateText.value,
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Fecha de nacimiento", color = Color.White.copy(alpha = 0.7f)) },
                leadingIcon   = { Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFF6C63FF)) },
                trailingIcon  = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        // Fecha máxima = hoy - MIN_AGE años
                        val maxCal = Calendar.getInstance().apply { add(Calendar.YEAR, -MIN_AGE) }

                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                cal.set(year, month, day)
                                viewModel.onBirthDateSelected(cal.timeInMillis)
                            },
                            // Por defecto mostrar la fecha máxima permitida
                            maxCal.get(Calendar.YEAR),
                            maxCal.get(Calendar.MONTH),
                            maxCal.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            // No permitir fechas futuras ni menores de MIN_AGE
                            datePicker.maxDate = maxCal.timeInMillis
                        }.show()
                    }) {
                        Icon(Icons.Default.DateRange, null, tint = Color.White.copy(alpha = 0.6f))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = fieldColors()
            )

            Spacer(Modifier.height(14.dp))

            // Email
            RegisterField(viewModel.email.value, viewModel::onEmailChange, "Email", Icons.Default.Email, KeyboardType.Email)

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
                        Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Color.White.copy(alpha = 0.6f))
                    }
                },
                colors = fieldColors()
            )

            if (viewModel.password.value.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                PasswordStrengthBar(viewModel.password.value)
            }

            Spacer(Modifier.height(14.dp))

            // Repetir contraseña
            OutlinedTextField(
                value         = viewModel.repeatPassword.value,
                onValueChange = viewModel::onRepeatPasswordChange,
                label         = { Text("Repetir contraseña", color = Color.White.copy(alpha = 0.7f)) },
                leadingIcon   = { Icon(Icons.Default.LockOpen, null, tint = Color(0xFF6C63FF)) },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                visualTransformation = if (repeatPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon  = {
                    IconButton(onClick = { repeatPasswordVisible = !repeatPasswordVisible }) {
                        Icon(if (repeatPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Color.White.copy(alpha = 0.6f))
                    }
                },
                supportingText = {
                    if (viewModel.repeatPassword.value.isNotEmpty()) {
                        if (viewModel.password.value == viewModel.repeatPassword.value)
                            Text("✓ Las contraseñas coinciden", color = Color(0xFF2E7D32), style = MaterialTheme.typography.labelSmall)
                        else
                            Text("✗ Las contraseñas no coinciden", color = Color(0xFFC62828), style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = fieldColors()
            )

            Spacer(Modifier.height(20.dp))

            // ---- Check política de privacidad y términos ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked         = acceptedPrivacy,
                    onCheckedChange = { acceptedPrivacy = it },
                    colors          = CheckboxDefaults.colors(checkedColor = Color(0xFF6C63FF))
                )
                Spacer(Modifier.width(4.dp))
                val privacyText = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White.copy(alpha = 0.8f))) { append("He leído y acepto la ") }
                    withStyle(SpanStyle(color = Color(0xFF6C63FF), textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold)) {
                        append("Política de privacidad")
                    }
                    withStyle(SpanStyle(color = Color.White.copy(alpha = 0.8f))) { append(" y los ") }
                    withStyle(SpanStyle(color = Color(0xFF6C63FF), textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold)) {
                        append("Términos y condiciones")
                    }
                }
                Text(
                    text     = privacyText,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .clickable { showPrivacyDialog = true }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ---- Check edad mínima ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked         = acceptedAge,
                    onCheckedChange = { acceptedAge = it },
                    colors          = CheckboxDefaults.colors(checkedColor = Color(0xFF6C63FF))
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Declaro que tengo $MIN_AGE años o más y estoy autorizado a usar esta aplicación",
                    color  = Color.White.copy(alpha = 0.8f),
                    style  = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(12.dp))

            // Error
            AnimatedVisibility(
                visible = viewModel.errorMessage.value != null,
                enter   = fadeIn() + slideInVertically(),
                exit    = fadeOut()
            ) {
                viewModel.errorMessage.value?.let {
                    Card(
                        shape  = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(it, color = Color(0xFFC62828), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Link a login
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("¿Ya tienes cuenta? ", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Inicia sesión",
                    color      = Color(0xFF6C63FF),
                    fontWeight = FontWeight.SemiBold,
                    style      = MaterialTheme.typography.bodyMedium,
                    modifier   = Modifier.clickable {
                        navController.navigate("login") {
                            popUpTo("register") { inclusive = true }
                        }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        // ---- Botón registrarse fijo abajo ----
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFF0D0D1A).copy(alpha = 0.95f))
                .padding(horizontal = 28.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = {
                    when {
                        !acceptedPrivacy -> viewModel.errorMessage.value = "Debes aceptar la política de privacidad y los términos"
                        !acceptedAge     -> viewModel.errorMessage.value = "Debes confirmar que tienes $MIN_AGE años o más"
                        else -> viewModel.register {
                            navController.navigate("login") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = !viewModel.isLoading.value,
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFF6C63FF),
                    disabledContainerColor = Color(0xFF6C63FF).copy(alpha = 0.5f)
                )
            ) {
                if (viewModel.isLoading.value)
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                else
                    Text("Crear cuenta", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        // ---- Snackbar éxito ----
        if (viewModel.showSuccessModal.value) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth(),
                shape  = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "¡Registro completado! Redirigiendo en ${viewModel.successCountdown.value}s",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // ---- Diálogo Política de privacidad y Términos ----
    if (showPrivacyDialog) {
        LegalDialog(
            title   = "Política de privacidad y Términos",
            content = """
POLÍTICA DE PRIVACIDAD

NeuroTracker es una aplicación educativa desarrollada como Trabajo de Fin de Ciclo (TFC). Los datos recogidos se almacenan únicamente en el dispositivo del usuario y no se comparten con terceros.

DATOS QUE RECOPILAMOS
• Nombre y email de registro
• Fecha de nacimiento (para verificar la edad)
• Resultados de los tests cognitivos
• Foto de perfil (opcional, almacenada localmente)

FINALIDAD DEL TRATAMIENTO
Los datos se utilizan exclusivamente para personalizar la experiencia dentro de la aplicación y mostrar estadísticas de rendimiento cognitivo al propio usuario.

DERECHOS DEL USUARIO
El usuario puede eliminar su cuenta y todos sus datos en cualquier momento desde la configuración de la aplicación.

TÉRMINOS Y CONDICIONES

1. Esta aplicación es de carácter educativo y no sustituye diagnósticos médicos o psicológicos profesionales.

2. El uso de la aplicación está restringido a personas de $MIN_AGE años o más, de acuerdo con el Reglamento General de Protección de Datos (RGPD) de la Unión Europea.

3. Los resultados de los tests son orientativos y no tienen validez clínica.

4. El usuario es responsable de mantener la confidencialidad de sus credenciales de acceso.

5. NeuroTracker se reserva el derecho de actualizar estos términos. Los cambios serán notificados al usuario en el momento del acceso.

Última actualización: Abril 2026
            """.trimIndent(),
            onDismiss = { showPrivacyDialog = false }
        )
    }
}

/**
 * Diálogo reutilizable para mostrar contenido legal.
 *
 * @param title Título del diálogo.
 * @param content Texto del contenido legal.
 * @param onDismiss Callback al cerrar el diálogo.
 */
@Composable
private fun LegalDialog(title: String, content: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                // Contenido scrolleable
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text  = content,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 20.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                ) {
                    Text("Entendido")
                }
            }
        }
    }
}

/**
 * Barra de fuerza de contraseña con indicador visual y textual.
 *
 * @param password Contraseña a evaluar.
 */
@Composable
private fun PasswordStrengthBar(password: String) {
    val strength = when {
        password.length >= 12 && password.any { it.isDigit() } && password.any { it.isUpperCase() } -> 3
        password.length >= 8 -> 2
        else -> 1
    }
    val (color, label) = when (strength) {
        3    -> Color(0xFF2E7D32) to "Fuerte"
        2    -> Color(0xFFE65100) to "Media"
        else -> Color(0xFFC62828) to "Débil"
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        if (index < strength) color else Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(2.dp)
                    )
            )
            if (index < 2) Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Campo de texto reutilizable para el formulario de registro.
 *
 * @param value Valor actual del campo.
 * @param onValueChange Callback al cambiar el valor.
 * @param label Etiqueta del campo.
 * @param icon Icono a mostrar a la izquierda.
 * @param keyboardType Tipo de teclado a mostrar.
 */
@Composable
private fun RegisterField(
    value: String, onValueChange: (String) -> Unit, label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.7f)) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFF6C63FF)) },
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = fieldColors()
    )
}

/**
 * Colores personalizados para los campos de texto del registro.
 *
 * @return OutlinedTextFieldColors con tema oscuro y acento morado.
 */
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Color(0xFF6C63FF),
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    focusedTextColor     = Color.White,
    unfocusedTextColor   = Color.White,
    cursorColor          = Color(0xFF6C63FF)
)