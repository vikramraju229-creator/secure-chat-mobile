package com.securechat.app

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SecureChatApplication : Application() {

    companion object {
        private const val TAG = "SecureChatApp"
        private var firebaseAvailable = false

        fun isFirebaseAvailable(): Boolean = firebaseAvailable
    }

    override fun onCreate() {
        super.onCreate()

        // Global crash handler — catches any unhandled exception
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "UNCAUGHT EXCEPTION on thread: ${thread.name}", throwable)
            // Log the crash but don't kill the app silently
            // In production, send to Crashlytics or similar
        }

        // Check if Firebase is properly configured
        checkFirebaseAvailability()
    }

    private fun checkFirebaseAvailability() {
        try {
            val firebaseApp = com.google.firebase.FirebaseApp.initializeApp(this)
            firebaseAvailable = firebaseApp != null
            Log.d(TAG, "Firebase initialized: $firebaseAvailable")
        } catch (e: Exception) {
            firebaseAvailable = false
            Log.w(TAG, "Firebase not available — running in offline mode", e)
        }
    }
}
