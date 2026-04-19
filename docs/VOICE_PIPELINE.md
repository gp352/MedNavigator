# MedNavigator — Voice Pipeline Details

## Current Implementation (Static MVP)

### Audio Capture

```
AudioRecorderService
    │
    ├── Format: PCM 16-bit, 16kHz, Mono
    ├── Source: MediaRecorder.AudioSource.MIC
    ├── Buffer: getMinBufferSize() from AudioRecord
    ├── Capture: coroutine on Dispatchers.IO
    ├── Output: ByteArrayOutputStream → ByteArray
    │
    └── Lifecycle:
        ├── startRecording() → creates AudioRecord, starts while-loop capture
        ├── stopRecording() → sets flag=false, AudioRecord.stop(), returns bytes
        └── release() → cleanup
```

### Speech Recognition

```
SpeechRecognitionService
    │
    ├── Engine: android.speech.SpeechRecognizer
    ├── Mode: LANGUAGE_MODEL_FREE_FORM
    ├── Language: from user profile (e.g., "en", "hi")
    ├── Partial results: enabled
    │
    └── Callbacks:
        ├── onPartialResult(text) → live transcription
        ├── onFinalResult(text)   → complete transcription
        └── onError(code)         → descriptive error message
            ├── ERROR_NO_MATCH → "Could not understand audio"
            ├── ERROR_SPEECH_TIMEOUT → "No speech detected"
            ├── ERROR_NETWORK → "Network unavailable"
            ├── ERROR_INSUFFICIENT_PERMISSIONS → "Permission not granted"
            └── ERROR_SERVER → "Server error"
```

### Text-to-Speech

```
TextToSpeechService
    │
    ├── Engine: android.speech.tts.TextToSpeech
    ├── Language: user's preferred language (falls back to English)
    ├── Queue: QUEUE_FLUSH (stops previous, speaks new)
    │
    └── Lifecycle:
        ├── init(context) → creates TTS engine, isReady when initialized
        ├── speak(text, language) → sets language, calls speak()
        ├── stop() → stops current speech
        └── shutdown() → releases TTS engine
```

### Response Generation

```
ResponseGenerator
    │
    ├── Input: hasAudio (boolean)
    │
    ├── hasAudio = true  → "Thank you for your voice input.
    │                        I received your query and will analyze it soon."
    │
    └── hasAudio = false → "I didn't catch that. Please try again."
```

### VoiceViewModel Orchestration

```
VoiceViewModel (AndroidViewModel)
    │
    ├── State exposed via StateFlow:
    │   ├── isRecording: Boolean
    │   ├── statusText: String
    │   ├── recognizedText: String    ← from SpeechRecognizer
    │   ├── responseText: String      ← from ResponseGenerator
    │   ├── isSpeaking: Boolean
    │   └── errorMessage: String?
    │
    ├── Dependencies:
    │   ├── AudioRecorderService
    │   ├── SpeechRecognitionService
    │   ├── TextToSpeechService
    │   └── OnboardingRepository (for language)
    │
    └── Flow:
        toggleRecording()
            ├── not recording → startRecording()
            │   ├── Launch coroutine: audioRecorder.startRecording()
            │   ├── Check: speechService.isAvailable()
            │   └── speechService.startListening(language, callback)
            │
            └── recording → stopRecording()
                ├── speechService.stopListening()
                ├── audioRecorder.stopRecording() → ByteArray
                └── finishRecording(hasContent)
                    ├── ResponseGenerator.generate()
                    ├── ttsService.speak(response)
                    └── Update all state flows
```

## Future: Gemma E4B Integration

### Planned Architecture

```
VoiceViewModel
    │
    ├── AudioRecorderService (same — captures PCM 16kHz)
    │
    ├── NEW: GemmaInferenceService
    │   ├── Loads: gemma-4-e4b-it-q4.litertlm (via MediaPipe GenAI)
    │   ├── Input: PCM audio bytes directly (140 language support)
    │   ├── Output: NavigatorResult JSON
    │   │
    │   └── Tool calls (model triggers these):
    │       ├── search_icd11_local(query) → List<IcdCondition>
    │       ├── find_specialists_local(condition, country) → List<HealthFacility>
    │       └── get_condition_info(icdCode) → IcdCondition
    │
    ├── SessionRepository.saveSession(result)
    │
    └── UI shows:
        ├── Condition suggestions with plain explanations
        ├── Urgency level (IMMEDIATE / SOON / ROUTINE)
        ├── Recommended specialists + facilities
        └── TTS reads explanation aloud
```

### Model Details

| Property | Value |
|----------|-------|
| Model | Gemma 4 E4B |
| Format | .litertlm (LiteRT for MediaPipe) |
| File | gemma-4-e4b-it-q4.litertlm |
| Quantization | Q4 |
| Max output tokens | 2048 |
| Vision token budget | 560 |
| Audio max duration | 30 seconds |
| Runtime | MediaPipe GenAI 0.10.14 |

### Constants for Model Integration

All defined in `Constants.kt`:

```kotlin
MODEL_FILENAME = "gemma-4-e4b-it-q4.litertlm"
MAX_OUTPUT_TOKENS = 2048
VISION_TOKEN_BUDGET = 560
AUDIO_MAX_SECONDS = 30
TOOL_SEARCH_ICD = "search_icd11_local"
TOOL_FIND_SPECIALISTS = "find_specialists_local"
TOOL_GET_CONDITION = "get_condition_info"
```
