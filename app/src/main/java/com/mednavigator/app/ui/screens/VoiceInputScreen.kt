package com.mednavigator.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mednavigator.app.ui.components.MicButton
import com.mednavigator.app.ui.components.ResponseCard
import com.mednavigator.app.ui.viewmodel.VoiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputScreen(
    navController: NavController,
    viewModel: VoiceViewModel = viewModel()
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val responseText by viewModel.responseText.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()

    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            val activity = context as? androidx.activity.ComponentActivity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.RECORD_AUDIO
                )
            ) {
                showPermissionDialog = true
            }
        }
    }

    fun checkAndRequestPermission(): Boolean {
        if (hasPermission) return true
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        return false
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Microphone Permission Required") },
            text = { Text("This app needs microphone access for voice input. Please enable it in app settings.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.fromParts("package", context.packageName, null)
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Input") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                color = if (isRecording) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            MicButton(
                isRecording = isRecording,
                onClick = {
                    if (checkAndRequestPermission()) {
                        viewModel.toggleRecording()
                    }
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Audio status card
            ResponseCard(
                icon = Icons.Rounded.GraphicEq,
                label = "Your Voice",
                text = when {
                    isRecording -> "Recording audio..."
                    responseText.isNotBlank() -> "Audio recorded successfully"
                    else -> ""
                },
                accentColor = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Response card
            ResponseCard(
                icon = Icons.Rounded.RecordVoiceOver,
                label = "Response",
                text = responseText,
                accentColor = MaterialTheme.colorScheme.secondary
            )

            // Action buttons
            if (responseText.isNotBlank()) {
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.speakResponse() },
                        modifier = Modifier.weight(1f),
                        enabled = !isSpeaking
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Text("Play")
                    }
                    OutlinedButton(
                        onClick = { viewModel.clearAll() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Clear, null)
                        Text("Clear")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
