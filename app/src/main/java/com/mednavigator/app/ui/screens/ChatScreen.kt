package com.mednavigator.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mednavigator.app.ui.components.ChatBubble
import com.mednavigator.app.ui.components.ChatInputBar
import com.mednavigator.app.ui.components.TypingIndicator
import com.mednavigator.app.ui.viewmodel.ChatViewModel
import com.mednavigator.app.services.ModelDownloadState
import kotlinx.coroutines.launch

/**
 * Main Chat Screen - Consolidated AI interface for Voice, Text, Scan, and Upload
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val currentResponse by viewModel.currentResponse.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val conversation by viewModel.currentConversation.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val modelReady by viewModel.modelReady.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MedNavigator AI",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = when {
                                downloadState is ModelDownloadState.Downloading -> 
                                    "Downloading model (${(downloadState as ModelDownloadState.Downloading).progress}%)"
                                !modelReady && downloadState is ModelDownloadState.Downloaded -> 
                                    "Initializing model..."
                                !modelReady -> "Model not ready"
                                else -> statusMessage
                            },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 2.dp),
                            color = if (modelReady) MaterialTheme.colorScheme.onPrimaryContainer 
                                    else MaterialTheme.colorScheme.error
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        enabled = messages.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear chat"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                onSendMessage = { message ->
                    viewModel.sendMessage(message)
                },
                onMicToggle = { turnOn ->
                    if (turnOn) {
                        viewModel.startVoiceRecording()
                    } else {
                        viewModel.stopVoiceRecording()
                    }
                },
                isRecording = isRecording,
                isProcessing = isProcessing
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Download/Loading indicator
                if (!modelReady) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    
                    if (downloadState is ModelDownloadState.NotDownloaded || downloadState is ModelDownloadState.Failed) {
                        Button(
                            onClick = { viewModel.startModelDownload() },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Text("Download Required AI Model")
                        }
                    }
                }

                if (messages.isEmpty() && !isTyping) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Start a conversation",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Type a message or use your microphone to get AI-powered medical guidance",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Chat messages
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(messages) { message ->
                            ChatBubble(message = message)
                        }

                        // Show typing indicator
                        if (isTyping) {
                            item {
                                TypingIndicator()
                            }
                        }

                        // Show current response being streamed
                        if (currentResponse.isNotEmpty()) {
                            item {
                                ChatBubble(
                                    message = com.mednavigator.app.data.models.ChatMessage(
                                        id = -1,
                                        conversationId = conversation?.id ?: 0,
                                        role = "assistant",
                                        content = currentResponse,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Recording indicator overlay - inside BoxScope to use align
            if (isRecording) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Animated recording indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.onError)
                        )
                        Text(
                            text = "Recording...",
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
