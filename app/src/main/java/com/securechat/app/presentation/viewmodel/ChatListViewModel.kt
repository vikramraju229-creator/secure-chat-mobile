package com.securechat.app.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.app.domain.model.Chat
import com.securechat.app.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ChatListUiState(
    val chats: List<Chat> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdChatId: Long? = null
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ChatListViewModel"
    }

    private val _uiState = MutableStateFlow(ChatListUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        loadChats()
    }

    fun loadChats() {
        viewModelScope.launch {
            try {
                chatRepository.getChats()
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        Log.e(TAG, "Error loading chats", e)
                        _uiState.update { it.copy(isLoading = false, error = "Failed to load chats") }
                    }
                    .collect { chats ->
                        _uiState.update { it.copy(chats = chats, isLoading = false) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "loadChats: unexpected error", e)
                _uiState.update { it.copy(isLoading = false, error = "Unexpected error: ${e.message}") }
            }
        }
    }

    fun createChat(partnerId: String) {
        if (partnerId.isBlank()) {
            _uiState.update { it.copy(error = "Partner ID is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    chatRepository.createChat(partnerId.trim())
                }
                result
                    .onSuccess { chat ->
                        _uiState.update { it.copy(isLoading = false, createdChatId = chat.chatId) }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to create chat") }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "createChat: unexpected error", e)
                _uiState.update { it.copy(isLoading = false, error = "Unexpected error: ${e.message}") }
            }
        }
    }

    fun clearCreatedChat() {
        _uiState.update { it.copy(createdChatId = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
