package com.securechat.app.presentation.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.securechat.app.presentation.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupSecretQuestionScreen(
    navController: NavController,
    chatId: Long,
    partnerId: String
) {
    val chatViewModel: ChatViewModel = hiltViewModel()
    val uiState by chatViewModel.uiState.collectAsState()

    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var confirmAnswer by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    // Load chat info
    LaunchedEffect(chatId) {
        chatViewModel.loadChat(chatId)
    }

    // Navigate when setup is done and chat is configured
    LaunchedEffect(uiState.chat) {
        val chat = uiState.chat
        if (chat != null && chat.secretQuestion != null && chat.creatorVerified) {
            navController.navigate("chat/$chatId/$partnerId") {
                popUpTo("chat_list")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Secret Question") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Chat Security",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Set a secret question for this chat.\nBoth participants must answer it correctly to unlock the conversation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = question,
                onValueChange = { question = it; localError = null },
                label = { Text("Your secret question") },
                placeholder = { Text("e.g. What was the name of your first pet?") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it; localError = null },
                label = { Text("Your answer") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmAnswer,
                onValueChange = { confirmAnswer = it; localError = null },
                label = { Text("Confirm answer") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Error display
            val displayError = localError ?: uiState.error
            if (displayError != null) {
                Text(
                    text = displayError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    when {
                        question.isBlank() -> localError = "Please enter a question"
                        answer.isBlank() -> localError = "Please enter an answer"
                        answer != confirmAnswer -> localError = "Answers do not match"
                        else -> chatViewModel.setSecretQuestion(chatId, question, answer)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
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
                Text("Set Question & Start Chat")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = {
                navController.navigate("chat/$chatId/$partnerId") {
                    popUpTo("chat_list")
                }
            }) {
                Text("Skip, start without secret question")
            }
        }
    }
}
