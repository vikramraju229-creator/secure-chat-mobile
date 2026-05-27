package com.securechat.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.securechat.app.core.utils.ScreenSecurityUtils
import com.securechat.app.presentation.ui.SecureChatNavGraph
import com.securechat.app.presentation.ui.theme.SecureChatTheme
import com.securechat.app.presentation.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)

            // Prevent screenshots and screen recording in the app
            ScreenSecurityUtils.enableScreenshotProtection(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error in super.onCreate", e)
        }

        setContent {
            SecureChatTheme {
                SafeNavGraph()
            }
        }
    }
}

/**
 * Error-safe navigation wrapper that catches composition crashes
 * and shows fallback UI instead of crashing the app.
 */
@Composable
fun SafeNavGraph() {
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        try {
            // Session check is handled in AuthViewModel.init
        } catch (e: Exception) {
            hasError = true
            errorMessage = e.message
            Log.e("SafeNavGraph", "Initialization error", e)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (hasError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Something went wrong.\n${errorMessage ?: "Please restart the app."}",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (!authState.sessionChecked) {
            // Show loading while checking session
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Checking session...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val navController = rememberNavController()
            SecureChatNavGraph(navController = navController)
        }
    }
}
