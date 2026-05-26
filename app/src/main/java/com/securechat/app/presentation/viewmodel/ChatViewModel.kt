package com.securechat.app.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.app.domain.model.Chat
import com.securechat.app.domain.model.Message
import com.securechat.app.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val chat: Chat? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLocked: Boolean = true,
    val secretQuestion: String? = null,
    val answerResult: Boolean? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    fun loadChat(chatId: Long) {
        viewModelScope.launch {
            try {
                chatRepository.getChatById(chatId)
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        Log.e(TAG, "Error loading chat $chatId", e)
                        _uiState.update { it.copy(error = "Failed to load chat") }
                    }
                    .collect { chat ->
                        if (chat != null) {
                            _uiState.update {
                                it.copy(
                                    chat = chat,
                                    isLocked = chat.isLocked,
                                    secretQuestion = chat.secretQuestion
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "loadChat: unexpected error", e)
                _uiState.update { it.copy(error = "Unexpected error: ${e.message}") }
            }
        }
    }

    fun loadMessages(chatId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                chatRepository.getMessages(chatId)
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        Log.e(TAG, "Error loading messages for chat $chatId", e)
                        _uiState.update { it.copy(isLoading = false, error = "Failed to load messages") }
                    }
                    .collect { messages ->
                        _uiState.update { it.copy(isLoading = false, messages = messages) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "loadMessages: unexpected error", e)
                _uiState.update { it.copy(isLoading = false, error = "Unexpected error: ${e.message}") }
            }
        }
    }

    fun sendMessage(chatId: Long, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                val encrypted = withContext(Dispatchers.IO) {
                    encryptPerChatMessage(chatId, content)
                }
                val result = withContext(Dispatchers.IO) {
                    chatRepository.sendMessage(chatId, encrypted)
                }
                result
                    .onFailure { e ->
                        _uiState.update { it.copy(error = e.message ?: "Failed to send message") }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage: unexpected error", e)
                _uiState.update { it.copy(error = "Unexpected error: ${e.message}") }
            }
        }
    }

    fun setSecretQuestion(chatId: Long, question: String, answer: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val answerHash = withContext(Dispatchers.IO) { sha256(answer) }
                val result = withContext(Dispatchers.IO) {
                    chatRepository.setSecretQuestion(chatId, question, answerHash)
                }
                result
                    .onSuccess {
                        _uiState.update { it.copy(isLoading = false, isLocked = true, secretQuestion = question) }
                        loadChat(chatId)
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to set question") }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "setSecretQuestion: unexpected error", e)
                _uiState.update { it.copy(isLoading = false, error = "Unexpected error: ${e.message}") }
            }
        }
    }

    fun verifySecretAnswer(chatId: Long, answer: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, answerResult = null) }
            try {
                val answerHash = withContext(Dispatchers.IO) { sha256(answer) }
                val result = withContext(Dispatchers.IO) {
                    chatRepository.verifySecretAnswer(chatId, answerHash)
                }
                result
                    .onSuccess { correct ->
                        if (correct) {
                            _uiState.update { it.copy(isLoading = false, isLocked = false, answerResult = true) }
                            loadMessages(chatId)
                        } else {
                            _uiState.update { it.copy(isLoading = false, answerResult = false) }
                        }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message ?: "Verification failed") }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "verifySecretAnswer: unexpected error", e)
                _uiState.update { it.copy(isLoading = false, error = "Unexpected error: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Per-chat encryption: derive a unique key from chat ID
     * so messages in different chats cannot be read across chats.
     */
    private fun encryptPerChatMessage(chatId: Long, content: String): String {
        return try {
            val chatKey = deriveChatKey(chatId)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, chatKey)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(content.toByteArray(Charsets.UTF_8))
            // Return iv + ciphertext as hex
            (iv + encrypted).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.w(TAG, "encryptPerChatMessage failed, using plaintext fallback", e)
            "ENC_ERR:$content"
        }
    }

    private fun deriveChatKey(chatId: Long): SecretKeySpec {
        val seed = "SecureChat_PerChatKey_v1_$chatId".toByteArray(Charsets.UTF_8)
        val hash = MessageDigest.getInstance("SHA-256").digest(seed)
        return SecretKeySpec(hash, "AES")
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
