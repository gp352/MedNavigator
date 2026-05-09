package com.mednavigator.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mednavigator.app.data.models.Conversation
import com.mednavigator.app.ui.navigation.Routes
import com.mednavigator.app.ui.theme.CozyOnPrimaryFixedVariant
import com.mednavigator.app.ui.theme.CozyPrimary
import com.mednavigator.app.ui.theme.CozyPrimaryFixed
import com.mednavigator.app.ui.theme.CozySecondaryContainer
import com.mednavigator.app.ui.theme.CozySurfaceContainerLowest
import com.mednavigator.app.ui.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = viewModel()
) {
    val conversations by viewModel.conversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }

    val filtered = remember(searchQuery, conversations) {
        if (searchQuery.isBlank()) conversations
        else conversations.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.systemSummary?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    // Delete confirmation dialog
    deleteTarget?.let { conv ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete conversation?") },
            text = { Text("\"${conv.title}\" will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteConversation(conv.id)
                    deleteTarget = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // ── Top bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CozyPrimaryFixed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Person, null,
                        tint = CozyOnPrimaryFixedVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Assistant",
                    style = MaterialTheme.typography.titleLarge,
                    color = CozyPrimary
                )
            }
            IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                Icon(Icons.Rounded.Settings, "Settings", tint = CozyPrimary)
            }
        }

        // ── Scrollable content ─────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "History",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Review your past conversations and health checkups.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search sessions...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = CircleShape,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedBorderColor    = Color.Transparent,
                        focusedBorderColor      = CozyPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Loading state
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CozyPrimary)
                    }
                }
            }

            // Empty state
            if (!isLoading && filtered.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(CozySecondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.History,
                                null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No conversations yet" else "No results found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isBlank())
                                "Start a voice session from the home screen to see your history here."
                            else
                                "Try different keywords.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Session cards
            items(filtered) { conv ->
                SessionCard(
                    conversation = conv,
                    onClick = {
                        navController.navigate(Routes.conversationDetail(conv.id))
                    },
                    onDelete = { deleteTarget = conv }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // ── Bottom nav ─────────────────────────────────────────────────────
        CozyBottomNav(
            currentRoute = Routes.HISTORY,
            onTalkClick = { navController.navigate(Routes.HOME) },
            onHistoryClick = { /* already here */ },
            onScanClick = { navController.navigate(Routes.HOME) }
        )
    }
}

@Composable
private fun SessionCard(
    conversation: Conversation,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val iconBg = when {
        conversation.messageCount >= 10 -> CozySecondaryContainer
        conversation.isOngoing()        -> CozyPrimaryFixed
        else                            -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(CozySurfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ChatBubbleOutline,
                contentDescription = null,
                tint = CozyOnPrimaryFixedVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text block
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = HistoryViewModel.formatTimestamp(conversation.startTime),
                    style = MaterialTheme.typography.labelLarge,
                    color = CozyPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(visible = conversation.isOngoing(), enter = fadeIn(), exit = fadeOut()) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CozyPrimary)
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = conversation.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!conversation.systemSummary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = conversation.systemSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${conversation.messageCount} message${if (conversation.messageCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
