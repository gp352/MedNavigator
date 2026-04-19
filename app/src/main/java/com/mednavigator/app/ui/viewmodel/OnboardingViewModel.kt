package com.mednavigator.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.utils.LanguageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingViewModel : ViewModel() {

    val name = MutableStateFlow("")
    val age = MutableStateFlow("")
    val selectedSex = MutableStateFlow("")
    val selectedCountry = MutableStateFlow("")
    val selectedLanguage = MutableStateFlow("en")

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val sexOptions = listOf("Male", "Female", "Other")
    val countryOptions = listOf(
        "India", "United States", "United Kingdom", "Kenya", "Nigeria",
        "Bangladesh", "China", "France", "Portugal", "Spain",
        "Saudi Arabia", "Pakistan", "Tanzania", "Ghana", "Other"
    )
    val languageOptions: Map<String, String> = LanguageUtils.supportedLanguages

    fun validateAndSave(repository: OnboardingRepository): Boolean {
        val nameVal = name.value.trim()
        val ageVal = age.value.trim().toIntOrNull()
        val sexVal = selectedSex.value
        val countryVal = selectedCountry.value
        val langVal = selectedLanguage.value

        if (nameVal.isBlank()) {
            _errorMessage.value = "Please enter your name"
            return false
        }
        if (ageVal == null || ageVal < 1 || ageVal > 120) {
            _errorMessage.value = "Please enter a valid age (1-120)"
            return false
        }
        if (sexVal.isBlank()) {
            _errorMessage.value = "Please select your biological sex"
            return false
        }
        if (countryVal.isBlank()) {
            _errorMessage.value = "Please select your country"
            return false
        }

        repository.saveUserProfile(nameVal, ageVal, sexVal, countryVal, langVal)
        _errorMessage.value = null
        return true
    }
}
