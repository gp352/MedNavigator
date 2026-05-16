package com.mednavigator.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mednavigator.app.data.ChatRepository
import com.mednavigator.app.data.models.Conversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ChatRepository(application)

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _conversations.value = repo.getRecentConversations()
            _isLoading.value = false
        }
    }

    fun deleteConversation(conversationId: Int) {
        viewModelScope.launch {
            repo.deleteConversation(conversationId)
            loadHistory()
        }
    }

    companion object {
        fun formatTimestamp(ts: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - ts
            return when {
                diff < 60_000L              -> "Just now"
                diff < 3_600_000L           -> "${diff / 60_000}m ago"
                diff < 86_400_000L          -> "Today, ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))}"
                diff < 172_800_000L         -> "Yesterday"
                else                        -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ts))
            }
        }
    }
}
