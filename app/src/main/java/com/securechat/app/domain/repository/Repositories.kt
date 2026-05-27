package com.securechat.app.domain.repository

import com.securechat.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun register(username: String, email: String, password: String): Result<User>
    suspend fun login(email: String, password: String): Result<User>
    fun getCurrentUser(): User?
    fun logout()
    suspend fun isEmailVerified(): Boolean
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

    // ── OTP Email Verification (replaces deprecated Dynamic Links) ──
    /** Generate a 6-digit OTP and store it in Firestore under users/{uid}/otp. Returns the OTP code. */
    suspend fun generateAndStoreOtp(uid: String): Result<String>
    /** Verify the provided OTP code against Firestore. Checks expiry (10 min). */
    suspend fun verifyOtp(uid: String, code: String): Result<Boolean>
    /** Resend (regenerate) OTP — generates new code, updates Firestore. */
    suspend fun resendOtp(uid: String): Result<String>
}

interface ChatRepository {
    fun getChats(): Flow<List<Chat>>
    fun getMessages(chatId: Long): Flow<List<Message>>
    suspend fun sendMessage(chatId: Long, content: String): Result<Message>
    suspend fun createChat(partnerId: String): Result<Chat>
    suspend fun setSecretQuestion(chatId: Long, question: String, answerHash: String): Result<Unit>
    suspend fun verifySecretAnswer(chatId: Long, answerHash: String): Result<Boolean>
    suspend fun unlockChat(chatId: Long): Result<Unit>
    fun getChatById(chatId: Long): Flow<Chat?>
}

interface UserRepository {
    suspend fun getPublicKey(userId: String): Result<ByteArray>
    suspend fun uploadPublicKey(publicKey: ByteArray): Result<Unit>
    fun getContacts(): Flow<List<User>>
}
