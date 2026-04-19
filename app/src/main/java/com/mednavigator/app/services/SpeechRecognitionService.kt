package com.mednavigator.app.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.util.Log

class SpeechRecognitionService(context: Context) {

    companion object {
        private const val TAG = "SpeechService"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var callback: SpeechCallback? = null
    private val appContext = context.applicationContext

    interface SpeechCallback {
        fun onPartialResult(text: String)
        fun onFinalResult(text: String)
        fun onError(message: String)
    }

    fun isAvailable(): Boolean {
        val available = SpeechRecognizer.isRecognitionAvailable(appContext)
        Log.d(TAG, "SpeechRecognizer available: $available")
        return available
    }

    fun startListening(languageCode: String, callback: SpeechCallback) {
        this.callback = callback

        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            Log.e(TAG, "SpeechRecognizer not available on this device")
            callback.onError("Speech recognition not available on this device. Ensure Google app is installed.")
            return
        }

        // Destroy any existing instance before creating a new one
        speechRecognizer?.destroy()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create SpeechRecognizer", e)
            callback.onError("Failed to initialize speech recognition: ${e.message}")
            return
        }

        // Build language tag properly: "en" → "en", "hi" → "hi"
        val locale = java.util.Locale(languageCode)
        val languageTag = locale.language
        Log.d(TAG, "Starting speech recognition with language: $languageTag")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d(TAG, "Partial: ${matches[0]}")
                    callback.onPartialResult(matches[0])
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = if (!matches.isNullOrEmpty()) matches[0] else ""
                Log.d(TAG, "Final result: $text")
                callback.onFinalResult(text)
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Could not understand audio. Please try speaking clearly."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Please try again."
                    SpeechRecognizer.ERROR_NETWORK -> "Network unavailable. Download offline speech model in device settings."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Try again or download offline speech model."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check microphone permission."
                    SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission not granted."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please wait."
                    SpeechRecognizer.ERROR_SERVER -> "Speech server error. Try offline model."
                    else -> "Speech recognition error (code: $error)"
                }
                Log.e(TAG, "SpeechRecognizer error $error: $message")
                callback.onError(message)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        callback = null
    }
}
