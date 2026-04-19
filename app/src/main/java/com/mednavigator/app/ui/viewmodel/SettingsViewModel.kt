package com.mednavigator.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.utils.LanguageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OnboardingRepository(application)

    private val _selectedLanguage = MutableStateFlow(repository.getUserLanguage())
    val selectedLanguage: StateFlow<String> = _selectedLanguage

    val languageOptions: Map<String, String> = LanguageUtils.supportedLanguages

    private val _savedMessage = MutableStateFlow<String?>(null)
    val savedMessage: StateFlow<String?> = _savedMessage

    fun updateLanguage(code: String) {
        _selectedLanguage.value = code
        // Update the stored language in SharedPreferences
        val prefs = getApplication<Application>().getSharedPreferences(
            com.mednavigator.app.utils.Constants.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        prefs.edit()
            .putString(com.mednavigator.app.utils.Constants.PREF_USER_LANGUAGE, code)
            .apply()
        _savedMessage.value = "Language updated to ${LanguageUtils.getDisplayName(code)}"
    }

    fun clearSavedMessage() {
        _savedMessage.value = null
    }
}
