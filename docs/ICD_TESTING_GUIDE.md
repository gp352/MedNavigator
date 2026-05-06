# ICD Integration Testing Guide - Complete

## Quick Start: Verify ICD Integration

### Step 1: Enable Logcat Filtering

In Android Studio:
1. Open **Logcat** (View → Tool Windows → Logcat)
2. In the filter box, type: `IcdRepository|ChatViewModel`
3. Click "Apply"

This will show only the relevant logs for testing.

## Test 1: Verify Database Initialization

### What to Check
- ICD database loads with 26 default conditions
- No errors during initialization

### Steps

**1. Clear App Data (Fresh Start)**
```
Device → Settings → Apps → MedNavigator → Storage → Clear Storage
```

**2. Launch App**
- Open MedNavigator
- Wait 5 seconds for initialization

**3. Check Logcat Output**
Look for these messages:

```
D/IcdRepository: ICD database is empty, loading default conditions...
D/IcdRepository: Successfully loaded 26 default ICD conditions
D/IcdRepository: ICD database initialization complete
D/ChatViewModel: ICD database initialized: {total=26, circulatory=4, respiratory=4}
```

✅ **Success:** All 26 default conditions loaded  
❌ **Failure:** See "Troubleshooting" section below

---

## Test 2: Symptom Detection in English

### Test Case 1: Single Symptom

**Message:** "I have chest pain"

**Expected Logcat:**
```
D/ChatViewModel: Detected symptoms in response: [chest pain, chest]
D/IcdRepository: Building context for symptoms: [chest pain]
D/IcdRepository: Search 'chest pain' returned 2 results in 45ms
```

**Expected Response:**
Should include references to:
- BA80: Chest pain
- BA83: Myocardial infarction

---

### Test Case 2: Multiple Symptoms

**Message:** "I have fever, cough, and sore throat"

**Expected Logcat:**
```
D/ChatViewModel: Detected symptoms in response: [fever, cough]
D/IcdRepository: Building context for symptoms: [fever, cough]
```

**Expected Response:**
Should mention:
- EA90: Fever
- CA90: Cough
- EA91: Influenza
- EA92: Common cold

---

### Test Case 3: No Symptoms Detected

**Message:** "How are you?"

**Expected Logcat:**
```
D/ChatViewModel: Detected symptoms in response: []
(No ICD context built)
```

**Expected Result:**
- Regular response without ICD references
- No performance impact

---

## Test 3: Gujarati Language Testing

### Setup: Change to Gujarati Language

**Option A: Via App (If Onboarding Screen)**
1. Open MedNavigator
2. On Onboarding:
   - Select Language: **Gujarati** (gu)
   - Fill other details
   - Click Save

**Option B: Via Android Settings**
1. Device Settings → Languages → Select **Gujarati**
2. This sets device default language
3. App will use Gujarati on next launch

**Option C: Manual SharedPreferences (For Testing)**
```bash
# Via adb shell
adb shell am instrument -e clearPackageData true -w com.mednavigator.app/androidx.test.runner.AndroidJUnitRunner
adb shell "
sqlite3 /data/data/com.mednavigator.app/shared_prefs/med_navigator_prefs.xml
UPDATE shared_prefs SET user_language='gu' WHERE key='user_language';
"
```

---

### Gujarati Test Cases

#### Test Case 1: Gujarati Chest Pain

**Message:** "વક્ષસ્થાનમાં પીડા છે" (waksthano me peeda che - "I have chest pain")
OR
**Message:** "છાતીમાં દર્દ છે" (chhati mein dard che - "chest pain")

**Expected:**
- System should detect symptom even though input is Gujarati
- ICD context should be injected
- Model response should be in Gujarati

**Logcat Check:**
```
D/ChatViewModel: User language: gu
D/IcdRepository: Building context...
```

#### Test Case 2: Gujarati Fever and Cough

**Message:** "તાપમાન છે અને ખાંસી થાય છે" (taapman che aur khansi thay che - "fever and cough")

**Expected Response:**
Should reference ICD codes even though user input is in Gujarati.

#### Test Case 3: Gujarati Headache

**Message:** "માથાનો દર્દ છે" (mathano dard che - "headache")

**Expected:**
- Detect headache symptom
- Reference DA20 (Headache), DA21 (Migraine)
- Response in Gujarati

---

## Test 4: Check System Prompt Injection

### Enable Debug Logging

**Create Debug Utility (Optional):**

Add to `ChatViewModel`:

```kotlin
private fun logSystemPrompt(prompt: String) {
    val lines = prompt.split("\n")
    Log.d(TAG, "=== SYSTEM PROMPT START ===")
    lines.forEach { line ->
        Log.d(TAG, "PROMPT: $line")
    }
    Log.d(TAG, "=== SYSTEM PROMPT END ===")
}

// Call before sending to model:
private suspend fun buildPromptWithIcdContext(userMessage: String): String {
    val basePrompt = buildPrompt()
    val symptoms = extractSymptoms(userMessage)
    val icdContext = if (symptoms.isNotEmpty()) {
        icdRepository.buildComprehensiveMedicalContext(symptoms)
    } else {
        ""
    }

    val finalPrompt = if (icdContext.isNotEmpty()) {
        """
        $basePrompt
        
        $icdContext
        """.trimIndent()
    } else {
        basePrompt
    }
    
    logSystemPrompt(finalPrompt)  // Add this
    return finalPrompt
}
```

### Expected Logcat Output

```
D/ChatViewModel: === SYSTEM PROMPT START ===
PROMPT: You are MedNavigator, a calm medical assistant...
PROMPT: ## Relevant ICD-11 Conditions:
PROMPT: 1. [BA80] Chest pain
PROMPT:    - Definition: A feeling of discomfort...
PROMPT:    - Body System: Circulatory system
PROMPT: === SYSTEM PROMPT END ===
```

---

## Test 5: Verify Performance

### Memory Usage Test

**Steps:**
1. Open Android Profiler (View → Tool Windows → Profiler)
2. Select Memory tab
3. Launch app
4. Send 5 messages with various symptoms
5. Observe memory usage

**Expected:**
- Baseline: ~80-100 MB
- After ICD load: ~110-130 MB (increase of 20-30 MB)
- No memory leaks (stays constant)

### Query Performance Test

**Steps:**
1. Send message with many symptom keywords
2. Check logcat for query times

**Expected Logcat:**
```
D/IcdRepository: Search 'chest pain' returned 2 results in 45ms
D/IcdRepository: Search 'fever' returned 3 results in 38ms
D/IcdRepository: Search 'cough' returned 2 results in 41ms
```

**Expected:** All queries <100ms

---

## Test 6: Multilingual Symptom Keywords

### Current Support

The app currently detects English symptom keywords. To test multilingual support:

### Add Gujarati Keywords (Test)

Edit `ChatViewModel.extractSymptoms()`:

```kotlin
private fun extractSymptoms(userMessage: String): List<String> {
    val commonSymptoms = listOf(
        // English
        "chest pain", "chest", "heart", "palpitations",
        "shortness of breath", "dyspnea", "breathing", "cough",
        "fever", "temperature", "headache", "head pain",
        
        // Gujarati (for testing)
        "છાતી", "દર્દ", "પીડા",  // chest, pain
        "તાપમાન", "તાપ",           // fever, temperature
        "માથા", "માથું",             // head
        "ખાંસી", "ખાંસ",             // cough
        "શ્વાસ", "શ્વાસની",         // breath
    )
    
    val lowerMessage = userMessage.lowercase()
    return commonSymptoms.filter { symptom ->
        lowerMessage.contains(symptom)
    }
}
```

### Test Gujarati Keywords

**Message:** "છાતીમાં દર્દ છે"

**Expected:**
- Detects "છાતી" (chest) and "દર્દ" (pain)
- ICD context injected
- Response references ICD codes

---

## Complete Testing Checklist

### Database Tests
- [ ] App launches without crashes
- [ ] Logcat shows "ICD database initialized with 26 conditions"
- [ ] No errors in initialization
- [ ] Stats show: total=26, circulatory=4, respiratory=4

### English Symptom Tests
- [ ] Single symptom detected (e.g., "chest pain")
- [ ] Multiple symptoms detected (e.g., "fever and cough")
- [ ] Correct ICD codes referenced in response
- [ ] Non-symptom messages don't trigger ICD context

### Gujarati Language Tests
- [ ] App language set to Gujarati successfully
- [ ] Gujarati text input works
- [ ] Symptom keywords detected in Gujarati
- [ ] Response is in Gujarati
- [ ] ICD codes appear in Gujarati responses

### Performance Tests
- [ ] Memory usage normal (<150 MB)
- [ ] Query response time <100ms
- [ ] No UI freezing during queries
- [ ] App responsive during chat

### Integration Tests
- [ ] System prompt contains ICD context
- [ ] Multiple conversations work correctly
- [ ] Chat history saves ICD responses
- [ ] Voice input works with ICD context

---

## Troubleshooting

### Issue 1: "ICD database is empty" on launch

**Cause:** `loadDefaultConditions()` not called

**Solution:**
```kotlin
// In IcdRepository, verify this is inside initializeIfNeeded():
if (count == 0) {
    Log.d(TAG, "ICD database is empty, loading default conditions...")
    loadDefaultConditions()  // ← Should be called
}
```

### Issue 2: Symptoms not detected

**Cause:** Keyword not in list or case mismatch

**Solution:**
1. Check keyword is exact substring
2. Add variant spellings
3. Log extracted symptoms:

```kotlin
private fun extractSymptoms(userMessage: String): List<String> {
    val lowerMessage = userMessage.lowercase()
    val commonSymptoms = listOf(/* ... */)
    
    val detected = commonSymptoms.filter { symptom ->
        lowerMessage.contains(symptom)
    }
    
    Log.d(TAG, "Input: '$userMessage' → Detected: $detected")  // Add this
    return detected
}
```

### Issue 3: ICD context not in response

**Cause:** Context not being built or injected

**Solution:**
1. Verify `buildPromptWithIcdContext()` is called
2. Check symptoms are detected
3. Verify ICD database has data:

```kotlin
// Test query manually in device shell
adb shell "
sqlite3 /data/data/com.mednavigator.app/databases/icd11_mms.db
SELECT COUNT(*) FROM icd_conditions;
SELECT * FROM icd_conditions WHERE code='BA80';
"
```

### Issue 4: Gujarati not working

**Cause:** Language not set or model doesn't support Gujarati

**Solution:**
1. Verify language setting:
```kotlin
val language = onboardingRepository.getUserLanguage()
Log.d(TAG, "Current language: $language")
```

2. Check if Gemma supports language:
   - Gemma 4 E4B supports 140+ languages including Gujarati
   - Should work automatically

3. Force language in OnboardingRepository:
```kotlin
prefs.edit()
    .putString(Constants.PREF_USER_LANGUAGE, "gu")
    .apply()
```

---

## Sample Test Messages by Language

### English Test Messages

1. "I have chest pain" → Expect: BA80, BA83
2. "fever and cough" → Expect: EA90, CA90, EA91
3. "severe headache" → Expect: DA20, DA21
4. "abdominal pain and vomiting" → Expect: DA95, DA96
5. "shortness of breath" → Expect: CA91
6. "back pain" → Expect: FA01
7. "fever" → Expect: EA90
8. "anxiety and depression" → Expect: QE80, QE81

### Gujarati Test Messages

1. **છાતીમાં દર્દ છે** (chest pain)
   - Expect: BA80, BA83

2. **તાપમાન અને ખાંસી** (fever and cough)
   - Expect: EA90, CA90

3. **માથાનો દર્દ** (headache)
   - Expect: DA20, DA21

4. **પેટમાં પીડા અને ઉલટી થાય છે** (abdominal pain and vomiting)
   - Expect: DA95, DA96

5. **તાપ છે અને શ્વાસમાં તકલીફ છે** (fever and shortness of breath)
   - Expect: EA90, CA91

6. **પીઠમાં દર્દ છે** (back pain)
   - Expect: FA01

7. **ચિંતા અને ઉદાસીનતા** (anxiety and depression)
   - Expect: QE80, QE81

---

## Verification Script (Advanced)

Create file `test_icd_integration.sh`:

```bash
#!/bin/bash

# Test ICD integration from command line
# Usage: ./test_icd_integration.sh

echo "Testing ICD Integration..."

# Check database exists
echo "1. Checking database..."
adb shell "sqlite3 /data/data/com.mednavigator.app/databases/icd11_mms.db 'SELECT COUNT(*) FROM icd_conditions;'"

# Check specific conditions
echo "2. Checking BA80 (Chest pain)..."
adb shell "sqlite3 /data/data/com.mednavigator.app/databases/icd11_mms.db 'SELECT * FROM icd_conditions WHERE code=\"BA80\";'"

# Grep logcat for ICD messages
echo "3. Checking logcat for ICD initialization..."
adb logcat -d | grep "IcdRepository\|ChatViewModel" | tail -20

echo "Done!"
```

---

## Next Steps After Testing

1. ✅ **Verify all tests pass**
2. **Expand disease database** (follow ICD_INTEGRATION_GUIDE.md)
3. **Add more Gujarati keywords** for better detection
4. **Test with voice input** (especially Gujarati)
5. **Deploy to production**

---

## Support

If issues occur:
1. Check Logcat filter: `IcdRepository|ChatViewModel`
2. Review relevant section in ICD_INTEGRATION_GUIDE.md
3. Check "Troubleshooting" section above
4. Clear app data and restart

All tests passing? ✅ **ICD Integration is working correctly!**

