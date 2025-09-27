# PoLA Online Protocol Editor

A standalone web-based replica of the PoLA protocol editor so you can load, validate, and export protocol `.txt` files directly from your browser. Drop the folder on any static host (or just open `index.html`) to start editing.

## Features

- 💾 **Open, save, and download** PoLA protocol files using the browser File System Access API when available (with graceful fallbacks).
- ✅ **Live validation** powered by a direct port of the in-app Kotlin validator, including label/GOTO checks, randomization balancing, numeric validations, and color syntax rules.
- 📋 **Issue navigation & filtering** so you can jump between errors/warnings or filter by line number / text.
- ⚡ **Command palette & templates** with quick inserts for common commands (Instruction, Timer, Scale, InputField, etc.).
- 🧠 **Recognized command cheat sheet** showcasing every supported directive and protocol authoring tips.

## Usage

1. Open `index.html` in a modern browser (Chrome, Edge, or any Chromium-based browser for full save support).
2. Click **Load…** to import an existing `.txt` file, or **New** to start from scratch.
3. Toggle **Auto validate** (on by default) or press **Validate now** to rerun the validator.
4. Use **Insert Command** or the _Recognized commands_ list to drop predefined templates at the cursor.
5. When you are ready, press **Save** (uses the browser save picker when available) or **Download** to export a copy.

> **Tip:** The editor warns before closing the tab if you have unsaved changes. You can also press <kbd>Ctrl</kbd>/<kbd>Cmd</kbd> + <kbd>F</kbd> inside the table to search the validation results, or use the built-in filter box.

## Development notes

- The validation logic in `main.js` mirrors `ProtocolValidator.kt` for feature parity. If you add new commands to the Android app, update both the `recognizedCommands` set and any command-specific rules in `handleKnown`.
- UI styles live in `styles.css`. The layout is responsive and works down to mobile widths.
- No build step is required—everything runs as plain HTML, CSS, and vanilla ES modules.
