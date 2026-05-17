package com.mednavigator.app.services

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class SpeechToTextService(private val application: Application) {

    companion object {
        private const val TAG = "SpeechToTextService"
    }

    private var speechRecognizer: SpeechRecognizer? = null

    /**
     * Lazily initialize SpeechRecognizer on the main thread.
     * The SpeechRecognizer API must be accessed from the application's main thread.
     */
    private suspend fun getOrCreateRecognizer(): SpeechRecognizer? = withContext(Dispatchers.Main) {
        if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(application)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create SpeechRecognizer", e)
            }
        }
        speechRecognizer
    }

    /**
     * Recognize speech and return the transcribed text.
     * This function ensures all SpeechRecognizer calls happen on the Main thread.
     */
    suspend fun recognizeSpeech(): Result<String> = withContext(Dispatchers.Main) {
        val recognizer = getOrCreateRecognizer() ?: return@withContext Result.failure(
            IllegalStateException("Speech recognition not available on this device")
        )

        suspendCancellableCoroutine { continuation ->
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }

            var hasResumed = false

            val listener = object : RecognitionListener {
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

                override fun onError(error: Int) {
                    if (!hasResumed) {
                        hasResumed = true
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                            else -> "Unknown error: $error"
                        }
                        Log.e(TAG, "Speech recognition error: $errorMessage")
                        continuation.resume(Result.failure(Exception(errorMessage)))
                    }
                }

                override fun onResults(results: Bundle?) {
                    if (!hasResumed) {
                        hasResumed = true
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        Log.d(TAG, "Speech recognition result: '$text'")
                        continuation.resume(Result.success(text))
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            }

            recognizer.setRecognitionListener(listener)
            recognizer.startListening(intent)

            continuation.invokeOnCancellation {
                if (!hasResumed) {
                    hasResumed = true
                    recognizer.cancel()
                }
            }
        }
    }

    /**
     * Release resources. This should be called from the main thread.
     */
    fun release() {
        speechRecognizer?.let {
            it.destroy()
            speechRecognizer = null
        }
    }
}
