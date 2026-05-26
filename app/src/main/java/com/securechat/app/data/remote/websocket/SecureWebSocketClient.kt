package com.securechat.app.data.remote.websocket

import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import javax.inject.Inject
import javax.inject.Singleton

class SecureWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) : WebSocketListener() {
    private var webSocket: WebSocket? = null

    fun connect(url: String, token: String, onMessageReceived: (String) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()
        
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                onMessageReceived(text)
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessageReceived(bytes.utf8())
            }
        })
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun disconnect() {
        webSocket?.close(1000, "Closing connection")
        webSocket = null
    }
}
