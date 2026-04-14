# Agent & contributor instructions

This file is for **human developers and any AI coding agent** working in this repository. Follow it for Java, builds, and device installs.

**Product context:** *Zeno Classic Dialer* targets **keyboard-centric, often square** Android phones (primary reference: **Zinwa Q25**). It expects to be the **default dialer** and uses **Telecom** (`InCallService`, call screening) accordingly. **`minSdk` is 29** (Android 10) for a practical default-dialer stack; **Unihertz Titan / Titan Elite** are *not* part of the regular test matrix. Shared preferences and feature flags are centralized in `AppPreferences.kt`—extend keys there rather than duplicating string literals across activities. The app supports **Original Classic**, **Modern Classic**, and **Pixel** themes.

## After every fix (required)

- **Always** build a **signed release** APK and **install that release on a connected device** after implementing or changing code (bug fixes, UI tweaks, refactors), unless the user explicitly says not to or asks for debug only.
- Requires **`keystore.properties`** in the project root (see `keystore.properties.example`) so `assembleRelease` is signed with the release keystore—not the debug key.
- Typical flow from the project root (with `JAVA_HOME` set to Android Studio’s JBR):

  ```bash
  ./gradlew assembleRelease
  adb install -r app/build/outputs/apk/release/zeno-classic-dialer-v2.0.1.apk
  ```

- Or use **Android Studio**: **Build → Generate Signed Bundle / APK** (or a release run configuration), then install `app-release.apk` with **`adb install -r`** as above.

## Java / JDK

- **Do not assume** a system `java` on `PATH` is configured for this project.
- **Use the JDK bundled with Android Studio** (JetBrains Runtime / embedded JBR), or the JDK explicitly selected under **Android Studio → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**.
- When running Gradle from a terminal, use an environment where that same JDK is available (e.g. **Terminal inside Android Studio**, or set `JAVA_HOME` to Android Studio’s JBR). Typical macOS path (adjust if your install differs):

  `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`

## Building

- **Prefer Android Studio** to sync Gradle and build: **Build → Make Project**, or **Build → Build Bundle(s) / APK(s)** as needed.
- For **device verification after changes**, default to **release**: `./gradlew assembleRelease` (signed; see `keystore.properties`).
- Command-line builds are fine **only** when the shell uses the same JDK as Android Studio (see above). For a quick **unsigned-of-release-pipeline** check only, `./gradlew assembleDebug` is allowed, but **install on device** should still use **signed release** per project policy unless the user requests debug.

## Installing on a device

- Use **`adb`** to install the built APK (USB debugging or authorized wireless debugging on the device).
- **Default for this project — signed release:**

  ```bash
  adb install -r app/build/outputs/apk/release/zeno-classic-dialer-v2.0.1.apk
  ```

- Debug APK (only when explicitly requested; different `applicationId` suffix `.debug` vs release):

  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```

- Use `adb devices` to confirm the device is listed before installing.

## Summary for agents

1. Treat **Android Studio’s JDK** as the source of truth for Java.  
2. **Build** signed release with **`./gradlew assembleRelease`** (or Android Studio equivalent) when validating changes.  
3. **Deploy** to hardware with **`adb install -r app/build/outputs/apk/release/zeno-classic-dialer-v2.0.1.apk`** — **always prefer signed release** for installs on device.  
4. **After every fix or code change:** **assembleRelease** + **adb install** the **release** APK (see **After every fix** above), unless the user opts out or explicitly wants debug.
