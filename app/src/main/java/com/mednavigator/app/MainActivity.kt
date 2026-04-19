package com.mednavigator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.ui.navigation.NavGraph
import com.mednavigator.app.ui.theme.MedNavigatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedNavigatorTheme {
                val navController = rememberNavController()
                val onboardingRepository = remember {
                    OnboardingRepository(applicationContext)
                }
                NavGraph(navController, onboardingRepository)
            }
        }
    }
}
