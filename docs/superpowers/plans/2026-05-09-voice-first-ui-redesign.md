# Voice-First UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign MedNavigator to be a voice-first medical assistant where the home screen is the primary voice interaction surface with speaking wave animation, text overlay, and full audio+transcript history.

**Architecture:** The HomeScreen becomes the single voice hub with 4 interaction states (Idle, Listening, Processing, Responding). ChatScreen is removed from navigation. A new ConversationDetailScreen provides history playback. Audio files are stored in internal storage with paths persisted in Room.

**Tech Stack:** Kotlin, Jetpack Compose, Room 2.7.1, Android TTS, Android SpeechRecognizer, LiteRT-LM Engine (Gemma)

---

## File Structure

**Create:**
- `app/src/main/java/com/mednavigator/app/ui/components/VoiceInteractionState.kt` — sealed class for 4 interaction states
- `app/src/main/java/com/mednavigator/app/ui/screens/ConversationDetailScreen.kt` — history detail with audio playback
- `app/src/main/java/com/mednavigator/app/ui/components/AudioPlaybackButton.kt` — reusable audio play/pause button

**Modify:**
- `app/src/main/java/com/mednavigator/app/data/models/AppDatabase.kt` — add audio columns, migration v2→v3
- `app/src/main/java/com/mednavigator/app/data/models/ChatModels.kt` — add audio path fields to ChatMessage
- `app/src/main/java/com/mednavigator/app/data/models/ChatExtensions.kt` — map new fields
- `app/src/main/java/com/mednavigator/app/data/ChatRepository.kt` — addMessage accepts audio paths
- `app/src/main/java/com/mednavigator/app/services/AudioRecorderService.kt` — save WAV to file
- `app/src/main/java/com/mednavigator/app/services/TextToSpeechService.kt` — synthesizeToFile + completion callback
- `app/src/main/java/com/mednavigator/app/ui/viewmodel/ChatViewModel.kt` — voice interaction state, audio saving pipeline
- `app/src/main/java/com/mednavigator/app/ui/viewmodel/HistoryViewModel.kt` — load conversation with audio paths
- `app/src/main/java/com/mednavigator/app/ui/screens/HomeScreen.kt` — 4 voice states, overlay card, SpeakingWave
- `app/src/main/java/com/mednavigator/app/ui/screens/HistoryScreen.kt` — navigate to detail, audio indicators
- `app/src/main/java/com/mednavigator/app/ui/screens/SplashScreen.kt` — reorder: model download first
- `app/src/main/java/com/mednavigator/app/ui/screens/OnboardingScreen.kt` — after save, always go HOME
- `app/src/main/java/com/mednavigator/app/ui/navigation/Routes.kt` — remove CHAT/VOICE_INPUT, add CONVERSATION_DETAIL
- `app/src/main/java/com/mednavigator/app/ui/navigation/NavGraph.kt` — updated routes
- `app/src/main/java/com/mednavigator/app/ui/components/SpeakingWave.kt` — parameterize bar count/size

---

### Task 1: VoiceInteractionState sealed class

**Files:**
- Create: `app/src/main/java/com/mednavigator/app/ui/components/VoiceInteractionState.kt`

- [ ] **Step 1: Create the sealed class**

```kotlin
package com.mednavigator.app.ui.components

sealed class VoiceInteractionState {
    data object Idle : VoiceInteractionState()
    data object Listening : VoiceInteractionState()
    data class Processing(val transcription: String = "") : VoiceInteractionState()
    data class Responding(
        val responseText: String = "",
        val isSpeaking: Boolean = false
    ) : VoiceInteractionState()
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mednavigator/app/ui/components/VoiceInteractionState.kt
git commit -m "feat: add VoiceInteractionState sealed class"
```

---

### Task 2: Database schema — add audio path columns

**Files:**
- Modify: `app/src/main/java/com/mednavigator/app/data/models/AppDatabase.kt`

- [ ] **Step 1: Add columns to ChatMessageEntity and migration**

In `AppDatabase.kt`, update `ChatMessageEntity` to add two new nullable columns:

```kotlin
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val conversationId: Int,
    val role: String,
    val content: String,
    val timestamp: Long,
    val messageType: String,
    val imageHash: String? = null,
    val reasoningJson: String? = null,
    val audioFilePath: String? = null,
    val responseAudioPath: String? = null
)
```

Add migration object before the `AppDatabase` class:

```kotlin
val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN audioFilePath TEXT")
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN responseAudioPath TEXT")
    }
}
```

Update `@Database` version from 2 to 3.

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mednavigator/app/data/models/AppDatabase.kt
git commit -m "feat: add audio file path columns to chat_messages"
```

---

### Task 3: Update ChatMessage model and extensions

**Files:**
- Modify: `app/src/main/java/com/mednavigator/app/data/models/ChatModels.kt`
- Modify: `app/src/main/java/com/mednavigator/app/data/models/ChatExtensions.kt`

- [ ] **Step 1: Add audio fields to ChatMessage**

In `ChatModels.kt`, update `ChatMessage`:

```kotlin
data class ChatMessage(
    val id: Int,
    val conversationId: Int,
    val role: String,
    val content: String,
    val timestamp: Long,
    val messageType: String = "TEXT",
    val imageHash: String? = null,
    val reasoningSteps: List<ReasoningStep> = emptyList(),
    val audioFilePath: String? = null,
    val responseAudioPath: String? = null
) {
    fun isFromUser(): Boolean = role == "user"
    fun isFromAssistant(): Boolean = role == "assistant"
}
```

- [ ] **Step 2: Update extension mappings**

In `ChatExtensions.kt`, update `ChatMessageEntity.toDomain()`:

```kotlin
fun ChatMessageEntity.toDomain(): ChatMessage {
    val reasoningSteps = if (reasoningJson?.isNotEmpty() == true) {
        try {
            Gson().fromJson(reasoningJson, Array<ReasoningStep>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    } else {
        emptyList()
    }

    return ChatMessage(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        timestamp = timestamp,
        messageType = messageType,
        imageHash = imageHash,
        reasoningSteps = reasoningSteps,
        audioFilePath = audioFilePath,
        responseAudioPath = responseAudioPath
    )
}
```

Update `ChatMessage.toEntity()`:

```kotlin
fun ChatMessage.toEntity(): ChatMessageEntity {
    val reasoningJson = if (reasoningSteps.isNotEmpty()) {
        Gson().toJson(reasoningSteps)
    } else {
        null
    }

    return ChatMessageEntity(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        timestamp = timestamp,
        messageType = messageType,
        imageHash = imageHash,
        reasoningJson = reasoningJson,
        audioFilePath = audioFilePath,
        responseAudioPath = responseAudioPath
    )
}
```

- [ ] **Step 3: Verify build**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/mednavigator/app/data/models/ChatModels.kt app/src/main/java/com/mednavigator/app/data/models/ChatExtensions.kt
git commit -m "feat: add audio path fields to ChatMessage model and extensions"
```

---

### Task 4: Update ChatRepository to support audio paths

**Files:**
- Modify: `app/src/main/java/com/mednavigator/app/data/ChatRepository.kt`

- [ ] **Step 1: Add migration to Room builder and update addMessage**

In `ChatRepository.kt`, add the migration import and register it in the Room builder:

```kotlin
import com.mednavigator.app.data.models.MIGRATION_2_3
```

Update the database builder:

```kotlin
private val database: AppDatabase = Room.databaseBuilder(
    context.applicationContext,
    AppDatabase::class.java,
    "med_navigator.db"
).addMigrations(MIGRATION_2_3).build()
```

Update `addMessage` signature to accept audio paths:

```kotlin
suspend fun addMessage(
    conversationId: Int,
    role: String,
    content: String,
    messageType: String = "TEXT",
    imageHash: String? = null,
    reasoningSteps: List<ReasoningStep> = emptyList(),
    audioFilePath: String? = null,
    responseAudioPath: String? = null
): Long {
    val reasoningJson = if (reasoningSteps.isNotEmpty()) {
        com.google.gson.Gson().toJson(reasoningSteps)
    } else {
        null
    }

    val message = ChatMessageEntity(
        conversationId = conversationId,
        role = role,
        content = content,
        timestamp = System.currentTimeMillis(),
        messageType = messageType,
        imageHash = imageHash,
        reasoningJson = reasoningJson,
        audioFilePath = audioFilePath,
        responseAudioPath = responseAudioPath
    )
    return messageDao.insertMessage(message)
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mednavigator/app/data/ChatRepository.kt
git commit -m "feat: register Room migration and add audio path params to ChatRepository"
```

---

### Task 5: AudioRecorderService — save WAV to file

**Files:**
- Modify: `app/src/main/java/com/mednavigator/app/services/AudioRecorderService.kt`

- [ ] **Step 1: Add saveToWavFile method**

Add a method that converts the PCM byte array from `stopRecording()` into a proper WAV file:

```kotlin
fun stopAndSaveToFile(outputFile: java.io.File): ByteArray? {
    val pcmBytes = stopRecording()
    if (pcmBytes == null || pcmBytes.isEmpty()) return null

    val dataLength = pcmBytes.size
    val totalLength = 36 + dataLength

    val header = java.io.ByteArrayOutputStream()
    // RIFF header
    header.write("RIFF".toByteArray())
    writeInt(header, totalLength)
    header.write("WAVE".toByteArray())
    // fmt chunk
    header.write("fmt ".toByteArray())
    writeInt(header, 16) // chunk size
    writeShort(header, 1.toShort()) // PCM
    writeShort(header, CHANNELS.toShort())
    writeInt(header, SAMPLE_RATE)
    writeInt(header, SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8) // byte rate
    writeShort(header, (CHANNELS * BITS_PER_SAMPLE / 8).toShort()) // block align
    writeShort(header, BITS_PER_SAMPLE.toShort())
    // data chunk
    header.write("data".toByteArray())
    writeInt(header, dataLength)

    outputFile.parentFile?.mkdirs()
    outputFile.writeBytes(header.toByteArray() + pcmBytes)
    return pcmBytes
}

private fun writeInt(out: java.io.ByteArrayOutputStream, value: Int) {
    out.write(value and 0xFF)
    out.write((value shr 8) and 0xFF)
    out.write((value shr 16) and 0xFF)
    out.write((value shr 24) and 0xFF)
}

private fun writeShort(out: java.io.ByteArrayOutputStream, value: Short) {
    out.write(value.toInt() and 0xFF)
    out.write((value.toInt() shr 8) and 0xFF)
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mednavigator/app/services/AudioRecorderService.kt
git commit -m "feat: add WAV file saving to AudioRecorderService"
```

---

### Task 6: TextToSpeechService — synthesizeToFile + completion callback

**Files:**
- Modify: `app/src/main/java/com/mednavigator/app/services/TextToSpeechService.kt`

- [ ] **Step 1: Add synthesizeToFile and UtteranceProgressListener**

Replace the entire file with:

```kotlin
package com.mednavigator.app.services

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.Locale

class TextToSpeechService(context: Context) {

    private var tts: TextToSpeech? = null
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private var onSpeakComplete: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _isReady.value = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        onSpeakComplete?.invoke()
                        onSpeakComplete = null
                    }
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        onSpeakComplete?.invoke()
                        onSpeakComplete = null
                    }
                })
            }
        }
    }

    fun speak(text: String, languageCode: String = "en", onComplete: (() -> Unit)? = null) {
        val engine = tts ?: return
        if (!_isReady.value) return

        val locale = Locale(languageCode)
        val available = engine.isLanguageAvailable(locale)
        val targetLocale = if (available >= TextToSpeech.LANG_AVAILABLE) locale else Locale.ENGLISH

        engine.language = targetLocale
        _isSpeaking.value = true
        onSpeakComplete = onComplete

        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mednav_response")
    }

    fun synthesizeToFile(text: String, outputFile: File, languageCode: String = "en") {
        val engine = tts ?: return
        if (!_isReady.value) return

        val locale = Locale(languageCode)
        val available = engine.isLanguageAvailable(locale)
        val targetLocale = if (available >= TextToSpeech.LANG_AVAILABLE) locale else Locale.ENGLISH

        engine.language = targetLocale
        outputFile.parentFile?.mkdirs()

        val params = Bundle()
        engine.synthesizeToFile(text, params, outputFile, "mednav_tts_file")
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
        _isSpeaking.value = false
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mednavigator/app/services/TextToSpeechService.kt
git commit -m "feat: add synthesizeToFile and utterance completion callback to TTS"
```

---

### Task 7: ChatViewModel — voice interaction state refactor

**Files:**
- Modify: `app/src/main/java/com/mednavigator/app/ui/viewmodel/ChatViewModel.kt`

This is the core refactor. The ViewModel gains voice interaction state management and audio file saving.

- [ ] **Step 1: Add new imports and state fields**

Add these imports at the top:

```kotlin
import com.mednavigator.app.ui.components.VoiceInteractionState
import java.io.File
```

Add these state fields after the existing `_isSpeaking` declaration (around line 79):

```kotlin
// Voice interaction state for HomeScreen
private val _voiceState = MutableStateFlow<VoiceInteractionState>(VoiceInteractionState.Idle)
val voiceState: StateFlow<VoiceInteractionState> = _voiceState

// User transcription text
private val _userTranscription = MutableStateFlow("")
val userTranscription: StateFlow<String> = _userTranscription

// Current AI response for overlay
private val _overlayResponse = MutableStateFlow("")
val overlayResponse: StateFlow<String> = _overlayResponse

// Exchange counter for current session
private val _exchangeCount = MutableStateFlow(0)
val exchangeCount: StateFlow<Int> = _exchangeCount
```

Add a helper method for getting the audio directory:

```kotlin
private fun getAudioDir(): File {
    val dir = File(getApplication<Application>().filesDir, "audio")
    dir.mkdirs()
    return dir
}
```

- [ ] **Step 2: Replace startVoiceRecording with state-aware version**

Replace the existing `startVoiceRecording()` method:

```kotlin
fun startVoiceRecording() {
    if (_isProcessing.value || _isRecording.value) return
    if (!ensureModelReady()) return

    _voiceState.value = VoiceInteractionState.Listening
    _isRecording.value = true
    _statusMessage.value = "Listening..."

    viewModelScope.launch {
        val result = audioRecorder.startRecording()
        if (result.isFailure) {
            Log.e(TAG, "Failed to start recording: ${result.exceptionOrNull()?.message}")
            withContext(Dispatchers.Main) {
                _isRecording.value = false
                _voiceState.value = VoiceInteractionState.Idle
                _statusMessage.value = "Recording failed"
            }
        }
    }
}
```

- [ ] **Step 3: Replace stopVoiceRecording with audio-saving pipeline**

Replace the existing `stopVoiceRecording()` method:

```kotlin
fun stopVoiceRecording() {
    if (!_isRecording.value) return
    _isRecording.value = false

    val ts = System.currentTimeMillis()
    val userAudioFile = File(getAudioDir(), "user_${ts}.wav")
    val audioBytes = audioRecorder.stopAndSaveToFile(userAudioFile)
    val hasAudio = audioBytes != null && audioBytes.isNotEmpty()

    if (!hasAudio) {
        _statusMessage.value = "No audio recorded"
        _voiceState.value = VoiceInteractionState.Idle
        return
    }

    val conversationId = _currentConversation.value?.id ?: run {
        _voiceState.value = VoiceInteractionState.Idle
        return
    }

    viewModelScope.launch {
        try {
            _isProcessing.value = true
            _statusMessage.value = "Processing audio..."
            _currentResponse.value = ""

            // Transition to processing state with a placeholder
            _voiceState.value = VoiceInteractionState.Processing("Transcribing your voice...")
            val prompt = buildPrompt()

            // Save user message with audio path
            withContext(Dispatchers.IO) {
                chatRepository.addMessage(
                    conversationId = conversationId,
                    role = "user",
                    content = "[Voice message]",
                    messageType = "AUDIO",
                    audioFilePath = userAudioFile.absolutePath
                )
            }

            // Generate response
            _statusMessage.value = "AI is processing..."
            val response = withContext(Dispatchers.IO) {
                gemmaService.generateResponse(prompt, audioBytes)
            }

            if (response.isSuccess) {
                val fullResponse = response.getOrDefault("")
                _overlayResponse.value = fullResponse

                // Save AI response with TTS audio
                val aiAudioFile = File(getAudioDir(), "ai_${ts}.wav")
                val language = onboardingRepository.getUserLanguage()
                ttsService.synthesizeToFile(fullResponse, aiAudioFile, language)

                val messageId = withContext(Dispatchers.IO) {
                    chatRepository.addMessage(
                        conversationId = conversationId,
                        role = "assistant",
                        content = fullResponse,
                        responseAudioPath = aiAudioFile.absolutePath
                    )
                }

                // Transition to responding state
                _voiceState.value = VoiceInteractionState.Responding(
                    responseText = fullResponse,
                    isSpeaking = true
                )

                _exchangeCount.value += 1
                _statusMessage.value = "Responding..."

                // Speak and transition back to Idle when done
                ttsService.speak(fullResponse, language) {
                    _voiceState.value = VoiceInteractionState.Idle
                    _isProcessing.value = false
                    _statusMessage.value = "Ready"
                }
            } else {
                val errorMsg = response.exceptionOrNull()?.message ?: "Unknown error"
                _statusMessage.value = "Error: $errorMsg"
                _voiceState.value = VoiceInteractionState.Idle
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing voice message", e)
            _statusMessage.value = "Error: ${e.message}"
            _voiceState.value = VoiceInteractionState.Idle
            _isProcessing.value = false
        }
    }
}
```

- [ ] **Step 4: Update speakResponse to use the new callback-based TTS**

Replace the existing `speakResponse` method:

```kotlin
private fun speakResponse(text: String) {
    if (text.isBlank()) return
    val language = onboardingRepository.getUserLanguage()
    ttsService.speak(text, language) {
        _isSpeaking.value = false
    }
    _isSpeaking.value = true
}
```

- [ ] **Step 5: Add loadConversationForDetail method**

Add this method for the history detail screen:

```kotlin
fun loadConversationForDetail(conversationId: Int) {
    viewModelScope.launch(Dispatchers.IO) {
        val conversation = chatRepository.getConversationWithMessages(conversationId)
        withContext(Dispatchers.Main) {
            _currentConversation.value = conversation
            _messages.value = conversation?.messages ?: emptyList()
        }
    }
}
```

- [ ] **Step 6: Add startNewVoiceSession method**

Add this for when the user wants a fresh session:

```kotlin
fun startNewVoiceSession() {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            endConversationSilent()
            val title = "Voice Session - ${System.currentTimeMillis()}"
            val conversationId = chatRepository.createConversation(title)
            val conversation = chatRepository.getConversationWithMessages(conversationId.toInt())
            withContext(Dispatchers.Main) {
                _currentConversation.value = conversation
                _messages.value = emptyList()
                _exchangeCount.value = 0
                _voiceState.value = VoiceInteractionState.Idle
                _statusMessage.value = "Ready"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start new voice session", e)
        }
    }
}

private suspend fun endConversationSilent() {
    val id = _currentConversation.value?.id ?: return
    try {
        chatRepository.endConversation(id)
    } catch (_: Exception) {}
}
```

- [ ] **Step 7: Verify build**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/mednavigator/app/ui/viewmodel/ChatViewModel.kt
git commit -m "feat: add voice interaction state management and audio saving pipeline"
```

---

### Task 8: Routes & NavGraph update

**Files:**
- Modify: `app/src/main/java/com/mednavigator/app/ui/navigation/Routes.kt`
- Modify: `app/src/main/java/com/mednavigator/app/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Update Routes**

Replace `Routes.kt`:

```kotlin
package com.mednavigator.app.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val MODEL_DOWNLOAD = "model_download"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val CONVERSATION_DETAIL = "conversation_detail/{conversationId}"

    fun conversationDetail(conversationId: Int): String = "conversation_detail/$conversationId"
}
```

- [ ] **Step 2: Update NavGraph**

Replace `NavGraph.kt`:

```kotlin
package com.mednavigator.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mednavigator.app.data.OnboardingRepository
import com.mednavigator.app.ui.screens.ConversationDetailScreen
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
            ModelDownloadScreen(navController)
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
            ConversationDetailScreen(navController, conversationId)
        }
    }
}
```

- [ ] **Step 3: Verify build**

Run: `./gradlew.bat assembleDebug`
Expected: May fail because ChatScreen, VoiceInputScreen imports are removed — that's expected, will be fixed in Task 9.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/mednavigator/app/ui/navigation/Routes.kt app/src/main/java/com/mednavigator/app/ui/navigation/NavGraph.kt
git commit -m "feat: update routes for voice-first navigation"
```

---

### Task 9: SplashScreen — reorder flow (model download first)

**Files:**
- Modify: `app/src/main/java/com/mednavigator/app/ui/screens/SplashScreen.kt`

- [ ] **Step 1: Reorder the destination logic**

In `SplashScreen.kt`, change the `LaunchedEffect` block to check model first:

```kotlin
val destination = if (!modelReady) {
    Routes.MODEL_DOWNLOAD
} else if (!isOnboarded) {
    Routes.ONBOARDING
} else {
    Routes.HOME
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mednavigator/app/ui/screens/SplashScreen.kt
git commit -m "feat: reorder splash to check model download before onboarding"
```

---

### Task 10: OnboardingScreen — always navigate to HOME after save

**Files:**
- Modify: `app/src/main/java/com/mednavigator/app/ui/screens/OnboardingScreen.kt`

- [ ] **Step 1: Simplify navigation after onboarding**

In `OnboardingScreen.kt`, find the `Button(onClick = { ... })` block (around line 172) and change the navigation to always go HOME since model is now downloaded first:

```kotlin
Button(onClick = {
    if (viewModel.validateAndSave(onboardingRepository)) {
        navController.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
    }
}, modifier = Modifier.fillMaxWidth().height(56.dp), shape = CircleShape,
    colors = ButtonDefaults.buttonColors(containerColor = CozyPrimary)) {
```

Also remove the now-unused imports:
- `import com.mednavigator.app.services.ModelDownloadManager` — remove this line

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mednavigator/app/ui/screens/OnboardingScreen.kt
git commit -m "feat: simplify onboarding to always navigate to HOME"
```

---

### Task 11: SpeakingWave — parameterize for integration

**Files:**
- Modify: `app/src/main/java/com/mednavigator/app/ui/components/SpeakingWave.kt`

- [ ] **Step 1: Make bar count, height, and width configurable**

Replace the entire file:

```kotlin
package com.mednavigator.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SpeakingWave(
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    barCount: Int = 5,
    barWidth: Dp = 10.dp,
    barHeight: Dp = 40.dp
) {
    val transition = rememberInfiniteTransition(label = "speaking")
    val scales = (0 until barCount).map { index ->
        transition.animateFloat(
            initialValue = 0.3f + (index % 3) * 0.1f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600 + index * 50, delayMillis = index * 100),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale$index"
        )
    }

    Row(modifier = modifier) {
        scales.forEach { scale ->
            WaveBar(
                scale = scale.value,
                color = barColor,
                width = barWidth,
                height = barHeight
            )
        }
    }
}

@Composable
private fun WaveBar(scale: Float, color: Color, width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .width(width)
            .height(height)
            .graphicsLayer { scaleY = scale }
            .clip(RoundedCornerShape(6.dp))
            .background(color)
    )
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mednavigator/app/ui/components/SpeakingWave.kt
git commit -m "feat: parameterize SpeakingWave bar count, width, and height"
```

---

### Task 12: AudioPlaybackButton component

**Files:**
- Create: `app/src/main/java/com/mednavigator/app/ui/components/AudioPlaybackButton.kt`

- [ ] **Step 1: Create the reusable audio playback button**

```kotlin
package com.mednavigator.app.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun AudioPlaybackButton(
    audioFilePath: String?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimary
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    val file = audioFilePath?.let { File(it) }
    val fileExists = file != null && file.exists()

    if (!fileExists) return

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable {
                if (isPlaying) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    isPlaying = false
                } else {
                    try {
                        val mp = MediaPlayer().apply {
                            setDataSource(file!!.absolutePath)
                            prepare()
                            setOnCompletionListener {
                                isPlaying = false
                                release()
                            }
                            start()
                        }
                        mediaPlayer = mp
                        isPlaying = true
                    } catch (_: Exception) {
                        isPlaying = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Stop" else "Play",
            tint = tint,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mednavigator/app/ui/components/AudioPlaybackButton.kt
git commit -m "feat: add AudioPlaybackButton component for audio replay"
```

---

### Task 13: HomeScreen — voice interaction UI redesign

**Files:**
- Modify: `app/src/main/java/com/mednavigator/app/ui/screens/HomeScreen.kt`

This is the largest change. The HomeScreen gets 4 voice states, a text overlay card, and SpeakingWave integration.

- [ ] **Step 1: Replace HomeScreen with voice interaction states**

Replace the entire file with the following. Key changes:
- Import VoiceInteractionState and collect voiceState/overlayResponse
- Orb appearance changes per state (bright green when listening, shimmer when processing, wave when responding)
- Glass cards and suggestions fade out during Listening/Processing/Responding
- Text overlay card appears during Responding state
- SpeakingWave animates below orb during Responding state

```kotlin
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
import androidx.compose.foundation.height
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
    val modelReady by viewModel.modelReady.collectAsState()

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

    // Ambient animations
    val inf = rememberInfiniteTransition(label = "bg")
    val a1 by inf.animateFloat(0.10f, 0.20f,
        infiniteRepeatable(tween(6000), RepeatMode.Reverse), label = "a1")
    val a2 by inf.animateFloat(0.06f, 0.14f,
        infiniteRepeatable(tween(8500, delayMillis = 1500), RepeatMode.Reverse), label = "a2")
    val orbScale by inf.animateFloat(0.95f, 1.05f,
        infiniteRepeatable(tween(2500), RepeatMode.Reverse), label = "orb")
    val glowA by inf.animateFloat(0.18f, 0.36f,
        infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "glow")

    // Listening pulse (faster, larger)
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

        // Background blobs
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

                    // Orb container
                    Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                        // Outer glow
                        Box(modifier = Modifier.fillMaxSize().blur(50.dp)
                            .graphicsLayer {
                                scaleX = when (voiceState) {
                                    is VoiceInteractionState.Listening -> listenPulse * 0.95f
                                    else -> orbScale * 0.95f
                                }
                                scaleY = when (voiceState) {
                                    is VoiceInteractionState.Listening -> listenPulse * 0.95f
                                    else -> orbScale * 0.95f
                                }
                            }
                            .background(
                                when (voiceState) {
                                    is VoiceInteractionState.Listening -> CozyPrimary.copy(alpha = 0.5f)
                                    else -> CozyPrimary.copy(alpha = glowA)
                                },
                                CircleShape
                            )
                        )
                        // Inner glow
                        Box(modifier = Modifier.size(160.dp).blur(30.dp)
                            .graphicsLayer {
                                scaleX = when (voiceState) {
                                    is VoiceInteractionState.Listening -> listenPulse
                                    else -> orbScale
                                }
                                scaleY = when (voiceState) {
                                    is VoiceInteractionState.Listening -> listenPulse
                                    else -> orbScale
                                }
                            }
                            .background(CozyPrimaryContainer.copy(alpha = 0.28f), CircleShape))
                        // Main orb
                        Box(
                            modifier = Modifier.size(160.dp)
                                .graphicsLayer {
                                    scaleX = when (voiceState) {
                                        is VoiceInteractionState.Listening -> listenPulse
                                        else -> orbScale
                                    }
                                    scaleY = when (voiceState) {
                                        is VoiceInteractionState.Listening -> listenPulse
                                        else -> orbScale
                                    }
                                }
                                .clip(CircleShape)
                                .background(
                                    when (voiceState) {
                                        is VoiceInteractionState.Listening -> Brush.radialGradient(
                                            listOf(CozyPrimaryFixed, CozyPrimary)
                                        )
                                        is VoiceInteractionState.Processing -> Brush.radialGradient(
                                            listOf(CozyPrimaryContainer, CozyPrimary)
                                        )
                                        is VoiceInteractionState.Responding -> Brush.radialGradient(
                                            listOf(CozyPrimaryFixed, CozyPrimary)
                                        )
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
                                    Icon(
                                        Icons.Rounded.Mic, "Listen",
                                        modifier = Modifier.size(64.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Status text
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
                            is VoiceInteractionState.Processing -> {
                                val proc = voiceState as VoiceInteractionState.Processing
                                proc.transcription
                            }
                            is VoiceInteractionState.Responding -> "AI response playing..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 36.dp)
                    )

                    // Exchange counter (visible after first exchange)
                    if (exchangeCount > 0 && isIdle) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "$exchangeCount exchange${if (exchangeCount != 1) "s" else ""} this session",
                            style = MaterialTheme.typography.labelMedium,
                            color = CozyPrimary
                        )
                    }

                    // AI Response overlay card
                    AnimatedVisibility(
                        visible = voiceState is VoiceInteractionState.Responding && overlayResponse.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            ),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = "AI Response",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CozyPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = overlayResponse,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Glass action cards — only visible when idle
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
                                    "\u201cWhat\u2019s my next dose?\u201d",
                                    "\u201cSummarize this lab result.\u201d",
                                    "\u201cFind a nearby clinic.\u201d"
                                )) { s -> SuggestionChip(s) }
                            }
                        }
                    }
                }
            }

            // Bottom nav
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

// Shared components remain the same

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
```

- [ ] **Step 2: Add stopSpeaking method to ChatViewModel**

This method is referenced by HomeScreen but not yet in ChatViewModel. Add it:

```kotlin
fun stopSpeaking() {
    ttsService.stop()
    _voiceState.value = VoiceInteractionState.Idle
    _isProcessing.value = false
    _statusMessage.value = "Ready"
}
```

- [ ] **Step 3: Verify build**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/mednavigator/app/ui/screens/HomeScreen.kt app/src/main/java/com/mednavigator/app/ui/viewmodel/ChatViewModel.kt
git commit -m "feat: redesign HomeScreen with 4 voice interaction states"
```

---

### Task 14: ConversationDetailScreen — history detail with audio playback

**Files:**
- Create: `app/src/main/java/com/mednavigator/app/ui/screens/ConversationDetailScreen.kt`

- [ ] **Step 1: Create the conversation detail screen**

```kotlin
package com.mednavigator.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mednavigator.app.data.models.ChatMessage
import com.mednavigator.app.ui.components.AudioPlaybackButton
import com.mednavigator.app.ui.theme.CozyPrimary
import com.mednavigator.app.ui.theme.CozyPrimaryFixed
import com.mednavigator.app.ui.viewmodel.ChatViewModel
import com.mednavigator.app.ui.viewmodel.HistoryViewModel

@Composable
fun ConversationDetailScreen(
    navController: NavController,
    conversationId: Int,
    chatViewModel: ChatViewModel = viewModel(),
    historyViewModel: HistoryViewModel = viewModel()
) {
    val messages by chatViewModel.messages.collectAsState()
    val conversation by chatViewModel.currentConversation.collectAsState()

    LaunchedEffect(conversationId) {
        chatViewModel.loadConversationForDetail(conversationId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = CozyPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation?.title ?: "Conversation",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                conversation?.startTime?.let { ts ->
                    Text(
                        text = HistoryViewModel.formatTimestamp(ts),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CozyPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    MessageWithAudioCard(message)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun MessageWithAudioCard(message: ChatMessage) {
    val isUser = message.isFromUser()
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val cardColor = if (isUser) CozyPrimaryFixed else MaterialTheme.colorScheme.surfaceContainerLow
    val audioPath = if (isUser) message.audioFilePath else message.responseAudioPath

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Role label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isUser) CozyPrimary else MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUser) Icons.Rounded.Mic else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = if (isUser) androidx.compose.ui.graphics.Color.White else CozyPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUser) "You" else "CareVoice",
                        style = MaterialTheme.typography.labelLarge,
                        color = CozyPrimary
                    )
                }

                // Message content
                if (message.content.isNotBlank() && message.content != "[Voice message]") {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Audio playback button
                if (audioPath != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        AudioPlaybackButton(
                            audioFilePath = audioPath,
                            size = 32.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Play audio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mednavigator/app/ui/screens/ConversationDetailScreen.kt
git commit -m "feat: add ConversationDetailScreen with audio playback"
```

---

### Task 15: HistoryScreen — navigate to detail and audio indicators

**Files:**
- Modify: `app/src/main/java/com/mednavigator/app/ui/screens/HistoryScreen.kt`

- [ ] **Step 1: Update session card click to navigate to detail**

In `HistoryScreen.kt`, find the `items(filtered)` block and change the `onClick`:

```kotlin
onClick = {
    navController.navigate(Routes.conversationDetail(conv.id))
},
```

Remove the import for `Routes.CHAT` if it exists. Ensure the `Routes` import is present.

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mednavigator/app/ui/screens/HistoryScreen.kt
git commit -m "feat: update history to navigate to conversation detail"
```

---

### Task 16: Final build verification and cleanup

- [ ] **Step 1: Full build**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Remove any unused imports in modified files**

Check for and remove any imports referencing `ChatScreen`, `VoiceInputScreen`, or the old `Routes.CHAT` / `Routes.VOICE_INPUT` across all files.

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "feat: voice-first UI redesign complete — home screen voice hub, audio history, speaking wave"
```
