# PocketLabApp

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
