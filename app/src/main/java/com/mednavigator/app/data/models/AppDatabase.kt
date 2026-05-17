package com.mednavigator.app.data.models

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

// Entity — one row per saved session
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val patientAge: Int,
    val patientSex: String,
    val patientCountry: String,
    val patientLanguage: String,
    val symptomText: String,
    val resultJson: String,         // serialised NavigatorResult
    val urgencyLevel: String,
    val hadPhoto: Boolean,
    val hadPdf: Boolean
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val startTime: Long,
    val endTime: Long? = null,
    val systemSummary: String? = null,
    val messageCount: Int = 0
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val conversationId: Int,
    val role: String,
    val content: String,
    val timestamp: Long,
    val messageType: String,
    val imageHash: String? = null,
    val reasoningJson: String? = null,
    val audioFilePath: String? = null,
    val responseAudioPath: String? = null,
    val voiceTranscript: String? = null   // raw STT transcript for VOICE messages
)

// DAO
@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentSessions(): List<SessionEntity>

    @Query("DELETE FROM sessions WHERE timestamp < :cutoff")
    suspend fun deleteOldSessions(cutoff: Long)
}

@Dao
interface ConversationDao {
    @Insert
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("SELECT * FROM conversations ORDER BY startTime DESC")
    suspend fun getRecentConversations(): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: Int): ConversationEntity?

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Int, title: String)
}

@Dao
interface ChatMessageDao {
    @Insert
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesForConversation(conversationId: Int): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesForConversation(conversationId: Int, limit: Int): List<ChatMessageEntity>

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: Int)
}

// Migration from v2 to v3: add audio path columns
val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN audioFilePath TEXT")
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN responseAudioPath TEXT")
    }
}

// Migration from v3 to v4: add voiceTranscript column
val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN voiceTranscript TEXT")
    }
}

// Database
@Database(
    entities = [SessionEntity::class, ConversationEntity::class, ChatMessageEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
}
