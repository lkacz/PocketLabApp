# Protocol Commands & Validation Reference

This document catalogs the protocol command syntax, validation rules, and available quick-fixes implemented in the editor dialog.

## Categories
- Content: INSTRUCTION, TIMER, SCALE, SCALE[RANDOMIZED], INPUTFIELD, INPUTFIELD[RANDOMIZED], LABEL, GOTO, HTML, TIMER_SOUND, LOG, END
- Randomization: RANDOMIZE_ON, RANDOMIZE_OFF
- Meta: STUDY_ID, TRANSITIONS
- Style (Colors): HEADER_COLOR, BODY_COLOR, RESPONSE_TEXT_COLOR, RESPONSE_BACKGROUND_COLOR, SCREEN_BACKGROUND_COLOR, CONTINUE_TEXT_COLOR, CONTINUE_BACKGROUND_COLOR, TIMER_COLOR
- Style (Sizes): HEADER_SIZE, BODY_SIZE, ITEM_SIZE, RESPONSE_SIZE, CONTINUE_SIZE, TIMER_SIZE
- Style (Alignment): HEADER_ALIGNMENT, BODY_ALIGNMENT, CONTINUE_ALIGNMENT, TIMER_ALIGNMENT
- Style (Screen Margins): TOP_MARGIN, BOTTOM_MARGIN

## General Notes
- Commands are semicolon-separated. Empty or comment lines (// ...) are ignored.
- Trailing semicolons are invalid (reported as stray semicolon).
- Case-insensitive matching internally; stored as typed.

## Command Formats

| Command | Expected Segments | Notes |
|---------|-------------------|-------|
| INSTRUCTION | 4 (INSTRUCTION;HEADER;BODY;CONTINUE) | Exactly 3 semicolons required. |
| TIMER | 5 (TIMER;HEADER;BODY;SECONDS;CONTINUE) | SECONDS must be non-negative integer; >3600 produces warning. |
| SCALE / SCALE[RANDOMIZED] | >=5 (SCALE;HEADER;BODY;ITEM1;ITEM2;...;CONTINUE) | At least one item required; randomization variant just flagged in name. |
| INPUTFIELD / INPUTFIELD[RANDOMIZED] | >=5 (INPUTFIELD;HEADER;BODY;FIELD1;FIELD2;...;CONTINUE) | At least one field; randomized variant warns if <2 fields. |
| LABEL | 2 (LABEL;NAME) | Duplicate names flagged. |
| GOTO | 2 (GOTO;LABEL_NAME) | Missing or undefined target flagged; quick-fix can insert missing LABEL after GOTO. |
| RANDOMIZE_ON | 1 | Cannot nest (only one open block). |
| RANDOMIZE_OFF | 1 | Must close an open RANDOMIZE_ON block. |
| STUDY_ID | 2 (STUDY_ID;VALUE) | Must appear once; duplicate flagged with quick-fix removal. |
| TRANSITIONS | 2 (TRANSITIONS;MODE) | Modes: off, slide, slideleft, fade, dissolve. |
| *_COLOR | 2 (CMD;#RRGGBB or #AARRGGBB) | Hex validation enforced. |
| *_SIZE | 2 (CMD;POSITIVE_NUMBER) | >200 produces warning. |
| *_ALIGNMENT | 2 (CMD;LEFT|CENTER|RIGHT) | Invalid tokens flagged. |
| TOP_MARGIN / BOTTOM_MARGIN | 2 (CMD;NON-NEGATIVE_DP) | Values ≥0 and ≤200 recommended; larger values warn. |
| HTML / TIMER_SOUND | 2 (CMD;FILENAME) | Resource existence checked if resources folder configured. |
| LOG | 2 (LOG;MESSAGE) | Free-form message. |
| END | 1 | Structural terminator (no validation rules). |

## Validation Highlights
- Randomization blocks tracked; unmatched RANDOMIZE_ON surfaced as global error (synthetic <EOF>).
- Duplicate LABELs: both occurrences flagged; quick-fix removes later ones.
- GOTO target existence enforced; missing targets error; quick-fix inserts LABEL after offending GOTO.
- STUDY_ID duplication & missing value detection with quick-fix (retain first).
- Stray trailing semicolons: quick-fix removes them globally.
- TIMER structural & numeric validation; quick-fix normalizes malformed TIMER lines to a safe default (60s).
- Color hex pattern: #RRGGBB or #AARRGGBB only.
- Size numeric >0; large size warning >200.
- Screen margin commands require numeric dp values ≥0 and warn when exceeding 200.
- Alignment restricted to LEFT|CENTER|RIGHT.

## Quick-Fixes Implemented
| Issue | Action |
|-------|--------|
| Unmatched RANDOMIZE_ON | Insert RANDOMIZE_OFF at end |
| Duplicate STUDY_ID | Remove duplicates (keep first) |
| Stray semicolons | Strip trailing semicolons on offending lines |
| Duplicate LABELs | Remove later duplicates |
| Undefined GOTO target | Insert missing LABEL after first offending GOTO |
| Malformed TIMER | Normalize to TIMER;Header;Body;60;Continue |

## Auto-Scroll Behavior
After any mutation (add/edit/move/quick-fix), view auto-scrolls to the first remaining issue to reduce navigation friction.

## Insert Dialog Features
- Full command list with category grouping + live search.
- Context-sensitive parameter inputs (spinners for alignment & transitions, numeric/time fields, color text, etc.).
- Prevents invalid insertions (duplicate STUDY_ID, RANDOMIZE_OFF without open block, nested RANDOMIZE_ON).

## Testing
Pure validator (`ProtocolValidator`) covered by unit tests for:
- Duplicate labels
- Randomization pairing
- STUDY_ID rules
- TIMER validation (normal, large, negative)
- Color & size validation
- INPUTFIELD randomized field count warning
- SCALE parameter presence
- GOTO missing & existing target cases

## Future Ideas
- Color quick-fix (normalize #RGB to #RRGGBB).
- Batch optimize large protocols (lazy row rendering, diff-based refresh).
- Export protocol skeleton generator.

---
Maintained automatically alongside feature commits; update this file when adding new commands or quick-fixes.
