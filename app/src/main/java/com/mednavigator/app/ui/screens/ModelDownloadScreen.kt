package com.mednavigator.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.services.ModelDownloadState
import com.mednavigator.app.ui.navigation.Routes
import com.mednavigator.app.ui.theme.CozyPrimary
import com.mednavigator.app.ui.theme.CozyPrimaryFixed
import com.mednavigator.app.ui.theme.CozyPrimaryFixedDim
import com.mednavigator.app.ui.theme.CozySurfaceContainerHigh
import com.mednavigator.app.ui.theme.CozySurfaceContainerHighest
import com.mednavigator.app.ui.theme.CozySurfaceContainerLow
import com.mednavigator.app.ui.viewmodel.ModelDownloadViewModel

@Composable
fun ModelDownloadScreen(
    navController: NavController,
    onboardingRepository: OnboardingRepository,
    viewModel: ModelDownloadViewModel = viewModel()
) {
    val downloadState by viewModel.downloadState.collectAsState()

    LaunchedEffect(downloadState) {
        if (downloadState is ModelDownloadState.Downloaded) {
            val destination = if (onboardingRepository.isOnboardingComplete()) Routes.HOME else Routes.ONBOARDING
            navController.navigate(destination) {
                popUpTo(Routes.MODEL_DOWNLOAD) { inclusive = true }
            }
        }
    }

    val progress = when (downloadState) {
        is ModelDownloadState.Downloading -> (downloadState as ModelDownloadState.Downloading).progress / 100f
        is ModelDownloadState.Downloaded  -> 1f
        else -> 0f
    }
    val progressLabel = when (downloadState) {
        is ModelDownloadState.Downloading -> "${(downloadState as ModelDownloadState.Downloading).progress}%"
        is ModelDownloadState.Downloaded  -> "100%"
        is ModelDownloadState.Failed      -> "Error"
        else -> "Waiting..."
    }
    val statusLabel = when (downloadState) {
        is ModelDownloadState.Downloading -> "Downloading..."
        is ModelDownloadState.Downloaded  -> "Ready!"
        is ModelDownloadState.Failed      -> (downloadState as ModelDownloadState.Failed).message
        else -> "Preparing..."
    }

    // Liquid wave rotation
    val inf = rememberInfiniteTransition(label = "wave")
    val rot1 by inf.animateFloat(
        0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart), label = "r1"
    )
    val rot2 by inf.animateFloat(
        360f, 0f, infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Restart), label = "r2"
    )
    val pulse by inf.animateFloat(
        0.97f, 1.03f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "p"
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Ambient background blobs
        Box(
            modifier = Modifier.size(200.dp).align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 40.dp).blur(60.dp)
                .background(CozyPrimary.copy(alpha = 0.05f), CircleShape)
        )
        Box(
            modifier = Modifier.size(160.dp).align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-20).dp).blur(50.dp)
                .background(CozyPrimaryFixedDim.copy(alpha = 0.08f), CircleShape)
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Liquid wave orb ─────────────────────────────────────────────
            Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                // Outer pulse glow
                Box(
                    modifier = Modifier.fillMaxSize().blur(40.dp)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                        .background(CozyPrimary.copy(alpha = 0.07f), CircleShape)
                )
                // Orb shell
                Box(
                    modifier = Modifier.size(176.dp).clip(CircleShape)
                        .background(CozySurfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    // Wave fill layer 1
                    Box(
                        modifier = Modifier.size(400.dp)
                            .offset(y = (88f * (1f - progress * 0.65f)).dp)
                            .graphicsLayer { rotationZ = rot1 }
                            .clip(CircleShape)
                            .background(CozyPrimaryFixed.copy(alpha = 0.35f))
                    )
                    // Wave fill layer 2
                    Box(
                        modifier = Modifier.size(400.dp)
                            .offset(y = (88f * (1f - progress * 0.65f) - 12f).dp)
                            .graphicsLayer { rotationZ = rot2 }
                            .clip(CircleShape)
                            .background(CozyPrimary.copy(alpha = 0.18f))
                    )
                    // Central icon
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape)
                            .background(CozySurfaceContainerHighest.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CloudDownload, null, tint = CozyPrimary, modifier = Modifier.size(36.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Getting things ready for you...",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We\u2019re setting up your personal health assistant.\nThis usually takes less than a minute.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(0.75f).height(10.dp).clip(CircleShape),
                color = CozyPrimary,
                trackColor = CozySurfaceContainerHigh,
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(0.75f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(progressLabel, style = MaterialTheme.typography.labelLarge, color = CozyPrimary)
                Text(statusLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Retry button — only shown on failure (ViewModel auto-starts download)
            if (downloadState is ModelDownloadState.Failed) {
                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = { viewModel.startDownload() },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = CozyPrimary),
                    modifier = Modifier.height(56.dp).fillMaxWidth(0.75f)
                ) {
                    Text(
                        "Retry Download",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }

        // "Your data is safe" card pinned to bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CozySurfaceContainerLow)
                .padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(CozyPrimaryFixedDim.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.VerifiedUser, null, tint = CozyPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Your data is safe", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Encryption is being initialized to ensure all your conversations remain private.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
