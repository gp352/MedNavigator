package com.mednavigator.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.services.AudioRecorderService
import com.mednavigator.app.services.GemmaInferenceService
import com.mednavigator.app.services.ModelDownloadManager
import com.mednavigator.app.services.ModelDownloadState
import com.mednavigator.app.services.TextToSpeechService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "VoiceViewModel"
    }

    private val audioRecorder = AudioRecorderService()
    private val ttsService = TextToSpeechService(application)
    private val onboardingRepository = OnboardingRepository(application)
    private val modelDownloadManager = ModelDownloadManager(application)
    private val gemmaService = GemmaInferenceService(application)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _statusText = MutableStateFlow("Tap the microphone to start speaking")
    val statusText: StateFlow<String> = _statusText

    private val _responseText = MutableStateFlow("")
    val responseText: StateFlow<String> = _responseText

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.NotDownloaded)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState

    private val _modelReady = MutableStateFlow(false)
    val modelReady: StateFlow<Boolean> = _modelReady

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    init {
        modelDownloadManager.refreshState()
        viewModelScope.launch {
            modelDownloadManager.downloadState.collect { state ->
                _downloadState.value = state
                if (state is ModelDownloadState.Downloaded) {
                    loadModelIfNeeded()
                }
            }
        }
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        if (_isProcessing.value) {
            _statusText.value = "Model is still responding. Please wait..."
            return
        }
        if (!ensureModelReady()) {
            return
        }
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

        if (!hasAudio) {
            _responseText.value = "No audio captured"
            _statusText.value = "No audio captured"
            return
        }

        _isProcessing.value = true
        _statusText.value = "Processing audio..."
        _responseText.value = "Generating response..."
        viewModelScope.launch(Dispatchers.IO) {
            val prompt = buildPrompt()
            Log.d(TAG, "Starting model inference")
            val response = gemmaService.generateResponse(prompt, audioBytes)
            withContext(Dispatchers.Main) {
                if (response.isSuccess) {
                    Log.d(TAG, "Model response received")
                    _responseText.value = response.getOrDefault("")
                    _statusText.value = "Response ready"
                    speakResponse()
                } else {
                    val error = response.exceptionOrNull()?.message ?: "Model inference failed"
                    Log.e(TAG, "Model inference failed: $error")
                    _responseText.value = "Sorry, I couldn't process that. $error"
                    _statusText.value = "Tap mic to record again"
                }
                _isProcessing.value = false
            }
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
        _isProcessing.value = false
        ttsService.stop()
        gemmaService.closeConversation()
    }

    fun startModelDownload() {
        modelDownloadManager.startDownload()
    }

    private fun ensureModelReady(): Boolean {
        return when (val state = _downloadState.value) {
            is ModelDownloadState.NotDownloaded -> {
                _statusText.value = "AI model not downloaded. Download required on first launch."
                false
            }
            is ModelDownloadState.Downloading -> {
                _statusText.value = "Downloading AI model (${state.progress}%)"
                false
            }
            is ModelDownloadState.Failed -> {
                _statusText.value = "Model download failed. Tap Download to retry."
                false
            }
            is ModelDownloadState.Downloaded -> {
                if (_modelReady.value) {
                    true
                } else {
                    _statusText.value = "Preparing on-device model..."
                    loadModelIfNeeded()
                    false
                }
            }
        }
    }

    private fun loadModelIfNeeded() {
        if (_modelReady.value) return
        viewModelScope.launch(Dispatchers.IO) {
            val modelFile = modelDownloadManager.getModelFile()
            if (!modelFile.exists()) {
                withContext(Dispatchers.Main) {
                    _modelReady.value = false
                }
                return@launch
            }
            Log.d(TAG, "Loading model from ${modelFile.absolutePath}")
            val result = gemmaService.loadModel(modelFile)
            withContext(Dispatchers.Main) {
                _modelReady.value = result.isSuccess
                if (!_modelReady.value) {
                    val message = result.exceptionOrNull()?.message ?: "Model load failed"
                    _statusText.value = "Model load failed: $message"
                } else {
                    Log.d(TAG, "Model loaded successfully")
                    _statusText.value = "Tap the microphone to start speaking"
                }
            }
        }
    }

    private fun buildPrompt(): String {
        val age = onboardingRepository.getUserAge().takeIf { it > 0 }?.toString() ?: "unknown"
        val sex = onboardingRepository.getUserSex().ifBlank { "unknown" }
        val country = onboardingRepository.getUserCountry().ifBlank { "unknown" }
        val language = onboardingRepository.getUserLanguage()

        return """
    ${com.mednavigator.app.utils.Constants.SYSTEM_PROMPT}
    The user will provide symptoms via audio.
    Use the audio to infer symptoms and provide a brief, safe response.
    User profile: age=$age, sex=$sex, country=$country, language=$language.
    Respond in the user's language ($language). Keep the response concise and practical.
    If urgent symptoms are inferred, advise seeking immediate medical care.
    """.trimIndent()
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
        ttsService.shutdown()
        gemmaService.close()
    }
}
