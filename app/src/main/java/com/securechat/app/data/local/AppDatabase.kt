package com.securechat.app.data.local

import android.content.Context
import androidx.room.*
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [UserEntity::class, ChatEntity::class, MessageEntity::class, GroupEntity::class], 
    version = 2, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun groupDao(): GroupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, passphrase: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportOpenHelperFactory(passphrase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "secure_chat_db"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * In-memory fallback database when SQLCipher initialization fails.
         * Data is not persisted, but app doesn't crash on startup.
         */
        fun getInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
