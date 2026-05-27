package com.securechat.app.di

import com.securechat.app.core.security.KeystoreManager
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
    fun provideKeystoreManager(): KeystoreManager = KeystoreManager()

    @Provides
    @Singleton
    fun provideSecurityManager(keystoreManager: KeystoreManager): SecurityManager {
        return SecurityManager(keystoreManager)
    }
}
