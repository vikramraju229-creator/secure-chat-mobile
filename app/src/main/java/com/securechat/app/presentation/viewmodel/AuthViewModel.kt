package com.securechat.app.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.app.domain.model.User
import com.securechat.app.domain.usecase.auth.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AuthUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    // Registration flow
    val registrationEmail: String = "",
    val registeredUserId: String = "",
    val verificationEmailSent: Boolean = false,
    val emailVerified: Boolean = false,
    // Profile setup
    val needsProfileSetup: Boolean = false,
    val profileSaved: Boolean = false,
    val usernameAvailable: Boolean? = null,
    // Password reset
    val passwordResetSent: Boolean = false,
    // Session check
    val sessionChecked: Boolean = false,
    val hasValidSession: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val sendEmailVerificationUseCase: SendEmailVerificationUseCase,
    private val checkEmailVerifiedUseCase: CheckEmailVerifiedUseCase,
    private val resendVerificationEmailUseCase: ResendVerificationEmailUseCase,
    private val sendPasswordResetUseCase: SendPasswordResetUseCase,
    private val checkUsernameAvailableUseCase: CheckUsernameAvailableUseCase,
    private val saveUserProfileUseCase: SaveUserProfileUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    getCurrentUserUseCase()
                }
                _uiState.update {
                    it.copy(
                        sessionChecked = true,
                        hasValidSession = user != null,
                        user = user,
                        isSuccess = user != null
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Session check failed", e)
                _uiState.update { it.copy(sessionChecked = true, hasValidSession = false) }
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your email") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your password") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }
            try {
                val result = withContext(Dispatchers.IO) {
                    loginUseCase(email.trim(), password)
                }
                result
                    .onSuccess { user ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = user,
                                isSuccess = true,
                                error = null,
                                emailVerified = true
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message, isSuccess = false) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "login: unexpected error", e)
                _uiState.update { it.copy(isLoading = false, error = "Unexpected error: ${e.message}", isSuccess = false) }
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "All fields are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }
            try {
                val result = withContext(Dispatchers.IO) {
                    registerUseCase(username.trim(), email.trim(), password)
                }
                result
                    .onSuccess { user ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = user,
                                isSuccess = true,
                                error = null,
                                verificationEmailSent = true,
                                registrationEmail = user.email,
                                registeredUserId = user.userId
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message, isSuccess = false) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "register: unexpected error", e)
                _uiState.update { it.copy(isLoading = false, error = "Unexpected error: ${e.message}", isSuccess = false) }
            }
        }
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val verified = withContext(Dispatchers.IO) {
                    checkEmailVerifiedUseCase()
                }
                if (verified) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            emailVerified = true,
                            needsProfileSetup = true,
                            isSuccess = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Email not verified yet. Please check your inbox and click the verification link."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkEmailVerification failed", e)
                _uiState.update { it.copy(isLoading = false, error = "Failed to check verification status") }
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    resendVerificationEmailUseCase()
                }
                result
                    .onSuccess {
                        _uiState.update { it.copy(isLoading = false, error = null, verificationEmailSent = true) }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "resendVerificationEmail failed", e)
                _uiState.update { it.copy(isLoading = false, error = "Failed to resend verification email") }
            }
        }
    }

    fun checkUsernameAvailable(username: String) {
        if (username.isBlank() || username.contains(" ")) {
            _uiState.update { it.copy(usernameAvailable = false) }
            return
        }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    checkUsernameAvailableUseCase(username.trim())
                }
                result
                    .onSuccess { available ->
                        _uiState.update { it.copy(usernameAvailable = available) }
                    }
                    .onFailure {
                        _uiState.update { it.copy(usernameAvailable = null) }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "checkUsernameAvailable failed", e)
            }
        }
    }

    fun saveProfile(fullName: String, username: String, phone: String, photoUrl: String) {
        val state = _uiState.value
        val uid = state.registeredUserId.ifBlank { state.user?.userId ?: "" }
        val email = state.registrationEmail.ifBlank { state.user?.email ?: "" }

        if (uid.isBlank()) {
            _uiState.update { it.copy(error = "No user session found") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    saveUserProfileUseCase(uid, fullName, username.trim(), email, phone, photoUrl)
                }
                result
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                profileSaved = true,
                                isSuccess = true,
                                user = it.user?.copy(
                                    fullName = fullName,
                                    username = username.trim(),
                                    phone = phone,
                                    photoUrl = photoUrl
                                )
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "saveProfile failed", e)
                _uiState.update { it.copy(isLoading = false, error = "Failed to save profile") }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your email") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    sendPasswordResetUseCase(email.trim())
                }
                result
                    .onSuccess {
                        _uiState.update { it.copy(isLoading = false, passwordResetSent = true) }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "sendPasswordReset failed", e)
                _uiState.update { it.copy(isLoading = false, error = "Failed to send password reset email") }
            }
        }
    }

    fun logout() {
        try {
            logoutUseCase()
            _uiState.update {
                AuthUiState(sessionChecked = true, hasValidSession = false)
            }
        } catch (e: Exception) {
            Log.w(TAG, "logout error", e)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetState() {
        _uiState.update {
            AuthUiState(sessionChecked = it.sessionChecked, hasValidSession = it.hasValidSession)
        }
    }
}
