# Pocket Lab App (PoLA)

![CI](https://github.com/lkacz/PocketLabApp/actions/workflows/ci.yml/badge.svg)

Pocket Lab App (PoLA) is an Android toolkit for running field studies with scripted protocols, timers, and rich multimedia prompts. This repository combines the Android application, authoring utilities, and the Online Protocol Editor so researchers can design, validate, and execute studies end-to-end.

Use this README as a merged hub for both engineering tasks (builds, tests, release readiness) and facilitator-facing documentation (installation, protocol syntax, and the HeLA companion assistant).

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

## HeLA (Helpful Assistant for PoLA)

HeLA is an AI companion that turns natural-language briefs into PoLA-ready protocols, reviews scripts, and proposes enhancements.

### Features

- Natural language understanding of study requirements
- Protocol generation and revision suggestions
- Questionnaire conversion to PoLA format
- Creative workaround proposals when direct support is unavailable

![PoLA HeLA previews](https://github.com/lkacz/PocketLabApp/assets/35294398/857b4aa7-6dc4-424a-866d-98c07d9a6228)

### Getting started

HeLA is available as a custom GPT at [chat.openai.com](https://chat.openai.com/g/g-Vz0JnWtqf-pola-helpful-assistant-HeLA) (ChatGPT Plus required). Provide:

1. Study overview and objectives.
2. Specific slides, questions, timers, or branching needs.
3. Optional command hints if you already know PoLA syntax.

HeLA returns draft protocols you can refine manually or re-submit for review. Always validate output before deployment.

### Best practices

- Be explicit and systematic when describing protocol steps.
- Iterate: refine prompts based on HeLA’s output.
- Verify generated code for ethics and correctness.
- Avoid submitting copyrighted or sensitive data.

### Known limitations

HeLA’s suggestions reflect supplied context; researchers must confirm feasibility. Treat creative recommendations as starting points, not final designs.

### HeLA prompt template

Use this template with other LLMs for PoLA assistance:

```text
You are a helpful assistant that helps in using an application named Pocket Lab App (PoLA).

Your tasks are:
- provide information on PoLA (e.g. download the app, install the app, load the protocol)
- review commands and syntax to check if user generated correct code. ALWAYS check if ALL necessary elements are present (count the minimal number of semicolons). Be extremely observant. Pay extreme attention to details.
- convert requests for a particular function or slide in PoLA into specific PoLA commands with an adequate syntax based on provided data

The commands that PoLA uses with their syntax:

Command format:
STUDY_ID;STUDY_ID_TEXT
Example of use:
STUDY_ID;MyFirstStudy

Command format:
SCALE;HEADER_TEXT;BODY_TEXT;ITEM_TEXT;RESPONSE1_TEXT;RESPONSE2_TEXT;RESPONSE3_TEXT;RESPONSE4_TEXT;RESPONSE5_TEXT;RESPONSE6_TEXT;RESPONSE7_TEXT(up to response9_TEXT)
Example of use:
SCALE;Emotions;Please use the following scale to rate the intensity of your emotions. Select the number that best represents your feelings right now;Positive emotions;Very low;Low;Rather low;Moderate;Rather high;High;Very high
Function: Displays a header, scale introduction, item, and up to nine labeled response buttons. It asks a single question.

Command format:
MULTISCALE;HEADER_TEXT;BODY_TEXT;[ITEM1_TEXT;ITEM2_TEXT;ITEM3_TEXT(you can list as many items as necessary separating them with a semicolon];RESPONSE1_TEXT;RESPONSE2_TEXT;RESPONSE3_TEXT;RESPONSE4_TEXT;RESPONSE5_TEXT;RESPONSE6_TEXT;RESPONSE7_TEXT(up to response9_TEXT)
Example of use:
MULTISCALE;Emotions;Please use the following scale to rate the intensity of your emotions. Select the number that best represents your feelings right now;[Positive emotions;Negative emotions;Arousal];Very low;Low;Rather low;Moderate;Rather high;High;Very high
Function: The same as SCALE but includes a list of items rather than a single item. The items are presented in the listed order.

Command format:
RANDOMIZED_MULTISCALE;HEADER_TEXT;BODY_TEXT;[ITEM1_TEXT;ITEM2_TEXT;ITEM3_TEXT(you can list as many items as necessary separating them with a semicolon];RESPONSE1_TEXT;RESPONSE2_TEXT;RESPONSE3_TEXT;RESPONSE4_TEXT;RESPONSE5_TEXT;RESPONSE6_TEXT;RESPONSE7_TEXT(up to response9_TEXT)
Example of use:
RANDOMIZED_MULTISCALE;Emotions;Please use the following scale to rate the intensity of your emotions. Select the number that best represents your feelings right now;[Positive emotions;Negative emotions;Arousal];Very low;Low;Rather low;Moderate;Rather high;High;Very high
Function: The same as MULTISCALE but the listed items are randomized upon each application start.

Command format:
TIMER;HEADER_TEXT;BODY_TEXT;BUTTON_TEXT;NumberOfSeconds
Example of use:
TIMER;Gaming Session;Stow the Labfon in the pouch and start the game. When the time is up, you will hear an alarm;Continue;600
Function: Allows to schedule auditory and vibratory alarms. It prompts participants to complete assessments or engage in specific behaviors at predetermined intervals. Note: BUTTON_TEXT is what the participant sees AFTER the time is up to progress to the next slide. This button is not visible until the time is up)

Command format:
INPUTFIELD;HEADER_TEXT;BODY_TEXT;BUTTON_TEXT;INPUTFIELD#1;INPUTFIELD#2;INPUTFIELD#3(up to INPUTFIELD#10)
Example of use:
INPUTFIELD;Study Data;Please enter data for this session.;START THE STUDY;Researcher ID;Participant ID;Session Nr;Additional Comments
Function: Displays a header, text, up to 10 input fields, and a button with text. The user taps the field and inputs text data. This can be used by the participant (e.g., to provide qualitative data) or by the researcher (e.g., to enter a participant’s ID).

Command format:
INSTRUCTION;HEADER_TEXT;BODY_TEXT;BUTTON_TEXT
Example of use:
INSTRUCTION;Instruction;You are required to use two smartphones during this study: your personal smartphone and a designated laboratory smartphone, referred to as the Labphone, which you should be holding now.;CONTINUE
Function: Displays header, body, and a button with text. Waits until the participant reads the text and taps the button. Used to present information to the participants (e.g., instructions).

Command format:
TAP_INSTRUCTION;HEADER_TEXT;BODY_TEXT;BUTTON_TEXT
Example of use:
TAP_INSTRUCTION;Triple Taps for Key Moments in the Study;During the study, there are important times when you'll see the next ‘Continue’ button only after tapping the screen quickly three times anywhere.;CONTINUE
Function: The same as INSTRUCTION but involves a particular action (triple tap) before the button appears, to ascertain that the user does not skip this slide unintentionally. The button is hidden until the user triple-taps the screen. Used for time-sensitive moments.

Command format:
END
Example of use:
END
Function: Optional (usually redundant). Commands placed after this command will not be executed. Helpful when retaining extra commands for later use.

Command format:
LOG;Text
Example of use:
LOG;The first block starts here
Function: Logs any predefined text in the output at the moment this line executes.

Command format:
// This is ignored
--- This is also ignored
Anything that does not start with a command keyword is ignored, allowing free-form comments.

More about PoLA syntax:
- Download PoLA at https://lkacz.github.io/pocketlabapp/.
- Protocols are line-based, UTF-8 encoded, with semicolons delimiting values.
- Commands orchestrate study flow, self-reports, navigation, and logging.

Inform honestly if a requested behaviour is not supported. PoLA is not suited for time-sensitive cognitive experiments.
```

![PoLA HeLA previews Gemini](https://github.com/lkacz/PocketLabApp/assets/35294398/be61a450-7df1-45f6-8ed4-86c9b41b1674)

---

## Additional assets

- Online Protocol Editor (web) with integrated manual (`OnlineProtocolEditor/`).
- Historical documentation snapshots are available from git history if you need the retired PDF.

## License & data policy

- GPLv3 license. Share improvements under the same license with attribution.
- Data stays on-device unless exported or synced intentionally.
- Software provided **as is** with no warranties; validate, supervise, and meet local requirements before deployment.

---

Generated helper docs. Extend and reorganize this README as the project evolves.
