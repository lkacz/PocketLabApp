# PocketLabApp

![CI](https://github.com/lkacz/PocketLabApp/actions/workflows/ci.yml/badge.svg)
<!-- Coverage badge placeholder (can be updated with shields.io + action in future) -->


Developer quick start.

## Prerequisites
- Android Studio / Android SDK (see `local.properties`)
- Java 17 runtime (Gradle 8.x compatible) or as required by Android Gradle Plugin
- PowerShell 5+ (Windows dev convenience scripts)

## First Run
```
git clone <repo>
cd PocketLabApp
./gradlew.bat assembleDebug
```
Or use the helper script:
```
powershell -ExecutionPolicy Bypass -File scripts/run-app.ps1
```

## Helper Scripts
- `scripts/run-app.ps1` – build, (re)start emulator, install & launch.
- `scripts/logcat.ps1` – filtered logcat (will create on first run if missing).

## Debug Tooling
- StrictMode enabled in debug to catch main-thread and VM violations.
- Timber for structured logging.
- PerfTimer utility (`PerfTimer.track("Label") { ... }`) for quick ad-hoc performance timing in logs.
- Dependency updates: run `./gradlew.bat :app:dependencyUpdates` to see newer versions.

### Developer Mode & Feature Flags
End-user UI is kept clean. A hidden developer mode (7 rapid taps on the designated area in the start screen) reveals:
- Feature Flags dialog (persisted via SharedPreferences)
- Dev Info dialog (environment + versions)
Long-press the same area to disable developer mode. Preview Protocol is always available (not gated) for broader usability.

### Protocol Validation & Transformation
The app includes a protocol preview & validation dialog:
- Performs deterministic transformation (merging multiline INPUTFIELD blocks, expanding SCALE / SCALE[RANDOMIZED], handling RANDOMIZE_ON blocks)
- Pure validator (`ProtocolValidator`) enabling unit tests and consistent UI results
- Validation features: caching, performance timing (PerfTimer), issue navigation (Prev/Next error buttons), summary counts (lines / errors / warnings), exportable textual report
- Rules validated: command recognition, label duplication, randomization pairing, TIMER structure & ranges, INPUTFIELD structure (including randomized minimum fields), alignment enum values, size positivity & reasonable upper bounds, hex color format (#RRGGBB / #AARRGGBB)
Add further rules + tests in `ProtocolValidatorTest.kt` before extending protocol syntax.

## VS Code Tasks
Press `Ctrl+Shift+B` to see the curated task list:

- **Run App (Pixel_API_34)** – builds the debug APK, ensures the Pixel_API_34 emulator (port 5580 with ANGLE) is running, installs, and launches the app.
- **Assemble Debug APK** – runs `:app:assembleDebug` without touching the emulator.
- **Logcat (App)** – tails filtered logcat output via `scripts/logcat.ps1`.
- **Deprecation Warnings** – assembles with `--warning-mode all` for Gradle deprecation insights.

These shell tasks call the PowerShell helpers under `scripts/` so they work out of the box on Windows.

## Code Style & Linting
Using `ktlint` via a Gradle plugin (see root plugin config). Run:
```
./gradlew.bat ktlintCheck
./gradlew.bat ktlintFormat
```

## Tests
Run unit tests:
```
./gradlew.bat testDebugUnitTest
```

### Continuous Integration
Every push / PR to `main` or `dev16` runs GitHub Actions workflow (`ci.yml`):
- Assemble debug build
- Unit tests (`testDebugUnitTest`)
- ktlint style check
- JaCoCo coverage report (artifact)
- Dependency update report (artifact)
Failed tests upload their HTML reports as artifacts for quick diagnosis.

### Protocol Transformation Tests
Protocol parsing / manipulation logic is covered by pure unit tests around `ProtocolTransformer`:
- Multi-line command merging (e.g. `INPUTFIELD;` blocks)
- RANDOMIZE_ON / RANDOMIZE_OFF shuffling blocks (including missing OFF fallback)
- SCALE / SCALE[RANDOMIZED] expansion with deterministic seeding in tests

Add new cases in `ProtocolTransformerTest.kt` to extend coverage before changing transformation rules.

## Updating Dependencies
List outdated (requires the versions plugin):
```
./gradlew.bat dependencyUpdates
```

## Troubleshooting
- If `No connected devices!` ensure emulator running (`scripts/run-app.ps1 -JustInstall` after).
- For deprecations: `./gradlew.bat --warning-mode all assembleDebug`.

---
Generated helper docs. Extend as project grows.
