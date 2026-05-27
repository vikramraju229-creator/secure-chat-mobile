package com.securechat.app.network

import android.util.Log
import com.securechat.app.crypto.CryptoUtils
import com.securechat.app.crypto.DoubleRatchet
import com.securechat.app.crypto.KeyStoreManager
import com.securechat.app.model.ChatMessage
import com.securechat.app.model.PeerUser
import com.securechat.app.model.SealedMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import okhttp3.*
import org.json.JSONObject

class SecureWebSocket(
    private val serverUrl: String,
    private val authToken: String? = null
) {
    // Internal structured scope tied to this WebSocket's lifecycle.
    // Recreated on connect() so disconnect() + reconnect() works correctly.
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ws: WebSocket? = null
    // TLS: rely on Android system trust store (no custom certificate pinning).
    // network_security_config.xml enforces system certs + blocks cleartext.
    private var client = OkHttpClient.Builder()
        .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private val _users = MutableStateFlow<List<PeerUser>>(emptyList())
    val users: StateFlow<List<PeerUser>> = _users

    private val _myId = MutableStateFlow("")
    val myId: StateFlow<String> = _myId

    private val _typingState = MutableStateFlow(false)
    val typingState: StateFlow<Boolean> = _typingState

    private var identityKeyPair = KeyStoreManager.getOrGenerateIdentityKeyPair()
    private var ratchets = mutableMapOf<String, DoubleRatchet>()
    private var verifiedPartners = mutableSetOf<String>()
    private var reconnectJob: Job? = null
    private var typingResetJob: Job? = null
    private var msgIdCounter = 0
    private var reconnectAttempts = 0

    fun connect() {
        // Recreate scope so disconnect() + reconnect() works correctly
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        _users.value = emptyList()
        _connectionState.value = false
        _myId.value = ""
        ratchets.clear()
        verifiedPartners.clear()
        msgIdCounter = 0
        reconnectAttempts = 0
        identityKeyPair = KeyStoreManager.getOrGenerateIdentityKeyPair()

        val requestBuilder = Request.Builder().url(serverUrl)
        if (!authToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $authToken")
        }
        val request = requestBuilder.build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                _connectionState.value = true
                reconnectAttempts = 0
            }

            override fun onMessage(ws: WebSocket, text: String) {
                scope.launch { handleMessage(text) }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = false
                scheduleReconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _connectionState.value = false
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectAttempts++
        val delay = (3000L * (1 shl minOf(reconnectAttempts, 5))).coerceAtMost(60000L)
        reconnectJob = scope.launch {
            delay(delay)
            connect()
        }
    }

    private suspend fun handleMessage(raw: String) {
        try {
            val json = JSONObject(raw)
            when (json.optString("type")) {
                "welcome" -> {
                    _myId.value = json.getString("id")
                    sendPublicKey()
                }
                "users" -> {
                    val usersList = mutableListOf<PeerUser>()
                    val arr = json.getJSONArray("users")
                    for (i in 0 until arr.length()) {
                        val u = arr.getJSONObject(i)
                        val uid = u.getString("id")
                        if (uid != _myId.value) {
                            usersList.add(PeerUser(uid, u.getString("publicKey")))
                        }
                    }
                    _users.value = usersList
                    usersList.forEach { setupRatchet(it) }
                }
                "sealed" -> handleSealed(json)
                "typing" -> {
                    val sender = json.optString("from")
                    if (sender.isNotEmpty() && sender != _myId.value) {
                        _typingState.value = true
                        typingResetJob?.cancel()
                        typingResetJob = scope.launch {
                            delay(3000)
                            _typingState.value = false
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SecureChat", "Invalid message", e)
        }
    }

    private fun sendPublicKey() {
        val msg = JSONObject().apply {
            put("type", "pubkey")
            put("key", CryptoUtils.base64Encode(identityKeyPair.public.encoded))
        }
        ws?.send(msg.toString())
    }

    private suspend fun setupRatchet(user: PeerUser) {
        try {
            val keyFactory = java.security.KeyFactory.getInstance("EC")
            val theirPubKey = keyFactory.generatePublic(
                java.security.spec.X509EncodedKeySpec(CryptoUtils.base64Decode(user.publicKey))
            )
            val sharedSecret = CryptoUtils.deriveSharedSecret(identityKeyPair.private, theirPubKey)
            val ratchet = DoubleRatchet()
            ratchet.initFromSharedSecret(sharedSecret)
            ratchets[user.id] = ratchet
        } catch (e: Exception) {
            Log.e("SecureChat", "Ratchet setup failed for ${user.id}", e)
        }
    }

    private suspend fun handleSealed(json: JSONObject) {
        try {
            val data = SealedMessage(
                json.getString("data"), json.getString("iv"),
                json.getString("dhrPub"), json.getInt("msgNum")
            )
            val ciphertext = CryptoUtils.base64Decode(data.data)
            val iv = CryptoUtils.base64Decode(data.iv)
            val dhrPub = CryptoUtils.base64Decode(data.dhrPub)

            for ((uid, ratchet) in ratchets) {
                val plaintext = ratchet.ratchetDecrypt(ciphertext, iv, dhrPub, data.msgNum) ?: continue
                val inner = JSONObject(String(plaintext))
                if (inner.optString("to") != _myId.value) continue

                verifiedPartners.add(uid)

                val msg = ChatMessage(
                    id = inner.optString("msgId"),
                    sender = inner.optString("from").take(6) + "\u2026",
                    text = inner.optString("text"),
                    isSent = false,
                    timestamp = inner.optLong("ts"),
                    timer = inner.optInt("timer"),
                    msgId = inner.optString("msgId")
                )
                _messages.update { it + msg }
                return
            }
        } catch (e: Exception) {
            Log.e("SecureChat", "Failed to decrypt sealed message", e)
        }
    }

    suspend fun sendMessage(text: String, targetId: String, timer: Int = 0) {
        if (_myId.value.isEmpty()) return
        val ratchet = ratchets[targetId] ?: return

        try {
            val msgId = "${_myId.value}_${++msgIdCounter}_${System.currentTimeMillis()}"
            val inner = JSONObject().apply {
                put("from", _myId.value); put("to", targetId); put("text", text)
                put("ts", System.currentTimeMillis()); put("timer", timer); put("msgId", msgId)
            }
            val result = ratchet.ratchetEncrypt(inner.toString().encodeToByteArray())
            val msg = JSONObject().apply {
                put("type", "sealed")
                put("data", CryptoUtils.base64Encode(result.ciphertext))
                put("iv", CryptoUtils.base64Encode(result.iv))
                put("dhrPub", CryptoUtils.base64Encode(result.dhrPub))
                put("msgNum", result.msgNum)
            }
            ws?.send(msg.toString())

            val chatMsg = ChatMessage(msgId, "You", text, true, System.currentTimeMillis(), timer, msgId)
            _messages.update { it + chatMsg }
        } catch (e: Exception) {
            Log.e("SecureChat", "Failed to send message", e)
        }
    }

    suspend fun sendTyping(targetId: String) {
        if (_myId.value.isEmpty()) return
        try {
            val msg = JSONObject().apply {
                put("type", "typing")
                put("to", targetId)
            }
            ws?.send(msg.toString())
        } catch (_: Exception) {}
    }

    fun disconnect() {
        reconnectJob?.cancel()
        typingResetJob?.cancel()
        scope.cancel()
        ws?.close(1000, "User disconnected")
        ws = null
    }
}
