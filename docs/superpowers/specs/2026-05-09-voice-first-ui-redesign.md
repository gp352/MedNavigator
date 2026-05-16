# Voice-First UI Redesign

**Date:** 2026-05-09
**Status:** Approved

## Goal

Redesign MedNavigator to be a voice-first medical assistant. The home screen becomes the primary voice interaction surface — no separate chat screen. Users tap a mic orb, speak, and receive both spoken and text responses. All interactions are saved with full audio and transcripts accessible via history.

## Navigation Flow

```
SplashScreen
  → ModelDownloadScreen (if model not downloaded)
  → OnboardingScreen (if not onboarded)
  → HomeScreen (voice hub — primary screen)

HomeScreen
  ├── Mic tap → voice recording starts on this screen
  ├── AI responds → text overlay card + SpeakingWave + TTS
  ├── Bottom nav: History / Listen / Scan
  ├── HistoryScreen (list of past sessions with audio playback)
  └── ConversationDetailScreen (full transcript + audio replays)
```

ChatScreen and VoiceInputScreen routes are removed. All voice input happens on HomeScreen.

## HomeScreen Voice Interaction States

Four states driven by ViewModel state:

### State 1: Idle (default)
- Mic orb with pulsing glow, "Tap to begin" text
- Glass cards (Scan, Upload) visible
- Suggestion chips at bottom

### State 2: Listening
- Orb turns bright green, larger pulse animation
- "Listening..." text replaces "Tap to begin"
- Glass cards and suggestions fade out
- Subtle waveform animation around the orb showing audio input

### State 3: Processing
- Orb shows a subtle spinner or shimmer
- User's transcribed text appears in a small card above the orb (e.g., "What you said: 'I've been having headaches'")
- "Thinking..." status text

### State 4: Responding
- SpeakingWave component animates below the orb (green bars)
- AI response text appears in a collapsible card sliding up from below the orb
- TTS speaks the response aloud
- When TTS finishes, wave stops, interaction auto-saves to history
- State returns to Idle

After each exchange, a subtle session indicator shows (e.g., "3 exchanges this session"). User can tap orb again to continue the conversation in the same session.

## HistoryScreen Enhancements

### History list (existing screen, enhanced)
- Each session card: date, title (auto-generated from first exchange), message count, duration
- Active/ongoing session highlighted with green dot
- Delete with confirmation dialog
- Search bar unchanged

### ConversationDetailScreen (new screen)
- Opens when user taps a session card
- Scrollable list of exchanges, each as a pair:
  - **User bubble:** transcribed text + play button for raw audio recording
  - **AI bubble:** response text + play button for TTS audio
- Top bar with back arrow and session title/date

## Storage Changes

- `ChatMessage` model gets two new optional fields:
  - `audioFilePath: String?` — path to user's raw WAV recording
  - `responseAudioPath: String?` — path to AI's TTS audio file
- Audio files stored in app internal storage (`files/audio/`)
- User audio already recorded as PCM/WAV by AudioRecorderService — save the file path
- TextToSpeechService gets a `synthesizeToFile(text, File)` method for AI response audio
- Conversations auto-saved after each complete exchange on HomeScreen

## Technical Changes

### Files to modify
- `HomeScreen.kt` — add 4 voice states, text overlay card, integrate SpeakingWave
- `NavGraph.kt` — reorder flow (ModelDownload → Onboarding → Home), remove Chat/VoiceInput routes, add ConversationDetail route
- `Routes.kt` — remove CHAT and VOICE_INPUT, add CONVERSATION_DETAIL with conversationId argument
- `HistoryScreen.kt` — add audio playback UI (play buttons on each exchange)
- `ChatViewModel.kt` — refactor to support voice-first flow: expose interaction state, handle recording → transcription → inference → TTS pipeline on HomeScreen
- ChatMessage model — add audioFilePath and responseAudioPath fields
- `SpeakingWave.kt` — minor tweaks to integrate below the orb during responding state
- `TextToSpeechService` — add synthesizeToFile method

### Files to create
- `ConversationDetailScreen.kt` — history detail view with full transcript + audio playback
- `VoiceInteractionState.kt` — sealed class for the 4 states (Idle, Listening, Processing, Responding)

### Dependencies
No new dependencies. Everything uses existing services.

## Response Delivery
- AI speaks response aloud via TTS
- Text shown in a collapsible overlay card on HomeScreen
- Both audio and text saved to history

## History Detail
- Full audio + transcript for both user and AI
- User's raw audio recording stored and playable
- AI's TTS audio stored and playable
