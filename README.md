# Calorie Tracker

A small Android calorie-tracking app built with Kotlin and Jetpack Compose. Say or
type what you ate, and the app estimates calories and macros, stores everything in
a local Room database, and posts an end-of-day summary notification.

The target invocation is:

> "Hey Google, open Calorie Tracker and log I ate an apple, a diet coke, and a
> turkey sandwich."

There are two voice paths:

1. **In-app mic, launcher shortcut, or deep link** — works today on any device,
   with no Play Store review.
2. **App Actions custom intent** — the one-sentence "Hey Google..." command
   above. It is declared in the app, but end users cannot use it until the app
   is published on Google Play and Google reviews/approves the action. See the
   current-status note below before relying on it.

## What it does

- Logs a free-form meal description such as `an apple, a diet coke, and a turkey sandwich`.
- Splits the description into individual foods and shows calories, protein, carbs,
  fat, fiber, sugar, and sodium.
- Estimates nutrition with the Gemini API when an API key is configured.
- Falls back to a built-in offline food database (about 80 common foods) when there
  is no key, no network, or Gemini fails.
- Stores the food log locally in Room. Nothing is synced to a server.
- Shows a daily total and progress against a calorie goal.
- Sends a daily summary notification with totals, remaining calories, macros, and
  a few suggestions. Default time is 21:00 and can be changed in Settings.
- Supports undo after logging, editing entries, deleting entries, and sending a
  test notification.

## Requirements

- Android Studio or a command-line Android SDK.
- JDK 17.
- Android SDK Platform 36 (`compileSdk`/`targetSdk` 36, `minSdk` 26).
- An Android device or emulator running Android 8.0+.
- Optional: a Gemini API key from
  <https://aistudio.google.com/app/apikey>.

## Run the emulator

### Android Studio

1. Open Android Studio.
2. Open **Tools → Device Manager**.
3. Click the play button next to an existing AVD, or **Create Virtual Device**
   and pick a Pixel device with an API 36 system image.
4. Once it boots, press **Run** to install and launch the app.

If the emulator starts but no separate window appears, Android Studio may have
launched it inside its **Running Devices** tool window. Open
**View → Tool Windows → Running Devices** to see the screen. To use a standalone
window instead, turn off **Settings → Tools → Emulator → Launch in a tool
window** (wording varies by Android Studio version) and start the AVD again.

### Command line

List the configured AVDs:

```sh
~/Library/Android/sdk/emulator/emulator -list-avds
```

Start one with a visible window:

```sh
~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.0
```

Wait for it to finish booting:

```sh
adb wait-for-device
adb shell getprop sys.boot_completed
```

`sys.boot_completed` should print `1`. Then install and launch the app:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.jacob.voicecalories/.MainActivity
```

To stop it:

```sh
adb -s emulator-5554 emu kill
```

Useful flags:

- `-gpu host` uses hardware acceleration on most Macs.
- `-wipe-data` starts from a clean device (deletes all emulator data).
- `-no-snapshot` always cold-boots; omit it to use quick-boot snapshots.
- `-no-window` runs headless. If you started an emulator this way, you will
  not see a window and must restart it without that flag.

## Build and install

```sh
./gradlew :app:assembleDebug --console=plain
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

If `adb` is not on your `PATH` on macOS, use
`~/Library/Android/sdk/platform-tools/adb`.

## Configure Gemini (optional)

1. Open the app and tap the settings gear.
2. Paste a Gemini API key into **Gemini API key**.
3. Leave **Gemini model** at the default `gemini-2.5-flash`, or enter another
   Gemini model that supports structured JSON output.
4. Tap **Save**.

The key is stored in DataStore on the phone and is sent only to the Gemini API
at `generativelanguage.googleapis.com`. The app never sends the key anywhere else.

Without a key the app still works using the built-in food database. The built-in
estimator is intentionally simple, so mixed or unusual meals are much more
accurate with Gemini.

To quickly test an API key without the UI, see the Gemini REST endpoint used by
`GeminiClient.kt`:

```text
POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=YOUR_KEY
```

## Use it

### 1. Assistant / App Actions

Capability name: `custom.actions.intent.LOG_FOOD`, declared in
`app/src/main/res/xml/shortcuts.xml`.

Example phrases:

- "Hey Google, open Calorie Tracker and log I ate an apple, a diet coke, and a turkey sandwich."
- "Hey Google, open Calorie Tracker and add a banana to my food log."
- "Hey Google, open Calorie Tracker and track a cookie."

The Assistant passes the spoken food description to `MainActivity` in the `meal`
string extra. The app then estimates and stores it.

> **Current status (November 2026): the "Hey Google" route is not usable yet.**
> App Actions are only registered for apps published on Google Play, and the
> action must be reviewed and approved by Google. Custom intents are also
> `en-US` only. An unpublished debug build can be invoked through adb (see
> below), but a real "Hey Google" request will not reach it.
>
> Google has also removed Google Assistant from Android phones starting
> September 4, 2026 and replaced it with Gemini. Custom App Actions are a legacy
> Assistant feature. The forward-looking equivalent is
> [AppFunctions](https://developer.android.com/ai/appfunctions) on Android 16,
> which is currently an experimental/private preview with Gemini. Treat the
> custom intent below as legacy best-effort, not a shipping guarantee.
>
> The reliable user-facing path today is the in-app mic, the launcher shortcut,
> or a deep link. Those use Android's speech recognizer directly and need no
> Google approval.

### 2. Launcher shortcut and deep links

Long-press the app icon and choose **Log food by voice**. This opens the app and
starts speech recognition immediately.

Deep links:

```text
voicecalories://listen          open the app and start listening
voicecalories://log?meal=...    log the meal text in the query parameter
```

### 3. In-app mic and typing

Open the app and tap **Log food** / **Speak a meal** to use Android speech
recognition. Tap **Type instead** to enter a meal manually. This works with no
Assistant, no Play Store, and no Gemini key.

## End-of-day notification

- Enabled by default at 21:00 local time.
- Scheduled with WorkManager as unique periodic work (`daily_summary_work`), so it
  survives app restarts and reboots.
- Uses the `daily_summary` notification channel, created at app startup.
- Includes a **Log another meal** action that opens the app and starts listening.
- Can be changed or disabled in **Settings → Daily summary**.
- **Send test notification** posts the summary immediately using today's entries.

Android 13+ requires the `POST_NOTIFICATIONS` permission. The app asks on first
launch and also shows an **Allow notifications** button in Settings if needed.

Note that Android may deliver periodic work a few minutes late; this is normal
WorkManager behavior.

## Test with adb

Start the app normally:

```sh
adb shell am start -n com.jacob.voicecalories/.MainActivity
```

Log a meal through the deep link (cold start or warm start):

```sh
adb shell am start -a android.intent.action.VIEW \
  -d "voicecalories://log?meal=an%20apple%20and%20a%20turkey%20sandwich" \
  -n com.jacob.voicecalories/.MainActivity
```

Open the app and start listening:

```sh
adb shell am start -a android.intent.action.VIEW \
  -d "voicecalories://listen" \
  -n com.jacob.voicecalories/.MainActivity
```

Simulate the App Actions custom intent (the `meal` string extra):

```sh
adb shell "am start -a android.intent.action.VIEW \
  -n com.jacob.voicecalories/.MainActivity --es meal 'a cookie'"
```

Quote the whole device command as shown. If the quotes are stripped before the
device shell sees them, adb sends `--es meal a cookie`, and only `a` becomes the
extra value.

Send a test notification from the UI, or inspect notifications directly:

```sh
adb shell dumpsys notification --noredact | grep -A8 com.jacob.voicecalories
```

Check that the daily summary worker is scheduled:

```sh
adb shell dumpsys jobscheduler | grep -A12 'com.jacob.voicecalories'
```

Look for `DailySummaryWorker` and a minimum latency matching the next 21:00.

## Emulator walkthrough

This is the quickest end-to-end test on an emulator (Pixel API 36).

### 1. Build, install, launch

```sh
./gradlew :app:assembleDebug --console=plain
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.jacob.voicecalories/.MainActivity
```

The home screen should show today's total (0 kcal on a fresh install) and the
**Log food** button.

### 2. Log a meal by typing

1. Tap **Log food**.
2. Tap **Type instead**.
3. Enter `an apple, a diet coke, and a turkey sandwich` and confirm.

Expected offline result: 475 kcal total (95 + 0 + 380). The entry appears in the
list and the daily total updates.

### 3. Test the voice path

Tap **Log food** and allow microphone access, or long-press the app icon and
choose **Log food by voice**.

The emulator needs a working speech-recognition provider. If the recognizer
shows an error or only a Google voice-data screen:

- Open the emulator's **Extended controls → Microphone** and enable
  **Virtual microphone uses host audio input**.
- Make sure **Speech Services by Google** is installed/enabled.
- Speech recognition quality varies by emulator image. If it still does not
  work, use **Type instead**, the deep links, or the `meal` extra below — those
  exercise the same logging path.

### 4. Test the launcher shortcut

Long-press the app icon; **Log food by voice** / **Speak a meal and log it**
should appear. You can confirm registration without the UI:

```sh
adb shell cmd shortcut get-shortcuts com.jacob.voicecalories
```

### 5. Test deep links and the App Actions payload

Deep link (cold or warm start):

```sh
adb shell am start -a android.intent.action.VIEW \
  -d "voicecalories://log?meal=a%20banana" \
  -n com.jacob.voicecalories/.MainActivity
```

Start listening:

```sh
adb shell am start -a android.intent.action.VIEW \
  -d "voicecalories://listen" \
  -n com.jacob.voicecalories/.MainActivity
```

Simulate the exact extra Assistant sends:

```sh
adb shell "am start -a android.intent.action.VIEW \
  -n com.jacob.voicecalories/.MainActivity --es meal 'a cookie'"
```

Quote the whole device command. Otherwise adb splits the value and only `a`
becomes the meal.

### 6. Test Gemini and the fallback

1. Open **Settings** and paste a valid Gemini API key.
2. Log a mixed meal and check that values look reasonable.
3. To verify the fallback, replace the key with an invalid value and log again:
   the entry should still be saved using the offline database. Watch logcat:

```sh
adb logcat -s GeminiClient
```

A bad key logs `Gemini HTTP 400: API key not valid...` and the app continues
offline.

### 7. Test notifications and the daily worker

Grant the Android 13+ permission, then use **Settings → Send test notification**:

```sh
adb shell pm grant com.jacob.voicecalories android.permission.POST_NOTIFICATIONS
adb shell dumpsys notification --noredact | grep -A8 com.jacob.voicecalories
```

To test the scheduled summary, set **Daily summary** to 1–3 minutes ahead, put
the app in the background, and wait. Do not rely on `cmd jobscheduler run -f`
for this worker. Confirm it is scheduled with:

```sh
adb shell dumpsys jobscheduler | grep -A12 'com.jacob.voicecalories'
```

Look for `DailySummaryWorker` and a minimum latency matching the next summary
time. Android may deliver periodic work a few minutes late.

### 8. Reset between runs

```sh
adb shell pm clear com.jacob.voicecalories
```

This removes the food log, settings, and the Gemini key but keeps the installed
APK. Uninstall/reinstall also resets notification permission.

### What cannot be tested in the emulator yet

A real **"Hey Google, open Calorie Tracker and log..."** invocation needs the app
published on Google Play and the App Actions custom intent approved by Google.
The adb commands above test the same intent delivery path without Assistant.

## Tests

Run the JVM unit tests:

```sh
./gradlew :app:testDebugUnitTest --console=plain
```

The parser tests in
`app/src/test/java/com/jacob/voicecalories/nutrition/LocalNutritionTest.kt`
verify the built-in estimator, including the example meal:

| Item | Calories |
| --- | ---: |
| Apple | 95 |
| Diet coke | 0 |
| Turkey sandwich | 380 |
| **Total** | **475** |

## Project structure

```text
app/src/main/java/com/jacob/voicecalories/
├── MainActivity.kt              deep links, meal extra, notification permission
├── VoiceCaloriesApp.kt          application setup and notification channel
├── data/                        Room entities/DAO/database, DataStore settings
├── nutrition/                   Gemini client, offline food database, orchestration
├── notifications/               WorkManager worker, scheduler, notification copy
└── ui/                          Compose screens, dialogs, ViewModel, theme
app/src/main/res/xml/shortcuts.xml   App Actions capability and launcher shortcut
app/src/main/AndroidManifest.xml     permissions, deep-link intent filters
```

## Privacy and limitations

- Food entries stay in a local Room database on the phone.
- `allowBackup=false` keeps the Gemini key out of Android cloud backups.
- Gemini requests contain the meal description and API key only.
- App Actions depend on Google Play publication, Google's review, and `en-US`.
- Nutrition values are estimates, not medical or dietary advice.
- The offline database is small; uncommon foods fall back to a generic
  "1 serving (estimated)" value.
