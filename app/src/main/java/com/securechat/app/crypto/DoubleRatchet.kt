package com.securechat.app.crypto

import java.security.KeyPair
import java.security.PublicKey
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

class DoubleRatchet {
    private var RK: ByteArray? = null
    private var CKs: ByteArray? = null
    private var CKr: ByteArray? = null
    private var DHRs: KeyPair? = null
    private var DHRr: ByteArray? = null
    private var Ns: Int = 0
    private var Nr: Int = 0
    private var PN: Int = 0
    var initialized: Boolean = false
        private set
    private var _dhRatchetCount: Int = 0
    val dhRatchetCount: Int get() = _dhRatchetCount
    private val skippedMks = mutableMapOf<Int, ByteArray>()

    data class EncryptResult(
        val ciphertext: ByteArray,
        val iv: ByteArray,
        val dhrPub: ByteArray,
        val msgNum: Int
    )

    suspend fun initFromSharedSecret(sharedSecret: ByteArray) {
        val derived = CryptoUtils.hkdf(
            ByteArray(32),
            sharedSecret,
            "SecureChat-DoubleRatchet-Init",
            96
        )
        RK = derived.copyOfRange(0, 32)
        CKs = derived.copyOfRange(32, 64)
        CKr = derived.copyOfRange(64, 96)
        DHRs = CryptoUtils.generateKeyPair()
        DHRr = null
        Ns = 0; Nr = 0; PN = 0; _dhRatchetCount = 0
        skippedMks.clear()
        initialized = true
    }

    suspend fun ratchetEncrypt(plaintext: ByteArray): EncryptResult {
        check(initialized) { "Ratchet not initialized" }
        val mk = symmetricRatchetAdvance("send")
        val msgNum = Ns++
        val iv = CryptoUtils.randomBytes(12)

        val ourDhrPub = DHRs!!.public.encoded
        val theirDhrPub = DHRr ?: ByteArray(0)
        val ad = CryptoUtils.concat(
            "SecureChat-DoubleRatchet".encodeToByteArray(),
            ourDhrPub, theirDhrPub,
            byteArrayOf((msgNum shr 24).toByte(), (msgNum shr 16).toByte(), (msgNum shr 8).toByte(), msgNum.toByte())
        )

        val ciphertext = CryptoUtils.aesEncrypt(mk, plaintext, iv, ad)
        return EncryptResult(ciphertext, iv, ourDhrPub, msgNum)
    }

    suspend fun ratchetDecrypt(ciphertext: ByteArray, iv: ByteArray, dhrPub: ByteArray, msgNum: Int): ByteArray? {
        if (!initialized) return null

        if (DHRr == null) {
            DHRr = dhrPub
        } else if (!dhrPub.contentEquals(DHRr)) {
            dhRatchet(dhrPub)
        }

        while (Nr < msgNum) {
            val mk = symmetricRatchetAdvance("recv")
            skippedMks[Nr] = mk
            Nr++
        }

        if (skippedMks.containsKey(msgNum)) {
            val mk = skippedMks.remove(msgNum)!!
            return tryDecrypt(ciphertext, iv, dhrPub, msgNum, mk)
        }

        val mk = symmetricRatchetAdvance("recv")
        Nr++
        return tryDecrypt(ciphertext, iv, dhrPub, msgNum, mk)
    }

    private suspend fun tryDecrypt(ciphertext: ByteArray, iv: ByteArray, dhrPub: ByteArray, msgNum: Int, mk: ByteArray): ByteArray? {
        val ourDhrPub = DHRs!!.public.encoded
        val ad = CryptoUtils.concat(
            "SecureChat-DoubleRatchet".encodeToByteArray(),
            dhrPub, ourDhrPub,
            byteArrayOf((msgNum shr 24).toByte(), (msgNum shr 16).toByte(), (msgNum shr 8).toByte(), msgNum.toByte())
        )
        return CryptoUtils.aesDecrypt(mk, ciphertext, iv, ad)
    }

    private suspend fun dhRatchet(theirNewDHRPub: ByteArray) {
        PN = Ns; Ns = 0; Nr = 0
        DHRr = theirNewDHRPub
        skippedMks.clear()
        _dhRatchetCount++

        val theirKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(theirNewDHRPub))
        val dhOut = CryptoUtils.deriveSharedSecret(DHRs!!.private, theirKey)
        val recvDerived = CryptoUtils.hkdf(RK!!, dhOut, "SecureChat-DHRatchet-Recv", 64)
        RK = recvDerived.copyOfRange(0, 32)
        CKr = recvDerived.copyOfRange(32, 64)

        DHRs = CryptoUtils.generateKeyPair()
        val theirKey2 = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(DHRr!!))
        val dhOut2 = CryptoUtils.deriveSharedSecret(DHRs!!.private, theirKey2)
        val sendDerived = CryptoUtils.hkdf(RK!!, dhOut2, "SecureChat-DHRatchet-Send", 64)
        RK = sendDerived.copyOfRange(0, 32)
        CKs = sendDerived.copyOfRange(32, 64)
    }

    private suspend fun symmetricRatchetAdvance(which: String): ByteArray {
        val ck = if (which == "send") CKs else CKr
        check(ck != null) { "No chain key for $which" }
        val mk = CryptoUtils.hmacSha256(ck, byteArrayOf(0x02))
        val nck = CryptoUtils.hmacSha256(ck, byteArrayOf(0x01))
        if (which == "send") CKs = nck else CKr = nck
        return mk
    }

    fun exportState(): Map<String, Any> {
        return mapOf(
            "RK" to RK!!,
            "CKs" to CKs!!,
            "CKr" to CKr!!,
            "DHRsPriv" to DHRs!!.private.encoded,
            "DHRsPub" to DHRs!!.public.encoded,
            "DHRr" to (DHRr ?: ByteArray(0)),
            "Ns" to Ns, "Nr" to Nr, "PN" to PN,
            "dhRatchetCount" to _dhRatchetCount
        )
    }

    fun importState(state: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        RK = state["RK"] as ByteArray
        CKs = state["CKs"] as ByteArray
        CKr = state["CKr"] as ByteArray
        val privBytes = state["DHRsPriv"] as ByteArray
        val pubBytes = state["DHRsPub"] as ByteArray
        val kf = KeyFactory.getInstance("EC")
        DHRs = KeyPair(
            kf.generatePublic(X509EncodedKeySpec(pubBytes)),
            kf.generatePrivate(java.security.spec.PKCS8EncodedKeySpec(privBytes))
        )
        DHRr = (state["DHRr"] as ByteArray).takeIf { it.isNotEmpty() }
        Ns = state["Ns"] as Int
        Nr = state["Nr"] as Int
        PN = state["PN"] as Int
        _dhRatchetCount = state["dhRatchetCount"] as Int
        initialized = true
    }
}
