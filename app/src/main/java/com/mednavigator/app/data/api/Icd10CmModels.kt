package com.mednavigator.app.data.api

import com.google.gson.annotations.SerializedName

/**
 * Response model for Clinical Tables ICD-10-CM API
 * Based on: https://clinicaltables.nlm.nih.gov/apidoc/conditions/v3/doc.html
 */
data class Icd10CmResponse(
    @SerializedName("0") val totalCount: Int,
    @SerializedName("1") val codes: List<String>,
    @SerializedName("2") val names: List<String>,
    @SerializedName("3") val preferredNames: List<String>? = null
)

/**
 * Individual ICD-10-CM condition from API
 */
data class Icd10CmCondition(
    val code: String,
    val name: String,
    val preferredName: String? = null
) {
    fun toIcdCondition(): com.mednavigator.app.data.models.IcdCondition {
        return com.mednavigator.app.data.models.IcdCondition(
            code = code,
            title = name,
            definition = preferredName ?: name, // Use preferred name as definition if available
            bodySystem = "Unknown", // API doesn't provide body system
            chapter = "ICD-10-CM",
            synonyms = "" // API doesn't provide synonyms
        )
    }
}
