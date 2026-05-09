package com.mednavigator.app.data

import android.content.Context
import androidx.room.Room
import com.mednavigator.app.data.models.*

class ChatRepository(context: Context) {

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "med_navigator.db"
    ).addMigrations(MIGRATION_2_3).build()

    private val conversationDao = database.conversationDao()
    private val messageDao = database.chatMessageDao()

    // Create a new conversation
    suspend fun createConversation(title: String): Int {
        val conversation = ConversationEntity(
            title = title,
            startTime = System.currentTimeMillis(),
            messageCount = 0
        )
        return conversationDao.insertConversation(conversation).toInt()
    }

    // Get all recent conversations
    suspend fun getRecentConversations(): List<Conversation> {
        return conversationDao.getRecentConversations().map { entity ->
            entity.toDomain()
        }
    }

    // Get a specific conversation with all its messages
    suspend fun getConversationWithMessages(conversationId: Int): Conversation? {
        val conversationEntity = conversationDao.getConversationById(conversationId) ?: return null
        val messages = messageDao.getMessagesForConversation(conversationId).map { it.toDomain() }
        return conversationEntity.toDomain(messages)
    }

    // Add a message to a conversation
    suspend fun addMessage(
        conversationId: Int,
        role: String,
        content: String,
        messageType: String = "TEXT",
        imageHash: String? = null,
        reasoningSteps: List<ReasoningStep> = emptyList(),
        audioFilePath: String? = null,
        responseAudioPath: String? = null
    ): Long {
        val reasoningJson = if (reasoningSteps.isNotEmpty()) {
            com.google.gson.Gson().toJson(reasoningSteps)
        } else {
            null
        }

        val message = ChatMessageEntity(
            conversationId = conversationId,
            role = role,
            content = content,
            timestamp = System.currentTimeMillis(),
            messageType = messageType,
            imageHash = imageHash,
            reasoningJson = reasoningJson,
            audioFilePath = audioFilePath,
            responseAudioPath = responseAudioPath
        )
        return messageDao.insertMessage(message)
    }

    // Get messages for a conversation (paginated)
    suspend fun getMessagesForConversation(
        conversationId: Int,
        limit: Int = 100
    ): List<ChatMessage> {
        return messageDao.getRecentMessagesForConversation(conversationId, limit)
            .sortedBy { it.timestamp }
            .map { it.toDomain() }
    }

    // Update conversation (e.g., close it, update title)
    suspend fun updateConversation(conversation: Conversation) {
        conversationDao.updateConversation(conversation.toEntity())
    }

    // End a conversation
    suspend fun endConversation(conversationId: Int) {
        val conversation = conversationDao.getConversationById(conversationId) ?: return
        val updated = conversation.copy(endTime = System.currentTimeMillis())
        conversationDao.updateConversation(updated)
    }

    // Delete a conversation and all its messages
    suspend fun deleteConversation(conversationId: Int) {
        val conversation = conversationDao.getConversationById(conversationId) ?: return
        messageDao.deleteMessagesForConversation(conversationId)
        conversationDao.deleteConversation(conversation)
    }

    // Get latest conversation (for resume/continue)
    suspend fun getLatestConversation(): Conversation? {
        val conversations = conversationDao.getRecentConversations()
        return conversations.firstOrNull()?.let { entity ->
            val messages = messageDao.getMessagesForConversation(entity.id).map { it.toDomain() }
            entity.toDomain(messages)
        }
    }

    // Count messages in a conversation
    suspend fun getMessageCount(conversationId: Int): Int {
        val messages = messageDao.getRecentMessagesForConversation(conversationId, 10000)
        return messages.size
    }
}
