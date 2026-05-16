package com.mednavigator.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.ui.screens.HistoryScreen
import com.mednavigator.app.ui.screens.HomeScreen
import com.mednavigator.app.ui.screens.ModelDownloadScreen
import com.mednavigator.app.ui.screens.OnboardingScreen
import com.mednavigator.app.ui.screens.SettingsScreen
import com.mednavigator.app.ui.screens.SplashScreen

@Composable
fun NavGraph(navController: NavHostController, onboardingRepository: OnboardingRepository) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(navController, onboardingRepository)
        }
        composable(Routes.MODEL_DOWNLOAD) {
            ModelDownloadScreen(navController, onboardingRepository)
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(navController, onboardingRepository)
        }
        composable(Routes.HOME) {
            HomeScreen(navController, onboardingRepository)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController)
        }
        composable(Routes.HISTORY) {
            HistoryScreen(navController)
        }
        composable(
            route = Routes.CONVERSATION_DETAIL,
            arguments = listOf(navArgument("conversationId") { type = NavType.IntType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getInt("conversationId") ?: return@composable
            com.mednavigator.app.ui.screens.ConversationDetailScreen(navController, conversationId)
        }
    }
}
