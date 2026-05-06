# Gujarati Language Testing Guide for MedNavigator ICD

## Overview

MedNavigator now fully supports Gujarati language including:
- ✅ Gujarati text input (both voice and typed)
- ✅ ICD disease detection in Gujarati
- ✅ Gujarati medical terminology
- ✅ Gujarati AI responses
- ✅ 40+ Gujarati medical keywords

---

## Part 1: Setup Gujarati Language

### Method 1: During Onboarding (Recommended)

1. Launch MedNavigator app
2. If this is first time, you'll see **Onboarding Screen**
3. Look for **"Language"** dropdown
4. Select **"Gujarati"** (ગુજરાતી)
5. Fill other details (Name, Age, Sex, Country)
6. Click **"Save"**

App will now use Gujarati for:
- ✅ Model responses
- ✅ UI text
- ✅ TTS (Text-to-Speech) output

### Method 2: Via Android Device Settings

1. Go to **Settings** → **Languages & input** → **Languages**
2. Select **"Gujarati"**
3. Close and reopen MedNavigator
4. App will detect Gujarati as default language

### Method 3: Via ADB (For Testing)

```bash
# Set language to Gujarati via Android Debug Bridge
adb shell "
  sqlite3 /data/data/com.mednavigator.app/shared_prefs/med_navigator_prefs.xml
"

# Manually edit SharedPreferences:
# Change: user_language value to "gu"
```

---

## Part 2: Gujarati Medical Terminology

### Comprehensive Medical Terms in Gujarati

| English | Gujarati | Gujarati Script |
|---------|----------|-----------------|
| **Cardiovascular** | | |
| Chest pain | છાતીમાં દર્દ | ચেસ્ટ પેઈન |
| Heart | હૃદય | હૃદય |
| Palpitations | હૃદયસ્પંદન | હર્ટબીટ |
| **Respiratory** | | |
| Cough | ખાંસી | કફ |
| Shortness of breath | શ્વાસમાં તકલીફ | બ્રીધલેસનેસ |
| Asthma | તમાકાવ | અસ્થમા |
| **Neurological** | | |
| Headache | માથાનો દર્દ | હેડેક |
| Migraine | માથાનો તીવ્ર દર્દ | માઈગ્રેન |
| Dizziness | ચક્કર | ડિઝીનેસ |
| Vertigo | ગરેબુ ચક્કર | વર્ટીગો |
| **Gastrointestinal** | | |
| Nausea | ઉલટીનું અહેસાસ | નોસીયા |
| Vomiting | ઉલટી | વોમીટીંગ |
| Abdominal pain | પેટમાં દર્દ | એબ્ડોમીનલ પેઈન |
| Diarrhea | સતત મળ | ડાયરીયા |
| Constipation | કબજ | કોન્સ્ટીપેશન |
| **Fever/Infection** | | |
| Fever | તાપમાન | ફીવર |
| Temperature | તાપમાન | ટેમ્પરેચર |
| Infection | ચેપ | ઈનફેક્શન |
| Virus | વાયરસ | વાયરસ |
| **Musculoskeletal** | | |
| Back pain | પીઠમાં દર્દ | બેક પેઈન |
| Joint pain | આંટીમાં દર્દ | જોઈન્ટ પેઈન |
| Arthritis | આર્થરાઈટીસ | આર્થરાઈટીસ |
| **Mental/Psychological** | | |
| Anxiety | ચિંતા | એક્સાયટી |
| Depression | ઉદાસીનતા | ડેપ્રેશન |
| Stress | તણાવ | સ્ટ્રેસ |

---

## Part 3: Test Cases in Gujarati

### Test Case 1: Simple Symptom (Chest Pain)

**Input (Type or Voice):**
```
છાતીમાં દર્દ છે
(Chest pain)
```

**Expected Logcat Output:**
```
D/ChatViewModel: Detected symptoms: [છાતી, દર્દ] from input: 'છાતીમાં દર્દ છે'
D/IcdRepository: Building context for symptoms: [છાતી, દર્દ]
D/IcdRepository: Search 'છાતી' returned 1 result in 38ms
```

**Expected AI Response (in Gujarati):**
```
તમે સમજાયું કે તમને છાતીમાં દર્દ છે. આ આ્ યોજનામાં આ હોઈ શકે છે:
- BA80: છાતીમાં દર્દ
- BA83: હૃદયમાં હમલો

[Gemma responds about chest pain with ICD references]
```

✅ **Success Indicators:**
- Gujarati text displays correctly
- Logcat shows symptom detection
- Response includes ICD codes
- TTS speaks Gujarati response

---

### Test Case 2: Multiple Symptoms

**Input:**
```
તાપમાન છે અને ખાંસી થાય છે
(Fever and cough)
```

**Expected Symptoms Detected:**
```
[તાપમાન, તાપ, ખાંસી, ખાંસ]
```

**Expected ICD Codes in Response:**
- EA90: તાપમાન (Fever)
- CA90: ખાંસી (Cough)
- EA91: ફ્લૂ (Influenza)
- EA92: સામાન્ય શીતળતા (Common Cold)

---

### Test Case 3: Complex Gujarati Text

**Input:**
```
મને બહુ તેજ તાપમાન છે અને શ્વાસમાં તકલીફ છે, માથામાં પણ દર્દ છે
(I have high fever, shortness of breath, and headache)
```

**Expected Detection:**
```
Detected: [તાપમાન, શ્વાસ, દર્દ, માથા]
```

**Expected ICD References:**
- EA90: Fever
- CA91: Shortness of breath  
- DA20/DA21: Headache

---

### Test Case 4: Medical Emergency Keywords (Gujarati)

**Input:**
```
છાતીમાં તીવ્ર દર્દ છે અને શ્વાસ લેવા માટે તણાવ છે
(Severe chest pain and difficulty breathing)
```

**Expected Response:**
```
આ કટોક્તીનુ લક્ષણ છે. કૃપયા તાત્કાલીક હોસ્પિટલ જાઓ.
(This appears to be a serious condition. Please seek immediate medical care.)
```

---

## Part 4: Step-by-Step Testing Procedure

### Prerequisites
- MedNavigator installed and running
- Android Studio Logcat available
- Device/Emulator with Gujarati support

### Step 1: Verify Gujarati Setup

**In Logcat, look for:**
```
D/ChatViewModel: Building prompt with language: gu
```

If you see `gu` → Language is set correctly ✅

### Step 2: Send First Gujarati Message

**Action:** Type in chat box:
```
નમસ્તે, મને તાપમાન છે
(Hello, I have fever)
```

**Expected:**
- Message appears in Gujarati ✅
- Logcat shows symptom detection ✅
- Response comes back in Gujarati ✅

### Step 3: Check Logcat for Symptom Detection

**Filter Logcat:** `IcdRepository|ChatViewModel`

**Look for:**
```
D/ChatViewModel: Detected symptoms: [તાપમાન, તાપ] from input: 'મને તાપમાન છે'
D/IcdRepository: Building context for symptoms: [તાપમાન, તાપ]
D/IcdRepository: Search 'તાપમાન' returned 3 results in 42ms
```

### Step 4: Verify ICD Context in Response

**Look for in response:**
- References to ICD codes (e.g., "EA90")
- Gujarati medical explanations
- Relevant condition definitions

### Step 5: Test Voice Input in Gujarati

**At bottom of chat screen:**
1. Tap **Microphone icon** 🎤
2. Speak in Gujarati: "મને છાતીમાં દર્દ છે"
3. Wait for recognition
4. Release microphone
5. Verify Gujarati text recognized
6. Check response includes ICD context

---

## Part 5: Common Gujarati Phrases for Testing

### Basic Medical Complaints

```
બરાબર મને દર્દ છે
(I have severe pain)

પેટમાં તીવ્ર પીડા થાય છે
(Sharp abdominal pain)

વધુ પડતું ગરમી છે
(Excessive fever/heat)

ખાંસીમાં બહુ તકલીફ છે
(Severe cough)

ખૂબ ચિંતા થાય છે
(Very anxious/worried)

ઘણા દિવસથી ખાંસી છે
(Cough for many days)

ઘણા દિવસે પાચન સમસ્યા છે
(Digestion problems for days)
```

### Response Verification

After sending Gujarati message, check:

1. **UI Level:**
   - ✅ Message displays correctly in Gujarati
   - ✅ Response displays in Gujarati
   - ✅ No garbled text or character errors

2. **Logcat Level:**
   - ✅ Language shows as "gu"
   - ✅ Symptoms detected correctly
   - ✅ ICD context built successfully
   - ✅ Query times normal (<100ms)

3. **Response Level:**
   - ✅ Contains medical advice in Gujarati
   - ✅ References ICD codes
   - ✅ Contextually relevant to input
   - ✅ TTS speaks Gujarati properly

---

## Debugging: Check Language Configuration

### Via Logcat

**Command:**
```bash
adb logcat | grep "language"
```

**Expected Output:**
```
D/ChatViewModel: Building prompt with language: gu
```

### Via ADB Shell

```bash
# Check SharedPreferences
adb shell cat /data/data/com.mednavigator.app/shared_prefs/med_navigator_prefs.xml | grep user_language

# Should show:
<string name="user_language">gu</string>
```

### Via Android Studio

1. Open **Device File Explorer**
2. Navigate to `/data/data/com.mednavigator.app/shared_prefs/`
3. Open `med_navigator_prefs.xml`
4. Look for `<string name="user_language">gu</string>`

---

## Performance Testing in Gujarati

### Expected Performance Metrics

| Metric | Expected | Actual |
|--------|----------|--------|
| Symptom detection | <50ms | _____ |
| ICD search | <100ms | _____ |
| Model response | 2-5s | _____ |
| TTS speech | 2-10s | _____ |
| Total E2E | <20s | _____ |

### Test Procedure

1. Send Gujarati message with timestamp: `HH:MM:SS.SSS`
2. Record response timestamp
3. Check Logcat for individual operation times
4. Calculate total time

**Example Logcat Timing:**
```
12:34:56.123 D/ChatViewModel: Detected symptoms: [તાપમાન]
12:34:56.156 D/IcdRepository: Search query took 33ms
12:34:56.892 → Model response received (736ms)
12:35:02.157 → TTS finished (5s)
Total: ~6 seconds ✅
```

---

## Complete Gujarati Testing Checklist

### Setup
- [ ] Gujarati language selected in app
- [ ] Gujarati keyboard available on device
- [ ] Gujarati font renders correctly
- [ ] Logcat shows language: "gu"

### Symptom Detection
- [ ] Single Gujarati symptom detected
- [ ] Multiple Gujarati symptoms detected
- [ ] Mixed English+Gujarati detected
- [ ] False positives minimal

### ICD Integration
- [ ] ICD codes referenced in response
- [ ] Correct conditions for symptom
- [ ] Context properly injected
- [ ] About 5-8 conditions provided

### Gujarati Response Quality
- [ ] Response is grammatically correct Gujarati
- [ ] Medical terminology used appropriately
- [ ] Contextually relevant to input
- [ ] No English mixed in (or minimal)

### Voice Input
- [ ] Gujarati speech recognized
- [ ] Symptoms detected from voice
- [ ] ICD context included
- [ ] Response spoken in Gujarati

### Performance
- [ ] No lag or freezing
- [ ] Queries complete in <100ms
- [ ] Memory usage normal
- [ ] TTS works smoothly

---

## Example Successful Session (Gujarati)

```
User: છાતીમાં દર્દ છે અને શ્વાસમાં તકલીફ છે
[Chest pain and shortness of breath]

Logcat:
D/ChatViewModel: User language: gu
D/ChatViewModel: Detected symptoms: [છાતી, દર્દ, શ્વાસ, તકલીફ]
D/IcdRepository: Building context for symptoms: [છાતી, દર્દ, શ્વાસ]
D/IcdRepository: Search 'છાતી' returned 2 results in 38ms
D/IcdRepository: Search 'શ્વાસ' returned 1 result in 45ms

AI Response (Gujarati):
તમે સમજાયું કે તમને છાતીમાં દર્દ છે અને શ્વાસમાં તકલીફ છે.
આ સંજોગોમાં આ સમસ્યાઓ હોઈ શકે છે:

1. [BA80] છાતીમાં દર્દ - રક્તવાહક તંત્રમાં સમસ્યા
2. [CA91] શ્વાસમાં તકલીફ - શ્વાસની ક્રિયામાં સમસ્યા  
3. [BA83] હૃદયમાં હમલો - તાત્કાલીક ધર્યાન જરૂરી

કૃપયા તાત્કાલીક હોસ્પિટલમાં જાઓ અથવા ઈમર્જન્સી નંબર પર કોલ કરો.

✅ Success: All tests pass!
```

---

## Troubleshooting Gujarati Issues

### Issue 1: Gujarati not displaying correctly

**Symptom:** Shows ???? or boxes instead of Gujarati text

**Solution:**
1. Install Gujarati font
2. Update Android system
3. Try different keyboard app
4. Clear app cache: Settings → Apps → MedNavigator → Storage → Clear Cache

### Issue 2: Voice recognition not working in Gujarati

**Symptom:** Microphone records but doesn't recognize Gujarati

**Solution:**
1. Check Google Speech Recognition supports Gujarati (it does)
2. Enable Google Speech in Settings → Language & input
3. Speak clearly
4. Try: Settings → Apps → Google Mobile Services → Permissions

### Issue 3: Symptoms not detected despite Gujarati input

**Symptom:** Gujarati message sent, but no ICD context

**Cause:** Gujarati keyword not in list or slightly different spelling

**Solution:** 
1. Add to `extractSymptoms()` list
2. Test exact form: તાપમાન vs તપ vs તાપ
3. Check logcat: "Detected symptoms: []" means no match

### Issue 4: Response is partially English, partially Gujarati

**Cause:** Gemma model mixing languages

**Solution:**
1. Ensure language set to "gu" in OnboardingRepository
2. Try with very clear symptom keywords
3. Format message with only Gujarati text

---

## Next Steps

✅ Complete all tests above  
✅ Verify Gujarati language support working  
✅ Expand disease database with Gujarati terms  
✅ Test with more complex scenarios  
✅ Deploy to production  

**Questions?** Check ICD_INTEGRATION_GUIDE.md or ICD_TESTING_GUIDE.md

