package com.mednavigator.app.data.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Client for Clinical Tables ICD-10-CM API
 */
class IcdApiClient {

    companion object {
        private const val TAG = "IcdApiClient"
        private const val BASE_URL = "https://clinicaltables.nlm.nih.gov/"
    }

    private val apiService: ClinicalTablesApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ClinicalTablesApiService::class.java)
    }

    /**
     * Search ICD-10-CM conditions by terms
     */
    suspend fun searchConditions(terms: String, maxResults: Int = 10): List<Icd10CmCondition> {
        return try {
            val response = apiService.searchConditions(terms, maxResults)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.d(TAG, "API search successful: ${body.totalCount} total results")
                    parseResponseToConditions(body)
                } else {
                    Log.w(TAG, "API response body is null")
                    emptyList()
                }
            } else {
                Log.e(TAG, "API search failed: ${response.code()} ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching ICD-10-CM conditions", e)
            emptyList()
        }
    }

    private fun parseResponseToConditions(response: Icd10CmResponse): List<Icd10CmCondition> {
        val conditions = mutableListOf<Icd10CmCondition>()

        val codes = response.codes
        val names = response.names
        val preferredNames = response.preferredNames ?: emptyList()

        val minSize = minOf(codes.size, names.size)

        for (i in 0 until minSize) {
            val code = codes[i]
            val name = names[i]
            val preferredName = if (i < preferredNames.size) preferredNames[i] else null

            conditions.add(Icd10CmCondition(
                code = code,
                name = name,
                preferredName = preferredName
            ))
        }

        return conditions
    }
}
