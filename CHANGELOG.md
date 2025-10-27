# Changelog

## [Unreleased]
_No changes yet._

# Changelog

## [1.3.0] - 2025-10-28
### Changed
- Version bump to 1.3.0

## [1.1.9] - 2025-10-28
### Changed
- Disabled R8 obfuscation completely to prevent class/method renaming issues in release builds
- Changed from `proguard-android-optimize.txt` to `proguard-android.txt` (non-optimizing)
- Added `-dontobfuscate` flag to ProGuard rules
- Kept source file names and line numbers for better crash stack traces
- Note: Minification and resource shrinking remain enabled for APK size optimization

## [1.1.8] - 2025-10-27
### Fixed
- Added ProGuard rules to prevent R8 obfuscation from stripping CompletionFragment and its companion object
- Fixed release build crash on completion screen (last slide) caused by R8 removing Fragment.newInstance() methods
- Added protection for Logger class and MainActivity callbacks to prevent R8 obfuscation issues

## [1.1.7] - 2025-10-27
### Fixed
- Fixed completion screen potentially closing prematurely by ensuring proper lifecycle management
- Added main thread safety checks and small delay before showing completion fragment
- Used commitAllowingStateLoss to prevent state loss exceptions during completion

## [1.1.6] - 2025-10-27
### Added
- Added welcome dialog for first-time users explaining PoLA's protocol-based workflow
- Added welcome dialog to Online Protocol Editor with localStorage persistence

### Changed
- Tutorial protocol now uses consistent 64dp top margin throughout for better visual spacing
- Moved asset copying to background thread to prevent UI blocking during tutorial setup
- Welcome dialog checkbox moved to footer for better visibility

### Fixed
- Fixed black screen during tutorial asset deployment by using Kotlin coroutines for async file copying
- Users now see immediate toast feedback when asset copying begins

## [1.1.5] - 2025-10-27
### Fixed
- **Critical:** Fixed release build crash caused by reflection accessing obfuscated Logger field. Replaced reflection-based context retrieval with direct Context parameter passing to FragmentLoader.

## [1.1.4] - 2025-10-27
### Added
- Introduced `TOP_MARGIN` and `BOTTOM_MARGIN` protocol commands for per-screen padding control
- Added screen margin sliders to the Appearance Customization dialog with live preview updates
- Documented new spacing commands in the in-app manual and protocol reference

### Changed
- Tutorial and demo protocols now showcase generous 48dp/64dp spacing and consistent double line breaks for breathing room
- Appearance presets and documentation refreshed to reference the new screen margin controls

### Fixed
- Restored missing `dpToPx` helper in `HTMLFragment` and resolved layout param conflicts in `ScaleFragment`

## [1.1.3] - 2025-10-27
### Added
- Added automatic tutorial resources setup on first run with guided folder selection
- Tutorial resources are now automatically copied from assets to user-selected folder
- Added TutorialResourcesSetup helper class for managing asset copying

### Changed
- Improved tutorial setup messaging when resources folder exists but lacks tutorial files
- Better UX for users switching between custom and tutorial protocols

### Fixed
- Fixed potential incomplete file copies with proper cleanup on failure
- Fixed resources folder URI persistence to only save after successful copy

## [1.1.2] - 2025-10-27
### Added
- Added assets fallback for media loading (audio, video, images, HTML) when no resources folder is configured
- Added interactive Snake game (psnakev2.html) to tutorial protocol
- Added completion screen after protocol END command with proper back button handling
- Added runtime storage permission requests for Android 6-12

### Changed
- Tutorial protocol now works out-of-the-box using bundled assets without requiring external resources folder
- Progress clearing moved from MainActivity to CompletionFragment for better UX

### Fixed
- Fixed tutorial protocol crash when starting without resources folder configured
- Fixed images not displaying in tutorial (HtmlImageLoader now falls back to assets)
- Fixed videos not playing from assets (copy to cache for VideoView compatibility)
- Fixed completion flow bug where app closed immediately after protocol END
- Fixed back button in CompletionFragment to close app instead of navigating back

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
