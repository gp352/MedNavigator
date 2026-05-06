package com.mednavigator.app.utils

import android.util.Log
import com.mednavigator.app.data.models.IcdConditionEntity
import com.google.gson.Gson

/**
 * Utility for importing ICD-11 data from external sources
 * Supports multiple data formats: JSON, CSV, or API responses
 */
object IcdDataImporter {
    private const val TAG = "IcdDataImporter"

    /**
     * Parse ICD conditions from JSON format
     */
    fun parseIcdConditionsFromJson(jsonString: String): List<IcdConditionEntity> {
        return try {
            val gson = Gson()
            val jsonArray = gson.fromJson(jsonString, com.google.gson.JsonArray::class.java)

            jsonArray.mapNotNull { element ->
                try {
                    val obj = element.asJsonObject
                    IcdConditionEntity(
                        code = obj.get("code")?.asString ?: return@mapNotNull null,
                        title = obj.get("title")?.asString ?: return@mapNotNull null,
                        definition = obj.get("definition")?.asString ?: "",
                        bodySystem = obj.get("bodySystem")?.asString ?: "",
                        chapter = obj.get("chapter")?.asString ?: "",
                        synonyms = obj.get("synonyms")?.asString ?: "",
                        searchKeywords = obj.get("searchKeywords")?.asString ?: ""
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse ICD condition entry", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON", e)
            emptyList()
        }
    }

    /**
     * Parse ICD conditions from CSV format
     * Expected CSV headers: code,title,definition,bodySystem,chapter,synonyms,searchKeywords
     */
    fun parseIcdConditionsFromCsv(csvString: String): List<IcdConditionEntity> {
        return try {
            csvString.lines()
                .drop(1)  // Skip header
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        val parts = line.split(",")
                        if (parts.size >= 6) {
                            IcdConditionEntity(
                                code = parts[0].trim(),
                                title = parts[1].trim(),
                                definition = parts[2].trim(),
                                bodySystem = parts[3].trim(),
                                chapter = parts[4].trim(),
                                synonyms = parts[5].trim(),
                                searchKeywords = if (parts.size > 6) parts[6].trim() else ""
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse CSV line: $line", e)
                        null
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse CSV", e)
            emptyList()
        }
    }

    /**
     * Transform WHO ICD-11 MMS API response to our format
     * This handles the specific structure of WHO's ICD-11 REST API
     */
    fun parseWhoIcd11ApiResponse(apiResponse: String): List<IcdConditionEntity> {
        return try {
            val gson = Gson()
            val json = gson.fromJson(apiResponse, com.google.gson.JsonObject::class.java)

            // Extract conditions from WHO API response structure
            val conditions = mutableListOf<IcdConditionEntity>()

            // Assuming WHO API returns array of conditions in "response" or similar field
            val responseArray = json.getAsJsonArray("response")
                ?: json.getAsJsonArray("results")
                ?: json.getAsJsonArray("conditions")
                ?: return emptyList()

            responseArray.forEach { element ->
                try {
                    val obj = element.asJsonObject
                    val condition = IcdConditionEntity(
                        code = extractIcdCode(obj),
                        title = obj.get("title")?.asJsonObject?.get("@value")?.asString
                            ?: obj.get("title")?.asString
                            ?: "",
                        definition = extractDefinition(obj),
                        bodySystem = extractBodySystem(obj),
                        chapter = extractChapter(obj),
                        synonyms = extractSynonyms(obj),
                        searchKeywords = extractSearchKeywords(obj)
                    )
                    conditions.add(condition)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse WHO API entry", e)
                }
            }

            conditions
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse WHO API response", e)
            emptyList()
        }
    }

    private fun extractIcdCode(obj: com.google.gson.JsonObject): String {
        // Try multiple paths for code extraction
        return  obj.get("code")?.asString
            ?: obj.get("id")?.asString?.substringAfterLast("/")
            ?: obj.get("uri")?.asString?.substringAfterLast("/")
            ?: ""
    }

    private fun extractDefinition(obj: com.google.gson.JsonObject): String {
        val definition = obj.get("definition")
        return when {
            definition?.isJsonArray == true -> {
                definition.asJsonArray.firstOrNull()
                    ?.asJsonObject?.get("@value")?.asString ?: ""
            }
            definition?.isJsonObject == true -> {
                definition.asJsonObject.get("@value")?.asString ?: ""
            }
            definition != null -> definition.asString
            else -> ""
        }
    }

    private fun extractBodySystem(obj: com.google.gson.JsonObject): String {
        return obj.get("bodySystem")?.asString
            ?: obj.get("system")?.asString
            ?: ""
    }

    private fun extractChapter(obj: com.google.gson.JsonObject): String {
        return obj.get("chapter")?.asString
            ?: obj.get("classKind")?.asString
            ?: ""
    }

    private fun extractSynonyms(obj: com.google.gson.JsonObject): String {
        return try {
            val synonyms = obj.get("synonyms")
            when {
                synonyms?.isJsonArray == true -> {
                    synonyms.asJsonArray
                        .mapNotNull { it.asJsonObject?.get("@value")?.asString }
                        .joinToString(", ")
                }
                synonyms != null -> synonyms.asString
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractSearchKeywords(obj: com.google.gson.JsonObject): String {
        // Generate keywords from title and definition
        val title = obj.get("title")?.asString ?: ""
        val definition = extractDefinition(obj).take(100)

        return "$title $definition"
            .split(" ")
            .filter { it.length > 3 }
            .distinct()
            .take(10)
            .joinToString(" ")
    }
}

