package com.mednavigator.app.data.models

import com.google.gson.Gson

// Extension functions to convert between Entity and UI models
fun ChatMessageEntity.toDomain(): ChatMessage {
    val reasoningSteps = if (reasoningJson?.isNotEmpty() == true) {
        try {
            Gson().fromJson(reasoningJson, Array<ReasoningStep>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    } else {
        emptyList()
    }

    return ChatMessage(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        timestamp = timestamp,
        messageType = messageType,
        imageHash = imageHash,
        reasoningSteps = reasoningSteps
    )
}

fun ChatMessage.toEntity(): ChatMessageEntity {
    val reasoningJson = if (reasoningSteps.isNotEmpty()) {
        Gson().toJson(reasoningSteps)
    } else {
        null
    }

    return ChatMessageEntity(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        timestamp = timestamp,
        messageType = messageType,
        imageHash = imageHash,
        reasoningJson = reasoningJson
    )
}

fun ConversationEntity.toDomain(messages: List<ChatMessage> = emptyList()): Conversation {
    return Conversation(
        id = id,
        title = title,
        startTime = startTime,
        endTime = endTime,
        systemSummary = systemSummary,
        messageCount = messageCount,
        messages = messages
    )
}

fun Conversation.toEntity(): ConversationEntity {
    return ConversationEntity(
        id = id,
        title = title,
        startTime = startTime,
        endTime = endTime,
        systemSummary = systemSummary,
        messageCount = messageCount
    )
}
