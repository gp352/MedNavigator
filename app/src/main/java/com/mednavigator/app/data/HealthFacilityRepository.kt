package com.mednavigator.app.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.mednavigator.app.data.models.HealthFacility
import com.mednavigator.app.data.models.HealthFacilityEntity
import com.mednavigator.app.data.models.HealthFacilityDatabase
import com.mednavigator.app.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Repository for accessing and searching health facilities in India
 * Provides search, lookup, and location-based facility recommendations
 */
class HealthFacilityRepository(private val context: Context) {

    companion object {
        private const val TAG = "HealthFacilityRepo"
        @Volatile
        private var instance: HealthFacilityRepository? = null

        fun getInstance(context: Context): HealthFacilityRepository =
            instance ?: synchronized(this) {
                instance ?: HealthFacilityRepository(context).also { instance = it }
            }
    }

    private val facilityDatabase: HealthFacilityDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            HealthFacilityDatabase::class.java,
            "health_facilities.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    private val dao by lazy { facilityDatabase.healthFacilityDao() }
    private val initMutex = Mutex()
    private var isInitialized = false
    private val searchCache = mutableMapOf<String, List<HealthFacility>>()

    /**
     * Initialize the health facilities database with Indian hospitals and clinics
     */
    suspend fun initializeIfNeeded() {
        initMutex.withLock {
            if (isInitialized) return

            try {
                withContext(Dispatchers.IO) {
                    val count = dao.getFacilityCount()
                    if (count == 0) {
                        Log.d(TAG, "Health facilities database is empty, loading default facilities...")
                        loadDefaultFacilities()
                    } else {
                        Log.d(TAG, "Health facilities database already initialized with $count facilities")
                    }
                }
                isInitialized = true
                Log.d(TAG, "Health facilities database initialization complete")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize health facilities database", e)
                isInitialized = true // Mark as initialized to prevent retries
                throw e
            }
        }
    }

    /**
     * Load comprehensive default health facilities for major Indian cities
     */
    private suspend fun loadDefaultFacilities() {
        val defaultFacilities = listOf(
            // Delhi NCR Hospitals
            HealthFacilityEntity(
                name = "All India Institute of Medical Sciences (AIIMS)",
                type = "Super Specialty Hospital",
                specialty = "Multi-specialty",
                address = "Ansari Nagar, New Delhi, Delhi 110029",
                phone = "+91-11-26588500",
                country = "India",
                city = "New Delhi",
                state = "Delhi",
                latitude = 28.5672,
                longitude = 77.2100,
                emergencyServices = true,
                beds = 2500
            ),
            HealthFacilityEntity(
                name = "Apollo Hospitals Delhi",
                type = "Multi-specialty Hospital",
                specialty = "Cardiology, Oncology, Neurology",
                address = "Mathura Road, Sarita Vihar, New Delhi, Delhi 110076",
                phone = "+91-11-26925858",
                country = "India",
                city = "New Delhi",
                state = "Delhi",
                latitude = 28.5328,
                longitude = 77.2875,
                emergencyServices = true,
                beds = 1000
            ),
            HealthFacilityEntity(
                name = "Max Super Speciality Hospital",
                type = "Super Specialty Hospital",
                specialty = "Cardiology, Oncology, Orthopedics",
                address = "1,2, Press Enclave Road, Saket, New Delhi, Delhi 110017",
                phone = "+91-11-26515050",
                country = "India",
                city = "New Delhi",
                state = "Delhi",
                latitude = 28.5276,
                longitude = 77.2197,
                emergencyServices = true,
                beds = 500
            ),
            HealthFacilityEntity(
                name = "Fortis Escorts Heart Institute",
                type = "Cardiac Specialty Hospital",
                specialty = "Cardiology, Cardiac Surgery",
                address = "Okhla Road, New Delhi, Delhi 110025",
                phone = "+91-11-26825000",
                country = "India",
                city = "New Delhi",
                state = "Delhi",
                latitude = 28.5628,
                longitude = 77.2825,
                emergencyServices = true,
                beds = 310
            ),

            // Mumbai Hospitals
            HealthFacilityEntity(
                name = "Tata Memorial Hospital",
                type = "Cancer Specialty Hospital",
                specialty = "Oncology, Cancer Treatment",
                address = "Dr. E Borges Road, Parel, Mumbai, Maharashtra 400012",
                phone = "+91-22-24177000",
                country = "India",
                city = "Mumbai",
                state = "Maharashtra",
                latitude = 18.9297,
                longitude = 72.8333,
                emergencyServices = true,
                beds = 600
            ),
            HealthFacilityEntity(
                name = "Kokilaben Dhirubhai Ambani Hospital",
                type = "Multi-specialty Hospital",
                specialty = "Multi-specialty",
                address = "Rao Saheb Acharya Marg, Four Bungalows, Andheri West, Mumbai, Maharashtra 400053",
                phone = "+91-22-30696969",
                country = "India",
                city = "Mumbai",
                state = "Maharashtra",
                latitude = 19.1363,
                longitude = 72.8277,
                emergencyServices = true,
                beds = 750
            ),
            HealthFacilityEntity(
                name = "Lilavati Hospital",
                type = "Multi-specialty Hospital",
                specialty = "Cardiology, Oncology, Neurology",
                address = "A-791, Bandra Reclamation, Bandra West, Mumbai, Maharashtra 400050",
                phone = "+91-22-26751000",
                country = "India",
                city = "Mumbai",
                state = "Maharashtra",
                latitude = 19.0544,
                longitude = 72.8354,
                emergencyServices = true,
                beds = 323
            ),

            // Chennai Hospitals
            HealthFacilityEntity(
                name = "Apollo Hospitals Chennai",
                type = "Multi-specialty Hospital",
                specialty = "Multi-specialty",
                address = "21, Greams Lane, Off Greams Road, Chennai, Tamil Nadu 600006",
                phone = "+91-44-28290200",
                country = "India",
                city = "Chennai",
                state = "Tamil Nadu",
                latitude = 13.0827,
                longitude = 80.2707,
                emergencyServices = true,
                beds = 560
            ),
            HealthFacilityEntity(
                name = "Christian Medical College (CMC) Vellore",
                type = "Multi-specialty Hospital",
                specialty = "Multi-specialty",
                address = "Ida Scudder Road, Vellore, Tamil Nadu 632004",
                phone = "+91-416-2281000",
                country = "India",
                city = "Vellore",
                state = "Tamil Nadu",
                latitude = 12.9202,
                longitude = 79.1325,
                emergencyServices = true,
                beds = 2600
            ),

            // Kolkata Hospitals
            HealthFacilityEntity(
                name = "AMRI Hospitals",
                type = "Multi-specialty Hospital",
                specialty = "Cardiology, Oncology, Neurology",
                address = "230, Barakhola Lane, Purba Jadavpur, Mukundapur, Kolkata, West Bengal 700099",
                phone = "+91-33-66800000",
                country = "India",
                city = "Kolkata",
                state = "West Bengal",
                latitude = 22.4997,
                longitude = 88.3711,
                emergencyServices = true,
                beds = 1000
            ),

            // Bangalore Hospitals
            HealthFacilityEntity(
                name = "Manipal Hospital",
                type = "Multi-specialty Hospital",
                specialty = "Multi-specialty",
                address = "98, HAL Airport Road, Bangalore, Karnataka 560017",
                phone = "+91-80-25024444",
                country = "India",
                city = "Bangalore",
                state = "Karnataka",
                latitude = 12.9592,
                longitude = 77.6974,
                emergencyServices = true,
                beds = 600
            ),
            HealthFacilityEntity(
                name = "Fortis Hospital Bangalore",
                type = "Multi-specialty Hospital",
                specialty = "Cardiology, Oncology, Neurology",
                address = "154/9, Bannerghatta Road, Opposite IIM-B, Bangalore, Karnataka 560076",
                phone = "+91-80-66214444",
                country = "India",
                city = "Bangalore",
                state = "Karnataka",
                latitude = 12.8944,
                longitude = 77.5966,
                emergencyServices = true,
                beds = 276
            ),

            // Ahmedabad Hospitals
            HealthFacilityEntity(
                name = "Apollo Hospitals Ahmedabad",
                type = "Multi-specialty Hospital",
                specialty = "Multi-specialty",
                address = "Plot No.1A, Bhat GIDC Estate, Gandhinagar, Gujarat 382428",
                phone = "+91-79-66701800",
                country = "India",
                city = "Ahmedabad",
                state = "Gujarat",
                latitude = 23.1880,
                longitude = 72.6284,
                emergencyServices = true,
                beds = 300
            ),
            HealthFacilityEntity(
                name = "Sterling Hospital",
                type = "Multi-specialty Hospital",
                specialty = "Cardiology, Oncology, Neurology",
                address = "Near Maharaja Agrasen Vidhyalaya, Sterling Hospital Road, Memnagar, Ahmedabad, Gujarat 380052",
                phone = "+91-79-40011111",
                country = "India",
                city = "Ahmedabad",
                state = "Gujarat",
                latitude = 23.0489,
                longitude = 72.5301,
                emergencyServices = true,
                beds = 300
            ),

            // Pune Hospitals
            HealthFacilityEntity(
                name = "Ruby Hall Clinic",
                type = "Multi-specialty Hospital",
                specialty = "Multi-specialty",
                address = "40, Sassoon Road, Pune, Maharashtra 411001",
                phone = "+91-20-26163391",
                country = "India",
                city = "Pune",
                state = "Maharashtra",
                latitude = 18.5308,
                longitude = 73.8478,
                emergencyServices = true,
                beds = 550
            ),

            // Hyderabad Hospitals
            HealthFacilityEntity(
                name = "Apollo Hospitals Hyderabad",
                type = "Multi-specialty Hospital",
                specialty = "Multi-specialty",
                address = "Road No 72, Opp. Bharatiya Vidya Bhavan School, Film Nagar, Hyderabad, Telangana 500033",
                phone = "+91-40-23607777",
                country = "India",
                city = "Hyderabad",
                state = "Telangana",
                latitude = 17.4193,
                longitude = 78.4483,
                emergencyServices = true,
                beds = 350
            ),

            // Jaipur Hospitals
            HealthFacilityEntity(
                name = "Santokba Durlabhji Memorial Hospital",
                type = "Multi-specialty Hospital",
                specialty = "Multi-specialty",
                address = "Near Rambagh Circle, Bhawani Singh Road, Jaipur, Rajasthan 302015",
                phone = "+91-141-2566256",
                country = "India",
                city = "Jaipur",
                state = "Rajasthan",
                latitude = 26.9124,
                longitude = 75.7873,
                emergencyServices = true,
                beds = 500
            ),

            // Chandigarh Hospitals
            HealthFacilityEntity(
                name = "Post Graduate Institute of Medical Education and Research (PGIMER)",
                type = "Super Specialty Hospital",
                specialty = "Multi-specialty",
                address = "Sector 12, Chandigarh 160012",
                phone = "+91-172-2746018",
                country = "India",
                city = "Chandigarh",
                state = "Chandigarh",
                latitude = 30.7650,
                longitude = 76.7800,
                emergencyServices = true,
                beds = 1500
            ),

            // Dermatology Clinics (for skin conditions)
            HealthFacilityEntity(
                name = "Dr. Batra's Positive Health Clinic",
                type = "Specialty Clinic",
                specialty = "Dermatology, Homeopathy",
                address = "1, Tardeo AC Market, Tardeo Road, Mumbai, Maharashtra 400034",
                phone = "+91-22-23525000",
                country = "India",
                city = "Mumbai",
                state = "Maharashtra",
                latitude = 18.9722,
                longitude = 72.8147,
                emergencyServices = false,
                beds = 0
            ),

            // Mental Health Facilities
            HealthFacilityEntity(
                name = "National Institute of Mental Health and Neurosciences (NIMHANS)",
                type = "Mental Health Hospital",
                specialty = "Psychiatry, Neurology",
                address = "Hosur Road, Bangalore, Karnataka 560029",
                phone = "+91-80-26995000",
                country = "India",
                city = "Bangalore",
                state = "Karnataka",
                latitude = 12.9430,
                longitude = 77.5966,
                emergencyServices = true,
                beds = 900
            ),

            // Pediatric Hospitals
            HealthFacilityEntity(
                name = "Rainbow Children's Hospital",
                type = "Children's Hospital",
                specialty = "Pediatrics, Neonatology",
                address = "Road No. 2, Banjara Hills, Hyderabad, Telangana 500034",
                phone = "+91-40-23322222",
                country = "India",
                city = "Hyderabad",
                state = "Telangana",
                latitude = 17.4243,
                longitude = 78.4294,
                emergencyServices = true,
                beds = 200
            )
        )

        try {
            dao.insertFacilities(defaultFacilities)
            Log.d(TAG, "Successfully loaded ${defaultFacilities.size} default health facilities")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading default facilities", e)
            throw e
        }
    }

    /**
     * Search health facilities by specialty, city, or name
     */
    suspend fun searchFacilities(
        query: String,
        city: String? = null,
        specialty: String? = null,
        limit: Int = Constants.FACILITIES_LIMIT
    ): List<HealthFacility> {
        if (query.isBlank() && city.isNullOrBlank() && specialty.isNullOrBlank()) return emptyList()

        val cacheKey = "search_${query}_${city}_${specialty}"
        searchCache[cacheKey]?.let { return it }

        return try {
            withContext(Dispatchers.IO) {
                val results = dao.searchFacilities(query, city ?: "", specialty ?: "", limit)
                    .map { it.toDomain() }
                searchCache[cacheKey] = results
                results
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching facilities", e)
            emptyList()
        }
    }

    /**
     * Find facilities by specialty in a specific city
     */
    suspend fun findFacilitiesBySpecialty(
        specialty: String,
        city: String,
        limit: Int = Constants.FACILITIES_LIMIT
    ): List<HealthFacility> {
        return searchFacilities("", city, specialty, limit)
    }

    /**
     * Get facilities near a location (simplified - by city for now)
     */
    suspend fun findNearbyFacilities(
        city: String,
        specialty: String? = null,
        limit: Int = Constants.FACILITIES_LIMIT
    ): List<HealthFacility> {
        return searchFacilities("", city, specialty, limit)
    }

    /**
     * Get emergency facilities in a city
     */
    suspend fun findEmergencyFacilities(city: String, limit: Int = 5): List<HealthFacility> {
        return try {
            withContext(Dispatchers.IO) {
                dao.getEmergencyFacilities(city, limit)
                    .map { it.toDomain() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding emergency facilities", e)
            emptyList()
        }
    }

    /**
     * Get all available cities with facilities
     */
    suspend fun getAvailableCities(): List<String> {
        return try {
            withContext(Dispatchers.IO) {
                dao.getAvailableCities()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available cities", e)
            emptyList()
        }
    }

    /**
     * Get all available specialties
     */
    suspend fun getAvailableSpecialties(): List<String> {
        return try {
            withContext(Dispatchers.IO) {
                dao.getAvailableSpecialties()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available specialties", e)
            emptyList()
        }
    }

    /**
     * Build facility context for LLM prompt based on user location and needs
     */
    suspend fun buildFacilityContext(
        city: String,
        specialty: String? = null,
        isEmergency: Boolean = false
    ): String {
        if (city.isBlank()) return ""

        return try {
            val facilities = if (isEmergency) {
                findEmergencyFacilities(city, 3)
            } else if (specialty != null) {
                findFacilitiesBySpecialty(specialty, city, 3)
            } else {
                findNearbyFacilities(city, null, 3)
            }

            if (facilities.isEmpty()) return ""

            val contextBuilder = StringBuilder()
            contextBuilder.append("\n## Recommended Health Facilities in $city:\n")
            facilities.forEachIndexed { index, facility ->
                contextBuilder.append("${index + 1}. ${facility.name} (${facility.type})\n")
                contextBuilder.append("   - Specialty: ${facility.specialty}\n")
                contextBuilder.append("   - Address: ${facility.address}\n")
                contextBuilder.append("   - Phone: ${facility.phone}\n")
                if (facility.emergencyServices) {
                    contextBuilder.append("   - Emergency Services: Available\n")
                }
            }
            contextBuilder.append("\nRecommend these facilities for specialist consultation.\n")

            contextBuilder.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error building facility context", e)
            ""
        }
    }

    /**
     * Get database statistics
     */
    suspend fun getDatabaseStats(): Map<String, Int> {
        return try {
            withContext(Dispatchers.IO) {
                val totalFacilities = dao.getFacilityCount()
                val emergencyCount = dao.getEmergencyFacilityCount()
                val citiesCount = dao.getAvailableCities().size

                mapOf(
                    "total" to totalFacilities,
                    "emergency" to emergencyCount,
                    "cities" to citiesCount
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting database stats", e)
            emptyMap()
        }
    }

    private fun HealthFacilityEntity.toDomain(): HealthFacility {
        return HealthFacility(
            name = this.name,
            type = this.type,
            specialty = this.specialty,
            address = this.address,
            phone = this.phone,
            country = this.country,
            city = this.city,
            latitude = this.latitude,
            longitude = this.longitude,
            emergencyServices = this.emergencyServices
        )
    }
}
