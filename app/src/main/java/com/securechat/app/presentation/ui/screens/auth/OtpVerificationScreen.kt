package com.securechat.app.presentation.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.securechat.app.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

/**
 * OTP Verification Screen.
 *
 * Shows 6 input boxes for the 6-digit code sent to the user's email.
 * The code is verified against Firestore (OTP-based, no Dynamic Links).
 *
 * Resend has a 60-second cooldown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(navController: NavController) {
    val viewModel: AuthViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    // 6 OTP digit inputs
    val otpDigits = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember {
        List(6) { FocusRequester() }
    }
    val focusManager = LocalFocusManager.current

    // 60-second resend cooldown
    var resendCooldown by remember { mutableIntStateOf(0) }
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    // Navigate to profile setup when email is verified
    LaunchedEffect(uiState.needsProfileSetup) {
        if (uiState.needsProfileSetup) {
            navController.navigate("profile_setup") {
                popUpTo("otp_verification") { inclusive = true }
            }
        }
    }

    // Auto-submit when all 6 digits filled
    LaunchedEffect(otpDigits.toList()) {
        if (otpDigits.all { it.isNotEmpty() }) {
            val code = otpDigits.joinToString("")
            viewModel.verifyOtp(code)
        }
    }

    // Focus first box on entry
    LaunchedEffect(Unit) {
        focusRequesters.first().requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Email Verification") })
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
            // ── Header ──
            Text(
                text = "Enter Verification Code",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We sent a 6-digit code to\n${uiState.registrationEmail.ifBlank { "your email" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Please check your inbox and spam folder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            // ── 6-digit OTP input boxes ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 0 until 6) {
                    val isFocused = otpDigits[i].isNotEmpty() || (i > 0 && otpDigits[i - 1].isNotEmpty())
                    OtpDigitBox(
                        digit = otpDigits[i],
                        onDigitChange = { newDigit ->
                            if (newDigit.length <= 1 && newDigit.all { it.isDigit() }) {
                                otpDigits[i] = newDigit
                                if (newDigit.isNotEmpty() && i < 5) {
                                    focusRequesters[i + 1].requestFocus()
                                }
                            }
                        },
                        onBackspace = {
                            if (otpDigits[i].isEmpty() && i > 0) {
                                focusRequesters[i - 1].requestFocus()
                            }
                        },
                        focusRequester = focusRequesters[i],
                        isError = uiState.error != null
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Loading / Submit State ──
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Verify Button ──
            Button(
                onClick = {
                    val code = otpDigits.joinToString("")
                    if (code.length == 6) {
                        viewModel.verifyOtp(code)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isLoading && otpDigits.all { it.isNotEmpty() }
            ) {
                Text("Verify Email")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Resend button ──
            val resendEnabled = !uiState.isLoading && resendCooldown == 0
            OutlinedButton(
                onClick = {
                    viewModel.resendOtp()
                    resendCooldown = 60
                    // Clear digits for fresh entry
                    for (i in 0 until 6) otpDigits[i] = ""
                    focusRequesters.first().requestFocus()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = resendEnabled
            ) {
                if (resendCooldown > 0) {
                    Text("Resend code in ${resendCooldown}s")
                } else {
                    Text("Resend Code")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Back to Login ──
            TextButton(
                onClick = {
                    viewModel.resetState()
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                enabled = !uiState.isLoading
            ) {
                Text("Back to Login")
            }
        }
    }
}

@Composable
private fun OtpDigitBox(
    digit: String,
    onDigitChange: (String) -> Unit,
    onBackspace: () -> Unit,
    focusRequester: FocusRequester,
    isError: Boolean
) {
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        digit.isNotEmpty() -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    OutlinedTextField(
        value = digit,
        onValueChange = { newValue ->
            if (newValue.length <= 1) {
                if (newValue.isEmpty()) {
                    onBackspace()
                }
                onDigitChange(newValue)
            }
        },
        modifier = Modifier
            .width(48.dp)
            .height(56.dp)
            .focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.headlineMedium.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { /* handled by LaunchedEffect */ }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = borderColor,
            unfocusedBorderColor = borderColor,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
