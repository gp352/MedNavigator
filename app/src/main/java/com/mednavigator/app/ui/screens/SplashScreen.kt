package com.mednavigator.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.ui.navigation.Routes

@Composable
fun SplashScreen(navController: NavController, onboardingRepository: OnboardingRepository) {
    LaunchedEffect(Unit) {
        val destination = if (onboardingRepository.isOnboardingComplete()) {
            Routes.HOME
        } else {
            Routes.ONBOARDING
        }
        navController.navigate(destination) {
            popUpTo(Routes.SPLASH) { inclusive = true }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.LocalHospital,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "MedNavigator",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(32.dp))
        CircularProgressIndicator(color = Color.White)
    }
}
