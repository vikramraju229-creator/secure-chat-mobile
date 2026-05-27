package com.securechat.app.presentation.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.securechat.app.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

/**
 * Email verification screen.
 *
 * Automatically polls verification status every 5 seconds for up to 3 minutes
 * so the app proceeds as soon as the user clicks the link.
 *
 * Also includes a 60-second cooldown on the "Resend" button to prevent spam.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(navController: NavController) {
    val viewModel: AuthViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var showResendConfirmation by remember { mutableStateOf(false) }

    // ── 60-second cooldown for resend button ──
    var resendCooldown by remember { mutableIntStateOf(0) }

    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    // ── Auto-poll every 5 seconds to detect verification ──
    var autoPollActive by remember { mutableStateOf(true) }
    LaunchedEffect(autoPollActive) {
        if (!autoPollActive) return@LaunchedEffect
        // Poll for up to 3 minutes (36 attempts × 5s)
        repeat(36) {
            delay(5000)
            if (!autoPollActive) return@repeat
            viewModel.checkEmailVerification()
        }
    }

    // Stop polling once verified
    LaunchedEffect(uiState.needsProfileSetup) {
        if (uiState.needsProfileSetup) {
            autoPollActive = false
        }
    }

    // Navigate to profile setup when email is verified
    LaunchedEffect(uiState.needsProfileSetup) {
        if (uiState.needsProfileSetup) {
            navController.navigate("profile_setup") {
                popUpTo("email_verification") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Email Verification") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Check your email",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We sent a verification email to\n${uiState.registrationEmail.ifBlank { "your email" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please verify your email before logging in. Check your inbox and click the verification link.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            // ── Error message ──
            if (uiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Start
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Success message after resend ──
            if (showResendConfirmation) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Verification email resent! Please check your inbox.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Auto-polling indicator ──
            if (autoPollActive && !uiState.needsProfileSetup) {
                Text(
                    text = "Auto-detecting verification status...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // ── "I've Verified My Email" button ──
            Button(
                onClick = {
                    autoPollActive = false
                    viewModel.checkEmailVerification()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("I've Verified My Email")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Resend button with 60s cooldown ──
            val resendEnabled = !uiState.isLoading && resendCooldown == 0
            OutlinedButton(
                onClick = {
                    viewModel.resendVerificationEmail()
                    showResendConfirmation = true
                    resendCooldown = 60
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = resendEnabled
            ) {
                if (resendCooldown > 0) {
                    Text("Resend available in ${resendCooldown}s")
                } else {
                    Text("Resend Verification Email")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Back to Login ──
            TextButton(
                onClick = {
                    autoPollActive = false
                    viewModel.resetState()
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                enabled = !uiState.isLoading
            ) {
                Text("Back to Login")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Firebase Console tip (informational only) ──
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Email from \"SecureChat\"? " +
                           "To fix spam folder issues, " +
                           "go to Firebase Console → Authentication → Templates → " +
                           "set Sender Name to \"SecureChat\".",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
