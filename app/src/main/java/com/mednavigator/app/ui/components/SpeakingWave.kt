package com.mednavigator.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SpeakingWave(
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    barCount: Int = 5,
    barWidth: Dp = 10.dp,
    barHeight: Dp = 40.dp
) {
    val transition = rememberInfiniteTransition(label = "speaking")
    val scales = (0 until barCount).map { index ->
        transition.animateFloat(
            initialValue = 0.3f + (index % 3) * 0.1f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600 + index * 50, delayMillis = index * 100),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale$index"
        )
    }

    Row(modifier = modifier) {
        scales.forEach { scale ->
            WaveBar(
                scale = scale.value,
                color = barColor,
                width = barWidth,
                height = barHeight
            )
        }
    }
}

@Composable
private fun WaveBar(scale: Float, color: Color, width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .width(width)
            .height(height)
            .graphicsLayer { scaleY = scale }
            .clip(RoundedCornerShape(6.dp))
            .background(color)
    )
}
