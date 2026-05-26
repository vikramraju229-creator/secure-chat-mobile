package com.securechat.app.core.security

import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.KeyAgreement

class KeyExchangeManager {
    private val KEY_ALGORITHM = "XDH" // X25519

    fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM)
        return kpg.generateKeyPair()
    }

    fun computeSharedSecret(privateKey: PrivateKey, publicKeyBytes: ByteArray): ByteArray {
        val kf = KeyFactory.getInstance(KEY_ALGORITHM)
        val publicKey = kf.generatePublic(X509EncodedKeySpec(publicKeyBytes))
        
        val ka = KeyAgreement.getInstance(KEY_ALGORITHM)
        ka.init(privateKey)
        ka.doPhase(publicKey, true)
        
        return ka.generateSecret()
    }
}
