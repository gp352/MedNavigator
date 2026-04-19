# MedNavigator — User Flow

## First Launch

```
App Opens
    │
    ▼
[SplashScreen]
    │  • Shows MedNavigator logo + loading spinner
    │  • Checks SharedPreferences: onboarding_done?
    │
    ├── false ──────────────────────────┐
    │                                    ▼
    │                          [OnboardingScreen]
    │                              │  • Full Name
    │                              │  • Age (1-120)
    │                              │  • Biological Sex (Male/Female/Other)
    │                              │  • Country (15 options)
    │                              │  • Preferred Language (14 languages)
    │                              │
    │                              ├── Invalid? → show error
    │                              └── Valid? → save to SharedPreferences
    │                                            │
    true ◄────────────────────────────────────────┘
    │
    ▼
[HomeScreen]
```

## Home Screen

```
[HomeScreen]
    │
    ├── "Hello, {name}!"
    │
    ├── [VOICE CARD — large, gradient, primary action]
    │       │
    │       └── Tap ──→ [VoiceInputScreen]
    │
    ├── [SCAN card] ──→ "Coming soon" snackbar
    ├── [CLICK card] ──→ "Coming soon" snackbar
    └── [UPLOAD card] ──→ "Coming soon" snackbar
```

## Voice Input Flow

```
[VoiceInputScreen]
    │
    ├── Status: "Tap the microphone to start speaking"
    │
    └── [MIC BUTTON — tap]
            │
            ├── Permission not granted?
            │       └── Request RECORD_AUDIO
            │           ├── Denied permanently → dialog: "Open Settings"
            │           └── Granted → continue
            │
            ├── ▼ START RECORDING
            │       │
            │       ├── AudioRecorderService starts (PCM 16kHz capture)
            │       ├── SpeechRecognizer starts (language from user profile)
            │       │       ├── onPartialResult → "Your Speech" card updates live
            │       │       ├── onFinalResult → auto-stop
            │       │       └── onError → graceful fallback
            │       │
            │       ├── Status: "Listening..."
            │       └── Mic button shows pulsing animation
            │
            ├── [MIC BUTTON — tap again] or STT auto-finishes
            │
            └── ▼ STOP RECORDING
                    │
                    ├── AudioRecorder captures PCM bytes
                    ├── ResponseGenerator creates static response
                    ├── TextToSpeech speaks the response aloud
                    │
                    ├── "Your Speech" card: transcribed text
                    ├── "Response" card: static message
                    │
                    └── [PLAY] ──→ TTS speaks response again
                       [CLEAR] ──→ reset all state
```

## Subsequent Launches

```
App Opens → SplashScreen
    │
    └── onboarding_done = true → [HomeScreen] (skips onboarding)
```

## Language Support

14 languages supported in onboarding:

| Code | Language | Code | Language |
|------|----------|------|----------|
| en | English | fr | French |
| hi | Hindi | pt | Portuguese |
| gu | Gujarati | es | Spanish |
| sw | Swahili | zh | Chinese |
| bn | Bengali | ur | Urdu |
| ta | Tamil | yo | Yoruba |
| ar | Arabic | ha | Hausa |

Device language is auto-detected as default during onboarding.
TTS falls back to English if the selected language voice is unavailable.
