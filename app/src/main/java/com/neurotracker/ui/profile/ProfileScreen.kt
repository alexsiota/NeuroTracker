package com.neurotracker.ui.profile

/**
 * Pantalla de perfil del usuario.
 *
 * ─── Cambios respecto a la versión anterior ───────────────────────────────────
 *
 *  1. Sección Cuenta: se añade "Cambiar nombre" como primera fila.
 *  2. [ChangeEmailDialog]: pide email actual + nuevo + contraseña.
 *     Si el nuevo email ya existe, muestra sugerencias pulsables.
 *  3. [ChangeNameDialog]: nuevo diálogo para cambiar el nombre de usuario.
 *     Al guardar, la card de perfil muestra "Hola, [NuevoNombre]" de inmediato.
 *  4. La card de usuario muestra "Hola, [nombre]" como subtítulo de bienvenida.
 */

import android.Manifest
import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage

/**
 * Composable principal de la pantalla de perfil.
 *
 * @param navController  Controlador de navegación.
 * @param isDarkMode     Estado actual del modo oscuro.
 * @param onToggleTheme  Callback para cambiar el tema.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    isDarkMode:    Boolean,
    onToggleTheme: (Boolean) -> Unit
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(application))

    var showLegalDialog   by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val cameraLauncher  = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> viewModel.onCameraTaken(success) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> viewModel.onGalleryImageSelected(uri) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            // Card de usuario con foto y saludo
            UserPhotoCard(viewModel)

            Spacer(Modifier.height(20.dp))

            // ── CUENTA ───────────────────────────────────────────────────────
            SectionLabel("Cuenta")
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column {
                    // Cambiar nombre (nuevo)
                    SettingsRow(Icons.Default.Person, "Cambiar nombre", subtitle = viewModel.userName.value, onClick = { viewModel.openChangeName() })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(Icons.Default.Lock,  "Cambiar contraseña", onClick = { viewModel.openChangePassword() })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(Icons.Default.Email, "Cambiar email", subtitle = viewModel.userEmail.value, onClick = { viewModel.openChangeEmail() })
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── APLICACIÓN ───────────────────────────────────────────────────
            SectionLabel("Aplicación")
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = if (isDarkMode) "Modo oscuro" else "Modo claro", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Modo oscuro", style = MaterialTheme.typography.bodyMedium)
                            Text(if (isDarkMode) "Activado" else "Desactivado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isDarkMode, onCheckedChange = { onToggleTheme(it) })
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRowDisabled(Icons.Default.Language,      "Idioma",         "Español")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRowDisabled(Icons.Default.Notifications, "Notificaciones", "Gestionar alertas")
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── SOPORTE ───────────────────────────────────────────────────────
            SectionLabel("Soporte")
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                SettingsRow(Icons.Default.Help, "Contactar con soporte", subtitle = "support@neurotracker.app", onClick = { showSupportDialog = true })
            }

            Spacer(Modifier.height(20.dp))

            // ── LEGAL ─────────────────────────────────────────────────────────
            SectionLabel("Legal")
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column {
                    SettingsRow(Icons.Default.Description, "Términos y condiciones", onClick = { showLegalDialog = true })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(Icons.Default.PrivacyTip,  "Política de privacidad", onClick = { showLegalDialog = true })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(Icons.Default.Gavel,       "Aviso legal",            onClick = { showLegalDialog = true })
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── INFORMACIÓN ───────────────────────────────────────────────────
            SectionLabel("Información")
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Sobre NeuroTracker", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Aplicación educativa desarrollada como TFC para simular mediciones de rendimiento cognitivo mediante tests validados.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("🧪 Kotlin · Jetpack Compose · Room", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("🎓 Proyecto Académico · 2026",       style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("👤 Álex Siota",                      style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Cerrar sesión ─────────────────────────────────────────────────
            Button(
                onClick = {
                    viewModel.logout()
                    navController.navigate("welcome") { popUpTo("main") { inclusive = true } }
                },
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(48.dp).semantics { contentDescription = "Cerrar sesión" }
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión")
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // BottomSheet foto
    if (viewModel.showPhotoOptions.value) {
        PhotoOptionsBottomSheet(
            viewModel = viewModel,
            onCamera  = { viewModel.closePhotoOptions(); cameraPermissionLauncher.launch(Manifest.permission.CAMERA); val uri = viewModel.prepareCameraUri(); cameraLauncher.launch(uri) },
            onGallery = { viewModel.closePhotoOptions(); galleryLauncher.launch("image/*") }
        )
    }

    if (showLegalDialog)                    LegalDialog(onDismiss = { showLegalDialog = false })
    if (showSupportDialog)                  SupportDialog(onDismiss = { showSupportDialog = false })
    if (viewModel.showChangePassword.value) ChangePasswordDialog(viewModel)
    if (viewModel.showChangeEmail.value)    ChangeEmailDialog(viewModel)
    if (viewModel.showChangeName.value)     ChangeNameDialog(viewModel)
}

// ─── COMPONENTES PRIVADOS ─────────────────────────────────────────────────────

/**
 * Card de usuario con foto y saludo "Hola, [nombre]".
 *
 * @param viewModel ViewModel con URI, photoVersion y datos del usuario.
 */
@Composable
private fun UserPhotoCard(viewModel: ProfileViewModel) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable { viewModel.openPhotoOptions() }
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .semantics { contentDescription = "Foto de perfil. Pulsa para cambiar" },
                contentAlignment = Alignment.Center
            ) {
                key(viewModel.photoVersion.intValue) {
                    if (viewModel.profilePhotoUri.value != null) {
                        AsyncImage(model = viewModel.profilePhotoUri.value, contentDescription = "Foto de perfil", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = "Avatar por defecto", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
                Box(modifier = Modifier.align(Alignment.BottomEnd).size(22.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                }
            }

            Spacer(Modifier.width(16.dp))

            Column {
                // Saludo con el nombre actual
                Text(
                    "Hola, ${viewModel.userName.value.ifBlank { "Usuario" }}",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(viewModel.userEmail.value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Pulsa para cambiar foto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/**
 * BottomSheet de opciones de foto de perfil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoOptionsBottomSheet(viewModel: ProfileViewModel, onCamera: () -> Unit, onGallery: () -> Unit) {
    ModalBottomSheet(onDismissRequest = { viewModel.closePhotoOptions() }) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text("Cambiar foto de perfil", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            HorizontalDivider()
            ListItem(headlineContent = { Text("Hacer una foto") },     leadingContent = { Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.primary) }, modifier = Modifier.clickable { onCamera() }.semantics { contentDescription = "Hacer foto con la cámara" })
            ListItem(headlineContent = { Text("Elegir de la galería") }, leadingContent = { Icon(Icons.Default.Photo, null, tint = MaterialTheme.colorScheme.primary) },     modifier = Modifier.clickable { onGallery() }.semantics { contentDescription = "Elegir de la galería" })
            if (viewModel.profilePhotoUri.value != null) {
                ListItem(
                    headlineContent   = { Text("Eliminar foto", color = MaterialTheme.colorScheme.error) },
                    leadingContent    = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    supportingContent = { Text("Vuelve al avatar por defecto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier          = Modifier.clickable { viewModel.removeProfilePhoto() }.semantics { contentDescription = "Eliminar foto y volver al avatar por defecto" }
                )
            }
            ListItem(headlineContent = { Text("Cancelar", color = MaterialTheme.colorScheme.error) }, leadingContent = { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }, modifier = Modifier.clickable { viewModel.closePhotoOptions() })
        }
    }
}

/**
 * Diálogo para cambiar el nombre del usuario.
 *
 * Solo actualiza el campo `name` en Room. Al completarse muestra
 * "Hola, [NuevoNombre]" en la card de perfil de inmediato.
 *
 * @param viewModel ViewModel con el estado y la lógica de cambio de nombre.
 */
@Composable
private fun ChangeNameDialog(viewModel: ProfileViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.closeChangeName() },
        title = { Text("Cambiar nombre") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (viewModel.nameSuccess.value) {
                    Text("✅ Nombre actualizado. Hola, ${viewModel.userName.value}", color = Color(0xFF2E7D32))
                } else {
                    OutlinedTextField(
                        value         = viewModel.newName.value,
                        onValueChange = viewModel::onNewNameChange,
                        label         = { Text("Nuevo nombre") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true
                    )
                    viewModel.nameError.value?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            if (viewModel.nameSuccess.value)
                TextButton(onClick = { viewModel.closeChangeName() }) { Text("Cerrar") }
            else
                TextButton(onClick = { viewModel.submitChangeName() }) { Text("Guardar") }
        },
        dismissButton = {
            if (!viewModel.nameSuccess.value)
                TextButton(onClick = { viewModel.closeChangeName() }) { Text("Cancelar") }
        }
    )
}

/**
 * Diálogo para cambiar el email del usuario.
 *
 * Solicita: email actual (verificación de identidad), nuevo email y contraseña.
 * Si el nuevo email está ocupado, muestra sugerencias pulsables.
 *
 * @param viewModel ViewModel con el estado y la lógica de cambio de email.
 */
@Composable
private fun ChangeEmailDialog(viewModel: ProfileViewModel) {
    var showPass by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { viewModel.closeChangeEmail() },
        title = { Text("Cambiar email") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (viewModel.emailSuccess.value) {
                    Text("✅ Email actualizado correctamente", color = Color(0xFF2E7D32))
                } else {
                    // FIX: pide email actual para verificar identidad
                    OutlinedTextField(
                        value         = viewModel.currentEmail.value,
                        onValueChange = viewModel::onCurrentEmailChange,
                        label         = { Text("Email actual") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true
                    )
                    OutlinedTextField(
                        value         = viewModel.newEmail.value,
                        onValueChange = viewModel::onNewEmailChange,
                        label         = { Text("Nuevo email") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true
                    )
                    PasswordField("Contraseña actual", viewModel.emailPassword.value, viewModel::onEmailPasswordChange, showPass) { showPass = !showPass }

                    viewModel.emailError.value?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    // FIX: sugerencias si el email ya existe
                    if (viewModel.emailSuggestions.value.isNotEmpty()) {
                        Text("Sugerencias disponibles:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        viewModel.emailSuggestions.value.forEach { suggestion ->
                            Surface(
                                shape    = RoundedCornerShape(8.dp),
                                color    = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onNewEmailChange(suggestion) }
                            ) {
                                Text(
                                    suggestion,
                                    style    = MaterialTheme.typography.bodySmall,
                                    color    = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (viewModel.emailSuccess.value)
                TextButton(onClick = { viewModel.closeChangeEmail() }) { Text("Cerrar") }
            else
                TextButton(onClick = { viewModel.submitChangeEmail() }) { Text("Guardar") }
        },
        dismissButton = {
            if (!viewModel.emailSuccess.value)
                TextButton(onClick = { viewModel.closeChangeEmail() }) { Text("Cancelar") }
        }
    )
}

/**
 * Diálogo de cambio de contraseña.
 */
@Composable
private fun ChangePasswordDialog(viewModel: ProfileViewModel) {
    var showCurrent by remember { mutableStateOf(false) }
    var showNew     by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { viewModel.closeChangePassword() },
        title = { Text("Cambiar contraseña") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (viewModel.passwordSuccess.value) {
                    Text("✅ Contraseña actualizada correctamente", color = Color(0xFF2E7D32))
                } else {
                    PasswordField("Contraseña actual",          viewModel.currentPassword.value, viewModel::onCurrentPasswordChange, showCurrent) { showCurrent = !showCurrent }
                    PasswordField("Nueva contraseña",           viewModel.newPassword.value,     viewModel::onNewPasswordChange,     showNew)     { showNew = !showNew }
                    PasswordField("Confirmar nueva contraseña", viewModel.confirmPassword.value, viewModel::onConfirmPasswordChange, showConfirm) { showConfirm = !showConfirm }
                    viewModel.passwordError.value?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = {
            if (viewModel.passwordSuccess.value) TextButton(onClick = { viewModel.closeChangePassword() }) { Text("Cerrar") }
            else                                 TextButton(onClick = { viewModel.submitChangePassword() }) { Text("Guardar") }
        },
        dismissButton = {
            if (!viewModel.passwordSuccess.value) TextButton(onClick = { viewModel.closeChangePassword() }) { Text("Cancelar") }
        }
    )
}

@Composable
private fun LegalDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.Gavel, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Términos, Privacidad y Aviso Legal") },
        text  = {
            Column {
                Text("Términos y condiciones", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text("Aplicación educativa. Uso sujeto a aceptación de términos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text("Política de privacidad", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text("Los datos se almacenan localmente en el dispositivo mediante Room. No se transfieren a servidores externos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text("Aviso legal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text("TFC del Ciclo Formativo de Grado Superior en DAM. Sin carácter comercial ni clínico.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun SupportDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.Help, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Contactar con soporte") },
        text  = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Escríbenos y te responderemos en un plazo de 48 horas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("support@neurotracker.app", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("L–V · 9:00 – 18:00", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit, visible: Boolean, onToggle: () -> Unit) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label) },
        modifier             = Modifier.fillMaxWidth(),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine           = true,
        trailingIcon         = {
            IconButton(onClick = onToggle) {
                Icon(if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = if (visible) "Ocultar" else "Mostrar")
            }
        }
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(if (subtitle != null) 64.dp else 52.dp).clickable { onClick() }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsRowDisabled(icon: ImageVector, title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().height(if (subtitle != null) 64.dp else 52.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), maxLines = 1) }
        }
        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text("Próximamente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp)
        }
    }
}
