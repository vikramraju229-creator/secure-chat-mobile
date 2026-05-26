package com.securechat.app.domain.exception

open class AuthException(message: String) : Exception(message)

class FirebaseNotAvailableException : AuthException("Firebase is not available. Running in offline mode.")
