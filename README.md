# PocketLabApp

![CI](https://github.com/lkacz/PocketLabApp/actions/workflows/ci.yml/badge.svg)

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
- LeakCanary (debug only) will show a notification if a leak is detected.
- PerfTimer utility (`PerfTimer.track("Label") { ... }`) for quick ad-hoc performance timing in logs.
- Dependency updates: run `./gradlew.bat :app:dependencyUpdates` to see newer versions.

## VS Code Tasks
Press `Ctrl+Shift+B` to see tasks once `.vscode/tasks.json` is present. Includes run-app, logcat, and deprecation check.

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
