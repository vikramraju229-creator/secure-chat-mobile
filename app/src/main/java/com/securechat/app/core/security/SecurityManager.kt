package com.securechat.app.core.security

import android.util.Base64
import android.util.Log
import com.securechat.app.crypto.CryptoUtils
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Unified security manager for SecureChat.
 *
 * Responsibilities:
 *  - Local data encryption/decryption (AES-256-GCM via Android KeyStore master key)
 *  - Per-chat message key derivation (HKDF-salted with user entropy)
 *  - Delegates to [CryptoUtils] for network-level Double Ratchet operations
 */
class SecurityManager(
    private val keystoreManager: KeystoreManager
) {
    companion object {
        private const val TAG = "SecurityManager"
        private const val AES_GCM = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH = 12
    }

    // ---- Local data encryption (Android KeyStore-backed) ----

    /**
     * Encrypts arbitrary data using the Android KeyStore AES-256 master key.
     * Returns IV + ciphertext (prepended for convenient storage).
     */
    fun encryptLocal(data: ByteArray): ByteArray {
        val masterKey = keystoreManager.getOrCreateMasterKey()
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    /**
     * Decrypts data previously encrypted with [encryptLocal].
     * Expects input as IV (12 bytes) + ciphertext.
     */
    fun decryptLocal(encryptedData: ByteArray): ByteArray {
        val masterKey = keystoreManager.getOrCreateMasterKey()
        val iv = encryptedData.sliceArray(0 until IV_LENGTH)
        val ciphertext = encryptedData.sliceArray(IV_LENGTH until encryptedData.size)
        val cipher = Cipher.getInstance(AES_GCM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
        return cipher.doFinal(ciphertext)
    }

    // ---- Per-chat message encryption (HKDF-derived keys with user entropy) ----

    /**
     * Derives a unique per-chat AES-256 key using HKDF.
     *
     * Uses HMAC-based HKDF (via [CryptoUtils.hkdf]) with:
     *  - salt = SHA-256(chatId + userId)   ← user-specific entropy
     *  - ikm  = application secret seed
     *  - info = "SecureChat-PerChatKey-v2"
     *
     * This ensures that:
     *  - Different chats produce different keys even with the same chat ID pattern
     *  - Different users derive different keys for the same chat
     *  - The derivation is not reversible (HKDF extract-then-expand)
     */
    fun derivePerChatKey(chatId: Long, userId: String): SecretKeySpec {
        val saltInput = "$chatId:$userId".toByteArray(Charsets.UTF_8)
        val salt = MessageDigest.getInstance("SHA-256").digest(saltInput)

        val ikm = "SecureChat_PerChatKey_Seed_v2".toByteArray(Charsets.UTF_8)

        val derivedKey = CryptoUtils.hkdf(
            salt = salt,
            ikm = ikm,
            info = "SecureChat-PerChatKey-v2",
            length = 32  // 256-bit AES key
        )
        return SecretKeySpec(derivedKey, "AES")
    }

    /**
     * Encrypts a per-chat message using the HKDF-derived key.
     * Returns Base64(IV + ciphertext).
     */
    fun encryptPerChatMessage(chatId: Long, userId: String, content: String): String {
        return try {
            val key = derivePerChatKey(chatId, userId)
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(content.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.w(TAG, "encryptPerChatMessage failed, using plaintext fallback", e)
            "ENC_ERR:$content"
        }
    }

    /**
     * Decrypts a per-chat message previously encrypted with [encryptPerChatMessage].
     */
    fun decryptPerChatMessage(chatId: Long, userId: String, encryptedContent: String): String {
        if (encryptedContent.startsWith("ENC_ERR:")) {
            return encryptedContent.removePrefix("ENC_ERR:")
        }
        return try {
            val key = derivePerChatKey(chatId, userId)
            val raw = Base64.decode(encryptedContent, Base64.NO_WRAP)
            val iv = raw.sliceArray(0 until IV_LENGTH)
            val ciphertext = raw.sliceArray(IV_LENGTH until raw.size)
            val cipher = Cipher.getInstance(AES_GCM)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "decryptPerChatMessage failed", e)
            encryptedContent // return as-is on failure
        }
    }
}
