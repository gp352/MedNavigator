package com.mednavigator.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mednavigator.app.services.ModelDownloadState
import com.mednavigator.app.ui.components.ModelDownloadCard
import com.mednavigator.app.ui.navigation.Routes
import com.mednavigator.app.ui.viewmodel.ModelDownloadViewModel

@Composable
fun ModelDownloadScreen(
    navController: NavController,
    viewModel: ModelDownloadViewModel = viewModel()
) {
    val downloadState by viewModel.downloadState.collectAsState()

    LaunchedEffect(downloadState) {
        if (downloadState is ModelDownloadState.Downloaded) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.MODEL_DOWNLOAD) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Setting up MedNavigator",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We need to download the on-device AI model once.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        ModelDownloadCard(
            state = downloadState,
            modelReady = downloadState is ModelDownloadState.Downloaded,
            onDownloadClick = { viewModel.startDownload() }
        )
    }
}
