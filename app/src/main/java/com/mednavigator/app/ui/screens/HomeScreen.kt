package com.mednavigator.app.ui.screens

import android.Manifest
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.ui.components.InputOptionCard
import com.mednavigator.app.ui.components.MicButton
import com.mednavigator.app.ui.components.ResponseCard
import com.mednavigator.app.ui.navigation.Routes
import com.mednavigator.app.ui.viewmodel.VoiceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onboardingRepository: OnboardingRepository,
    voiceViewModel: VoiceViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val userName = remember { onboardingRepository.getUserName() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val isRecording by voiceViewModel.isRecording.collectAsState()
    val statusText by voiceViewModel.statusText.collectAsState()
    val responseText by voiceViewModel.responseText.collectAsState()
    val isSpeaking by voiceViewModel.isSpeaking.collectAsState()

    var showVoiceSheet by remember { mutableStateOf(false) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var showMicPermissionDialog by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            showVoiceSheet = true
        } else {
            val activity = context as? androidx.activity.ComponentActivity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.RECORD_AUDIO
                )
            ) {
                showMicPermissionDialog = true
            }
        }
    }

    fun requestMicPermission() {
        if (hasMicPermission) {
            showVoiceSheet = true
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            scope.launch { snackbarHostState.showSnackbar("Scan captured") }
        } else {
            scope.launch { snackbarHostState.showSnackbar("Scan cancelled") }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            scanLauncher.launch(null)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Camera permission required") }
        }
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = getDisplayName(context, uri)
            scope.launch { snackbarHostState.showSnackbar("Uploaded: $name") }
        }
    }

    fun requestCameraPermissionAndScan() {
        if (hasCameraPermission) {
            scanLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(showVoiceSheet) {
        if (showVoiceSheet && !isRecording) {
            voiceViewModel.toggleRecording()
        }
        if (!showVoiceSheet && isRecording) {
            voiceViewModel.toggleRecording()
        }
    }

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
                    .clickable { requestMicPermission() },
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
                    onClick = { requestCameraPermissionAndScan() }
                )
                InputOptionCard(
                    icon = Icons.Rounded.CameraAlt,
                    label = "Click",
                    onClick = { scope.launch { snackbarHostState.showSnackbar("Coming soon") } }
                )
                InputOptionCard(
                    icon = Icons.Rounded.UploadFile,
                    label = "Upload",
                    onClick = { uploadLauncher.launch(arrayOf("application/pdf", "image/*")) }
                )
            }
        }
    }

    if (showMicPermissionDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showMicPermissionDialog = false },
            title = { Text("Microphone Permission Required") },
            text = { Text("Please enable microphone access to start voice input.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showMicPermissionDialog = false
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.fromParts("package", context.packageName, null)
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showMicPermissionDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showVoiceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVoiceSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isRecording) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                MicButton(
                    isRecording = isRecording,
                    onClick = { voiceViewModel.toggleRecording() }
                )

                Spacer(modifier = Modifier.height(24.dp))

                ResponseCard(
                    icon = Icons.Rounded.DocumentScanner,
                    label = "Your Voice",
                    text = when {
                        isRecording -> "Listening..."
                        responseText.isNotBlank() -> "Audio recorded successfully"
                        else -> ""
                    },
                    accentColor = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                ResponseCard(
                    icon = Icons.Rounded.RecordVoiceOver,
                    label = "Response",
                    text = responseText,
                    accentColor = MaterialTheme.colorScheme.secondary
                )

                if (responseText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { voiceViewModel.speakResponse() },
                            modifier = Modifier.weight(1f),
                            enabled = !isSpeaking
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null)
                            Text("Play")
                        }
                        androidx.compose.material3.OutlinedButton(
                            onClick = { voiceViewModel.clearAll() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Clear, null)
                            Text("Clear")
                        }
                    }
                }
            }
        }
    }
}

private fun getDisplayName(context: android.content.Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && it.moveToFirst()) {
            return it.getString(nameIndex)
        }
    }
    return uri.lastPathSegment ?: "file"
}
