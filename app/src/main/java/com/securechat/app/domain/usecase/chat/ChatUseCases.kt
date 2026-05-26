package com.securechat.app.domain.usecase.chat

import com.securechat.app.domain.model.*
import com.securechat.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatsUseCase @Inject constructor(private val repository: ChatRepository) {
    operator fun invoke(): Flow<List<Chat>> = repository.getChats()
}

class GetMessagesUseCase @Inject constructor(private val repository: ChatRepository) {
    operator fun invoke(chatId: Long): Flow<List<Message>> = repository.getMessages(chatId)
}

class SendMessageUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: Long, content: String): Result<Message> {
        return repository.sendMessage(chatId, content)
    }
}

class CreateChatUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(partnerId: String): Result<Chat> {
        return repository.createChat(partnerId)
    }
}

class SetSecretQuestionUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: Long, question: String, answerHash: String): Result<Unit> {
        return repository.setSecretQuestion(chatId, question, answerHash)
    }
}

class VerifySecretAnswerUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: Long, answerHash: String): Result<Boolean> {
        return repository.verifySecretAnswer(chatId, answerHash)
    }
}
