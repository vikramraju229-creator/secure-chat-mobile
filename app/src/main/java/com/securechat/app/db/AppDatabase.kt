package com.securechat.app.db

import android.content.Context
import androidx.room.*

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val msgId: String,
    val sender: String,
    val text: String,
    val isSent: Boolean,
    val timestamp: Long,
    val timer: Int
)

@Entity(tableName = "ratchet_state")
data class RatchetStateEntity(
    @PrimaryKey val partnerId: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val state: ByteArray
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAll(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: MessageEntity)

    @Query("DELETE FROM messages WHERE msgId = :msgId")
    suspend fun delete(msgId: String)

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}

@Dao
interface RatchetStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(state: RatchetStateEntity)

    @Query("SELECT * FROM ratchet_state WHERE partnerId = :partnerId")
    suspend fun get(partnerId: String): RatchetStateEntity?

    @Query("DELETE FROM ratchet_state")
    suspend fun clearAll()
}

@Database(entities = [MessageEntity::class, RatchetStateEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun ratchetStateDao(): RatchetStateDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context, AppDatabase::class.java, "securechat.db")
                    .fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
