package com.mednavigator.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MicButton(
    isRecording: Boolean,
    onClick: () -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        if (isRecording) {
            PulsingCircle(color = MaterialTheme.colorScheme.primary)
        }
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    if (isRecording) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Rounded.Mic else Icons.Rounded.MicNone,
                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                modifier = Modifier.size(36.dp),
                tint = if (isRecording) Color.White else MaterialTheme.colorScheme.primary
            )
        }
    }
}
