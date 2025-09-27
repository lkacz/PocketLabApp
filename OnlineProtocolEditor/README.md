# PoLA Online Protocol Editor

A standalone web-based replica of the PoLA protocol editor so you can load, validate, and export protocol `.txt` files directly from your browser. Drop the folder on any static host (or just open `index.html`) to start editing.

## Features

- 💾 **Open, save, and download** PoLA protocol files using the browser File System Access API when available (with graceful fallbacks).
- ✅ **Live validation** powered by a direct port of the in-app Kotlin validator, including label/GOTO checks, randomization balancing, numeric validations, and color syntax rules.
- 📋 **Issue navigation & filtering** so you can jump between errors/warnings or filter by line number / text.
- ⚡ **Command palette & templates** with quick inserts for common commands (Instruction, Timer, Scale, InputField, etc.), tag-based filtering, and one-click favorites to surface the right study quickly.
- 🧠 **Recognized command cheat sheet** showcasing every supported directive and protocol authoring tips.

## Usage

1. Serve the folder from a simple static web server (for example `scripts/serve-editor.ps1 -OpenBrowser` or `python -m http.server`) and open `index.html` in a modern browser (Chrome, Edge, or any Chromium-based browser for full save support).<br />
	_Why?_ Browsers block `fetch` access to local files when running directly from `file://`, which disables the external template manifest. When hosted over `http(s)` you'll get the full experience. If you must open via `file://`, the editor still works but only ships with the built-in fallback templates.
2. Click **Load…** to import an existing `.txt` file, or **New** to start from scratch.
3. Toggle **Auto validate** (on by default) or press **Validate now** to rerun the validator.
4. Use **Insert Command** or the _Recognized commands_ list to drop predefined templates at the cursor.
5. Open the **Templates** dialog to browse curated studies pulled from `templates/index.json`. Use the search box or tag chips to narrow the list, tap the star icons to mark favorites, and flip the **Favorites only** toggle when you want to focus on your go-to studies before inserting them.
6. When you are ready, press **Save** (uses the browser save picker when available) or **Download** to export a copy.

> **Tip:** The editor warns before closing the tab if you have unsaved changes. You can also press <kbd>Ctrl</kbd>/<kbd>Cmd</kbd> + <kbd>F</kbd> inside the table to search the validation results, or use the built-in filter box.

## Development notes

- The validation logic in `main.js` mirrors `ProtocolValidator.kt` for feature parity. If you add new commands to the Android app, update both the `recognizedCommands` set and any command-specific rules in `handleKnown`.
- Template metadata now lives in `templates/index.json`, with each template stored as a standalone `.txt`. The editor fetches these at runtime and falls back to bundled defaults when the manifest is unavailable. Remember to update both the manifest entry and the referenced file when adding new studies, and optionally provide a `tags` array to power the filter chips in the dialog.
- Favorited templates are stored in the browser via `localStorage`; if storage is unavailable (for example, in some private browsing sessions) the star control still works for the current page load but does not persist.
- UI styles live in `styles.css`. The layout is responsive and works down to mobile widths.
- No build step is required—everything runs as plain HTML, CSS, and vanilla ES modules.

## Custom templates

Want to share your own study flows?

1. Duplicate an existing entry in `templates/index.json` and give it a unique `id`, human-friendly `name`, `summary`, optional `tags` array (lowercase keywords), and the filename that should be loaded.
2. Place your protocol text inside `templates/your-template-name.txt` (UTF-8 encoded, one command per line just like PoLA expects).
3. Host the folder over `http(s)` and reopen the editor. The new template will automatically appear in the dialog, complete with search, preview, and insertion support.

If your manifest or text files fail to load, the editor logs a warning to the browser console and reverts to the built-in fallback set. Double-check paths, casing, and ensure your server exposes the `templates/` directory.
