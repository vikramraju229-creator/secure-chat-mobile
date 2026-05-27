package com.securechat.app.data.remote

import android.util.Log
import android.util.Patterns
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.securechat.app.SecureChatApplication
import com.securechat.app.domain.exception.AuthException
import com.securechat.app.domain.exception.FirebaseNotAvailableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseAuthManager(private val auth: FirebaseAuth?) {

    companion object {
        private const val TAG = "FirebaseAuthManager"
    }

    private fun isFirebaseReady(): Boolean {
        return try {
            auth != null && SecureChatApplication.isFirebaseAvailable()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase not ready", e)
            false
        }
    }

    private fun requireAuth(): FirebaseAuth {
        if (!isFirebaseReady() || auth == null) {
            throw FirebaseNotAvailableException()
        }
        return auth
    }

    fun getCurrentFirebaseUser(): FirebaseUser? {
        return try {
            if (isFirebaseReady()) auth?.currentUser else null
        } catch (e: Exception) {
            Log.w(TAG, "getCurrentFirebaseUser failed", e)
            null
        }
    }

    suspend fun signIn(email: String, password: String): FirebaseUser {
        val fbAuth = requireAuth()
        return withContext(Dispatchers.IO) {
            try {
                val result = fbAuth.signInWithEmailAndPassword(email, password).await()
                result.user ?: throw AuthException("Login failed: unknown error")
            } catch (e: FirebaseAuthInvalidUserException) {
                throw AuthException("No account found with this email")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                throw AuthException("Incorrect password")
            } catch (e: AuthException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "signIn failed: ${e.message}", e)
                throw AuthException(e.message ?: "Login failed")
            }
        }
    }

    suspend fun signUp(email: String, password: String): FirebaseUser {
        val fbAuth = requireAuth()
        return withContext(Dispatchers.IO) {
            try {
                val result = fbAuth.createUserWithEmailAndPassword(email, password).await()
                result.user ?: throw AuthException("Registration failed: unknown error")
            } catch (e: FirebaseAuthWeakPasswordException) {
                throw AuthException("Password is too weak. Please use a stronger password.")
            } catch (e: FirebaseAuthUserCollisionException) {
                throw AuthException("An account already exists with this email")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                throw AuthException("Invalid email format")
            } catch (e: AuthException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "signUp failed: ${e.message}", e)
                throw AuthException(e.message ?: "Registration failed")
            }
        }
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val fbAuth = requireAuth()
            val user = fbAuth.currentUser ?: return Result.failure(AuthException("No user is signed in"))
            withContext(Dispatchers.IO) {
                // Use ActionCodeSettings for better delivery and longer expiry
                val actionCodeSettings = ActionCodeSettings.newBuilder()
                    .setHandleCodeInApp(false)
                    .setUrl("https://chat-8d3c9.firebaseapp.com/__/auth/action")
                    .build()
                user.sendEmailVerification(actionCodeSettings).await()
            }
            Result.success(Unit)
        } catch (e: AuthException) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.w(TAG, "sendEmailVerification failed", e)
            Result.failure(AuthException("Failed to send verification email"))
        }
    }

    /**
     * Reloads the current Firebase user and returns the latest isEmailVerified status.
     * Call this after the user clicks the verification link to get the updated status.
     */
    suspend fun reloadUserAndCheckVerified(): Boolean {
        return try {
            if (!isFirebaseReady()) return false
            val user = auth?.currentUser ?: return false
            withContext(Dispatchers.IO) {
                user.reload().await()
            }
            user.isEmailVerified
        } catch (e: Exception) {
            Log.w(TAG, "reloadUser failed", e)
            // Fall back to cached value
            auth?.currentUser?.isEmailVerified ?: false
        }
    }

    fun isEmailVerified(): Boolean {
        return try {
            if (!isFirebaseReady()) return false
            val user = auth?.currentUser ?: return false
            user.isEmailVerified
        } catch (e: Exception) {
            Log.w(TAG, "isEmailVerified check failed", e)
            false
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            val fbAuth = requireAuth()
            withContext(Dispatchers.IO) {
                fbAuth.sendPasswordResetEmail(email).await()
            }
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(AuthException("No account found with this email"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(AuthException("Invalid email format"))
        } catch (e: AuthException) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.w(TAG, "sendPasswordResetEmail failed", e)
            Result.failure(AuthException("Failed to send password reset email"))
        }
    }

    fun signOut() {
        try {
            if (isFirebaseReady()) auth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "signOut failed", e)
        }
    }
}
