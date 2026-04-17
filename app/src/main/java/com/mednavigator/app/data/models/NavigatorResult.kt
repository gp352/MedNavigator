package com.mednavigator.app.data.models

data class NavigatorResult(
    val conditions: List<ConditionSuggestion>,
    val specialists: List<HealthFacility>,
    val urgencyLevel: String,           // IMMEDIATE / SOON / ROUTINE
    val urgencyExplanation: String,     // plain language reason
    val rawModelResponse: String,       // full E4B response for debugging
    val timestamp: Long = System.currentTimeMillis()
)

data class ConditionSuggestion(
    val icdCode: String,
    val title: String,
    val plainExplanation: String,   // E4B-generated plain language explanation
    val whyItMatches: String,       // which symptoms/findings support this
    val suggestedTests: String,     // what a doctor would test next
    val bodySystem: String
)
