package com.mednavigator.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mednavigator.app.data.ChatRepository
import com.mednavigator.app.data.IcdRepository
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.data.models.ChatMessage
import com.mednavigator.app.data.models.Conversation
import com.mednavigator.app.data.models.ReasoningStep
import com.mednavigator.app.services.AudioRecorderService
import com.mednavigator.app.services.GemmaInferenceService
import com.mednavigator.app.services.ModelDownloadManager
import com.mednavigator.app.services.ModelDownloadState
import com.mednavigator.app.services.TextToSpeechService
import com.mednavigator.app.services.SpeechToTextService
import com.mednavigator.app.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val audioRecorder = AudioRecorderService()
    private val ttsService = TextToSpeechService(application)
    private val onboardingRepository = OnboardingRepository(application)
    private val chatRepository = ChatRepository(application)
    private val gemmaService = GemmaInferenceService(application)
    private val modelDownloadManager = ModelDownloadManager(application)
    private val icdRepository = IcdRepository.getInstance(application)
    private val sttService = SpeechToTextService(application)

    // Model and Download state
    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.NotDownloaded)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState

    private val _modelReady = MutableStateFlow(false)
    val modelReady: StateFlow<Boolean> = _modelReady

    // Current conversation state
    private val _currentConversation = MutableStateFlow<Conversation?>(null)
    val currentConversation: StateFlow<Conversation?> = _currentConversation

    // Chat messages in current conversation
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    // Audio recording state
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    // Processing state (while waiting for model response)
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    // Current response being streamed
    private val _currentResponse = MutableStateFlow("")
    val currentResponse: StateFlow<String> = _currentResponse

    // Typing indicator
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    // Current reasoning steps being shown
    private val _currentReasoning = MutableStateFlow<List<ReasoningStep>>(emptyList())
    val currentReasoning: StateFlow<List<ReasoningStep>> = _currentReasoning

    // Status/error messages
    private val _statusMessage = MutableStateFlow("Ready")
    val statusMessage: StateFlow<String> = _statusMessage

    // Speaking state
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    init {
        // Initialize model state
        modelDownloadManager.refreshState()
        viewModelScope.launch {
            modelDownloadManager.downloadState.collect { state ->
                _downloadState.value = state
                if (state is ModelDownloadState.Downloaded) {
                    loadModelIfNeeded()
                }
            }
        }

        // Initialize ICD knowledge base in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                icdRepository.initializeIfNeeded()
                val stats = icdRepository.getDatabaseStats()
                Log.d(TAG, "ICD database initialized: $stats")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize ICD database", e)
                // Continue anyway - app can still function without ICD DB
            }
        }

        // Load or create a conversation on init
        viewModelScope.launch(Dispatchers.IO) {
            val latest = chatRepository.getLatestConversation()
            withContext(Dispatchers.Main) {
                if (latest != null && latest.isOngoing()) {
                    _currentConversation.value = latest
                    _messages.value = latest.messages
                } else {
                    startNewConversation()
                }
            }
        }
    }

    fun loadModelIfNeeded() {
        if (_modelReady.value) return
        viewModelScope.launch(Dispatchers.IO) {
            val modelFile = modelDownloadManager.getModelFile()
            if (!modelFile.exists()) {
                withContext(Dispatchers.Main) {
                    _modelReady.value = false
                    _statusMessage.value = "Model file not found"
                }
                return@launch
            }
            Log.d(TAG, "Loading model from ${modelFile.absolutePath}")
            val result = gemmaService.loadModel(modelFile)
            withContext(Dispatchers.Main) {
                _modelReady.value = result.isSuccess
                if (!_modelReady.value) {
                    val message = result.exceptionOrNull()?.message ?: "Model load failed"
                    _statusMessage.value = "Model load failed: $message"
                } else {
                    Log.d(TAG, "Model loaded successfully")
                    _statusMessage.value = "Ready"
                }
            }
        }
    }

    fun startModelDownload() {
        modelDownloadManager.startDownload()
    }

    private fun ensureModelReady(): Boolean {
        return when (val state = _downloadState.value) {
            is ModelDownloadState.NotDownloaded -> {
                _statusMessage.value = "AI model not downloaded"
                false
            }
            is ModelDownloadState.Downloading -> {
                _statusMessage.value = "Downloading model (${state.progress}%)"
                false
            }
            is ModelDownloadState.Failed -> {
                _statusMessage.value = "Download failed. Tap to retry."
                false
            }
            is ModelDownloadState.Downloaded -> {
                if (_modelReady.value) {
                    true
                } else {
                    _statusMessage.value = "Preparing model..."
                    loadModelIfNeeded()
                    false
                }
            }
        }
    }

    /**
     * Start a new conversation
     */
    fun startNewConversation() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val title = "Chat - ${System.currentTimeMillis()}"
                val conversationId = chatRepository.createConversation(title)
                val conversation = chatRepository.getConversationWithMessages(conversationId.toInt())
                withContext(Dispatchers.Main) {
                    _currentConversation.value = conversation
                    _messages.value = emptyList()
                    _statusMessage.value = "New conversation started"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start new conversation", e)
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Error: ${e.message}"
                }
            }
        }
    }

    /**
     * Send a text message and get AI response
     */
    fun sendMessage(userText: String) {
        if (userText.isBlank() || _currentConversation.value == null) return

        val conversationId = _currentConversation.value!!.id

        viewModelScope.launch {
            try {
                // Add user message
                _isProcessing.value = true
                _statusMessage.value = "Sending message..."

                withContext(Dispatchers.IO) {
                    chatRepository.addMessage(
                        conversationId = conversationId,
                        role = "user",
                        content = userText
                    )
                }

                // Create user chat bubble in UI
                val userMessage = ChatMessage(
                    id = 0,
                    conversationId = conversationId,
                    role = "user",
                    content = userText,
                    timestamp = System.currentTimeMillis()
                )
                _messages.value = _messages.value + userMessage

                // Get AI response (streaming for better UX)
                _isTyping.value = true
                _statusMessage.value = "AI is thinking..."
                _currentResponse.value = ""

                // Build prompt with ICD knowledge base context
                val prompt = withContext(Dispatchers.IO) {
                    buildPromptWithIcdContext(userText)
                }

                // Use streaming for text input as well
                try {
                    var fullResponse = ""
                    gemmaService.generateResponseStream(prompt, ByteArray(0)).collect { chunk ->
                        fullResponse += chunk
                        _currentResponse.value = fullResponse
                        _statusMessage.value = "AI is responding..."
                    }

                    // Extract symptoms from model response for better context next time
                    val detectedSymptoms = extractSymptoms(fullResponse)
                    if (detectedSymptoms.isNotEmpty()) {
                        Log.d(TAG, "Detected symptoms in response: $detectedSymptoms")
                    }

                    // Save AI response to database
                    val messageId = withContext(Dispatchers.IO) {
                        chatRepository.addMessage(
                            conversationId = conversationId,
                            role = "assistant",
                            content = fullResponse
                        )
                    }

                    // Create AI message bubble
                    val aiMessage = ChatMessage(
                        id = messageId.toInt(),
                        conversationId = conversationId,
                        role = "assistant",
                        content = fullResponse,
                        timestamp = System.currentTimeMillis()
                    )
                    _messages.value = _messages.value + aiMessage

                    _statusMessage.value = "Response ready"
                    speakResponse(fullResponse)
                } catch (e: Exception) {
                    Log.e(TAG, "Streaming failed for text, falling back to single response", e)
                    val response = gemmaService.generateResponse(prompt, ByteArray(0))
                    if (response.isSuccess) {
                        val responseText = response.getOrDefault("I couldn't understand that. Please try again.")
                        _currentResponse.value = responseText

                        // Save AI response to database
                        val messageId = withContext(Dispatchers.IO) {
                            chatRepository.addMessage(
                                conversationId = conversationId,
                                role = "assistant",
                                content = responseText
                            )
                        }

                        // Create AI message bubble
                        val aiMessage = ChatMessage(
                            id = messageId.toInt(),
                            conversationId = conversationId,
                            role = "assistant",
                            content = responseText,
                            timestamp = System.currentTimeMillis()
                        )
                        _messages.value = _messages.value + aiMessage

                        _statusMessage.value = "Response ready"
                        speakResponse(responseText)
                    } else {
                        val errorMsg = response.exceptionOrNull()?.message ?: "Unknown error"
                        _statusMessage.value = "Error: $errorMsg"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Error: ${e.message}"
                }
            } finally {
                _isProcessing.value = false
                _isTyping.value = false
            }
        }
    }

    /**
     * Start voice recording for audio input
     */
    fun startVoiceRecording() {
        if (_isProcessing.value || _isRecording.value) return
        
        if (!ensureModelReady()) return

        _isRecording.value = true
        _statusMessage.value = "Listening..."

        viewModelScope.launch {
            val result = audioRecorder.startRecording()
            if (result.isFailure) {
                Log.e(TAG, "Failed to start recording: ${result.exceptionOrNull()?.message}")
                withContext(Dispatchers.Main) {
                    _isRecording.value = false
                    _statusMessage.value = "Recording failed"
                }
            }
        }
    }

    /**
     * Stop voice recording and send as message
     */
    fun stopVoiceRecording() {
        if (!_isRecording.value) return
        _isRecording.value = false

        val audioBytes = audioRecorder.stopRecording()
        val hasAudio = audioBytes != null && audioBytes.isNotEmpty()

        if (!hasAudio) {
            _statusMessage.value = "No audio recorded"
            return
        }

        val conversationId = _currentConversation.value?.id ?: return

        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _statusMessage.value = "Transcribing audio..."

                // First, transcribe the audio to text
                val transcriptionResult = withContext(Dispatchers.IO) {
                    sttService.recognizeSpeech()
                }

                val transcribedText = if (transcriptionResult.isSuccess) {
                    transcriptionResult.getOrDefault("")
                } else {
                    Log.w(TAG, "STT failed: ${transcriptionResult.exceptionOrNull()?.message}")
                    "[Voice message - transcription failed]"
                }

                Log.d(TAG, "Transcribed text: '$transcribedText'")

                // Save transcribed message to database
                withContext(Dispatchers.IO) {
                    chatRepository.addMessage(
                        conversationId = conversationId,
                        role = "user",
                        content = transcribedText
                    )
                }

                // Create user message bubble with transcribed text
                val userMessage = ChatMessage(
                    id = 0,
                    conversationId = conversationId,
                    role = "user",
                    content = transcribedText,
                    timestamp = System.currentTimeMillis(),
                    messageType = "AUDIO"
                )
                _messages.value = _messages.value + userMessage

                // Now process with AI
                _isTyping.value = true
                _statusMessage.value = "AI is thinking..."
                _currentResponse.value = ""

                // Build prompt with ICD knowledge base context for transcribed text
                val prompt = withContext(Dispatchers.IO) {
                    buildPromptWithIcdContext(transcribedText)
                }

                // Use streaming for voice input
                try {
                    var fullResponse = ""
                    gemmaService.generateResponseStream(prompt, audioBytes).collect { chunk ->
                        fullResponse += chunk
                        _currentResponse.value = fullResponse
                        _statusMessage.value = "AI is responding..."
                    }

                    // Extract symptoms from model response for better context next time
                    val detectedSymptoms = extractSymptoms(fullResponse)
                    if (detectedSymptoms.isNotEmpty()) {
                        Log.d(TAG, "Detected symptoms in response: $detectedSymptoms")
                    }

                    // Save AI response
                    val messageId = withContext(Dispatchers.IO) {
                        chatRepository.addMessage(
                            conversationId = conversationId,
                            role = "assistant",
                            content = fullResponse
                        )
                    }

                    // Create AI message bubble
                    val aiMessage = ChatMessage(
                        id = messageId.toInt(),
                        conversationId = conversationId,
                        role = "assistant",
                        content = fullResponse,
                        timestamp = System.currentTimeMillis()
                    )

                    withContext(Dispatchers.Main) {
                        _messages.value = _messages.value + aiMessage
                        _statusMessage.value = "Response ready"
                        speakResponse(fullResponse)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Streaming failed, falling back to single response", e)
                    val response = gemmaService.generateResponse(prompt, audioBytes)
                    if (response.isSuccess) {
                        val fullResponse = response.getOrDefault("")
                        val messageId = withContext(Dispatchers.IO) {
                            chatRepository.addMessage(
                                conversationId = conversationId,
                                role = "assistant",
                                content = fullResponse
                            )
                        }
                        val aiMessage = ChatMessage(
                            id = messageId.toInt(),
                            conversationId = conversationId,
                            role = "assistant",
                            content = fullResponse,
                            timestamp = System.currentTimeMillis()
                        )
                        withContext(Dispatchers.Main) {
                            _messages.value = _messages.value + aiMessage
                            _statusMessage.value = "Response ready"
                            speakResponse(fullResponse)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing voice message", e)
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Error: ${e.message}"
                }
            } finally {
                _isProcessing.value = false
                _isTyping.value = false
            }
        }
    }

    /**
     * Speak the AI response using TTS
     */
    private fun speakResponse(text: String) {
        if (text.isBlank()) return
        val language = onboardingRepository.getUserLanguage()
        ttsService.speak(text, language)
        _isSpeaking.value = true

        viewModelScope.launch {
            kotlinx.coroutines.delay(4000)
            _isSpeaking.value = false
        }
    }

    /**
     * Clear the current conversation and messages
     */
    fun clearChat() {
        _messages.value = emptyList()
        _currentResponse.value = ""
        _statusMessage.value = "Chat cleared"
        _currentReasoning.value = emptyList()
    }

    /**
     * End current conversation
     */
    fun endConversation() {
        val conversationId = _currentConversation.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.endConversation(conversationId)
                withContext(Dispatchers.Main) {
                    startNewConversation()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error ending conversation", e)
            }
        }
    }

    /**
     * Load a previous conversation
     */
    fun loadConversation(conversationId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val conversation = chatRepository.getConversationWithMessages(conversationId)
                withContext(Dispatchers.Main) {
                    _currentConversation.value = conversation
                    _messages.value = conversation?.messages ?: emptyList()
                    _statusMessage.value = "Conversation loaded"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading conversation", e)
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Error: ${e.message}"
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
    ${Constants.SYSTEM_PROMPT}
    The user will provide symptoms via text or audio.
    Provide a helpful medical guidance response.
    User profile: age=$age, sex=$sex, country=$country, language=$language.
    Respond in the user's language ($language). Keep the response concise and practical.
    If urgent symptoms are inferred, advise seeking immediate medical care.
    """.trimIndent()
    }

    /**
     * Extract potential symptoms from user message for ICD context
     */
    private fun extractSymptoms(userMessage: String): List<String> {
        // Common symptom keywords to detect
        val commonSymptoms = listOf(
            "chest pain", "chest", "heart", "palpitations",
            "shortness of breath", "dyspnea", "breathing", "cough",
            "fever", "temperature", "headache", "head pain",
            "dizziness", "vertigo", "nausea", "vomiting",
            "abdominal pain", "stomach", "diarrhea", "constipation",
            "back pain", "joint pain", "arthritis",
            "anxiety", "depression", "stress",
            "flu", "cold", "infection", "virus"
        )

        val lowerMessage = userMessage.lowercase()
        return commonSymptoms.filter { symptom ->
            lowerMessage.contains(symptom)
        }
    }

    /**
     * Build prompt with ICD knowledge base context
     */
    private suspend fun buildPromptWithIcdContext(userMessage: String): String {
        val basePrompt = buildPrompt()

        // Extract symptoms and build ICD context
        val symptoms = extractSymptoms(userMessage)
        val icdContext = if (symptoms.isNotEmpty()) {
            icdRepository.buildComprehensiveMedicalContext(symptoms)
        } else {
            ""
        }

        val language = onboardingRepository.getUserLanguage()
        Log.d(TAG, "Building prompt with language: $language, detected symptoms: $symptoms")

        return if (icdContext.isNotEmpty()) {
            """
            $basePrompt
            
            $icdContext
            """.trimIndent()
        } else {
            basePrompt
        }
    }

    /**
     * Get comprehensive medical statistics (for testing)
     */
    suspend fun getIcdDatabaseStats(): Map<String, Int> {
        return icdRepository.getDatabaseStats()
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
        ttsService.shutdown()
        gemmaService.close()
        sttService.release()
    }
}
