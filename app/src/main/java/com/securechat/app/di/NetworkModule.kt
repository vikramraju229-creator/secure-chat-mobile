package com.securechat.app.di

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.securechat.app.SecureChatApplication
import com.securechat.app.data.remote.FirebaseAuthManager
import com.securechat.app.data.remote.api.SecureChatApi
import com.securechat.app.data.remote.websocket.SecureWebSocketClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TAG = "NetworkModule"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return try {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "OkHttp init failed, using default", e)
            OkHttpClient.Builder().build()
        }
    }

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return try {
            Retrofit.Builder()
                .baseUrl("https://api.securechat.example.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "Retrofit init failed", e)
            throw e // Retrofit is essential, let it crash
        }
    }

    @Provides
    @Singleton
    fun provideSecureChatApi(retrofit: Retrofit): SecureChatApi {
        return retrofit.create(SecureChatApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWebSocketClient(okHttpClient: OkHttpClient): SecureWebSocketClient {
        return SecureWebSocketClient(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth? {
        return try {
            if (SecureChatApplication.isFirebaseAvailable()) {
                FirebaseAuth.getInstance()
            } else {
                Log.w(TAG, "Firebase not available, auth will use local fallback")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth.getInstance() failed", e)
            null
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseAuthManager(auth: FirebaseAuth?): FirebaseAuthManager {
        return FirebaseAuthManager(auth)
    }
}
