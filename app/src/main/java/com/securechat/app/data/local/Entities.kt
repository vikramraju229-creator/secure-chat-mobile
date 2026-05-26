package com.securechat.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val publicKey: ByteArray,
    val lastSeen: Long,
    val isContact: Boolean = false
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val chatId: Long = 0,
    val chatPartnerId: String, // For 1-on-1 chats
    val groupId: String? = null, // For group chats
    val lastMessage: String?,
    val lastTimestamp: Long,
    val unreadCount: Int = 0,
    // Secret question fields for chat access security
    val secretQuestion: String? = null,
    val answerHash: String? = null, // SHA-256 hash of the correct answer
    val creatorVerified: Boolean = false, // Chat creator has set the question
    val partnerVerified: Boolean = false, // Partner has answered correctly
    val isLocked: Boolean = true // Chat starts locked until both verify
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    val senderId: String,
    val content: String, // Encrypted content
    val timestamp: Long,
    val isFromMe: Boolean,
    val status: MessageStatus
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val groupId: String,
    val groupName: String,
    val creatorId: String,
    val members: List<String>, // Handled by TypeConverter
    val createdAt: Long
)

enum class MessageStatus {
    SENT, DELIVERED, READ, ERROR
}
