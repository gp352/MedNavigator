package com.mednavigator.app.services

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.Locale

class TextToSpeechService(context: Context) {

    private var tts: TextToSpeech? = null
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private var onSpeakComplete: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _isReady.value = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        onSpeakComplete?.invoke()
                        onSpeakComplete = null
                    }
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        onSpeakComplete?.invoke()
                        onSpeakComplete = null
                    }
                })
            }
        }
    }

    fun speak(text: String, languageCode: String = "en", onComplete: (() -> Unit)? = null) {
        val engine = tts ?: return
        if (!_isReady.value) return

        val locale = Locale(languageCode)
        val available = engine.isLanguageAvailable(locale)
        val targetLocale = if (available >= TextToSpeech.LANG_AVAILABLE) locale else Locale.ENGLISH

        engine.language = targetLocale
        _isSpeaking.value = true
        onSpeakComplete = onComplete

        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mednav_response")
    }

    fun synthesizeToFile(text: String, outputFile: File, languageCode: String = "en") {
        val engine = tts ?: return
        if (!_isReady.value) return

        val locale = Locale(languageCode)
        val available = engine.isLanguageAvailable(locale)
        val targetLocale = if (available >= TextToSpeech.LANG_AVAILABLE) locale else Locale.ENGLISH

        engine.language = targetLocale
        outputFile.parentFile?.mkdirs()

        val params = Bundle()
        engine.synthesizeToFile(text, params, outputFile, "mednav_tts_file")
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
