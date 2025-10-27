# Changelog

## [Unreleased]
_No changes yet._

## [1.1.1] - 2025-10-27
### Changed
- Updated targetSdk and compileSdk to API 35 (Android 15) to meet Google Play requirements
- Enabled R8 code optimization and resource shrinking for release builds

## [1.1.0] - 2025-10-02
### Highlights
- Unified About/Manual content across Android and web by consuming the shared Markdown bundle.
- Hardened Markdown rendering on Android to avoid percent-sign crashes and display dialogs reliably.
- Smoothed onboarding for the Online Protocol Editor with clearer copy and template manifest documentation.

### Added
- Online Protocol Editor now loads protocol templates from `templates/index.json`, allowing teams to manage curated `.txt` studies outside the JavaScript bundle.
- Bundled mindfulness, Pokémon GO, branching, and randomized check-in templates as standalone files in `OnlineProtocolEditor/templates/`.

### Changed
- Updated Online Protocol Editor README with guidance on hosting over HTTP(S), customizing templates, and troubleshooting manifest loading.
- Tweaked the editor tagline copy to better reflect streamlined validation workflow.

### Fixed
- Template insertion normalizes line endings to keep protocol formatting consistent across operating systems.
- Android About/Manual dialogs now locate the correct Markdown assets and render without crashing when `%` appears in content.

## [1.0.0] - 2025-09-26
### Highlights
- Promote the multimedia-ready Insert Command dialog with validation for audio, video, and custom HTML assets.
- Persist the last opened protocol and auto-resume it on startup for quicker recovery between sessions.
- Ship the comprehensive tutorial protocol and immersive briefing HTML asset covering every command and media placeholder.
- Harden protocol validation with issue navigation, undo/redo, quick fixes, and improved resource existence checks.
- Disable cloud/device backups until a formal data-retention policy is defined.

### Quality
- ktlint enforced across the module with passing checks.
- Automated unit tests (`testDebugUnitTest`) and release/ debug assemblies verified during the 1.0 audit.

### Known considerations
- Media assets referenced by tutorial placeholders must be supplied by deployments (see `tutorial_protocol.txt`).
- Developer mode remains accessible via the hidden gesture for diagnostic purposes.
