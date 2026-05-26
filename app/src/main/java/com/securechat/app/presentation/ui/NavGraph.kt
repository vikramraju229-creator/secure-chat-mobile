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
import com.securechat.app.presentation.ui.screens.auth.EmailVerificationScreen
import com.securechat.app.presentation.ui.screens.auth.ForgotPasswordScreen
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
        composable("email_verification") {
            EmailVerificationScreen(navController = navController)
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
            route = "setup_secret_question/{chatId}/{partnerName}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.LongType },
                navArgument("partnerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
            val partnerName = backStackEntry.arguments?.getString("partnerName") ?: "Unknown"
            SetupSecretQuestionScreen(
                navController = navController,
                chatId = chatId,
                partnerName = partnerName
            )
        }

        // Answer secret question (to unlock a locked chat)
        composable(
            route = "answer_secret_question/{chatId}/{partnerName}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.LongType },
                navArgument("partnerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
            val partnerName = backStackEntry.arguments?.getString("partnerName") ?: "Unknown"
            AnswerSecretQuestionScreen(
                navController = navController,
                chatId = chatId,
                partnerName = partnerName
            )
        }

        // Chat screen
        composable(
            route = "chat/{chatId}/{partnerName}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.LongType },
                navArgument("partnerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
            val partnerName = backStackEntry.arguments?.getString("partnerName") ?: "Unknown"
            ChatScreen(navController = navController, chatId = chatId, partnerName = partnerName)
        }

        composable("profile") {
            // ProfileScreen placeholder
        }
    }
}
