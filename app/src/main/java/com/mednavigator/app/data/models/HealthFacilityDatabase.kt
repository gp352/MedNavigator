package com.mednavigator.app.data.models

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

// Entity for health facilities
@Entity(tableName = "health_facilities")
data class HealthFacilityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val type: String,           // "Hospital", "Clinic", "Specialist Centre"
    val specialty: String,      // e.g. "Dermatology", "Cardiology"
    val address: String,
    val phone: String,
    val country: String,
    val city: String,
    val state: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val emergencyServices: Boolean = false,
    val beds: Int = 0
)

// DAO for health facilities
@Dao
interface HealthFacilityDao {
    @Insert
    suspend fun insertFacility(facility: HealthFacilityEntity): Long

    @Insert
    suspend fun insertFacilities(facilities: List<HealthFacilityEntity>)

    @Query("SELECT * FROM health_facilities WHERE (name LIKE '%' || :query || '%' OR specialty LIKE '%' || :query || '%' OR type LIKE '%' || :query || '%') AND city LIKE '%' || :city || '%' AND specialty LIKE '%' || :specialty || '%' ORDER BY name LIMIT :limit")
    suspend fun searchFacilities(query: String, city: String, specialty: String, limit: Int): List<HealthFacilityEntity>

    @Query("SELECT * FROM health_facilities WHERE city LIKE '%' || :city || '%' AND specialty LIKE '%' || :specialty || '%' ORDER BY name LIMIT :limit")
    suspend fun findBySpecialtyAndCity(specialty: String, city: String, limit: Int): List<HealthFacilityEntity>

    @Query("SELECT * FROM health_facilities WHERE city LIKE '%' || :city || '%' ORDER BY name LIMIT :limit")
    suspend fun findByCity(city: String, limit: Int): List<HealthFacilityEntity>

    @Query("SELECT * FROM health_facilities WHERE emergencyServices = 1 AND city LIKE '%' || :city || '%' ORDER BY beds DESC LIMIT :limit")
    suspend fun getEmergencyFacilities(city: String, limit: Int): List<HealthFacilityEntity>

    @Query("SELECT DISTINCT city FROM health_facilities ORDER BY city")
    suspend fun getAvailableCities(): List<String>

    @Query("SELECT DISTINCT specialty FROM health_facilities ORDER BY specialty")
    suspend fun getAvailableSpecialties(): List<String>

    @Query("SELECT COUNT(*) FROM health_facilities")
    suspend fun getFacilityCount(): Int

    @Query("SELECT COUNT(*) FROM health_facilities WHERE emergencyServices = 1")
    suspend fun getEmergencyFacilityCount(): Int
}

// Database
@Database(
    entities = [HealthFacilityEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HealthFacilityDatabase : RoomDatabase() {
    abstract fun healthFacilityDao(): HealthFacilityDao
}
