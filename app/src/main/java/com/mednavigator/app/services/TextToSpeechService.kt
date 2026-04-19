package com.mednavigator.app.services

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TextToSpeechService(context: Context) {

    private var tts: TextToSpeech? = null
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _isReady.value = true
            }
        }
    }

    fun speak(text: String, languageCode: String = "en") {
        val engine = tts ?: return
        if (!_isReady.value) return

        val locale = Locale(languageCode)
        val available = engine.isLanguageAvailable(locale)
        val targetLocale = if (available >= TextToSpeech.LANG_AVAILABLE) locale else Locale.ENGLISH

        engine.language = targetLocale
        _isSpeaking.value = true

        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mednav_response")
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
        _isSpeaking.value = false
    }
}
