package com.mednavigator.app.utils

object Constants {
    // Model
    const val MODEL_FILENAME = "gemma-4-e4b-it-q4.litertlm"
    const val MAX_OUTPUT_TOKENS = 2048
    const val VISION_TOKEN_BUDGET = 560   // high detail for medical photos
    const val AUDIO_MAX_SECONDS = 30

    // Database
    const val ICD_DB_NAME = "icd11_mms.db"
    const val FACILITIES_JSON = "health_facilities.json"
    const val APP_DB_NAME = "med_navigator.db"
    const val APP_DB_VERSION = 1

    // ICD search
    const val ICD_SEARCH_LIMIT = 8
    const val FACILITIES_LIMIT = 3

    // Urgency levels
    const val URGENCY_IMMEDIATE = "IMMEDIATE"   // go to ER now
    const val URGENCY_SOON = "SOON"             // see doctor within 48h
    const val URGENCY_ROUTINE = "ROUTINE"       // schedule appointment

    // Tool call names — must match what E4B calls
    const val TOOL_SEARCH_ICD = "search_icd11_local"
    const val TOOL_FIND_SPECIALISTS = "find_specialists_local"
    const val TOOL_GET_CONDITION = "get_condition_info"

    // Shared prefs keys
    const val PREFS_NAME = "med_navigator_prefs"
    const val PREF_ONBOARDING_DONE = "onboarding_done"
    const val PREF_USER_LANGUAGE = "user_language"
    const val PREF_USER_COUNTRY = "user_country"
    const val PREF_USER_AGE = "user_age"
    const val PREF_USER_SEX = "user_sex"
}
