package com.mednavigator.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mednavigator.app.services.ModelDownloadState

@Composable
fun ModelDownloadCard(
    state: ModelDownloadState,
    modelReady: Boolean,
    onDownloadClick: () -> Unit
) {
    if (state is ModelDownloadState.Downloaded && modelReady) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when (state) {
                    is ModelDownloadState.Failed -> Icons.Rounded.Error
                    is ModelDownloadState.Downloaded -> Icons.Rounded.Verified
                    else -> Icons.Rounded.CloudDownload
                }
                val tint = when (state) {
                    is ModelDownloadState.Failed -> MaterialTheme.colorScheme.error
                    is ModelDownloadState.Downloaded -> Color(0xFF2E7D32)
                    else -> MaterialTheme.colorScheme.primary
                }

                Icon(icon, contentDescription = null, tint = tint)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Model Setup",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (state) {
                is ModelDownloadState.NotDownloaded -> {
                    Text(
                        text = "Download the on-device model to enable voice analysis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onDownloadClick) {
                        Text("Download model")
                    }
                }
                is ModelDownloadState.Downloading -> {
                    Text(
                        text = "Downloading model (${state.progress}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val totalMb = if (state.totalBytes > 0) state.totalBytes / (1024f * 1024f) else 0f
                    val doneMb = state.bytesDownloaded / (1024f * 1024f)
                    val subtitle = if (state.totalBytes > 0) {
                        String.format("%.1f MB of %.1f MB", doneMb, totalMb)
                    } else {
                        String.format("%.1f MB downloaded", doneMb)
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is ModelDownloadState.Downloaded -> {
                    Text(
                        text = if (modelReady) "Model ready" else "Model downloaded, preparing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!modelReady) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                is ModelDownloadState.Failed -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onDownloadClick) {
                        Text("Retry download")
                    }
                }
            }
        }
    }
}
