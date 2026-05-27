package com.securechat.app.domain.usecase.auth

import android.util.Patterns
import com.securechat.app.domain.exception.AuthException
import com.securechat.app.domain.model.User
import com.securechat.app.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(AuthException("Please enter a valid email address"))
        }
        // Validate password not empty
        if (password.isBlank()) {
            return Result.failure(AuthException("Password cannot be empty"))
        }
        return repository.login(email.trim(), password)
    }
}

class RegisterUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(username: String, email: String, password: String): Result<User> {
        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(AuthException("Please enter a valid email address"))
        }
        // Validate username
        if (username.isBlank()) {
            return Result.failure(AuthException("Username is required"))
        }
        if (username.contains(" ")) {
            return Result.failure(AuthException("Username must not contain spaces"))
        }
        // Validate password
        val passwordError = validatePassword(password)
        if (passwordError != null) {
            return Result.failure(AuthException(passwordError))
        }
        return repository.register(username.trim(), email.trim(), password)
    }

    private fun validatePassword(password: String): String? {
        if (password.length < 8) {
            return "Password must be at least 8 characters long"
        }
        if (!password.any { it.isUpperCase() }) {
            return "Password must contain at least 1 uppercase letter"
        }
        if (!password.any { it.isDigit() }) {
            return "Password must contain at least 1 number"
        }
        if (!password.any { "!@#\$%^&*".contains(it) }) {
            return "Password must contain at least 1 special character (!@#\$%^&*)"
        }
        return null
    }
}

class LogoutUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke() {
        repository.logout()
    }
}

class SendEmailVerificationUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.sendEmailVerification()
    }
}

class CheckEmailVerifiedUseCase @Inject constructor(private val repository: AuthRepository) {
    /**
     * Reloads the Firebase user first to get the latest isEmailVerified status,
     * then returns the result. This ensures the app picks up verification
     * immediately after the user clicks the link.
     */
    suspend operator fun invoke(): Boolean {
        return repository.reloadUserAndCheckVerified()
    }
}

class ResendVerificationEmailUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.resendVerificationEmail()
    }
}

class SendPasswordResetUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return repository.sendPasswordResetEmail(email)
    }
}

class CheckUsernameAvailableUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(username: String): Result<Boolean> {
        return repository.checkUsernameAvailable(username)
    }
}

class SaveUserProfileUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(
        uid: String,
        fullName: String,
        username: String,
        email: String,
        phone: String,
        photoUrl: String
    ): Result<Unit> {
        if (fullName.isBlank()) {
            return Result.failure(AuthException("Full name is required"))
        }
        if (username.isBlank()) {
            return Result.failure(AuthException("Username is required"))
        }
        if (username.contains(" ")) {
            return Result.failure(AuthException("Username must not contain spaces"))
        }
        if (phone.isBlank()) {
            return Result.failure(AuthException("Phone number is required"))
        }
        return repository.saveUserProfile(uid, fullName, username.trim(), email, phone, photoUrl)
    }
}

class GetCurrentUserUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(): User? {
        return repository.getCurrentUser()
    }
}
