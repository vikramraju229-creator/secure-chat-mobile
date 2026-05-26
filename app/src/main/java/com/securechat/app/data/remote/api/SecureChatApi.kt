package com.securechat.app.data.remote.api

import retrofit2.http.*
import com.securechat.app.domain.model.User

interface SecureChatApi {
    @POST("auth/register")
    suspend fun registerUser(@Body request: RegisterRequest): UserResponse

    @POST("auth/login")
    suspend fun loginUser(@Body request: LoginRequest): UserResponse

    @GET("users/{userId}/public-key")
    suspend fun getPublicKey(@Path("userId") userId: String): PublicKeyResponse

    @POST("users/public-key")
    suspend fun uploadPublicKey(@Body request: PublicKeyRequest)

    @GET("contacts")
    suspend fun getContacts(@Header("Authorization") token: String): List<User>
}

data class RegisterRequest(val username: String, val password: String)
data class LoginRequest(val username: String, val password: String)
data class UserResponse(val userId: String, val token: String)
data class PublicKeyResponse(val publicKey: String)
data class PublicKeyRequest(val userId: String, val publicKey: String)
