# Pocket Lab App (PoLA)

![CI](https://github.com/lkacz/PocketLabApp/actions/workflows/ci.yml/badge.svg)

Pocket Lab App (PoLA) is an Android toolkit for running field studies with scripted protocols, timers, and rich multimedia prompts. This repository combines the Android application, authoring utilities, and the Online Protocol Editor so researchers can design, validate, and execute studies end-to-end.

Use this README as a merged hub for both engineering tasks (builds, tests, release readiness) and facilitator-facing documentation (installation guidance, protocol syntax, and authoring resources).

---

## Repository snapshot

**About**  
No description, website, or topics provided.

**Resources**

- Readme
- License

**License**  
Unknown, GPL-3.0 licenses found

**Activity**

- Stars: 0 stars
- Watchers: 1 watching
- Forks: 0 forks

---

## Release status

- **Current version:** 1.0.0 (see [`CHANGELOG.md`](./CHANGELOG.md))
- **Build targets:** Debug and release builds validated with Gradle 8 / Java 17
- **Quality gates:** `ktlintCheck`, `testDebugUnitTest`, and `assembleRelease` must pass before tagging a release

Refer to the changelog for detailed highlights and known considerations for the 1.0.0 rollout.

## Developer quick start

```bash
git clone <repo>
cd PocketLabApp
./gradlew.bat assembleDebug
```

Or use the helper script:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-app.ps1
```

### Prerequisites

- Android Studio / Android SDK (configured via `local.properties`)
- Java 17 runtime (Gradle 8.x compatible)
- PowerShell 5+ on Windows for helper scripts

### Helper scripts

- `scripts/run-app.ps1` – build, (re)start emulator, install & launch.
- `scripts/logcat.ps1` – filtered logcat (created on first run if missing).

### Online Protocol Editor quick start

The editor lives in `OnlineProtocolEditor/` and expects to be served over HTTP(S) so browsers can fetch the template manifest and assets. Launch a simple local server from that directory:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/serve-editor.ps1 -OpenBrowser
```

Or run it manually:

```powershell
Set-Location OnlineProtocolEditor
python -m http.server 8080
```

Then open <http://localhost:8080/> in your browser (the script can do this for you with `-OpenBrowser`). The **Templates** dialog now reads from `templates/index.json` and loads the referenced `.txt` studies. When you finish, press `Ctrl+C` in the PowerShell window to stop the server.

> **Tip:** Keep `OnlineProtocolEditor/README.md` nearby for authoring workflows, validation tips, and hosting alternatives.

#### Add or customize templates

1. Edit `OnlineProtocolEditor/templates/index.json` to add a new entry with `id`, `name`, `summary`, and the corresponding `.txt` filename.
2. Drop the template source file into `OnlineProtocolEditor/templates/` and follow the existing naming style (kebab-case is preferred).
3. Refresh your browser tab. The editor re-fetches the manifest and surfaces the new template immediately.

If the manifest fails to load, the editor falls back to the bundled defaults and posts a warning in the console. Serve the directory over HTTP(S) to avoid `CORS` errors from `file://` URLs.

### Debug tooling

- StrictMode enabled in debug to catch main-thread and VM violations.
- Timber for structured logging.
- `PerfTimer.track("Label") { ... }` for ad-hoc performance timing in logs.
- Dependency updates: run `./gradlew.bat :app:dependencyUpdates` to check newer versions.

### Developer mode & feature flags

End-user UI stays streamlined. Tap the designated area in the start screen 7 times to reveal:

- Feature Flags dialog (SharedPreferences-backed)
- Dev Info dialog (environment + versions)

Long-press the same area to disable developer mode. Preview Protocol remains available to everyone.

### Protocol validation & transformation

The app includes protocol preview and validation:

- Deterministic transformation (multiline `INPUTFIELD`, `SCALE`, randomization blocks)
- Validator parity with UI and unit tests
- Issue navigation (Prev/Next), summary counts, exportable reports
- Rules cover command recognition, label duplication, randomization pairing, timer ranges, alignment enums, numeric bounds, and color formats

Extend validation rules alongside tests in `ProtocolValidatorTest.kt`.

### VS Code tasks

`Ctrl+Shift+B` shows curated tasks:

- **Run App (Pixel_API_34)** – builds debug APK, ensures emulator, installs, launches.
- **Assemble Debug APK** – runs `:app:assembleDebug` without emulator.
- **Logcat (App)** – tails filtered logcat via `scripts/logcat.ps1`.
- **Deprecation Warnings** – assembles with `--warning-mode all`.

### Code style & linting

Run `ktlint` via Gradle:

```bash
./gradlew.bat ktlintCheck
./gradlew.bat ktlintFormat
```

### Tests

```bash
./gradlew.bat testDebugUnitTest
```

### Continuous integration

Every push / PR to `main` or release branches runs `ci.yml`:

- Assemble debug build
- Unit tests (`testDebugUnitTest`)
- ktlint style check
- JaCoCo coverage report (artifact)
- Dependency report (artifact)

Failures publish HTML reports for quick diagnosis.

### Protocol transformation tests

`ProtocolTransformer` unit tests cover input merging, randomization, and scale expansion. Extend `ProtocolTransformerTest.kt` when changing transformation logic.

### Updating dependencies

```bash
./gradlew.bat dependencyUpdates
```

### Troubleshooting

- `No connected devices!` → ensure emulator running (`scripts/run-app.ps1 -JustInstall`).
- Deprecation warnings → `./gradlew.bat --warning-mode all assembleDebug`.

### License summary

- Pocket Lab App is released under the [GNU GPLv3](./LICENSE.txt).
- Protocol files and captured data stay local unless you export or sync them.
- Software provided **as is**; validate protocols, supervise fieldwork, follow institutional and legal requirements.

---

## Documentation hub

- `OnlineProtocolEditor/README.md` – live manual for the web-based editor, including template tags, favorites, validation workflows, and hosting tips.
- Offline copy: open the editor manual in your browser and export to PDF when you need a printable packet.
- Release notes: see [`CHANGELOG.md`](./CHANGELOG.md) for app highlights and upgrade notes.
- Troubleshooting & FAQs: start with the editor manual and the comments in `OnlineProtocolEditor/templates/index.json`; open a GitHub issue if something’s missing.

## Additional assets

- Online Protocol Editor (web) with integrated manual (`OnlineProtocolEditor/`).
- Historical documentation snapshots are available from git history if you need the retired PDF.

## License & data policy

- GPLv3 license. Share improvements under the same license with attribution.
- Data stays on-device unless exported or synced intentionally.
- Software provided **as is** with no warranties; validate, supervise, and meet local requirements before deployment.

---

Generated helper docs. Extend and reorganize this README as the project evolves.
