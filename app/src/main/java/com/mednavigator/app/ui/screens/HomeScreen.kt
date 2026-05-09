package com.mednavigator.app.ui.screens

import android.Manifest
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.ui.components.SpeakingWave
import com.mednavigator.app.ui.components.VoiceInteractionState
import com.mednavigator.app.ui.navigation.Routes
import com.mednavigator.app.ui.theme.CozyOnPrimaryFixedVariant
import com.mednavigator.app.ui.theme.CozyPrimary
import com.mednavigator.app.ui.theme.CozyPrimaryContainer
import com.mednavigator.app.ui.theme.CozyPrimaryFixed
import com.mednavigator.app.ui.theme.CozyPrimaryFixedDim
import com.mednavigator.app.ui.theme.CozySecondaryContainer
import com.mednavigator.app.ui.theme.CozyTertiaryFixed
import com.mednavigator.app.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavController,
    onboardingRepository: OnboardingRepository,
    viewModel: ChatViewModel = viewModel()
) {
    val userName = remember { onboardingRepository.getUserName() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val contextActivity = context as? androidx.activity.ComponentActivity

    val voiceState by viewModel.voiceState.collectAsState()
    val overlayResponse by viewModel.overlayResponse.collectAsState()
    val exchangeCount by viewModel.exchangeCount.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var showMicPermissionDialog by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) viewModel.startVoiceRecording()
        else if (contextActivity != null && !ActivityCompat.shouldShowRequestPermissionRationale(
                contextActivity, Manifest.permission.RECORD_AUDIO)) {
            showMicPermissionDialog = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) scope.launch { snackbarHostState.showSnackbar("Camera permission required") }
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> if (uri != null) scope.launch { snackbarHostState.showSnackbar("File selected") } }

    fun handleMicTap() {
        if (voiceState is VoiceInteractionState.Responding) {
            viewModel.stopSpeaking()
            return
        }
        if (hasMicPermission) viewModel.startVoiceRecording()
        else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun handleMicRelease() {
        if (voiceState is VoiceInteractionState.Listening) {
            viewModel.stopVoiceRecording()
        }
    }

    fun scanOrChat() {
        if (hasCameraPermission) scope.launch { snackbarHostState.showSnackbar("Scan coming soon") }
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val isIdle = voiceState is VoiceInteractionState.Idle

    val inf = rememberInfiniteTransition(label = "bg")
    val a1 by inf.animateFloat(0.10f, 0.20f,
        infiniteRepeatable(tween(6000), RepeatMode.Reverse), label = "a1")
    val a2 by inf.animateFloat(0.06f, 0.14f,
        infiniteRepeatable(tween(8500, delayMillis = 1500), RepeatMode.Reverse), label = "a2")
    val orbScale by inf.animateFloat(0.95f, 1.05f,
        infiniteRepeatable(tween(2500), RepeatMode.Reverse), label = "orb")
    val glowA by inf.animateFloat(0.18f, 0.36f,
        infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "glow")
    val listenPulse by inf.animateFloat(1.0f, 1.12f,
        infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "listenPulse")

    if (showMicPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showMicPermissionDialog = false },
            title = { Text("Microphone Permission Required") },
            text = { Text("Please enable microphone access to start voice input.") },
            confirmButton = {
                TextButton(onClick = {
                    showMicPermissionDialog = false
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.fromParts("package", context.packageName, null)
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = { TextButton(onClick = { showMicPermissionDialog = false }) { Text("Cancel") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        Box(modifier = Modifier.size(500.dp).align(Alignment.TopStart)
            .offset(x = (-80).dp, y = 60.dp).blur(100.dp)
            .background(CozyPrimary.copy(alpha = a1), CircleShape))
        Box(modifier = Modifier.size(320.dp).align(Alignment.Center)
            .offset(x = 80.dp, y = (-60).dp).blur(80.dp)
            .background(CozyPrimaryFixedDim.copy(alpha = a2), CircleShape))
        Box(modifier = Modifier.size(280.dp).align(Alignment.BottomEnd)
            .offset(x = 40.dp, y = (-80).dp).blur(70.dp)
            .background(CozySecondaryContainer.copy(alpha = 0.10f), CircleShape))

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(CozyPrimaryFixed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Person, null, tint = CozyOnPrimaryFixedVariant, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("CareVoice", style = MaterialTheme.typography.titleLarge, color = CozyPrimary)
                        Text("Hello, $userName!", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Rounded.Settings, "Settings", tint = CozyPrimary)
                    }
                }
            }

            // Central voice orb
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                ) {
                    Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.fillMaxSize().blur(50.dp)
                            .graphicsLayer {
                                val s = if (voiceState is VoiceInteractionState.Listening) listenPulse * 0.95f else orbScale * 0.95f
                                scaleX = s; scaleY = s
                            }
                            .background(
                                if (voiceState is VoiceInteractionState.Listening) CozyPrimary.copy(alpha = 0.5f)
                                else CozyPrimary.copy(alpha = glowA),
                                CircleShape
                            )
                        )
                        Box(modifier = Modifier.size(160.dp).blur(30.dp)
                            .graphicsLayer {
                                val s = if (voiceState is VoiceInteractionState.Listening) listenPulse else orbScale
                                scaleX = s; scaleY = s
                            }
                            .background(CozyPrimaryContainer.copy(alpha = 0.28f), CircleShape))
                        Box(
                            modifier = Modifier.size(160.dp)
                                .graphicsLayer {
                                    val s = if (voiceState is VoiceInteractionState.Listening) listenPulse else orbScale
                                    scaleX = s; scaleY = s
                                }
                                .clip(CircleShape)
                                .background(
                                    when (voiceState) {
                                        is VoiceInteractionState.Listening -> Brush.radialGradient(listOf(CozyPrimaryFixed, CozyPrimary))
                                        is VoiceInteractionState.Processing -> Brush.radialGradient(listOf(CozyPrimaryContainer, CozyPrimary))
                                        is VoiceInteractionState.Responding -> Brush.radialGradient(listOf(CozyPrimaryFixed, CozyPrimary))
                                        else -> Brush.radialGradient(listOf(CozyPrimaryContainer, CozyPrimary))
                                    }
                                )
                                .clickable {
                                    when (voiceState) {
                                        is VoiceInteractionState.Idle -> handleMicTap()
                                        is VoiceInteractionState.Listening -> handleMicRelease()
                                        else -> {}
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (voiceState) {
                                is VoiceInteractionState.Processing -> {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(48.dp),
                                        strokeWidth = 4.dp
                                    )
                                }
                                is VoiceInteractionState.Responding -> {
                                    SpeakingWave(
                                        barColor = Color.White,
                                        barCount = 5,
                                        barWidth = 8.dp,
                                        barHeight = 36.dp
                                    )
                                }
                                else -> {
                                    Icon(Icons.Rounded.Mic, "Listen", modifier = Modifier.size(64.dp), tint = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = when (voiceState) {
                            is VoiceInteractionState.Idle -> "Tap to begin"
                            is VoiceInteractionState.Listening -> "Listening..."
                            is VoiceInteractionState.Processing -> "Thinking..."
                            is VoiceInteractionState.Responding -> "Speaking..."
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (voiceState) {
                            is VoiceInteractionState.Idle -> "Tell me about your symptoms or ask about your prescription."
                            is VoiceInteractionState.Listening -> "Speak now. Tap the orb when done."
                            is VoiceInteractionState.Processing -> (voiceState as VoiceInteractionState.Processing).transcription
                            is VoiceInteractionState.Responding -> "AI response playing..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 36.dp)
                    )

                    if (exchangeCount > 0 && isIdle) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "$exchangeCount exchange${if (exchangeCount != 1) "s" else ""} this session",
                            style = MaterialTheme.typography.labelMedium,
                            color = CozyPrimary
                        )
                    }

                    AnimatedVisibility(
                        visible = voiceState is VoiceInteractionState.Responding && overlayResponse.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("AI Response", style = MaterialTheme.typography.labelLarge, color = CozyPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(overlayResponse, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    AnimatedVisibility(visible = isIdle, enter = fadeIn(), exit = fadeOut()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                GlassCard(Icons.Rounded.DocumentScanner, "Scan Paperwork", CozySecondaryContainer,
                                    Modifier.weight(1f)) { scanOrChat() }
                                GlassCard(Icons.Rounded.UploadFile, "Upload File", CozyTertiaryFixed,
                                    Modifier.weight(1f)) {
                                    uploadLauncher.launch(arrayOf("application/pdf", "image/*"))
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text("Try saying", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 8.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(listOf(
                                    "“What’s my next dose?”",
                                    "“Summarize this lab result.”",
                                    "“Find a nearby clinic.”"
                                )) { s -> SuggestionChip(s) }
                            }
                        }
                    }
                }
            }

            CozyBottomNav(
                currentRoute = Routes.HOME,
                onTalkClick = { handleMicTap() },
                onHistoryClick = { navController.navigate(Routes.HISTORY) },
                onScanClick = { scanOrChat() }
            )
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun CozyBottomNav(
    currentRoute: String,
    onTalkClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onScanClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(Icons.Rounded.History, "History", currentRoute == Routes.HISTORY, onHistoryClick)
        BottomNavItem(Icons.Rounded.Mic, "Listen", currentRoute == Routes.HOME, onTalkClick, highlighted = true)
        BottomNavItem(Icons.Rounded.DocumentScanner, "Scan", false, onScanClick)
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector, label: String, isActive: Boolean,
    onClick: () -> Unit, highlighted: Boolean = false
) {
    val bg = if (isActive || highlighted) CozyPrimaryFixed else Color.Transparent
    Column(
        modifier = Modifier.clip(CircleShape).background(bg).clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label,
            tint = if (isActive || highlighted) CozyPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = if (isActive || highlighted) CozyPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun GlassCard(icon: ImageVector, label: String, iconBg: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(116.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .border(1.dp, CozyPrimary.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SuggestionChip(text: String) {
    Box(
        modifier = Modifier.clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun getDisplayName(context: android.content.Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && it.moveToFirst()) return it.getString(nameIndex)
    }
    return uri.lastPathSegment ?: "file"
}
