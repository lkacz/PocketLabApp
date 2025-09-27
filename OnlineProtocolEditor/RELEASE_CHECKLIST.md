# Online Protocol Editor Release Checklist

## 1. Prepare the workspace
- [ ] Checkout the release branch (e.g. `merge-main-into-dev18`) and ensure it is up to date with remote.
- [ ] Verify `git status` shows only the expected Online Protocol Editor changes plus supporting assets.
- [ ] Review `CHANGELOG.md` (root and `OnlineProtocolEditor/CHANGELOG.md`) for accurate release notes.

## 2. Smoke-test the template manifest
- [ ] From the repository root run a static server:
  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts/serve-editor.ps1
  ```
  _Or manually run `python -m http.server 8080` from `OnlineProtocolEditor/`._
- [ ] Open `http://localhost:8080/OnlineProtocolEditor/index.html` in a Chromium-based browser.
- [ ] Click **Templates** and confirm the manifest-driven list loads the four bundled studies.
- [ ] Select each template to verify summary text, preview content, and insertion work without console errors.
- [ ] Toggle the search box with queries like `walk`, `pokemon`, `branch`, `random` to ensure filtering behaves.

## 3. Validate editor basics
- [ ] Create a new protocol, insert a template, and run **Validate now** to ensure no unexpected warnings appear for the inserted content.
- [ ] Confirm **Save** and **Download** options prompt as expected (note: full File System Access API requires Chromium).

## 4. Finalize the release
- [ ] Commit all Online Protocol Editor changes with a descriptive message (e.g. `feat(editor): load templates from manifest`).
- [ ] Open a pull request from the release branch into `main`, referencing the updated changelog entries.
- [ ] Once approved, merge into `main` and create a release tag if appropriate.
- [ ] Publish release notes summarizing the new manifest workflow and hosting recommendations.

> Tip: When opening `index.html` directly from `file://`, the manifest fetch is blocked by the browser. This is expected—serve the folder over HTTP(S) for full functionality.
