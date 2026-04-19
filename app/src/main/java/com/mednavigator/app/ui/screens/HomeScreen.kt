package com.mednavigator.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.ui.components.InputOptionCard
import com.mednavigator.app.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onboardingRepository: OnboardingRepository
) {
    val userName = remember { onboardingRepository.getUserName() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MedNavigator") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Rounded.Settings, "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hello, $userName!",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "How would you like to input your query?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Voice card (primary)
            val primaryColor = MaterialTheme.colorScheme.primary
            val primaryDark = MaterialTheme.colorScheme.primaryContainer

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { navController.navigate(Routes.VOICE_INPUT) },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor, primaryDark)
                            )
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tap to Speak",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Tap here to start voice input",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Other Options",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InputOptionCard(
                    icon = Icons.Rounded.DocumentScanner,
                    label = "Scan",
                    onClick = { scope.launch { snackbarHostState.showSnackbar("Coming soon") } }
                )
                InputOptionCard(
                    icon = Icons.Rounded.CameraAlt,
                    label = "Click",
                    onClick = { scope.launch { snackbarHostState.showSnackbar("Coming soon") } }
                )
                InputOptionCard(
                    icon = Icons.Rounded.UploadFile,
                    label = "Upload",
                    onClick = { scope.launch { snackbarHostState.showSnackbar("Coming soon") } }
                )
            }
        }
    }
}
