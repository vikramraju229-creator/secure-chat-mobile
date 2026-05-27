package com.securechat.app.presentation.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.securechat.app.R
import com.securechat.app.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val viewModel: AuthViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Navigate on successful login with valid session
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess && uiState.user != null && uiState.emailVerified) {
            navController.navigate("chat_list") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    // Navigate to verification if user registered but not yet verified
    LaunchedEffect(uiState.verificationEmailSent) {
        if (uiState.verificationEmailSent) {
            navController.navigate("email_verification") {
                popUpTo("login") { inclusive = false }
            }
        }
    }

    // ── Dark teal → navy gradient background ──────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0A1C2E), Color(0xFF071421)),
                    start = androidx.compose.ui.geometry.Offset.Zero,
                    end = androidx.compose.ui.geometry.Offset.Infinite
                )
            )
    ) {
        // ── Top-right mini logo badge ──────────────────────────
        Image(
            painter = painterResource(id = R.drawable.ic_logo_mini),
            contentDescription = "SecureChat",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .size(36.dp)
        )

        // ── Scrollable content ─────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            // ── Center logo (large) ───────────────────────────
            Image(
                painter = painterResource(id = R.drawable.ic_logo_shield),
                contentDescription = "SecureChat Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── "SecureChat" bold white ───────────────────────
            Text(
                text = "SecureChat",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )

            // ── "Messenger" lighter weight ────────────────────
            Text(
                text = "Messenger",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Light,
                color = Color(0xFFB0BEC5),
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ── Email field ───────────────────────────────────
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; viewModel.clearError() },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = Color(0x0DFFFFFF),
                    unfocusedContainerColor = Color(0x08FFFFFF),
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Password field ─────────────────────────────────
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; viewModel.clearError() },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility
                                          else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide" else "Show",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = Color(0x0DFFFFFF),
                    unfocusedContainerColor = Color(0x08FFFFFF),
                )
            )

            // ── Forgot Password ───────────────────────────────
            TextButton(
                onClick = { navController.navigate("forgot_password") },
                enabled = !uiState.isLoading,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    "Forgot Password?",
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // ── Error message ─────────────────────────────────
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0x33EF5350)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Start
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Login Button (cyan) ───────────────────────────
            Button(
                onClick = { viewModel.login(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = if (uiState.isLoading) "Signing in..." else "Sign In",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Register link ─────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Don't have an account? ",
                    color = Color(0xFFB0BEC5),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = { navController.navigate("register") },
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        "Register",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
