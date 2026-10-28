# Smart Switchboard - Android Native App

Smart Switchboard is a high-performance, native Android application built with Kotlin and Jetpack Compose (Material 3). It remotely controls a 4-relay ESP8266 IoT switchboard via Firebase Realtime Database with sub-second bi-directional live sync, zero background battery drain, quick action scenes, Quick Settings tiles, home-screen widget, and biometric app lock.

---

## 1. Firebase Setup & Registration

Follow these exact steps to connect your Firebase Realtime Database and Auth:

1. Open the [Firebase Console](https://console.firebase.google.com/).
2. Select your existing Firebase project (the one your ESP8266 board connects to).
3. In Project Overview, click **Add App** and select the **Android** icon.
4. Set the **Android package name** to exactly:
   ```
   com.smartswitchboard.app
   ```
5. (Optional) Set the App nickname to `Smart Switchboard`.
6. Click **Register app**.
7. Download the `google-services.json` file.
8. Place `google-services.json` into the `app/` directory of this project:
   ```
   app/google-services.json
   ```
9. **Security Note:** Add `google-services.json` to your `.gitignore` and **NEVER commit it to public repositories**, as it contains your project identifier and client keys.
10. In Firebase Console, ensure **Authentication** -> **Sign-in method** has **Email/Password** enabled. Create user account(s) under the **Users** tab.
11. Under **Realtime Database** -> **Rules**, configure:
    ```json
    {
      "rules": {
        "switchboard1": {
          ".read": "auth != null",
          ".write": "auth != null"
        }
      }
    }
    ```

---

## 2. How to Build & Install

### Prerequisites
- Android Studio Ladybug / Koala or newer with JDK 17/21.
- Android device or emulator running Android 8.0+ (API 26 to Android 14/15/16).

### Building a Debug APK
Run the following Gradle command in the root project directory:
```bash
gradle :app:assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Building a Signed Release APK (Minified under 8MB with R8)
Run:
```bash
gradle :app:assembleRelease
```
The minified, optimized APK will be located at:
`app/build/outputs/apk/release/app-release.apk`

### Installing on your Phone
1. Connect your phone via USB and enable **USB Debugging** in Developer Options.
2. Install directly via ADB:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   Or transfer `app-release.apk` / `app-debug.apk` to your phone's storage and tap it to install.

---

## 3. How the Database Paths Work

The app interfaces directly with the existing ESP8266 schema on Firebase Realtime Database:

| Path | Type | Description |
| :--- | :--- | :--- |
| `/{boardId}/relay1` | `boolean` | State of Relay 1 (`true` = ON, `false` = OFF) |
| `/{boardId}/relay2` | `boolean` | State of Relay 2 (`true` = ON, `false` = OFF) |
| `/{boardId}/relay3` | `boolean` | State of Relay 3 (`true` = ON, `false` = OFF) |
| `/{boardId}/relay4` | `boolean` | State of Relay 4 (`true` = ON, `false` = OFF) |
| `/{boardId}/lastSeen` | `number` (optional) | Epoch timestamp in milliseconds written by ESP8266 heartbeat |
| `/.info/connected` | `boolean` | Firebase client connection status |
| `/.info/serverTimeOffset` | `number` | Time sync offset used to evaluate board online status |

- **Default Board ID:** `switchboard1` (can be changed in the app under **Settings -> Advanced Hardware Config** without recompiling).
- **Multi-Switch Atomic Updates:** "All ON", "All OFF", and Scenes execute a single atomic `updateChildren()` call.
- **Zero Background Drain:** The Realtime listener is attached only while the app is active and completely detached when backgrounded.
- **Offline Protection:** Firebase disk persistence is explicitly disabled so commands are never stale-queued when offline.

---

## 4. Customizing Switches or Adding More Relays

1. **Changing Default Names & Icons in App:**
   - Long-press any switch card on the main screen to rename it and choose between 8 appliance icons (Bulb, Fan, Plug, TV, Lamp, AC, Heater, Speaker).
2. **Changing Default Fallback Names in Code:**
   - In `app/src/main/java/com/example/data/PreferencesRepository.kt`:
     Edit lines with `prefs[Keys.SWITCH1_NAME] ?: "Switch 1"`.
3. **Scaling from 4 to 8 or More Relays:**
   - In `app/src/main/java/com/example/ui/MainViewModel.kt`:
     Update the range `(1..4)` in `switches` Flow to `(1..8)` and adjust atomic maps in `setAllSwitches` and `applyScene`.
   - In `app/src/main/java/com/example/ui/MainScreen.kt`:
     The `LazyVerticalGrid` dynamically accommodates any number of switches in 2-column (portrait) or 4-column (landscape) mode.
   - For additional Quick Settings tiles, create additional `SwitchTileService` classes in `com.example.tiles` and declare them in `AndroidManifest.xml`.
