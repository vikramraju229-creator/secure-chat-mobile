package com.securechat.app.di

import android.content.Context
import com.securechat.app.core.security.EncryptionManager
import com.securechat.app.core.security.KeyExchangeManager
import com.securechat.app.core.security.KeystoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.crypto.SecretKey

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideKeystoreManager(): KeystoreManager = KeystoreManager()

    @Provides
    @Singleton
    fun provideMasterKey(keystoreManager: KeystoreManager): SecretKey {
        return keystoreManager.getOrCreateMasterKey()
    }

    @Provides
    @Singleton
    fun provideEncryptionManager(masterKey: SecretKey): EncryptionManager {
        return EncryptionManager(masterKey)
    }

    @Provides
    @Singleton
    fun provideKeyExchangeManager(): KeyExchangeManager = KeyExchangeManager()
}
