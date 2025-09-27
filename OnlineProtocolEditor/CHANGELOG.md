# Online Protocol Editor Changelog

## Unreleased

### Added
- Manifest-driven template loader that fetches metadata from `templates/index.json` with graceful fallback to bundled defaults when external files are unavailable.
- External `templates/` directory containing curated `.txt` examples for mindful walks, Pokémon GO outings, branching routes, and randomized check-ins.

### Changed
- README now recommends serving the editor over HTTP(S) to unlock dynamic template loading and documents how to add custom study templates.

### Fixed
- Template preview and insertion now normalize line endings to keep protocol spacing consistent across operating systems.
