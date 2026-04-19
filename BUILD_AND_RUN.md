# MedNavigator — Build & Run Commands

## Prerequisites

- USB debugging enabled on your Android phone
- Phone connected via USB
- Project built successfully

---

## 1. Build the App (Debug APK)

```powershell
./gradlew.bat assembleDebug
```

**What it does:** Compiles your Kotlin code, packages resources, and creates a debug APK.

**Output:** `app/build/outputs/apk/debug/app-debug.apk`

**When to use:** Every time you change code and want to test. This must run before install.

---

## 2. Install the App on Your Phone

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
```

**What it does:** Copies the APK from your PC to your phone and installs it.

- `-r` = replace existing version (keeps your data)

**When to use:** After a successful build. Overwrites the previous app on your phone.

---

## 3. Launch the App on Your Phone

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.mednavigator.app/.MainActivity
```

**What it does:** Sends a command to your phone to open MedNavigator immediately.

**When to use:** After installing. Opens the app without touching your phone.

---

## One-Liner (Build + Install + Launch)

```powershell
./gradlew.bat assembleDebug && & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk && & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.mednavigator.app/.MainActivity
```

Runs all three steps in sequence. If build fails, it stops — won't install a stale APK.

---

## Optional: Add `adb` to PATH

Run once in PowerShell to use `adb` without the full path:

```powershell
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";$env:LOCALAPPDATA\Android\Sdk\platform-tools", "User")
```

Then **restart your terminal**. After this, commands simplify to:

```powershell
./gradlew.bat assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.mednavigator.app/.MainActivity
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `adb not recognized` | Use full path or add to PATH (see above) |
| `device not found` | Enable USB debugging on phone, reconnect USB cable |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Uninstall the old app first: `adb uninstall com.mednavigator.app` |
| `Unauthorized` | Check your phone screen — accept the USB debugging prompt |
