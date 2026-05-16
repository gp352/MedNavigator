# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build debug APK
./gradlew.bat assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.mednavigator.app/.MainActivity

# Build + install + launch (one-liner)
./gradlew.bat assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.mednavigator.app/.MainActivity
```

adb is at `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` if not on PATH. No tests exist beyond placeholder `ExampleUnitTest.kt` — run with `./gradlew.bat test` if needed.

## Architecture

**MVVM with no DI framework.** All services and repositories are manually constructed and passed as constructor parameters or created inside ViewModels.

**Three layers:**
- **UI** — Jetpack Compose screens collect `StateFlow<T>` via `collectAsState()`. ViewModels in `ui/viewmodel/`.
- **Service** — `services/` contains AudioRecorderService (PCM 16kHz), SpeechRecognitionService (Android SpeechRecognizer), TextToSpeechService, GemmaInferenceService (on-device LLM), ModelDownloadManager.
- **Data** — Repositories in `data/` wrap SharedPreferences (OnboardingRepository) and Room databases (ChatRepository, SessionRepository, IcdRepository). Retrofit client in `data/api/` for NIH Clinical Tables ICD-10-CM search.

**Two Room databases:**
1. `med_navigator.db` (AppDatabase) — sessions, conversations, chat_messages
2. `icd11_mms.db` (IcdDatabase) — pre-loaded ICD-11 conditions (~30 defaults)

IcdRepository is a thread-safe singleton (`getInstance()`). All other repositories are plain classes.

## Navigation Flow

```
SplashScreen → checks onboarding + model state
  → OnboardingScreen (if not onboarded)
  → ModelDownloadScreen (if model not downloaded)
  → HomeScreen (if ready)
     → ChatScreen (primary AI interaction — text and voice)
     → VoiceInputScreen
     → SettingsScreen
```

Forward navigations clear the back stack (`popUpTo(SPLASH) { inclusive = true }`). Routes are string constants in `ui/navigation/Routes.kt`, NavHost in `ui/navigation/NavGraph.kt`.

## On-Device AI

Gemma 4 E4B model runs via LiteRT-LM Engine API (`services/GemmaInferenceService.kt`). Model file (`.litertlm`) is downloaded from HuggingFace at runtime via `ModelDownloadManager`. Supports both single-shot and streaming (Flow-based) inference. Audio input is converted from PCM to WAV (trimmed to 30s max) before being fed to the model.

The ChatViewModel builds medical context by extracting symptoms from user messages, searching the ICD database, and injecting relevant conditions into the LLM system prompt along with patient demographics from SharedPreferences.

## Key Technical Details

- Kotlin 2.2.0, Compose BOM 2025.04.01, AGP 8.7.3, KSP (not kapt)
- minSdk 26, targetSdk 34, Java 17 compatibility
- Compose Navigation 2.7.7 with string-based routes
- Room 2.7.1 with KSP processor
- Gson for JSON, no Moshi or kotlinx.serialization
- CameraX 1.3.3 declared but not yet used in UI
- iText7 7.2.5 for future PDF generation
- No CI/CD pipeline
