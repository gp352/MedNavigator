package com.mednavigator.app.data.models

import com.google.gson.Gson

// UI-layer data model for a chat message
data class ChatMessage(
    val id: Int,
    val conversationId: Int,
    val role: String,              // "user" or "assistant"
    val content: String,
    val timestamp: Long,
    val messageType: String = "TEXT", // TEXT, VOICE, IMAGE, ANNOTATION
    val imageHash: String? = null,
    val reasoningSteps: List<ReasoningStep> = emptyList(),
    val audioFilePath: String? = null,
    val responseAudioPath: String? = null,
    val voiceTranscript: String? = null  // raw STT transcript (set when messageType == "VOICE")
) {
    fun isFromUser(): Boolean = role == "user"
    fun isFromAssistant(): Boolean = role == "assistant"
    fun isVoiceMessage(): Boolean = messageType == "VOICE"
}

// Reasoning step exposed to user for explainability
data class ReasoningStep(
    val stepNumber: Int,
    val content: String,
    val confidence: Float = 0.8f
)

// Conversation model for chat history
data class Conversation(
    val id: Int,
    val title: String,
    val startTime: Long,
    val endTime: Long? = null,
    val systemSummary: String? = null,
    val messageCount: Int = 0,
    val messages: List<ChatMessage> = emptyList() // lazy-loaded
) {
    fun isOngoing(): Boolean = endTime == null
}
