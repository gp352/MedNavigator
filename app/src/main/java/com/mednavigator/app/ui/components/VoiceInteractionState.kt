package com.mednavigator.app.ui.components

sealed class VoiceInteractionState {
    data object Idle : VoiceInteractionState()
    data object Listening : VoiceInteractionState()
    data class Processing(val transcription: String = "") : VoiceInteractionState()
    data class Responding(
        val responseText: String = "",
        val isSpeaking: Boolean = false
    ) : VoiceInteractionState()
}
