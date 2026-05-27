package com.securechat.app.di

import com.securechat.app.core.security.AesKeyManager
import com.securechat.app.core.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideAesKeyManager(): AesKeyManager = AesKeyManager()

    @Provides
    @Singleton
    fun provideSecurityManager(aesKeyManager: AesKeyManager): SecurityManager {
        return SecurityManager(aesKeyManager)
    }
}
