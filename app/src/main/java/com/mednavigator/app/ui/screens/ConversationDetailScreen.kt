package com.mednavigator.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mednavigator.app.data.models.ChatMessage
import com.mednavigator.app.ui.components.AudioPlaybackButton
import com.mednavigator.app.ui.theme.CozyPrimary
import com.mednavigator.app.ui.theme.CozyPrimaryFixed
import com.mednavigator.app.ui.viewmodel.ChatViewModel
import com.mednavigator.app.ui.viewmodel.HistoryViewModel

@Composable
fun ConversationDetailScreen(
    navController: NavController,
    conversationId: Int,
    chatViewModel: ChatViewModel = viewModel(),
    historyViewModel: HistoryViewModel = viewModel()
) {
    val messages by chatViewModel.messages.collectAsState()
    val conversation by chatViewModel.currentConversation.collectAsState()

    LaunchedEffect(conversationId) {
        chatViewModel.loadConversationForDetail(conversationId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = CozyPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation?.title ?: "Conversation",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                conversation?.startTime?.let { ts ->
                    Text(
                        text = HistoryViewModel.formatTimestamp(ts),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CozyPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    MessageWithAudioCard(message)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun MessageWithAudioCard(message: ChatMessage) {
    val isUser = message.isFromUser()
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val cardColor = if (isUser) CozyPrimaryFixed else MaterialTheme.colorScheme.surfaceContainerLow
    val audioPath = if (isUser) message.audioFilePath else message.responseAudioPath

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isUser) CozyPrimary else MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUser) Icons.Rounded.Mic else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = if (isUser) androidx.compose.ui.graphics.Color.White else CozyPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUser) "You" else "CareVoice",
                        style = MaterialTheme.typography.labelLarge,
                        color = CozyPrimary
                    )
                }

                if (message.content.isNotBlank() && message.content != "[Voice message]") {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (audioPath != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        AudioPlaybackButton(audioFilePath = audioPath, size = 32.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Play audio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
