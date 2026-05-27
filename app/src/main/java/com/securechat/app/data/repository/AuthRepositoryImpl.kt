package com.securechat.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.securechat.app.SecureChatApplication
import com.securechat.app.data.remote.FirebaseAuthManager
import com.securechat.app.domain.exception.AuthException
import com.securechat.app.domain.model.User
import com.securechat.app.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ThreadLocalRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthManager: FirebaseAuthManager
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepositoryImpl"
        private const val USERS_COLLECTION = "users"
        private const val OTP_EXPIRY_MINUTES = 10L
    }

    /** Firestore instance is nullable; all access goes through [withFirestore] for consistency. */
    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (SecureChatApplication.isFirebaseAvailable()) {
                FirebaseFirestore.getInstance()
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Firestore not available", e)
            null
        }
    }

    /**
     * Safely executes [block] with a non-null [FirebaseFirestore] reference.
     * Returns [onError] (default: failure result) if Firestore is unavailable.
     */
    private suspend fun <T> withFirestore(
        onError: Result<T> = Result.failure(AuthException("Firestore is not available")),
        block: suspend (FirebaseFirestore) -> Result<T>
    ): Result<T> {
        val db = firestore
        return if (db != null) block(db) else onError
    }

    // ── OTP helpers ──────────────────────────────────────────────────────────

    /** Generate a cryptographically-random 6-digit code. */
    private fun generateOtpCode(): String {
        val code = ThreadLocalRandom.current().nextInt(100_000, 1_000_000)
        return code.toString()
    }

    override suspend fun generateAndStoreOtp(uid: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val code = generateOtpCode()
                val expiresAt = System.currentTimeMillis() + (OTP_EXPIRY_MINUTES * 60_000)

                val result = withFirestore { db ->
                    db.collection(USERS_COLLECTION).document(uid)
                        .update(
                            mapOf(
                                "otp.code" to code,
                                "otp.expiresAt" to expiresAt,
                                "otp.verified" to false
                            )
                        )
                        .await()
                    Log.d(TAG, "OTP stored for user $uid, expires in ${OTP_EXPIRY_MINUTES}min")
                    Result.success(code)
                }
                result
            } catch (e: Exception) {
                Log.w(TAG, "generateAndStoreOtp failed", e)
                Result.failure(AuthException("Failed to generate verification code"))
            }
        }
    }

    override suspend fun verifyOtp(uid: String, code: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                withFirestore { db ->
                    val doc = db.collection(USERS_COLLECTION).document(uid).get().await()
                    if (!doc.exists()) {
                        return@withFirestore Result.failure(AuthException("User not found"))
                    }

                    val otpCode = doc.getString("otp.code")
                    val expiresAt = doc.getLong("otp.expiresAt") ?: 0L
                    val alreadyVerified = doc.getBoolean("otp.verified") ?: false

                    if (alreadyVerified) {
                        return@withFirestore Result.success(true)
                    }

                    if (otpCode == null || otpCode.isEmpty()) {
                        return@withFirestore Result.failure(
                            AuthException("No verification code found. Request a new one.")
                        )
                    }

                    if (System.currentTimeMillis() > expiresAt) {
                        return@withFirestore Result.failure(
                            AuthException("Verification code expired. Request a new one.")
                        )
                    }

                    if (code != otpCode) {
                        return@withFirestore Result.success(false)
                    }

                    // Code is correct — mark verified in Firestore
                    db.collection(USERS_COLLECTION).document(uid)
                        .update(
                            mapOf(
                                "otp.verified" to true,
                                "emailVerified" to true,
                                "emailVerifiedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        .await()

                    Log.d(TAG, "OTP verified for user $uid")
                    Result.success(true)
                }
            } catch (e: AuthException) {
                Result.failure(e)
            } catch (e: Exception) {
                Log.w(TAG, "verifyOtp failed", e)
                Result.failure(AuthException("Failed to verify code"))
            }
        }
    }

    override suspend fun resendOtp(uid: String): Result<String> {
        // Same as generate but generates a fresh code
        return generateAndStoreOtp(uid)
    }

    // ── Auth implementation ──────────────────────────────────────────────────

    override suspend fun register(username: String, email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "register: attempting Firebase signUp for $email")
                val firebaseUser = firebaseAuthManager.signUp(email, password)

                // Update Firebase Auth display name
                try {
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()
                    currentUser?.updateProfile(profileUpdates)?.await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update display name during registration", e)
                }

                // Initialize Firestore user document with emailVerified = false
                withFirestore { db ->
                    val userData = hashMapOf(
                        "username" to username,
                        "email" to email,
                        "emailVerified" to false,
                        "createdAt" to System.currentTimeMillis(),
                        "otp" to hashMapOf(
                            "code" to "",
                            "expiresAt" to 0L,
                            "verified" to false
                        )
                    )
                    db.collection(USERS_COLLECTION).document(firebaseUser.uid)
                        .set(userData)
                        .await()
                    Result.success(Unit)
                }

                val user = User(
                    userId = firebaseUser.uid,
                    username = username,
                    email = firebaseUser.email ?: email,
                    publicKey = byteArrayOf(),
                    lastSeen = System.currentTimeMillis(),
                    isEmailVerified = false
                )
                Result.success(user)
            } catch (e: AuthException) {
                Log.w(TAG, "register failed: ${e.message}")
                Result.failure(e)
            } catch (e: Exception) {
                Log.w(TAG, "register: unexpected error", e)
                Result.failure(AuthException("Registration failed: ${e.message}"))
            }
        }
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "login: attempting Firebase signIn for $email")
                val firebaseUser = firebaseAuthManager.signIn(email, password)

                // Check email verification from Firestore (OTP-based)
                val verified = firestore?.let { db ->
                    try {
                        val doc = db.collection(USERS_COLLECTION).document(firebaseUser.uid).get().await()
                        doc.getBoolean("emailVerified") ?: false
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to read emailVerified from Firestore", e)
                        false
                    }
                } ?: false

                if (!verified) {
                    // Sign out since Firebase signed them in but we're rejecting
                    firebaseAuthManager.signOut()
                    return@withContext Result.failure(
                        AuthException("Please verify your email first. Use the OTP code sent to your email.")
                    )
                }

                // Try to get profile from Firestore
                val savedProfile = firestore?.let { db ->
                    try {
                        val doc = db.collection(USERS_COLLECTION).document(firebaseUser.uid).get().await()
                        if (doc.exists()) {
                            User(
                                userId = firebaseUser.uid,
                                username = doc.getString("username") ?: firebaseUser.displayName ?: email.substringBefore("@"),
                                email = doc.getString("email") ?: firebaseUser.email ?: email,
                                fullName = doc.getString("fullName") ?: "",
                                phone = doc.getString("phone") ?: "",
                                photoUrl = doc.getString("photoUrl") ?: "",
                                publicKey = byteArrayOf(),
                                lastSeen = System.currentTimeMillis(),
                                isEmailVerified = true
                            )
                        } else null
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to read profile from Firestore", e)
                        null
                    }
                }

                val user = savedProfile ?: User(
                    userId = firebaseUser.uid,
                    username = firebaseUser.displayName ?: email.substringBefore("@"),
                    email = firebaseUser.email ?: email,
                    publicKey = byteArrayOf(),
                    lastSeen = System.currentTimeMillis(),
                    isEmailVerified = true
                )

                Result.success(user)
            } catch (e: AuthException) {
                Log.w(TAG, "login failed: ${e.message}")
                Result.failure(e)
            } catch (e: Exception) {
                Log.w(TAG, "login: unexpected error", e)
                Result.failure(AuthException("Login failed: ${e.message}"))
            }
        }
    }

    override fun getCurrentUser(): User? {
        return try {
            val firebaseUser = firebaseAuthManager.getCurrentFirebaseUser() ?: return null
            // Check Firestore for OTP-based email verification status
            val isVerified = firestore?.let { db ->
                try {
                    val doc = db.collection(USERS_COLLECTION).document(firebaseUser.uid).get().await()
                    doc.getBoolean("emailVerified") ?: false
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read emailVerified from Firestore", e)
                    false
                }
            } ?: false

            if (!isVerified) return null

            User(
                userId = firebaseUser.uid,
                username = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "user",
                email = firebaseUser.email ?: "",
                publicKey = byteArrayOf(),
                lastSeen = System.currentTimeMillis(),
                isEmailVerified = true
            )
        } catch (e: Exception) {
            Log.w(TAG, "getCurrentUser failed", e)
            null
        }
    }

    override fun logout() {
        try {
            firebaseAuthManager.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "logout failed", e)
        }
    }

    override suspend fun isEmailVerified(): Boolean {
        return try {
            val uid = firebaseAuthManager.getCurrentFirebaseUser()?.uid ?: return false
            firestore?.let { db ->
                val doc = db.collection(USERS_COLLECTION).document(uid).get().await()
                doc.getBoolean("emailVerified") ?: false
            } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "isEmailVerified check failed", e)
            false
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return firebaseAuthManager.sendPasswordResetEmail(email)
    }

    override suspend fun checkUsernameAvailable(username: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                withFirestore(onError = Result.success(true)) { db ->
                    val existing = db.collection(USERS_COLLECTION)
                        .whereEqualTo("username", username)
                        .get()
                        .await()
                    Result.success(existing.isEmpty)
                }
            } catch (e: Exception) {
                Log.w(TAG, "checkUsernameAvailable failed", e)
                Result.failure(AuthException("Failed to check username availability"))
            }
        }
    }

    override suspend fun saveUserProfile(
        uid: String,
        fullName: String,
        username: String,
        email: String,
        phone: String,
        photoUrl: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val result = withFirestore { db ->
                    val profile = hashMapOf(
                        "fullName" to fullName,
                        "username" to username,
                        "email" to email,
                        "phone" to phone,
                        "photoUrl" to photoUrl,
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection(USERS_COLLECTION).document(uid)
                        .set(profile)
                        .await()

                    // Also update Firebase Auth display name
                    try {
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName.ifBlank { username })
                            .build()
                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            ?.updateProfile(profileUpdates)?.await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to update display name", e)
                    }

                    Log.d(TAG, "Profile saved for user $uid")
                    Result.success(Unit)
                }
                result
            } catch (e: Exception) {
                Log.w(TAG, "saveUserProfile failed", e)
                Result.failure(AuthException("Failed to save profile: ${e.message}"))
            }
        }
    }

    override suspend fun getSavedProfile(uid: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                withFirestore { db ->
                    val doc = db.collection(USERS_COLLECTION).document(uid).get().await()
                    if (doc.exists()) {
                        val user = User(
                            userId = uid,
                            username = doc.getString("username") ?: "",
                            email = doc.getString("email") ?: "",
                            fullName = doc.getString("fullName") ?: "",
                            phone = doc.getString("phone") ?: "",
                            photoUrl = doc.getString("photoUrl") ?: "",
                            publicKey = byteArrayOf(),
                            lastSeen = System.currentTimeMillis()
                        )
                        Result.success(user)
                    } else {
                        Result.failure(AuthException("Profile not found"))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "getSavedProfile failed", e)
                Result.failure(AuthException("Failed to load profile"))
            }
        }
    }
}
