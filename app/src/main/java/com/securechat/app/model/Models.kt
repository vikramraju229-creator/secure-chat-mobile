package com.securechat.app.model

data class PeerUser(
    val id: String,
    val publicKey: String
)

data class SealedMessage(
    val data: String,
    val iv: String,
    val dhrPub: String,
    val msgNum: Int
)

data class IncomingMessage(
    val type: String,
    val data: String? = null,
    val iv: String? = null,
    val dhrPub: String? = null,
    val msgNum: Int? = null,
    val id: String? = null,
    val users: List<PeerUser>? = null,
    val from: String? = null,
    val text: String? = null
)

data class ChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val isSent: Boolean,
    val timestamp: Long,
    val timer: Int = 0,
    val msgId: String = ""
)
