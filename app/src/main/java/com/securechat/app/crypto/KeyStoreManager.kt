package com.securechat.app.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey

/**
 * Manages the user's identity key pair in Android KeyStore.
 * Keys are hardware-backed (if device supports it) and persist across app restarts.
 */
object KeyStoreManager {

    private const val KEYSTORE_TYPE = "AndroidKeyStore"
    private const val IDENTITY_KEY_ALIAS = "SecureChatIdentityKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    /**
     * Get or generate the identity EC P-256 key pair stored in Android KeyStore.
     */
    fun getOrGenerateIdentityKeyPair(): KeyPair {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
        ks.load(null)

        // Return existing key if present
        if (ks.containsAlias(IDENTITY_KEY_ALIAS)) {
            val priv = ks.getKey(IDENTITY_KEY_ALIAS, null) as PrivateKey
            val pub = ks.getCertificate(IDENTITY_KEY_ALIAS).publicKey
            return KeyPair(pub, priv)
        }

        // Generate new key via KeyPairGenerator with Android KeyStore provider
        val spec = KeyGenParameterSpec.Builder(
            IDENTITY_KEY_ALIAS,
            KeyProperties.PURPOSE_AGREE_KEY  // ECDH key agreement
        )
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
            .setKeySize(256)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false) // No biometric required for messenger usage
            .setIsStrongBoxBacked(true)          // Use StrongBox if available
            .build()

        val kpg = java.security.KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE
        )
        kpg.initialize(spec)
        return kpg.generateKeyPair()
    }

    /**
     * Check if the identity key exists in KeyStore.
     */
    fun hasIdentityKey(): Boolean {
        return try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            ks.containsAlias(IDENTITY_KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete the identity key from KeyStore (e.g., on logout).
     */
    fun deleteIdentityKey() {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
        ks.load(null)
        if (ks.containsAlias(IDENTITY_KEY_ALIAS)) {
            ks.deleteEntry(IDENTITY_KEY_ALIAS)
        }
    }
}
