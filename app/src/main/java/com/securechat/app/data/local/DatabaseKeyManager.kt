package com.securechat.app.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

class DatabaseKeyManager(context: Context) {

    companion object {
        private const val TAG = "DatabaseKeyManager"
        private const val FALLBACK_KEY = "fallback_db_passphrase"
    }

    private val sharedPreferences: SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_chat_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w(TAG, "EncryptedSharedPreferences failed, using fallback", e)
        // Fallback: use regular SharedPreferences (data still encrypted at DB level by SQLCipher)
        context.getSharedPreferences("secure_chat_prefs_fallback", Context.MODE_PRIVATE)
    }

    fun getDatabaseKey(): ByteArray {
        return try {
            val keyString = sharedPreferences?.getString("db_passphrase", null)
            if (keyString != null) {
                keyString.toByteArray(Charsets.UTF_8)
            } else {
                val newKey = generateRandomKey()
                sharedPreferences?.edit()?.putString("db_passphrase", newKey.decodeToString())?.apply()
                newKey
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get DB key, generating fresh one", e)
            generateRandomKey()
        }
    }

    private fun generateRandomKey(): ByteArray {
        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        return key
    }
}
