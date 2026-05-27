package com.securechat.app.data.repository

import android.util.Log
import com.securechat.app.data.local.*
import com.securechat.app.domain.model.Chat
import com.securechat.app.domain.model.Message
import com.securechat.app.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) : ChatRepository {

    companion object {
        private const val TAG = "ChatRepositoryImpl"
    }

    override fun getChats(): Flow<List<Chat>> {
        return chatDao.getAllChats()
            .map { entities ->
                try {
                    entities.map { it.toDomain() }
                } catch (e: Exception) {
                    Log.w(TAG, "Error mapping chats", e)
                    emptyList()
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override fun getChatById(chatId: Long): Flow<Chat?> {
        return chatDao.getChatById(chatId)
            .map { entity ->
                try {
                    entity?.toDomain()
                } catch (e: Exception) {
                    Log.w(TAG, "Error mapping chat $chatId", e)
                    null
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override fun getMessages(chatId: Long): Flow<List<Message>> {
        return messageDao.getMessagesForChat(chatId)
            .map { entities ->
                try {
                    entities.map { it.toDomain() }
                } catch (e: Exception) {
                    Log.w(TAG, "Error mapping messages for chat $chatId", e)
                    emptyList()
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun sendMessage(chatId: Long, content: String): Result<Message> {
        return withContext(Dispatchers.IO) {
            try {
                val message = MessageEntity(
                    chatId = chatId,
                    senderId = "me",
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    isFromMe = true,
                    status = MessageStatus.SENT
                )
                val id = messageDao.insertMessage(message)

                // Update chat's last message
                try {
                    val chat = chatDao.getChatByIdSync(chatId)
                    if (chat != null) {
                        chatDao.updateChat(chat.copy(
                            lastMessage = content.take(50),
                            lastTimestamp = System.currentTimeMillis()
                        ))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update chat lastMessage", e)
                }

                Result.success(message.toDomain().copy(id = id))
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage failed", e)
                Result.failure(Exception("Failed to send message: ${e.message}"))
            }
        }
    }

    override suspend fun createChat(partnerId: String): Result<Chat> {
        return withContext(Dispatchers.IO) {
            try {
                val chatEntity = ChatEntity(
                    chatPartnerId = partnerId,
                    lastMessage = null,
                    lastTimestamp = System.currentTimeMillis(),
                    isLocked = true
                )
                val chatId = chatDao.insertChat(chatEntity)
                Result.success(Chat(
                    chatId = chatId,
                    partnerId = partnerId,
                    groupId = null,
                    lastMessage = null,
                    lastTimestamp = System.currentTimeMillis(),
                    unreadCount = 0,
                    isLocked = true
                ))
            } catch (e: Exception) {
                Log.e(TAG, "createChat failed", e)
                Result.failure(Exception("Failed to create chat: ${e.message}"))
            }
        }
    }

    override suspend fun setSecretQuestion(chatId: Long, question: String, answerHash: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                chatDao.setSecretQuestion(chatId, question, answerHash)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "setSecretQuestion failed", e)
                Result.failure(Exception("Failed to set secret question: ${e.message}"))
            }
        }
    }

    override suspend fun verifySecretAnswer(chatId: Long, answerHash: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val chat = chatDao.getChatByIdSync(chatId)
                if (chat == null) {
                    return@withContext Result.failure(Exception("Chat not found"))
                }
                if (chat.answerHash == null) {
                    return@withContext Result.failure(Exception("No secret question set for this chat"))
                }
                val isCorrect = chat.answerHash == answerHash
                if (isCorrect) {
                    chatDao.verifyPartnerAnswer(chatId)
                }
                Result.success(isCorrect)
            } catch (e: Exception) {
                Log.e(TAG, "verifySecretAnswer failed", e)
                Result.failure(Exception("Failed to verify answer: ${e.message}"))
            }
        }
    }

    override suspend fun unlockChat(chatId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                chatDao.unlockChat(chatId)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "unlockChat failed", e)
                Result.failure(Exception("Failed to unlock chat: ${e.message}"))
            }
        }
    }
}

// Extension functions for entity <-> domain mapping

fun ChatEntity.toDomain() = Chat(
    chatId = chatId,
    partnerId = chatPartnerId,
    groupId = groupId,
    lastMessage = lastMessage,
    lastTimestamp = lastTimestamp,
    unreadCount = unreadCount,
    secretQuestion = secretQuestion,
    isLocked = isLocked,
    creatorVerified = creatorVerified,
    partnerVerified = partnerVerified
)

fun MessageEntity.toDomain() = Message(
    id = id,
    chatId = chatId,
    senderId = senderId,
    content = content,
    timestamp = timestamp,
    isFromMe = isFromMe,
    status = status
)

fun Message.toEntity() = MessageEntity(
    id = id,
    chatId = chatId,
    senderId = senderId,
    content = content,
    timestamp = timestamp,
    isFromMe = isFromMe,
    status = status
)
