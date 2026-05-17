package com.mednavigator.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.services.ModelDownloadManager
import com.mednavigator.app.ui.navigation.Routes
import com.mednavigator.app.ui.theme.CozyOnPrimaryFixedVariant
import com.mednavigator.app.ui.theme.CozyPrimary
import com.mednavigator.app.ui.theme.CozyPrimaryContainer
import com.mednavigator.app.ui.theme.CozyPrimaryFixed
import com.mednavigator.app.ui.theme.CozyPrimaryFixedDim

@Composable
fun SplashScreen(navController: NavController, onboardingRepository: OnboardingRepository) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val isOnboarded = onboardingRepository.isOnboardingComplete()
        val modelReady = ModelDownloadManager(context).getModelFile().exists()

        val destination = when {
            !modelReady  -> Routes.MODEL_DOWNLOAD
            !isOnboarded -> Routes.ONBOARDING
            else         -> Routes.HOME
        }
        navController.navigate(destination) {
            popUpTo(Routes.SPLASH) { inclusive = true }
        }
    }

    // Ambient glow animations — same style as HomeScreen blobs
    val inf = rememberInfiniteTransition(label = "splash")
    val a1 by inf.animateFloat(0.12f, 0.22f,
        infiniteRepeatable(tween(5000), RepeatMode.Reverse), label = "a1")
    val orbPulse by inf.animateFloat(0.92f, 1.08f,
        infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "pulse")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Background ambient blobs
        Box(
            modifier = Modifier
                .size(480.dp)
                .align(Alignment.TopStart)
                .offset(x = (-100).dp, y = (-60).dp)
                .blur(120.dp)
                .background(CozyPrimary.copy(alpha = a1), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 80.dp)
                .blur(90.dp)
                .background(CozyPrimaryFixedDim.copy(alpha = 0.15f), CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Mic orb — identical to HomeScreen idle state
            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                // Outer glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(40.dp)
                        .graphicsLayer { scaleX = orbPulse * 0.95f; scaleY = orbPulse * 0.95f }
                        .background(CozyPrimary.copy(alpha = 0.3f), CircleShape)
                )
                // Inner glow
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .blur(20.dp)
                        .graphicsLayer { scaleX = orbPulse; scaleY = orbPulse }
                        .background(CozyPrimaryContainer.copy(alpha = 0.25f), CircleShape)
                )
                // Solid orb
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .graphicsLayer { scaleX = orbPulse; scaleY = orbPulse }
                        .background(
                            Brush.radialGradient(listOf(CozyPrimaryFixed, CozyPrimary)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "CareVoice",
                style = MaterialTheme.typography.headlineLarge,
                color = CozyPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your personal health companion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

