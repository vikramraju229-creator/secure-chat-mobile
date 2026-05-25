package com.securechat.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.securechat.app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.urlInput.setText("wss://parrot.tail81f180.ts.net?pw=veni229")
        binding.passwordInput.setText("veni229")

        binding.connectBtn.setOnClickListener {
            val url = binding.urlInput.text.toString().trim()
            val pw = binding.passwordInput.text.toString().trim()

            if (url.isBlank() || pw.isBlank()) {
                Toast.makeText(this, "Enter URL and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("SERVER_URL", url)
                putExtra("PASSWORD", pw)
            }
            startActivity(intent)
        }
    }
}
