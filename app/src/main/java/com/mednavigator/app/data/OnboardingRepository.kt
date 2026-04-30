package com.mednavigator.app.data

import android.content.Context
import com.mednavigator.app.utils.Constants
import com.mednavigator.app.utils.LanguageUtils

class OnboardingRepository(context: Context) {

    private val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    fun isOnboardingComplete(): Boolean {
        return prefs.getBoolean(Constants.PREF_ONBOARDING_DONE, false)
    }

    fun saveUserProfile(name: String, age: Int, sex: String, country: String, language: String) {
        prefs.edit()
            .putBoolean(Constants.PREF_ONBOARDING_DONE, true)
            .putString(Constants.PREF_USER_NAME, name)
            .putInt(Constants.PREF_USER_AGE, age)
            .putString(Constants.PREF_USER_SEX, sex)
            .putString(Constants.PREF_USER_COUNTRY, country)
            .putString(Constants.PREF_USER_LANGUAGE, language)
            .apply()
    }

    fun getUserName(): String {
        return prefs.getString(Constants.PREF_USER_NAME, null) ?: "User"
    }

    fun getUserLanguage(): String {
        return prefs.getString(Constants.PREF_USER_LANGUAGE, null)
            ?: LanguageUtils.getDeviceLanguageCode()
    }

    fun getUserAge(): Int {
        return prefs.getInt(Constants.PREF_USER_AGE, 0)
    }

    fun getUserSex(): String {
        return prefs.getString(Constants.PREF_USER_SEX, "") ?: ""
    }

    fun getUserCountry(): String {
        return prefs.getString(Constants.PREF_USER_COUNTRY, "") ?: ""
    }
}
