package com.mednavigator.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.services.AudioRecorderService
import com.mednavigator.app.services.TextToSpeechService
import com.mednavigator.app.utils.ResponseGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "VoiceViewModel"
    }

    private val audioRecorder = AudioRecorderService()
    private val ttsService = TextToSpeechService(application)
    private val onboardingRepository = OnboardingRepository(application)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _statusText = MutableStateFlow("Tap the microphone to start speaking")
    val statusText: StateFlow<String> = _statusText

    private val _responseText = MutableStateFlow("")
    val responseText: StateFlow<String> = _responseText

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        _isRecording.value = true
        _statusText.value = "Listening..."
        _responseText.value = ""

        viewModelScope.launch {
            val result = audioRecorder.startRecording()
            if (result.isFailure) {
                Log.e(TAG, "AudioRecorder failed: ${result.exceptionOrNull()?.message}")
                _isRecording.value = false
                _statusText.value = "Recording failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    private fun stopRecording() {
        if (!_isRecording.value) return
        _isRecording.value = false

        val audioBytes = audioRecorder.stopRecording()
        val hasAudio = audioBytes != null && audioBytes.isNotEmpty()
        Log.d(TAG, "Recording stopped: ${audioBytes?.size ?: 0} bytes, hasAudio=$hasAudio")

        val response = ResponseGenerator.generate(hasAudio)
        _responseText.value = response
        _statusText.value = if (hasAudio) "Tap mic to record again" else "No audio captured"

        if (hasAudio) {
            speakResponse()
        }
    }

    fun speakResponse() {
        if (_responseText.value.isBlank()) return
        val language = onboardingRepository.getUserLanguage()
        ttsService.speak(_responseText.value, language)
        _isSpeaking.value = true

        viewModelScope.launch {
            kotlinx.coroutines.delay(4000)
            _isSpeaking.value = false
        }
    }

    fun clearAll() {
        _responseText.value = ""
        _statusText.value = "Tap the microphone to start speaking"
        _isSpeaking.value = false
        ttsService.stop()
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
        ttsService.shutdown()
    }
}
