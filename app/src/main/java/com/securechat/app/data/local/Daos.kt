package com.securechat.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY lastTimestamp DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE chatId = :chatId")
    fun getChatById(chatId: Long): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE chatPartnerId = :partnerId OR groupId = :groupId LIMIT 1")
    suspend fun getChat(partnerId: String, groupId: String?): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity): Long

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("UPDATE chats SET secretQuestion = :question, answerHash = :answerHash, creatorVerified = 1, isLocked = 1 WHERE chatId = :chatId")
    suspend fun setSecretQuestion(chatId: Long, question: String, answerHash: String)

    @Query("UPDATE chats SET partnerVerified = 1, isLocked = CASE WHEN creatorVerified = 1 THEN 0 ELSE 1 END WHERE chatId = :chatId")
    suspend fun verifyPartnerAnswer(chatId: Long)

    @Query("SELECT * FROM chats WHERE chatId = :chatId")
    suspend fun getChatByIdSync(chatId: Long): ChatEntity?
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: Long, status: MessageStatus)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups WHERE groupId = :groupId")
    suspend fun getGroupById(groupId: String): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)
}
