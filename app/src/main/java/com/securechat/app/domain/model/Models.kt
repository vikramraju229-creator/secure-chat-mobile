package com.securechat.app.domain.model

import com.securechat.app.data.local.MessageStatus

data class User(
    val userId: String,
    val username: String,
    val email: String = "",
    val fullName: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val publicKey: ByteArray,
    val lastSeen: Long,
    val isEmailVerified: Boolean = false
)

data class Chat(
    val chatId: Long,
    val partnerId: String,
    val groupId: String?,
    val lastMessage: String?,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val secretQuestion: String? = null,
    val isLocked: Boolean = true,
    val creatorVerified: Boolean = false,
    val partnerVerified: Boolean = false
)

data class Message(
    val id: Long,
    val chatId: Long,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val status: MessageStatus
)
