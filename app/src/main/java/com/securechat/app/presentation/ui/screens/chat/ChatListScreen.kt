package com.securechat.app.presentation.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.securechat.app.domain.model.Chat
import com.securechat.app.presentation.viewmodel.ChatListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(navController: NavController) {
    val viewModel: ChatListViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var showNewChatDialog by remember { mutableStateOf(false) }
    var partnerIdInput by remember { mutableStateOf("") }

    // Handle newly created chat — navigate to secret question setup
    LaunchedEffect(uiState.createdChatId) {
        val chatId = uiState.createdChatId
        if (chatId != null && chatId > 0) {
            viewModel.clearCreatedChat()
            // Navigate to secret question setup with placeholder partner name
            navController.navigate("setup_secret_question/$chatId/${partnerIdInput.ifBlank { "Partner" }}")
            partnerIdInput = ""
        }
    }

    // New Chat Dialog
    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = {
                showNewChatDialog = false
                partnerIdInput = ""
            },
            title = { Text("New Chat") },
            text = {
                OutlinedTextField(
                    value = partnerIdInput,
                    onValueChange = { partnerIdInput = it; viewModel.clearError() },
                    label = { Text("Enter partner's ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (partnerIdInput.isNotBlank()) {
                            viewModel.createChat(partnerIdInput.trim())
                        }
                    },
                    enabled = partnerIdInput.isNotBlank() && !uiState.isLoading
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewChatDialog = false
                    partnerIdInput = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                actions = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.chats.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No chats yet.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap + to start a conversation!")
                        }
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.chats) { chat ->
                            ChatItem(chat = chat, onClick = {
                                if (chat.isLocked && chat.secretQuestion != null) {
                                    // Need to answer secret question first
                                    navController.navigate("answer_secret_question/${chat.chatId}/${chat.partnerId}")
                                } else if (chat.isLocked && chat.secretQuestion == null) {
                                    // Chat exists but no question set yet (shouldn't happen in normal flow)
                                    navController.navigate("chat/${chat.chatId}/${chat.partnerId}")
                                } else {
                                    navController.navigate("chat/${chat.chatId}/${chat.partnerId}")
                                }
                            })
                        }
                    }
                }
            }

            // Error snackbar
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.error!!)
                }
            }
        }
    }
}

@Composable
fun ChatItem(chat: Chat, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(chat.partnerId)
                if (chat.isLocked) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        supportingContent = { Text(chat.lastMessage ?: "No messages yet") },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTime(chat.lastTimestamp),
                    style = MaterialTheme.typography.bodySmall
                )
                if (chat.unreadCount > 0) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    )
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
