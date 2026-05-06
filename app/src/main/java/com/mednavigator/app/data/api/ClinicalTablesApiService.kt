package com.mednavigator.app.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service for Clinical Tables ICD-10-CM API
 * Base URL: https://clinicaltables.nlm.nih.gov/
 */
interface ClinicalTablesApiService {

    /**
     * Search ICD-10-CM conditions
     * @param terms Search terms (e.g., "chest pain")
     * @param maxList Maximum number of results (default 10)
     * @param df Display fields: 0=total, 1=codes, 2=names, 3=preferred_names
     */
    @GET("api/conditions/v3/search")
    suspend fun searchConditions(
        @Query("terms") terms: String,
        @Query("maxList") maxList: Int = 10,
        @Query("df") df: String = "0,1,2,3" // Include all fields
    ): Response<Icd10CmResponse>
}
