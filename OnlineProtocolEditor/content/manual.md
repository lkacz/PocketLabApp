<!-- Converted from app/src/main/assets/about.txt -->

# Pocket Lab App Manual

How to prepare, launch, and quality-check PoLA study runs.

<table class="meta"><tbody><tr><th>Audience</th><td>Researchers and facilitators configuring Pocket Lab App studies.</td></tr><tr><th>Includes</th><td>Quick-start setup, workflow guidance, and the full command reference.</td></tr><tr><th>Last updated</th><td>September 2025</td></tr></tbody></table>

**Jump to:**

-   [Quick start](#quickstart)
-   [Participant setup](#participant)
-   [Files & exports](#files)
-   [Customization & tips](#customization)
-   [Command reference](#command-reference)
-   [Support](#support)

## Quick start

Complete these steps before inviting a participant to begin:

-   **Load protocol:** Choose _Load Protocol_ to import a plain-text study script. The runtime interprets commands line by line, so you can update behavior without rebuilding the app.
-   **Review the flow:** Open _Protocol Manager_ to scan for warnings, search commands, and verify branching before deployment.
-   **Explore bundled studies:** Use the tutorial or demo protocol to preview the interface and confirm device capabilities like audio playback.

[Back to top ↑](#top)

## Participant setup

-   **Protocol name:** Confirm the active script shown beneath _Protocol:_. Tap another protocol option to swap it instantly.
-   **Participant ID:** Capture an identifier to tag collected data exports.
-   **Resume vs. restart:** If PoLA detects an interrupted session, it offers to resume. Choose _Don’t continue_ when starting a fresh run.
-   **Start Study:** Launch the sequence once all devices, timers, and accessories are ready. The app logs the exact start time.

[Back to top ↑](#top)

## Files & exports

-   **Select output folder:** Choose a writable directory on the lab smartphone where TSV exports and logs will be stored. Verify access on the device before field work.
-   **Select resources folder:** Point PoLA to the directory that holds audio, video, images, and HTML referenced in your protocol.
-   **Back up regularly:** Copy both protocol scripts and exported data to secure storage after each session.

[Back to top ↑](#top)

## Customization & tips

-   **Layout:** Adjust typography, button colors, and alignment in the Layout dialog. Use the restore defaults action if styling becomes inconsistent.
-   **Sounds:** Configure alarm tones or mute them in the Sounds dialog. Timer commands will reuse the selected audio.
-   **Facilitator tips:** Tap the app title seven times to unlock developer tools. Long-press it to disable developer mode.

[Back to top ↑](#top)

## Protocol command manual

This reference describes every command the runtime understands. Protocols are plain-text files encoded in UTF-8. Each non-empty line represents one command. Values are separated with semicolons (`;`). Lines beginning with `//` are comments and ignored during execution.

**Quick tips:**

-   Resource references (images, audio) should match the filenames stored in your project resources.
-   Commands are case-insensitive, but the conventional style is UPPERCASE.
-   Media placeholders such as `<audio.mp3>`, `<video.mp4,60>`, or `<custom.html>` can be embedded in most text fields.

**Sections:**

-   [Study metadata & flow control](#manual-flow)
-   [Instructional & survey content](#manual-content)
-   [Media & interaction helpers](#manual-media)
-   [Layout & styling commands](#manual-layout)
-   [Best practices](#manual-practices)

Study metadata & flow control

`STUDY_ID;identifier`

Defines a unique identifier stored in exports and logs, helping you group sessions that belong to the same protocol version. Only one `STUDY_ID` is allowed per file.  
**Example:** `STUDY_ID;POLA_FIELD_STUDY_01`

`LABEL;Name`

Marks a jump target. Labels are case-sensitive and should avoid spaces. Duplicate labels trigger validation errors.  
**Example:** `LABEL;INTRO_COMPLETED`

`GOTO;LabelName`

Transfers execution to the line immediately after the labeled command. Use this to branch participants based on earlier responses.  
**Example:** `GOTO;FOLLOW_UP_BRANCH`

`LOG;message`

Writes a custom entry to the session .tsv log without presenting UI. Useful for marking experimental phases, counterbalancing groups, or debugging.  
**Example:** `LOG;Condition=A`

`RANDOMIZE_ON` … `RANDOMIZE_OFF`

Wraps a block of commands that should be presented in randomized order. Ensure every `RANDOMIZE_ON` has a matching `RANDOMIZE_OFF`. Nested randomization is not permitted.

`TRANSITIONS;mode`

Controls screen-to-screen animations. Accepted modes: `off`, `slide`, `slideleft`, `fade`, `dissolve`.  
**Example:** `TRANSITIONS;slide`

`END`

Terminates the protocol immediately, ignoring all subsequent commands. Useful during development for keeping unused parts of a protocol in the same file.

Instructional & survey content

`INSTRUCTION;Header;Body;Continue text`

Displays a formatted instruction page. Body text can include HTML, media placeholders, and accessibility modifiers like `[TAP]` or `[HOLD]` (attach to the continue text).  
**Example:** `INSTRUCTION;Welcome;<b>Please read carefully.</b>;Start`

`INPUTFIELD;Header;Body;[Field1;Field2;…];Continue text`

Collects free-text responses. Fields can be listed inline (`Field1;Field2`) or wrapped in brackets to include semicolons within labels. Append `[HOLD]` to the continue caption to require a long press.  
**Example:** `INPUTFIELD;Background survey;Tell us about yourself;[Age;Occupation;City];Continue`

`INPUTFIELD[RANDOMIZED]`

Presents the listed input fields in a randomized order while keeping header, body, and continue text fixed.

`SCALE;Header;Body;Item label;Response1;…`

Shows a single-item scale with tap targets. Responses may include optional branch labels in square brackets. `Response text[Label]` directs flow to a matching `LABEL` when selected.  
**Example:** `SCALE;Mood check;How do you feel now?;Mood;Very low;Low;Neutral;High;Very high`
**Example:** `SCALE;Mood intervention;How do you feel now?;Mood;Low[Low_Mood_Label];Neutral[Neutral_Mood_Label];High[High_Mood_Label]`

`SCALE[RANDOMIZED]`

Randomizes the order of response options to reduce order effects. Branch labels remain attached to their original response text.

**Example:** `SCALE;Emotions;Emotion intensity right now?;[Sadness;Happines;Anger]; Very low;Low;Neutral;High;Very high`

`TIMER;Header;Body;Seconds;Continue text`

Displays a countdown timer. When the timer reaches zero, participants may proceed using the continue button (or immediately if you specify `0` seconds).  
**Example:** `TIMER;Break;Rest for one minute;60;Resume task`

`HTML;file.html;Continue text`

Loads an HTML file from the resources folder and renders it inside the study screen, preserving layouts and embedded media.  
**Example:** `HTML;consent_form.html;I agree`

Media & interaction helpers

`TIMER_SOUND;file.mp3`

Sets the audio file used by subsequent `TIMER` screens. Place the referenced file in the resources folder. Optional volume can follow the filename (e.g., `file.mp3,70`).

`<resource placeholders>`

Within any commmand's textual field you can embed:

-   `<audio.mp3,80>` — plays audio at 80% volume.
-   `<video.mp4,60>` — plays video at 60% volume (0–100).
-   `<image.jpg>` — displays an inline image.
-   `<custom.html>` — injects HTML content into instruction screens.

Layout & styling commands

Apply these commands before the screens you want to style. Settings remain active until changed again or reset via another command.

`HEADER_SIZE;value`

Sets header text size (SP). **Accepted values:** decimal numbers such as `24`.

`BODY_SIZE;value`

Adjusts body text size (SP). **Accepted values:** decimal numbers.

`ITEM_SIZE;value`

Controls the size of scale item labels. **Accepted values:** decimal numbers.

`RESPONSE_SIZE;value`

Changes response button text size. **Accepted values:** decimal numbers.

`CONTINUE_SIZE;value`

Updates the continue button text size. **Accepted values:** decimal numbers.

`TIMER_SIZE;value`

Sets the countdown digits size. **Accepted values:** decimal numbers.

`HEADER_ALIGNMENT;LEFT|CENTER|RIGHT`

Aligns header text horizontally. **Accepted values:** keywords `LEFT`, `CENTER`, or `RIGHT`.

`BODY_ALIGNMENT;LEFT|CENTER|RIGHT`

Aligns body copy. **Accepted values:** `LEFT`, `CENTER`, `RIGHT`.

`CONTINUE_ALIGNMENT;Horizontal;Vertical`

Places the continue button. **Accepted values:** horizontal keyword (`LEFT`, `CENTER`, `RIGHT`) plus vertical keyword (`TOP` or `BOTTOM`).

`TIMER_ALIGNMENT;LEFT|CENTER|RIGHT`

Aligns the timer component. **Accepted values:** `LEFT`, `CENTER`, `RIGHT`.

`HEADER_COLOR;#RRGGBB`

Sets header text color. **Accepted values:** hex codes (e.g., `#FF0055`) or HTML color names.

`BODY_COLOR;#RRGGBB`

Sets body text color. **Accepted values:** hex codes or HTML names.

`RESPONSE_TEXT_COLOR;#RRGGBB`

Colors the response text. **Accepted values:** hex codes or HTML names.

`RESPONSE_BACKGROUND_COLOR;#RRGGBB`

Colors the background of response buttons. **Accepted values:** hex codes or HTML names.

`SCREEN_BACKGROUND_COLOR;#RRGGBB`

Sets the entire screen background. **Accepted values:** hex codes or HTML names.

`CONTINUE_TEXT_COLOR;#RRGGBB`

Sets continue button text color. **Accepted values:** hex codes or HTML names.

`CONTINUE_BACKGROUND_COLOR;#RRGGBB`

Sets continue button background color. **Accepted values:** hex codes or HTML names.

`TIMER_COLOR;#RRGGBB`

Sets timer digits color. **Accepted values:** hex codes or HTML names.

Best practices

-   **Validate early:** Use the in-app protocol validator or the online editor to catch missing labels, malformed commands, or resource mismatches.
-   **Use comments:** Prefix lines with `//` to annotate sections or disable commands temporarily.
-   **Back up resources:** Store referenced media inside the PoLA resources folder so sessions remain consistent across devices.
-   **Version your scripts:** Pair each public release with a tagged protocol file and bump `STUDY_ID` as you iterate.

[Back to top ↑](#top)

## Support

Questions, collaboration ideas, or bug reports are welcome at [lkacz@amu.edu.pl](mailto:lkacz@amu.edu.pl). Contributions from the community—new commands, UI refinements, translations—are encouraged under the GNU GPLv3 license.

[Back to top ↑](#top)

Questions, collaboration ideas, or bug reports are welcome at [lkacz@amu.edu.pl](mailto:lkacz@amu.edu.pl).  
Project repository: [github.com/lkacz/PocketLabApp](https://github.com/lkacz/PocketLabApp).  
PoLA Online Protocol Editor: [lkacz.github.io/PocketLabApp](https://lkacz.github.io/PocketLabApp/).

Thank you for exploring PoLA. We hope these tools help you run precise, participant-friendly outdoor studies wherever your research takes you.
