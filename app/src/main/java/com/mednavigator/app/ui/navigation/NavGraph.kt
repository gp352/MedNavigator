package com.mednavigator.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.ui.screens.HomeScreen
import com.mednavigator.app.ui.screens.ModelDownloadScreen
import com.mednavigator.app.ui.screens.OnboardingScreen
import com.mednavigator.app.ui.screens.SettingsScreen
import com.mednavigator.app.ui.screens.SplashScreen
import com.mednavigator.app.ui.screens.VoiceInputScreen

@Composable
fun NavGraph(navController: NavHostController, onboardingRepository: OnboardingRepository) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(navController, onboardingRepository)
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(navController, onboardingRepository)
        }
        composable(Routes.MODEL_DOWNLOAD) {
            ModelDownloadScreen(navController)
        }
        composable(Routes.HOME) {
            HomeScreen(navController, onboardingRepository)
        }
        composable(Routes.VOICE_INPUT) {
            VoiceInputScreen(navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController)
        }
    }
}
