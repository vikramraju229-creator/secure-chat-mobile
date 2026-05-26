package com.securechat.app.di

import android.content.Context
import android.util.Log
import com.securechat.app.data.local.*
import com.securechat.app.data.repository.ChatRepositoryImpl
import com.securechat.app.domain.repository.ChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val TAG = "DatabaseModule"

    @Provides
    @Singleton
    fun provideDatabaseKeyManager(@ApplicationContext context: Context): DatabaseKeyManager {
        return try {
            DatabaseKeyManager(context)
        } catch (e: Exception) {
            Log.w(TAG, "DatabaseKeyManager init failed", e)
            // Will be re-created, but if it fails here the app still starts
            throw e
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyManager: DatabaseKeyManager
    ): AppDatabase {
        return try {
            AppDatabase.getDatabase(context, keyManager.getDatabaseKey())
        } catch (e: Exception) {
            Log.e(TAG, "Database initialization failed", e)
            // In-memory fallback so app doesn't crash
            AppDatabase.getInMemoryDatabase(context)
        }
    }

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideGroupDao(db: AppDatabase): GroupDao = db.groupDao()

    @Provides
    @Singleton
    fun provideChatRepository(chatDao: ChatDao, messageDao: MessageDao): ChatRepository {
        return ChatRepositoryImpl(chatDao, messageDao)
    }
}
