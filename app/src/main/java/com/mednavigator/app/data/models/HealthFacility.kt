package com.mednavigator.app.data.models

data class HealthFacility(
    val name: String,
    val type: String,           // "Hospital", "Clinic", "Specialist Centre"
    val specialty: String,      // e.g. "Dermatology", "Cardiology"
    val address: String,
    val phone: String,
    val country: String,
    val city: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    fun toJson(): String {
        return """
            {
              "name": "$name",
              "type": "$type",
              "specialty": "$specialty",
              "address": "$address",
              "phone": "$phone",
              "city": "$city"
            }
        """.trimIndent()
    }
}
