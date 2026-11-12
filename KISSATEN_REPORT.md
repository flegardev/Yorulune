> Repository handoff: source, assets and reviewed QA summaries are prepared for GitHub synchronization on 12 November 2026. The audit below records the preceding local verification run. Its statements about no remote push describe that earlier run. Source publication does not constitute an Android or Play release. Original recovery archives and raw runtime records remain private on the workstation.

# Yorulune — implementation and verification

Status: **BLOCKED for production**. Source implementation and local QA are reviewable. Owner upload credentials and the published signing identity were not available in these supplied projects; this is not a signed Play release. Branding remains provisional. No publication, repository rename, remote push, key creation/replacement, app uninstall or personal-phone test was performed.

## Real changes

- Replaced the inherited unverified widget latency/NLP headline with accurate offline task-capture copy; this is a local parser, not an AI feature.
- Condensed the decorative welcome panel/date heading at large font scales and kept phone navigation labels on one line. Preserved task layout/filter and calendar view/day selections across recreation; the device check recreates both Kanban and Weekly calendar views.
- Removed unused cloud/AI/authentication build scaffolding and retired a pre-existing unused Google configuration containing an API-key-form value into private recovery storage. No key reset/revocation or external account change occurred.
- Preserved main tabs, task filters/layout and the selected studio page through activity recreation. The device regression now recreates Tasks and checks that its title/search remain visible, rather than merely checking that the bottom navigation still exists.
- Added private timer recovery with explicit paused restart, monotonic elapsed time, selected-task/sound recovery and reboot-safe clock handling. Corrected early focus logging, which previously credited the full planned session; new duration-policy tests cover early finish, natural completion, stopwatch and extreme inputs.
- Rebranded visible app, launcher, native widget and export filenames to Yorulune while preserving com.aistudio.orbitproductivity.vxqpmr, Room v2, all existing persisted fields and technical actions.
- Added original café/star/orbit artwork, local licensed Japanese-designed fonts, original adaptive/monochrome/density/store icons, a feature graphic and semantic light/dark/system/OLED surfaces.
- Fixed a real blank Tasks screen caused by a horizontal toolbar consuming all header width. Separated the workspace title/search and scrolling controls; added a device assertion for All Tasks and the search field. Reflowed the date header controls and corrected system-bar icon contrast.
- Added optional #RRGGBB accent support in the existing backed-up accentPreset field. Kept named palette strings and schema intact; native widget actions use legible contrasting ink.
- Added confirmation before reset/empty-trash actions. Prevented starter data from reappearing in a deliberately emptied existing workspace.
- Moved document generation, provider I/O, backup inspection/import and previews off the UI thread. Exports are generated from the ViewModel after the picker returns, avoiding a lost Activity-local payload during configuration recreation.
- Added 16 MiB bounded UTF-8 document reads, malformed-UTF-8 rejection and a JSON nesting limit of 64. Preserved transactional restore/merge and ICS compatibility.
- Disabled automatic cloud/device-transfer backup and replaced the unimplemented WebDAV settings placeholder with an explanation of working user-chosen document exports. No functioning network sync implementation was removed. Enabled Java time desugaring for the existing minSdk 24.

The family design system and scope are documented in [BRAND_GUIDE](verification/BRAND_GUIDE.md). Original source recovery snapshots and SHA-256 manifests are in `../kissaten/baseline`. Retired unused assets remain recoverable there. These directories arrived without Git metadata; no history was initialized or replaced.

## Verification results

| Check | Result | Evidence |
|---|---|---|
| Latest host build task batch | Exit 0 | [exact command/log metadata](verification/evidence/copy-final.json) |
| Host unit/Robolectric tests | 32 tests, 0 failures/errors | [suite totals](verification/evidence/host-tests.json) |
| Debug Android lint | 0 errors; 73 warnings retained | [individual findings](verification/evidence/lint-summary.json) |
| Device instrumentation | 1 tests; passed=True | [runner result](verification/evidence/instrumentation/KissatenDeviceTest.json) |
| Package, namespace, preference/action identifiers | Preserved | [compatibility audit](verification/evidence/compatibility.json) |
| Release bundle | Built; unsigned=True | [hash, native ELF and signature audit](verification/evidence/artifact-audit.json) |
| Runtime dependency inventory | 83 resolved Maven artifacts | [coordinates, purposes and licenses](verification/evidence/dependency-inventory.json) |
| OSV Maven lookup | 0 returned matches; database coverage is limited | [response summary](verification/evidence/osv-audit.json) |
| Store phone screenshots | 7 genuine selected captures | [provenance](play-store/SCREENSHOT_PROVENANCE.json) |
| process-death | PASS | [JSON](verification/evidence/process-death.json) |
| upgrade-data | PASS | [JSON](verification/evidence/upgrade-data.json) |
| document-flow | PASS | [JSON](verification/evidence/document-flow.json) |
| timer-process-death | PASS | [JSON](verification/evidence/timer-process-death.json) |

Host suite details:

| Suite | Tests | Failures/errors |
|---|---|---|
| com.example.DocumentSafetyTest | 3 | 0 |
| com.example.ExampleRobolectricTest | 10 | 0 |
| com.example.ExampleUnitTest | 10 | 0 |
| com.example.FocusDurationTest | 4 | 0 |
| com.example.FocusRecoveryTest | 4 | 0 |
| com.example.KissatenContrastTest | 1 | 0 |

Exact latest full host-check wrapper command (Windows):

```powershell
Set-Location -LiteralPath '<workspace>\orbit (1)'
& "<workspace>\orbit (1)\gradlew.bat" --no-daemon --no-configuration-cache --console plain --max-workers=1 -I <workspace>\kissaten\tools\qa.init.gradle assembleDebug testDebugUnitTest lintDebug bundleRelease -PkissatenUnsignedRelease=true
```

The QA init script reuses this workstation's existing debug certificate and optionally suppresses unavailable release signing for validation only. It never generates or changes a key. Source signing blocks are byte-for-byte unchanged. The Java test runtime is complete Android Studio JBR 25, Robolectric 4.17; compileSdk 37, targetSdk 36 and original minSdk/versionCode are preserved. `--no-configuration-cache`, one worker and nonpersistent daemons avoid the observed build-tool/cache/file-lock failures. Earlier failed build/test logs remain in evidence; only the successful latest batch supports the final build claim.

Device commands from the family workspace:

```powershell
Set-Location -LiteralPath '<workspace>'
python kissaten/tools/device.py install orbit
python kissaten/tools/instrument.py orbit com.example.KissatenDeviceTest
python kissaten/tools/matrix.py orbit
python kissaten/tools/runtime_data.py orbit
python kissaten/tools/upgrade.py orbit
```

Device evidence is bounded to `emulator-5554`, boot-completed `HydroNudge_Pixel10a_API37`, Android API 37. Tests use direct install-r/instrument, not a connected Gradle task that might uninstall the app. Resolution/density/font changes simulate several layouts on this one emulator. They do not establish minimum-API, OEM, physical-tablet or production-certificate behavior. See [device matrix](verification/evidence/device-matrix.json), per-run logs and [gallery](verification/GALLERY.html).



Runtime license/notice copies and the inventory are packaged in the final APK/AAB assets as well as the source. The [license audit](verification/research/DEPENDENCY_LICENSES.md) distinguishes Yorulune's GPL-with-Classpath-Exception backport library. The [runtime limits](verification/research/RUNTIME_LIMITS.md) retain the system/display failures and investigation. A successful later test is bounded evidence, not a declaration that those causes are solved.

## Data compatibility

- Version 2, identity `25dab622851e57e68e5f990f51ae552f`: Original compiled APK contains identical Room identity hash; source export was missing.

The original compiled APK and transformed debug APK are round-tripped with install-r, with fixture Room row counts/digests and SQLite integrity checks recorded. This verifies source-version fixture compatibility under the workstation QA certificate. It does not prove an update from every historical store version or the owner's production signing key. Supplied older schema exports are incomplete; available migration fixtures are tested without inventing missing historical artifacts. Raw fixture databases/JSON are in private-runtime evidence and excluded from store assets. The existing Hydro UI fixture temporarily replaces emulator Room rows under snapshot/finally-restore; fixture-only document restores also replace or merge emulator records. No adb clear-data action or production/personal database reset occurred.

## Screenshot review

Earlier screenshots exposed a blank Tasks content area and a vertically wrapped date heading. Both header layouts were corrected; the device regression now explicitly checks visible task title/search. The actual final matrix, calendar variants and platform widget captures should be inspected in the gallery. Ellipsis is intentional for long record titles; it is not a claim of exhaustive accessibility compliance.

Use the [gallery](verification/GALLERY.html) for standard/small/large/tablet, 150% font, theme, offline restart and secondary-screen captures actually present. Historical prototypes and old pulled screenshots are excluded. Store captures are 1080x1920 RGB PNGs with source hashes and pixel equality after RGB encoding; no UI compositing or feature simulation. Icon: 512x512 RGB; feature graphic: 1024x500 RGB. See [play-store](play-store) and [asset provenance](play-store/ASSET_PROVENANCE.md).

## Privacy, dependencies and security

Merged debug/release manifests were inspected. Release permissions: `android.permission.VIBRATE`, `android.permission.POST_NOTIFICATIONS`, `android.permission.RECEIVE_BOOT_COMPLETED`, `com.aistudio.orbitproductivity.vxqpmr.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. Internet permission is absent. Automatic backup is disabled and cloud/device-transfer rules exclude local app data. Core persistence remains private Room/DataStore. User-created exports are unencrypted; a document provider selected by the user may store them remotely, as disclosed in the app and privacy draft.

No advertising, billing, login, remote AI, cloud database, external analytics or network-client SDK surfaced in the runtime inventory/source inspection. A redacted source-pattern scan found no private-key/token signatures, destructive Room fallback, mutable pending intents or the checked unbounded document-read pattern. This is a bounded audit, not a security certification. [Manifest components](verification/evidence/manifest-audit.json), [source scan](verification/evidence/source-security-scan.json), [licenses](THIRD_PARTY_NOTICES.md) and [original artifact notices](licenses/notice-provenance.json) remain reviewable. Font SIL OFL notices are bundled; source font files are unmodified.

All included 64-bit native ELF LOAD segments meet the measured 16 KiB alignment check. Complete installed-APK/Play validation is a separate release gate. Zero OSV matches means none returned for the queried Maven coordinates, not that vulnerabilities cannot exist. Lint warnings remain visible, including inherited hardcoded-string/deprecation/update recommendations; no lint baseline was added to hide them.

## Remaining release blockers

- Verify owner upload/app-signing certificates, obtain the existing signing credentials securely, choose an eligible increasing versionCode and build/verify the production-signed artifact. Current AAB is unsigned and must not be uploaded as release-ready.
- Complete Google Play/name/logo similarity and jurisdiction-specific trademark clearance. Exact searches and zero returned Apple Slovenia results are limited evidence. See [brand investigation](verification/research/BRAND_CLEARANCE.md).
- Supply legal owner/support details and a controlled public privacy-policy URL; review real Data safety, audience, content rating and all declarations. Draft placeholders are not submission-ready.
- Verify the owner's actual Play Console state, account eligibility and required testing. No testers, opt-ins, feedback, review approval or production access are claimed. Mizupuku additionally needs the health/wellness declaration and the existing account-eligibility question resolved.
- Repeat display/font stress on a stable platform and physical devices. The Hydro input ANR and later Android system-server/display transition failure remain in the evidence; their root cause is not established by this run. Cold-profile successes do not erase them.
- Expand minimum-API/OEM, TalkBack/switch-access, landscape/foldable, extreme text/locale, long-running timer/process death, periodic reminder/Doze/reboot/exact-alarm permission and full historical production-upgrade coverage. Check every relevant secondary screen/palette/widget action before making an exhaustive claim.
- Complete final legal notice/redistribution review and installed APK/Play native-page-size validation. Runtime artifact metadata and source license inventory are prepared, not a legal certification.

No owner-only approval was requested because no publication, signing-identity change or account mutation was attempted. Technical and owner gates keep the final status **BLOCKED**.

## Changed files

Machine-readable [changed-files.json](verification/evidence/changed-files.json) compares each retained source file against the pre-edit SHA-256 manifest, excluding build caches. Retired files are recoverable from the original snapshot.

### changed

- `.env.example`
- `.gitignore`
- `README.md`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/MainActivity.kt`
- `app/src/main/java/com/example/data/local/OrbitLocalDatabase.kt`
- `app/src/main/java/com/example/data/model/OrbitModels.kt`
- `app/src/main/java/com/example/data/notification/OrbitReminderScheduler.kt`
- `app/src/main/java/com/example/ui/screens/AuthAndOnboardingScreen.kt`
- `app/src/main/java/com/example/ui/screens/CalendarAndFocusScreen.kt`
- `app/src/main/java/com/example/ui/screens/MissionControlScreen.kt`
- `app/src/main/java/com/example/ui/screens/MoreAndEcosystemScreen.kt`
- `app/src/main/java/com/example/ui/screens/WorkspaceAndViewsScreen.kt`
- `app/src/main/java/com/example/ui/theme/Color.kt`
- `app/src/main/java/com/example/ui/theme/Theme.kt`
- `app/src/main/java/com/example/ui/theme/Type.kt`
- `app/src/main/java/com/example/ui/viewmodel/OrbitViewModel.kt`
- `app/src/main/java/com/example/widget/OrbitHomeWidgetProvider.kt`
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/layout/orbit_appwidget.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/mipmap-hdpi/ic_launcher.png`
- `app/src/main/res/mipmap-hdpi/ic_launcher_round.png`
- `app/src/main/res/mipmap-mdpi/ic_launcher.png`
- `app/src/main/res/mipmap-mdpi/ic_launcher_round.png`
- `app/src/main/res/mipmap-xhdpi/ic_launcher.png`
- `app/src/main/res/mipmap-xhdpi/ic_launcher_round.png`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.png`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/test/java/com/example/ExampleRobolectricTest.kt`
- `build.gradle.kts`
- `gradle.properties`
- `gradle/libs.versions.toml`
### added

- `KISSATEN_REPORT.md`
- `THIRD_PARTY_NOTICES.md`
- `app/schemas/com.example.data.local.OrbitLocalDatabase/2.json`
- `app/src/androidTest/java/com/example/KissatenDeviceTest.kt`
- `app/src/debug/AndroidManifest.xml`
- `app/src/debug/java/com/example/qa/WidgetPreviewActivity.kt`
- `app/src/main/assets/licenses/ShipporiMincho-OFL.txt`
- `app/src/main/assets/licenses/ZenMaruGothic-OFL.txt`
- `app/src/main/assets/third_party/THIRD_PARTY_NOTICES.md`
- `app/src/main/assets/third_party/dependency-inventory.json`
- `app/src/main/assets/third_party/licenses/Apache-2.0.txt`
- `app/src/main/assets/third_party/licenses/artifact-notices/4b9abebc4338048a7c2dc184e9f800deb349366bdf28eb23c2677a77b4c87726.txt`
- `app/src/main/assets/third_party/licenses/artifact-notices/68834f116f8ff545f05d14753357b620748156d60ee36b26beab4cb3f317efe4.txt`
- `app/src/main/assets/third_party/licenses/artifact-notices/809fa1ed21450f59827d1e9aec720bbc4b687434fa22283c6cb5dd82a47ab9c0.txt`
- `app/src/main/assets/third_party/licenses/notice-provenance.json`
- `app/src/main/java/com/example/data/DocumentSafety.kt`
- `app/src/main/java/com/example/data/FocusRecoveryStore.kt`
- `app/src/main/java/com/example/data/model/FocusDuration.kt`
- `app/src/main/java/com/example/ui/theme/Kissaten.kt`
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- `app/src/main/res/font/shippori_regular.ttf`
- `app/src/main/res/font/zen_maru_bold.ttf`
- `app/src/main/res/font/zen_maru_regular.ttf`
- `app/src/test/java/com/example/DocumentSafetyTest.kt`
- `app/src/test/java/com/example/FocusDurationTest.kt`
- `app/src/test/java/com/example/FocusRecoveryTest.kt`
- `app/src/test/java/com/example/KissatenContrastTest.kt`
- `gradle/gradle-daemon-jvm.properties`
- `licenses/Apache-2.0.txt`
- `licenses/artifact-notices/4b9abebc4338048a7c2dc184e9f800deb349366bdf28eb23c2677a77b4c87726.txt`
- `licenses/artifact-notices/68834f116f8ff545f05d14753357b620748156d60ee36b26beab4cb3f317efe4.txt`
- `licenses/artifact-notices/809fa1ed21450f59827d1e9aec720bbc4b687434fa22283c6cb5dd82a47ab9c0.txt`
- `licenses/notice-provenance.json`
- `play-store/ASSET_PROVENANCE.md`
- `play-store/SCREENSHOT_PROVENANCE.json`
- `play-store/feature-1024x500.png`
- `play-store/icon-512.png`
- `play-store/icon-source.svg`
- `play-store/listing-en.json`
- `play-store/privacy-policy-draft.md`
- `play-store/release-notes-en.txt`
- `play-store/screenshots/phone/01-light-home.png`
- `play-store/screenshots/phone/02-tasks.png`
- `play-store/screenshots/phone/03-calendar.png`
- `play-store/screenshots/phone/04-habits.png`
- `play-store/screenshots/phone/05-focus.png`
- `play-store/screenshots/phone/06-dark-home.png`
- `play-store/screenshots/phone/07-oled-home.png`
- `play-store/screenshots/phone/README.md`
### retired

- `app/google-services.json`
- `app/src/main/res/drawable/img_orbit_hero_1794326526728.jpg`
- `app/src/main/res/drawable/img_orbit_launcher_fg_1794326523639.jpg`
- `app/src/main/res/font/jetbrains_mono.ttf`
- `app/src/main/res/font/playfair_display.ttf`
- `app/src/main/res/font/plus_jakarta_sans.ttf`
- `app/src/main/res/font/space_grotesk.ttf`
