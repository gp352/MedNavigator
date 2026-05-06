# ICD Integration - Quick Testing Script

## 5-Minute Verification Test

Follow these steps to verify ICD integration is working:

---

## Step 1: Enable Logcat Filter (30 seconds)

**In Android Studio:**
1. Open **View → Tool Windows → Logcat**
2. In the search box, type: `IcdRepository|ChatViewModel`
3. Click **Apply** or press Enter
4. See only ICD-related logs

---

## Step 2: Send First Test Message in English (1 min)

**Action:** Open MedNavigator and type in chat:
```
I have chest pain
```

**What to look for in Logcat:**
```
✅ D/ChatViewModel: Detected symptoms: [chest pain, chest]
✅ D/IcdRepository: Building context for symptoms: [chest pain, chest]
✅ D/IcdRepository: Search 'chest pain' returned 2 results
```

**Result:** If you see these logs → ICD is working! ✅

**Expected Response:** Should mention BA80 (Chest pain) or BA83 (Myocardial infarction)

---

## Step 3: Set Language to Gujarati (2 min)

**Option A: Via Settings**
1. Close MedNavigator
2. Device Settings → Languages → Select **Gujarati**
3. Reopen MedNavigator

**Option B: Via App Onboarding**
1. Clear app data: Settings → Apps → MedNavigator → Storage → Clear Storage
2. Reopen MedNavigator
3. On Onboarding screen, select **Gujarati** language
4. Fill other details and save

---

## Step 4: Send Gujarati Test Message (1 min)

**Action:** Type in chat:
```
છાતીમાં દર્દ છે
```
(Chest pain in Gujarati)

**What to look for in Logcat:**
```
✅ D/ChatViewModel: Building prompt with language: gu
✅ D/ChatViewModel: Detected symptoms: [છાતી, દર્દ]
✅ D/IcdRepository: Building context for symptoms: [છાતી, દર્દ]
```

**Result:** If you see "language: gu" → Gujarati working! ✅

**Expected Response:** Should be in Gujarati language with ICD references

---

## Full Verification Checklist

### Database Initialization ✅

```bash
# Check if app initializes without crashes
adb logcat | grep "ICD database initialized"
```

Expected output:
```
D/IcdRepository: ICD database initialized: {total=26, circulatory=4, respiratory=4}
```

---

### Symptom Detection Test ✅

**English Test:**
```
Message: "fever and cough"
Expected: Detected: [fever, cough]
```

**Gujarati Test:**
```
Message: "તાપમાન અને ખાંસી"
Expected: Detected: [તાપમાન, તાપ, ખાંસી, ખાંસ]
```

---

### ICD Context Injection ✅

**Check that response includes:**
- ICD codes (e.g., BA80, CA90, EA91)
- Medical explanations
- Condition definitions

Example in response:
```
[BA80] Chest pain - A feeling of discomfort in chest area
[BA83] Myocardial infarction - Heart attack
```

---

### Language Support ✅

**Check Logcat for:**
```
D/ChatViewModel: Building prompt with language: gu
```

If you see `gu` → Gujarati support confirmed ✅

---

## Detailed Test Messages

### English Test Suite

Send each message and verify ICD context appears:

| Message | Expected ICD Codes | Expected in Response |
|---------|-------------------|----------------------|
| "chest pain" | BA80, BA83 | Chest, Heart, Cardiac |
| "fever and cough" | EA90, CA90, EA91 | Fever, Cough, Flu |
| "severe headache" | DA20, DA21 | Headache, Migraine |
| "anxiety" | QE80 | Mental, Anxiety, Stress |

---

### Gujarati Test Suite

Send each message in Gujarati:

| Gujarati Message | English | Expected ICD |
|------------------|---------|--------------|
| છાતીમાં દર્દ છે | Chest pain | BA80, BA83 |
| તાપમાન અને ખાંસી | Fever and cough | EA90, CA90 |
| માથાનો દર્દ | Headache | DA20, DA21 |
| પેટમાં દર્દ | Abdominal pain | DA95, DA96 |

---

## Logcat Command Reference

### Filter for ICD Logs Only

```bash
adb logcat | grep "IcdRepository\|ChatViewModel" | head -50
```

### View Database Stats

```bash
# Check database file exists
adb shell ls -la /data/data/com.mednavigator.app/databases/

# Should show: icd11_mms.db
```

### Check Language Setting

```bash
adb shell cat /data/data/com.mednavigator.app/shared_prefs/med_navigator_prefs.xml | grep user_language
```

Expected output:
```
<string name="user_language">gu</string>
```

### Real-time Logcat Monitoring

```bash
# Terminal 1: Start logcat filter
adb logcat IcdRepository:* ChatViewModel:* *:S

# Terminal 2: Send test message via UI
# Watch Terminal 1 for logs
```

---

## Success Indicators

### All Tests Passing ✅

- [ ] Database initializes with 26 conditions
- [ ] English symptoms detected correctly
- [ ] Gujarati symptoms detected correctly
- [ ] ICD codes appear in chat responses
- [ ] Language switches to Gujarati successfully
- [ ] No crashes or errors in logcat
- [ ] Response time <5 seconds
- [ ] TTS speaks in correct language

---

## Troubleshooting Quick Fix

### Issue: No symptoms detected

**Fix:** Check logcat for:
```
D/ChatViewModel: Detected symptoms: []
```

**Solution:** Keyword not in extractSymptoms() list → Add it

### Issue: ICD not in response

**Fix:** Check if context was built:
```
D/IcdRepository: Building context for symptoms:
```

**Solution:** Manual verify database:
```bash
adb shell sqlite3 /data/data/com.mednavigator.app/databases/icd11_mms.db \
  "SELECT COUNT(*) FROM icd_conditions;"
```

Should show: `26`

### Issue: Gujarati not showing

**Fix:** Check language:
```bash
adb logcat | grep "language:"
```

**Solution:** Should show `gu` - if not, reset:
```bash
adb shell pm clear com.mednavigator.app
```

---

## Video/Screenshot Evidence

### What to Screenshot After Each Test

1. **English Test:** Chat with "chest pain" + response mentioning BA80
2. **Gujarati Setup:** Settings showing Gujarati language selected
3. **Gujarati Test:** Chat with Gujarati text + response in Gujarati
4. **Logcat:** Terminal showing detection logs
5. **Database:** ADB shell showing 26 conditions

---

## One-Command Full Test

```bash
#!/bin/bash
# Complete test automation

echo "1. Clear app data..."
adb shell pm clear com.mednavigator.app

echo "2. Restart app and wait for init..."
adb shell am start -n com.mednavigator.app/.MainActivity
sleep 3

echo "3. Check ICD init in logs..."
adb logcat | grep "ICD database initialized" | head -1

echo "4. Query database..."
adb shell sqlite3 /data/data/com.mednavigator.app/databases/icd11_mms.db \
  "SELECT code, title FROM icd_conditions LIMIT 5;"

echo "5. Done! Check above for verification"
```

---

## Success Confirmation

When you see this in Logcat, **ICD Integration is 100% working:**

```
D/IcdRepository: ICD database initialized: {total=26, circulatory=4, respiratory=4}
D/ChatViewModel: Detected symptoms: [<your detected symptom>]
D/IcdRepository: Building context for symptoms: [<your detected symptom>]
D/IcdRepository: Search '<symptom>' returned <count> results in <time>ms
```

And the AI response includes official ICD codes like:
- **BA80** - Chest pain
- **EA90** - Fever
- **DA20** - Headache
- etc.

---

## Testing Timeline

| Step | Time | Status |
|------|------|--------|
| 1. Logcat Setup | 30s | ⏱️ |
| 2. English Test | 60s | ⏱️ |
| 3. Gujarati Setup | 120s | ⏱️ |
| 4. Gujarati Test | 60s | ⏱️ |
| 5. Verification | 30s | ⏱️ |
| **Total** | **~5 min** | ✅ |

---

## Next Steps After Verification

Once all tests pass:

1. ✅ ICD is working
2. 📚 Review `ICD_INTEGRATION_GUIDE.md` for details
3. 🌍 Add more Gujarati keywords (optional)
4. 📊 Expand disease database (optional)
5. 🚀 Deploy to production

---

## Support Resources

- 📖 Full Details: `docs/ICD_TESTING_GUIDE.md`
- 🇬🇺 Gujarati Guide: `docs/GUJARATI_LANGUAGE_TESTING.md`
- 🏗️ Architecture: `docs/ICD_INTEGRATION_GUIDE.md`
- 🔧 Setup: `docs/ICD_DEVELOPER_SETUP.md`

---

## Quick Links

- [ICD Integration Guide](ICD_INTEGRATION_GUIDE.md)
- [Testing Guide](ICD_TESTING_GUIDE.md)
- [Gujarati Testing](GUJARATI_LANGUAGE_TESTING.md)
- [Developer Setup](ICD_DEVELOPER_SETUP.md)

**Done! Your ICD integration is ready for testing.** 🎉

