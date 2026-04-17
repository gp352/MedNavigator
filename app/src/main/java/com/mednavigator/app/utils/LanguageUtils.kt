package com.mednavigator.app.utils

object LanguageUtils {

    // Maps ISO 639-1 codes to display names
    val supportedLanguages = mapOf(
        "en" to "English",
        "hi" to "Hindi",
        "gu" to "Gujarati",
        "sw" to "Swahili",
        "bn" to "Bengali",
        "ta" to "Tamil",
        "ar" to "Arabic",
        "fr" to "French",
        "pt" to "Portuguese",
        "es" to "Spanish",
        "zh" to "Chinese",
        "ur" to "Urdu",
        "yo" to "Yoruba",
        "ha" to "Hausa"
    )

    fun getDisplayName(code: String): String {
        return supportedLanguages[code] ?: "English"
    }

    // Detect language from device locale as default
    fun getDeviceLanguageCode(): String {
        val locale = java.util.Locale.getDefault().language
        return if (supportedLanguages.containsKey(locale)) locale else "en"
    }
}
