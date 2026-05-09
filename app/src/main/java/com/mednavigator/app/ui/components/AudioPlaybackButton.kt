package com.mednavigator.app.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun AudioPlaybackButton(
    audioFilePath: String?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    val file = audioFilePath?.let { File(it) }
    val fileExists = file != null && file.exists()

    if (!fileExists) return

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable {
                if (isPlaying) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    isPlaying = false
                } else {
                    try {
                        val mp = MediaPlayer().apply {
                            setDataSource(file!!.absolutePath)
                            prepare()
                            setOnCompletionListener {
                                isPlaying = false
                                release()
                            }
                            start()
                        }
                        mediaPlayer = mp
                        isPlaying = true
                    } catch (_: Exception) {
                        isPlaying = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Stop" else "Play",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
