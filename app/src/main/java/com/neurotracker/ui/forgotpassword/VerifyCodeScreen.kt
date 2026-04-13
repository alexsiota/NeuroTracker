package com.neurotracker.ui.forgotpassword

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.neurotracker.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyCodeScreen(
    navController: NavController,
    viewModel: ForgotPasswordViewModel
) {
    val focusRequester = remember { FocusRequester() }

    // Cada vez que se entra en esta pantalla: nuevo código + limpiar cuadros
    LaunchedEffect(Unit) {
        viewModel.resendCode()
        runCatching { focusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D1A), Color(0xFF1A1A2E), Color(0xFF0D0D1A))
                )
            )
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        IconButton(
            onClick  = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
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
            Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = Color(0xFF6C63FF), modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(16.dp))
            Text("Introduce el código", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Introduce el código de 6 caracteres\nque aparece a continuación.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            // Banner demo — siempre visible al montar porque resendCode() ya generó el código
            if (viewModel.demoCode.value.isNotBlank()) {
                Card(
                    shape  = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                ) {
                    Column(
                        modifier            = Modifier.padding(12.dp)
                            .semantics { contentDescription = "Código de verificación: ${viewModel.demoCode.value}" },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Modo demo", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            viewModel.demoCode.value,
                            fontSize     = 26.sp,
                            fontWeight   = FontWeight.Bold,
                            color        = Color(0xFF4E342E),
                            letterSpacing = 4.sp
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // 6 cuadros de verificación (era 7, reducido para que entren mejor)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics {
                    contentDescription = "Código: ${viewModel.enteredCode.value}. ${viewModel.enteredCode.value.length} de 6."
                }
            ) {
                repeat(6) { index ->
                    val char    = viewModel.enteredCode.value.getOrNull(index)
                    val filled  = char != null
                    val current = index == viewModel.enteredCode.value.length

                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 56.dp)
                            .background(
                                if (filled) Color(0xFF6C63FF).copy(alpha = 0.85f)
                                else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = if (current) 2.dp else 1.dp,
                                color = when {
                                    current -> Color(0xFF6C63FF)
                                    filled  -> Color(0xFF6C63FF).copy(alpha = 0.5f)
                                    else    -> Color.White.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            char?.toString() ?: "",
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 20.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // TextField invisible de 1dp — captura el teclado
            OutlinedTextField(
                value           = viewModel.enteredCode.value,
                onValueChange   = viewModel::onCodeChange,
                modifier        = Modifier.size(1.dp).focusRequester(focusRequester),
                singleLine      = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType   = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Characters
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            TextButton(
                onClick  = { runCatching { focusRequester.requestFocus() } },
                modifier = Modifier.semantics { contentDescription = "Pulsa para escribir el código" }
            ) {
                Text("Pulsa aquí para escribir", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick  = { viewModel.verifyCode(onSuccess = { navController.navigate(Routes.RESET_PASSWORD) }) },
                enabled  = viewModel.enteredCode.value.length == 6,
                modifier = Modifier.fillMaxWidth().height(52.dp)
                    .semantics { contentDescription = "Verificar código" },
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFF6C63FF),
                    disabledContainerColor = Color(0xFF6C63FF).copy(alpha = 0.4f)
                )
            ) {
                Text("Verificar código", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick  = { viewModel.resendCode() },
                modifier = Modifier.semantics { contentDescription = "Generar nuevo código" }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF6C63FF), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Generar nuevo código", color = Color(0xFF6C63FF))
            }

            viewModel.errorStep2.value?.let { error ->
                Spacer(Modifier.height(8.dp))
                Card(
                    shape  = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Row(
                        modifier           = Modifier.padding(12.dp)
                            .semantics { contentDescription = "Error: $error" },
                        verticalAlignment  = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(error, color = Color(0xFFC62828), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
