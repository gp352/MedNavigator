package com.mednavigator.app.data.models

data class PatientContext(
    val age: Int,
    val biologicalSex: String,      // "male" / "female" / "other"
    val country: String,            // e.g. "India", "Kenya"
    val language: String,           // ISO 639-1 e.g. "hi", "sw", "gu"
    val symptomDuration: String     // e.g. "3 days", "2 weeks"
)
