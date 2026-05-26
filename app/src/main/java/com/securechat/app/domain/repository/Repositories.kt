package com.securechat.app.domain.repository

import com.securechat.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun register(username: String, email: String, password: String): Result<User>
    suspend fun login(email: String, password: String): Result<User>
    fun getCurrentUser(): User?
    fun logout()
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun isEmailVerified(): Boolean
    suspend fun resendVerificationEmail(): Result<Unit>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun checkUsernameAvailable(username: String): Result<Boolean>
    suspend fun saveUserProfile(
        uid: String,
        fullName: String,
        username: String,
        email: String,
        phone: String,
        photoUrl: String
    ): Result<Unit>
    suspend fun getSavedProfile(uid: String): Result<User>
}

interface ChatRepository {
    fun getChats(): Flow<List<Chat>>
    fun getMessages(chatId: Long): Flow<List<Message>>
    suspend fun sendMessage(chatId: Long, content: String): Result<Message>
    suspend fun createChat(partnerId: String): Result<Chat>
    suspend fun setSecretQuestion(chatId: Long, question: String, answerHash: String): Result<Unit>
    suspend fun verifySecretAnswer(chatId: Long, answerHash: String): Result<Boolean>
    fun getChatById(chatId: Long): Flow<Chat?>
}

interface UserRepository {
    suspend fun getPublicKey(userId: String): Result<ByteArray>
    suspend fun uploadPublicKey(publicKey: ByteArray): Result<Unit>
    fun getContacts(): Flow<List<User>>
}
