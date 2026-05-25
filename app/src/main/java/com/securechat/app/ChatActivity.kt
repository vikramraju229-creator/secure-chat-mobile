package com.securechat.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.securechat.app.databinding.ActivityChatBinding
import com.securechat.app.network.SecureWebSocket
import com.securechat.app.ui.adapters.MessageAdapter
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var ws: SecureWebSocket
    private var targetId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serverUrl = intent.getStringExtra("SERVER_URL") ?: return

        ws = SecureWebSocket(serverUrl, lifecycleScope)

        adapter = MessageAdapter()
        binding.messagesRecycler.adapter = adapter
        binding.messagesRecycler.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            ws.messages.collect { msgs ->
                adapter.submitList(msgs)
                if (msgs.isNotEmpty()) {
                    binding.messagesRecycler.smoothScrollToPosition(msgs.size - 1)
                }
            }
        }

        lifecycleScope.launch {
            ws.connectionState.collect { connected ->
                binding.toolbar.title = if (connected) "SecureChat" else "Disconnected..."
            }
        }

        lifecycleScope.launch {
            ws.users.collect { users ->
                targetId = users.firstOrNull()?.id
            }
        }

        binding.sendBtn.setOnClickListener { sendMessage() }

        ws.connect()
    }

    private fun sendMessage() {
        val text = binding.inputField.text.toString().trim()
        val target = targetId
        if (text.isBlank() || target == null) return

        lifecycleScope.launch {
            ws.sendMessage(text, target)
        }
        binding.inputField.setText("")
    }

    override fun onDestroy() {
        super.onDestroy()
        ws.disconnect()
    }
}
