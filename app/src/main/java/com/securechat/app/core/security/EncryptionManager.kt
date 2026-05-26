package com.securechat.app.core.security

import android.util.Log
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyProperties

class EncryptionManager(private val masterKey: SecretKey) {
    private val ALGORITHM = "AES/GCM/NoPadding"
    private val TAG_LENGTH = 128
    private val IV_LENGTH = 12

    fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)
        return iv + encryptedData
    }

    fun decrypt(encryptedData: ByteArray): ByteArray {
        val iv = encryptedData.sliceArray(0 until IV_LENGTH)
        val ciphertext = encryptedData.sliceArray(IV_LENGTH until encryptedData.size)
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
        return cipher.doFinal(ciphertext)
    }
}
