# Publishing and Google registration

There is no separate "register with Google Assistant" portal. Getting this app
to users involves three independent tracks:

1. **Google Play developer account** — required to distribute the app.
2. **App Actions review** — the legacy `"Hey Google..."` route.
3. **AppFunctions Early Access Program** — the Gemini-era replacement.

Only track 1 is required for people to install and use the app. Track 2 is
currently a legacy Assistant feature. Track 3 is not generally available yet.

## 1. Google Play developer account

Sign up at <https://play.google.com/console/signup>.

Requirements:

- A Google Account and age 18+.
- A one-time **US$25** registration fee.
- Government ID and payment-card verification under your legal name.
- Choose **personal** or **organization**:
  - **Personal** accounts created after November 13, 2023 must run a closed
    test with **at least 12 testers opted in continuously for 14 days** before
    applying for production access. Personal accounts must also verify access
    to an Android device using the Play Console mobile app.
  - **Organization** accounts need a D-U-N-S number and business verification,
    but are not subject to the new-personal-account 12-tester rule.
- Play registration also covers Android developer verification for apps
  distributed through Play.

Do not pay for a "review service" or tester marketplace. The 12 testers must be
real opted-in users for 14 consecutive days.

## 2. Create the app in Play Console

In <https://play.google.com/console>:

1. **All apps → Create app**.
2. App name: `Calorie Tracker`.
3. Default language: `en-US`.
4. App or game: **App**. Free or paid: **Free**.
5. Accept the declarations.

The package name is permanent: `com.jacob.voicecalories`. It is already set in
`app/build.gradle.kts`.

Complete **App content**:

- Privacy policy URL.
- Ads declaration.
- Content rating questionnaire.
- Target audience and content.
- Data safety form.
- Health apps declaration if Google asks for one. Food and calorie logs may be
  treated as health data; answer accurately.
- Government apps, financial features, and other declarations as applicable.

You will need a public privacy policy URL. The app stores food entries locally
and can send the meal description plus the user's Gemini API key to Google's
Generative Language API.

## 3. Store listing assets

Prepare:

- App name: `Calorie Tracker` (max 30 characters).
- Short description (max 80 characters).
- Full description (max 4000 characters).
- App icon: 512 × 512 PNG, 32-bit.
- Feature graphic: 1024 × 500 PNG or JPEG.
- At least 2 phone screenshots (up to 8).
- Optional tablet screenshots.

The current launcher icon is a vector drawable. It is fine for the app itself,
but you still need a 512 × 512 listing icon and preferably an adaptive icon.

## 4. Sign the release build

Signing is already wired up. `app/build.gradle.kts` reads `keystore.properties`
from the project root and, when that file exists, applies it to release builds.

The local files are:

```text
upload-keystore.jks      # PKCS12 upload key, alias "upload"
keystore.properties      # passwords + path, mode 600
```

`keystore.properties` contains a randomly generated password. Read it once and
save it in your password manager:

```sh
cat keystore.properties
```

Do not paste that output into chat, email, or a bug report.

### Back up the key now

Copy both `upload-keystore.jks` and `keystore.properties` somewhere safe and
off this machine. If you lose them, you cannot sign updates for this app
listing without going through Google's upload-key reset process. Play App
Signing means Google holds the *app-signing* key, but you still need this
*upload* key for every future release.

### Wipe and start over

If nothing has been uploaded yet, you can safely discard the key. Delete the
old key, its properties, and any AAB signed with it:

```sh
cd /Users/jacob/Desktop/code/voiceCaloriesApp
rm -f upload-keystore.jks upload-keystore.previous-*.jks keystore.properties
rm -f app/build/outputs/bundle/release/app-release.aab
```

Generate a fresh PKCS12 key. For PKCS12 the store and key passwords must be the
same, so use one strong password:

```sh
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -storetype PKCS12 \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Then create `keystore.properties` with either the helper script:

```sh
./scripts/configure-signing.sh
```

or by hand:

```properties
storeFile=/Users/jacob/Desktop/code/voiceCaloriesApp/upload-keystore.jks
storePassword=YOUR_PASSWORD
keyAlias=upload
keyPassword=YOUR_PASSWORD
```

Lock the file down:

```sh
chmod 600 keystore.properties upload-keystore.jks
```

The signing config in `app/build.gradle.kts` looks like this:

```kotlin
import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

Never commit `keystore.properties` or `upload-keystore.jks`.

### Build the bundle

```sh
./gradlew :app:bundleRelease --console=plain
```

The signed bundle is written to:

```text
app/build/outputs/bundle/release/app-release.aab
```

You can also use Android Studio: **Build → Generate Signed Bundle / APK → Android
App Bundle**.

## 5. Testing tracks and production

1. **Internal testing** — upload the AAB, add up to 100 testers by email. No
   12-tester requirement. This is the fastest way to let friends install it.
2. **Closed testing** — required for a new personal account: at least 12
   testers opted in continuously for 14 days.
3. **Apply for production** — Play Console → Dashboard → **Apply for
   production**. Answer the questions about your test and app. Review usually
   takes seven days or less.
4. **Production** — publish to all users.

Internal testing is enough for personal use and small groups. It does not give
you a public Play Store listing or a public "Hey Google" action.

## 6. App Actions review (legacy Assistant route)

This is the part that would make `"Hey Google, open Calorie Tracker and log I
ate..."` work. It is separate from app review.

1. In Play Console, open the app.
2. Go to **Advanced settings → App Actions**.
3. Accept the App Actions terms of service.
4. Upload a bundle that contains `app/src/main/res/xml/shortcuts.xml`.
5. Google reviews the App Actions configuration separately from the app.

Requirements and limitations:

- The app must be published on Google Play.
- Custom intents are **en-US only**, and the device/Assistant language must
  match.
- The action is not available to public users until Google approves it.
- Google's own docs still describe this flow, but Google Assistant was removed
  from Android phones starting September 4, 2026 and replaced by Gemini.
  Custom App Actions are a legacy Assistant feature and may not be honored by
  Gemini.

The declared capability in this app is:

```text
custom.actions.intent.LOG_FOOD
```

Query patterns are in `app/src/main/res/values/strings.xml`. The app receives the
food text in the `meal` string extra. That part is already implemented and
tested through adb.

## 7. AppFunctions / Gemini (future route)

The forward-looking replacement is **AppFunctions** on Android 16. It exposes
Kotlin functions as tools that Gemini and other agents can call.

- Docs: <https://developer.android.com/ai/appfunctions>
- Early Access Program form: <https://forms.gle/GN5ybjQFhzHRCguM7>

As of May 2026, Gemini integration is in a private preview with trusted
testers. You can implement and test AppFunctions locally, but general
availability is not guaranteed yet. Registering interest in the EAP does not
automatically grant access.

A future implementation would expose something like:

```kotlin
@AppFunctionDeclaration(isDescribedByKDoc = true)
suspend fun logMeal(description: String): LoggedMeal
```

with KDoc describing the parameter and return value.

## 8. Current gaps in this project

Before a production release:

- [x] Release AAB is signed with the local upload key (`jar verified.`).
- [ ] No privacy policy URL.
- [ ] No Play Store listing text, 512 × 512 icon, feature graphic, or
      screenshots.
- [ ] Data safety and health declarations not completed.
- [ ] `versionCode` is 1, `versionName` is 1.0 — fine for the first upload.
- [ ] The app asks each user for their own Gemini API key. That is fine for a
      developer/personal build, but poor UX for a public app. For public users,
      either ship offline-only or add a backend proxy so the Gemini key is not
      embedded in the APK.
- [ ] "Hey Google" is not guaranteed even after publication, because Assistant
      has been replaced by Gemini.
- [x] Debug build, release build, and unit tests pass.
- [x] Target SDK 36 satisfies the current Play target API requirement.
