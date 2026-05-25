package com.securechat.app.network

import com.securechat.app.crypto.CryptoUtils
import com.securechat.app.crypto.DoubleRatchet
import com.securechat.app.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject

class SecureWebSocket(
    private val serverUrl: String,
    private val scope: CoroutineScope
) {
    private var ws: WebSocket? = null
    private var client = OkHttpClient.Builder()
        .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _myId = MutableStateFlow("")
    val myId: StateFlow<String> = _myId

    private var identityKeyPair = CryptoUtils.generateKeyPair()
    private var ratchets = mutableMapOf<String, DoubleRatchet>()
    private var verifiedPartners = mutableSetOf<String>()
    private var reconnectJob: Job? = null
    private var msgIdCounter = 0

    fun connect() {
        val request = Request.Builder().url(serverUrl).build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                _connectionState.value = true
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
        reconnectJob = scope.launch {
            delay(3000)
            connect()
        }
    }

    private suspend fun handleMessage(raw: String) {
        val json = JSONObject(raw)
        when (json.optString("type")) {
            "welcome" -> {
                _myId.value = json.getString("id")
                sendPublicKey()
            }
            "users" -> {
                val usersList = mutableListOf<User>()
                val arr = json.getJSONArray("users")
                for (i in 0 until arr.length()) {
                    val u = arr.getJSONObject(i)
                    val uid = u.getString("id")
                    if (uid != _myId.value) {
                        usersList.add(User(uid, u.getString("publicKey")))
                    }
                }
                _users.value = usersList
                usersList.firstOrNull()?.let { setupRatchet(it) }
            }
            "sealed" -> handleSealed(json)
        }
    }

    private fun sendPublicKey() {
        val pubB64 = CryptoUtils.base64Encode(identityKeyPair.public.encoded)
        val msg = JSONObject().apply {
            put("type", "pubkey")
            put("key", pubB64)
        }
        ws?.send(msg.toString())
    }

    private suspend fun setupRatchet(user: User) {
        try {
            val theirPubKey = java.security.KeyFactory.getInstance("EC")
                .generatePublic(java.security.spec.X509EncodedKeySpec(CryptoUtils.base64Decode(user.publicKey)))
            val sharedSecret = CryptoUtils.deriveSharedSecret(identityKeyPair.private, theirPubKey)
            val ratchet = DoubleRatchet()
            ratchet.initFromSharedSecret(sharedSecret)
            ratchets[user.id] = ratchet
        } catch (e: Exception) { e.printStackTrace() }
    }

    private suspend fun handleSealed(json: JSONObject) {
        val data = SealedMessage(
            json.getString("data"), json.getString("iv"),
            json.getString("dhrPub"), json.getInt("msgNum")
        )
        val ciphertext = CryptoUtils.base64Decode(data.data)
        val iv = CryptoUtils.base64Decode(data.iv)
        val dhrPub = CryptoUtils.base64Decode(data.dhrPub)

        for ((uid, ratchet) in ratchets) {
            try {
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
                _messages.value = _messages.value + msg
                return
            } catch (_: Exception) {}
        }
    }

    suspend fun sendMessage(text: String, targetId: String, timer: Int = 0) {
        val ratchet = ratchets[targetId] ?: return
        if (targetId !in verifiedPartners) return

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
        _messages.value = _messages.value + chatMsg
    }

    fun disconnect() {
        reconnectJob?.cancel()
        ws?.close(1000, "User disconnected")
    }
}
