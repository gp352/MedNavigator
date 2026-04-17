package com.mednavigator.app.data.models

data class IcdCondition(
    val code: String,           // e.g. "BA80"
    val title: String,          // e.g. "Chest pain"
    val definition: String,     // plain definition from ICD-11
    val bodySystem: String,     // e.g. "Circulatory system"
    val chapter: String,        // ICD-11 chapter
    val synonyms: String        // comma-separated synonyms
) {
    fun toJson(): String {
        return """
            {
              "code": "$code",
              "title": "$title",
              "definition": "${definition.take(300)}",
              "body_system": "$bodySystem",
              "synonyms": "$synonyms"
            }
        """.trimIndent()
    }
}
