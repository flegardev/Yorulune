# Yorulune build and release

| Package | Play Alpha version observed November 25, 2026 | Update |
| --- | --- | --- |
| com.tiniflegar.yorulune | 1.0 (1) | 1.1.0 (2) |

Use JetBrains Runtime 25, the Android SDK with API 37, and the checked-in Gradle wrapper. The app targets API 36 and supports API 24 and later.

For debug builds, provide a local QA-only keystore as `debug.keystore` in the repository root with the conventional Android debug alias and password. Use the same QA key on both revisions for local upgrade tests. Use a disposable Android emulator for tests that create data or install APKs.

```powershell
.\gradlew.bat --no-daemon --no-configuration-cache :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
```

Set the existing upload keystore path as `KEYSTORE_PATH` and inject `STORE_PASSWORD` and `KEY_PASSWORD` through your local signing environment. The Gradle signing configuration uses alias `upload`. For a different verified alias, use the Android Studio signing wizard. Check the upload certificate SHA-256 in Play Console before signing. Compare it with the signed bundle certificate, run bundletool validation, inspect the bundle manifest and check page-size compatibility before uploading to Alpha.

Play Console showed the Alpha release available to testers and production access pending. A production-access application requires at least 12 opt-ins sustained for at least 14 days.
