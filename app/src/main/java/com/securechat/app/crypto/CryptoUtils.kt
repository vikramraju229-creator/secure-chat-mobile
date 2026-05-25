package com.securechat.app.crypto

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val HMAC_ALG = "HmacSHA256"
    private const val AES_ALG = "AES/GCM/NoPadding"
    private const val GCM_TAG_LEN = 128
    private const val KEY_DERIVATION_ALG = "ECDH"
    private const val CURVE = "secp256r1"

    fun generateKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance(CURVE)
        gen.initialize(256, SecureRandom())
        return gen.generateKeyPair()
    }

    fun deriveSharedSecret(privateKey: java.security.PrivateKey, publicKey: java.security.PublicKey): ByteArray {
        val ka = KeyAgreement.getInstance(KEY_DERIVATION_ALG)
        ka.init(privateKey)
        ka.doPhase(publicKey, true)
        return ka.generateSecret()
    }

    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALG)
        mac.init(SecretKeySpec(key, HMAC_ALG))
        return mac.doFinal(data)
    }

    fun hkdf(salt: ByteArray, ikm: ByteArray, info: String, length: Int): ByteArray {
        val prk = hmacSha256(salt, ikm)
        val n = (length + 31) / 32
        val t = mutableListOf(ByteArray(0))
        val infoBytes = info.encodeToByteArray()
        val result = mutableListOf<ByteArray>()

        for (i in 1..n) {
            val input = t[i - 1] + infoBytes + byteArrayOf(i.toByte())
            t.add(hmacSha256(prk, input))
            result.add(t[i])
        }
        return result.flatMap { it.toList() }.take(length).toByteArray()
    }

    fun aesEncrypt(key: ByteArray, plaintext: ByteArray, iv: ByteArray, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_ALG)
        val spec = GCMParameterSpec(GCM_TAG_LEN, iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), spec)
        cipher.updateAAD(aad)
        return cipher.doFinal(plaintext)
    }

    fun aesDecrypt(key: ByteArray, ciphertext: ByteArray, iv: ByteArray, aad: ByteArray): ByteArray? {
        return try {
            val cipher = Cipher.getInstance(AES_ALG)
            val spec = GCMParameterSpec(GCM_TAG_LEN, iv)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), spec)
            cipher.updateAAD(aad)
            cipher.doFinal(ciphertext)
        } catch (e: Exception) { null }
    }

    fun concat(vararg arrays: ByteArray): ByteArray {
        val total = arrays.sumOf { it.size }
        val result = ByteArray(total)
        var offset = 0
        for (arr in arrays) {
            arr.copyInto(result, offset)
            offset += arr.size
        }
        return result
    }

    fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    fun base64Encode(data: ByteArray): String = android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
    fun base64Decode(str: String): ByteArray = android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
}
