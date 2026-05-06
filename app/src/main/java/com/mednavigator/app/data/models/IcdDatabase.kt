package com.mednavigator.app.data.models

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Index

/**
 * ICD-11 Condition Entity for local database storage with fast indexing
 */
@Entity(
    tableName = "icd_conditions",
    indices = [
        Index("code"),
        Index("title"),
        Index("bodySystem"),
        Index("chapter")
    ]
)
data class IcdConditionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val code: String,                   // e.g. "BA80"
    val title: String,                  // e.g. "Chest pain"
    val definition: String,             // plain definition from ICD-11
    val bodySystem: String,             // e.g. "Circulatory system"
    val chapter: String,                // ICD-11 chapter
    val synonyms: String,               // comma-separated synonyms
    val searchKeywords: String = ""     // extra keywords for search optimization
)

/**
 * DAO for ICD-11 Condition queries
 */
@Dao
interface IcdConditionDao {
    @Insert
    suspend fun insertCondition(condition: IcdConditionEntity): Long

    @Insert
    suspend fun insertConditions(conditions: List<IcdConditionEntity>): List<Long>

    @Query("SELECT * FROM icd_conditions WHERE code = :code LIMIT 1")
    suspend fun getConditionByCode(code: String): IcdConditionEntity?

    @Query("""
        SELECT * FROM icd_conditions 
        WHERE title LIKE '%' || :query || '%' 
        OR synonyms LIKE '%' || :query || '%'
        OR searchKeywords LIKE '%' || :query || '%'
        ORDER BY title
        LIMIT :limit
    """)
    suspend fun searchConditionsByTitle(query: String, limit: Int = 10): List<IcdConditionEntity>

    @Query("""
        SELECT * FROM icd_conditions 
        WHERE bodySystem LIKE '%' || :system || '%'
        ORDER BY title
        LIMIT :limit
    """)
    suspend fun searchConditionsByBodySystem(system: String, limit: Int = 20): List<IcdConditionEntity>

    @Query("""
        SELECT * FROM icd_conditions 
        WHERE chapter = :chapter
        ORDER BY title
        LIMIT :limit
    """)
    suspend fun getConditionsByChapter(chapter: String, limit: Int = 50): List<IcdConditionEntity>

    @Query("SELECT * FROM icd_conditions LIMIT :limit")
    suspend fun getAllConditions(limit: Int = 1000): List<IcdConditionEntity>

    @Query("SELECT COUNT(*) FROM icd_conditions")
    suspend fun getConditionCount(): Int

    @Query("SELECT COUNT(*) FROM icd_conditions WHERE bodySystem = :system")
    suspend fun getConditionCountByBodySystem(system: String): Int

    @Query("DELETE FROM icd_conditions")
    suspend fun clearAllConditions()
}

/**
 * Separate Room database for ICD-11 conditions
 * This is kept separate from AppDatabase to allow independent initialization
 */
@Database(
    entities = [IcdConditionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class IcdDatabase : RoomDatabase() {
    abstract fun icdConditionDao(): IcdConditionDao
}

