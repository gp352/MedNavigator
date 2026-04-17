package com.mednavigator.app.data.models

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

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

// Database
@Database(entities = [SessionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
