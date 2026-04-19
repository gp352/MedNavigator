# MedNavigator — Architecture

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.2.0 |
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation 2.7.7 |
| Database | Room 2.7.1 |
| Audio | Android AudioRecord (PCM 16kHz) |
| Speech | Android SpeechRecognizer |
| TTS | Android TextToSpeech |
| Build | AGP 8.7.3, Gradle 9.3.1, KSP |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 |

## Project Structure

```
app/src/main/java/com/mednavigator/app/
│
├── MainActivity.kt                    # Entry point — hosts NavGraph
│
├── data/
│   ├── OnboardingRepository.kt        # SharedPreferences for user profile
│   ├── SessionRepository.kt           # Room DB wrapper for session history
│   └── models/
│       ├── AppDatabase.kt             # Room DB (SessionEntity + SessionDao)
│       ├── HealthFacility.kt          # Health facility referral model
│       ├── IcdCondition.kt            # ICD-11 condition model
│       ├── NavigatorResult.kt         # Model inference output model
│       └── PatientContext.kt          # Patient demographics model
│
├── services/
│   ├── AudioRecorderService.kt        # Mic capture (PCM 16kHz, mono, 16-bit)
│   ├── SpeechRecognitionService.kt    # Android SpeechRecognizer wrapper
│   └── TextToSpeechService.kt         # Android TTS wrapper
│
├── ui/
│   ├── components/
│   │   ├── InputOptionCard.kt         # Scan/Click/Upload option card
│   │   ├── MicButton.kt              # Animated mic toggle button
│   │   ├── PulsingCircle.kt          # Pulse animation for active mic
│   │   └── ResponseCard.kt           # Labeled text card (speech/response)
│   ├── navigation/
│   │   ├── NavGraph.kt               # Compose NavHost definition
│   │   └── Routes.kt                 # Route string constants
│   ├── screens/
│   │   ├── HomeScreen.kt             # Main hub with input options
│   │   ├── OnboardingScreen.kt       # First-run user profile form
│   │   ├── SplashScreen.kt           # Route to onboarding or home
│   │   └── VoiceInputScreen.kt       # Voice recording + transcription
│   └── viewmodel/
│       ├── OnboardingViewModel.kt    # Form state + validation
│       └── VoiceViewModel.kt         # Recording/STT/TTS orchestration
│
└── utils/
    ├── BitmapUtils.kt                # Image resize + base64 (for Gemma vision)
    ├── Constants.kt                  # All app-wide constants
    ├── JsonUtils.kt                  # (Placeholder for E4B JSON parsing)
    ├── LanguageUtils.kt              # 14-language ISO 639-1 mapping
    └── ResponseGenerator.kt          # Static response logic
```

## Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                      UI LAYER                           │
│  Screens (Compose)  ←→  ViewModels (StateFlow)          │
│  Components          ←→  Navigation (NavController)      │
└─────────────┬──────────────────────┬────────────────────┘
              │                      │
┌─────────────▼──────────┐  ┌───────▼─────────────────────┐
│    SERVICE LAYER        │  │      DATA LAYER              │
│  AudioRecorderService   │  │  OnboardingRepository       │
│  SpeechRecognitionService│  │  (SharedPreferences)        │
│  TextToSpeechService    │  │                              │
│                         │  │  SessionRepository           │
│  ResponseGenerator      │  │  (Room Database)             │
└─────────────┬──────────┘  └──────────────────────────────┘
              │
┌─────────────▼──────────────────────────────────────────┐
│                  ANDROID PLATFORM                       │
│  AudioRecord  |  SpeechRecognizer  |  TextToSpeech      │
│  SharedPreferences  |  Room SQLite                      │
└────────────────────────────────────────────────────────┘
```

## Navigation Flow

```
[SplashScreen]
    │
    ├── onboarding_done? ──→ [HomeScreen]
    │                            │
    │                            ├── Voice card ──→ [VoiceInputScreen]
    │                            ├── Scan card  ──→ "Coming soon"
    │                            ├── Click card ──→ "Coming soon"
    │                            └── Upload card ──→ "Coming soon"
    │
    └── not done ──→ [OnboardingScreen]
                        │
                        └── save ──→ [HomeScreen]
```

All forward navigations clear the back stack (`popUpTo(SPLASH) { inclusive = true }`).
User cannot navigate back to Splash or Onboarding after completing them.

## State Management

ViewModels expose `StateFlow<T>` properties. Screens collect them via `collectAsState()`.

| ViewModel | State |
|-----------|-------|
| `OnboardingViewModel` | `name`, `age`, `selectedSex`, `selectedCountry`, `selectedLanguage`, `errorMessage` |
| `VoiceViewModel` | `isRecording`, `statusText`, `recognizedText`, `responseText`, `isSpeaking`, `errorMessage` |

No dependency injection framework. Repositories and services are constructed manually and passed where needed.

## Data Persistence

| Storage | Data | File |
|---------|------|------|
| SharedPreferences | User profile (name, age, sex, country, language, onboarding flag) | `med_navigator_prefs` |
| Room SQLite | Session history (symptom text, result JSON, urgency, timestamps) | `med_navigator.db` |

## Voice Pipeline (Current — Static)

```
User taps mic
    │
    ├── AudioRecorderService starts (PCM 16kHz → ByteArrayOutputStream)
    ├── SpeechRecognitionService starts (Android SpeechRecognizer)
    │       │
    │       ├── onPartialResult → update recognizedText in real-time
    │       └── onFinalResult / onError → trigger finish
    │
    User taps mic again (or STT auto-finishes)
    │
    ├── AudioRecorderService.stopRecording() → ByteArray (for future Gemma)
    ├── ResponseGenerator.generate(hasContent) → static response string
    ├── TextToSpeechService.speak(response) → voice output
    │
    └── UI shows: recognizedText + responseText
```

## Voice Pipeline (Future — Gemma E4B)

```
User taps mic
    │
    ├── AudioRecorderService starts (PCM 16kHz)
    │
    User stops recording
    │
    ├── AudioRecorderService.stopRecording() → ByteArray
    │
    ├── MediaPipe GenAI loads Gemma E4B model
    │       │
    │       ├── Feed audio bytes directly to model (140 languages)
    │       ├── Model outputs: condition suggestions, urgency, specialists
    │       │
    │       └── Tool calls: search_icd11_local, find_specialists_local, get_condition_info
    │
    ├── Parse NavigatorResult from model output
    ├── TextToSpeechService.speak(explanation)
    ├── SessionRepository.saveSession()
    │
    └── UI shows: full medical analysis + voice output
```

## Permissions

| Permission | Purpose | Required For |
|-----------|---------|-------------|
| `RECORD_AUDIO` | Microphone access | Voice input, SpeechRecognizer |
| `INTERNET` | (Not declared) | Not needed — fully offline |

## Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Compose BOM | 2025.04.01 | UI framework |
| Material 3 | (from BOM) | Design system |
| Compose Navigation | 2.7.7 | Screen navigation |
| Lifecycle ViewModel Compose | 2.8.0 | ViewModel in Compose |
| Room | 2.7.1 | Local SQLite database |
| CameraX | 1.3.3 | Photo capture (future) |
| MediaPipe GenAI | 0.10.14 | On-device Gemma inference (future) |
| Gson | 2.10.1 | JSON parsing |
| iText7 | 7.2.5 | PDF report generation (future) |
| Coroutines | 1.7.3 | Async operations |
| ML Kit Speech | 1.0.0-alpha1 | Alternative ASR (unused, kept for future) |

## Future Integration Points

| Component | Extension Point | Current State |
|-----------|----------------|---------------|
| ResponseGenerator | `generate()` — swap static → Gemma inference | Static placeholder |
| JsonUtils | Parse E4B tool-call JSON outputs | Empty file |
| IcdCondition + ICD DB | Local ICD-11 search for tool calls | Model defined, no DB bundled |
| HealthFacility + JSON | Specialist finder for tool calls | Model defined, no JSON bundled |
| CameraX | Photo capture → Gemma vision | Dependencies present, no UI |
| MediaPipe GenAI | Load `.litertlm` model, run inference | Dependencies present, no service |
