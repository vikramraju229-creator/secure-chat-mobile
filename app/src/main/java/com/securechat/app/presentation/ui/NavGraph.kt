package com.securechat.app.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.securechat.app.presentation.ui.screens.auth.ForgotPasswordScreen
import com.securechat.app.presentation.ui.screens.auth.OtpVerificationScreen
import com.securechat.app.presentation.ui.screens.auth.LoginScreen
import com.securechat.app.presentation.ui.screens.auth.ProfileSetupScreen
import com.securechat.app.presentation.ui.screens.auth.RegisterScreen
import com.securechat.app.presentation.ui.screens.auth.SetupSecretQuestionScreen
import com.securechat.app.presentation.ui.screens.auth.AnswerSecretQuestionScreen
import com.securechat.app.presentation.ui.screens.chat.ChatListScreen
import com.securechat.app.presentation.ui.screens.chat.ChatScreen
import com.securechat.app.presentation.viewmodel.AuthViewModel

@Composable
fun SecureChatNavGraph(
    navController: NavHostController
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    // Determine start destination based on session
    val startDestination = when {
        authState.sessionChecked && authState.hasValidSession -> "chat_list"
        else -> "login"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth screens
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("register") {
            RegisterScreen(navController = navController)
        }
        composable("otp_verification") {
            OtpVerificationScreen(navController = navController)
        }
        composable("profile_setup") {
            ProfileSetupScreen(navController = navController)
        }
        composable("forgot_password") {
            ForgotPasswordScreen(navController = navController)
        }

        // Main screens
        composable("chat_list") {
            ChatListScreen(navController = navController)
        }

        // Secret question setup (after creating a new chat)
        composable(
            route = "setup_secret_question/{chatId}/{partnerId}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.LongType },
                navArgument("partnerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
            val partnerId = backStackEntry.arguments?.getString("partnerId") ?: "Unknown"
            SetupSecretQuestionScreen(
                navController = navController,
                chatId = chatId,
                partnerId = partnerId
            )
        }

        // Answer secret question (to unlock a locked chat)
        composable(
            route = "answer_secret_question/{chatId}/{partnerId}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.LongType },
                navArgument("partnerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
            val partnerId = backStackEntry.arguments?.getString("partnerId") ?: "Unknown"
            AnswerSecretQuestionScreen(
                navController = navController,
                chatId = chatId,
                partnerId = partnerId
            )
        }

        // Chat screen
        composable(
            route = "chat/{chatId}/{partnerId}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.LongType },
                navArgument("partnerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
            val partnerId = backStackEntry.arguments?.getString("partnerId") ?: "Unknown"
            ChatScreen(navController = navController, chatId = chatId, partnerId = partnerId)
        }

        composable("profile") {
            // ProfileScreen placeholder
        }
    }
}
