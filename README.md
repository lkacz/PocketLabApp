# Pocket Lab App (PoLA) Documentation 
## (ver. 13.03.2024)

### Introduction
The Pocket Lab App (PoLA) is a research tool designed to streamline the process of conducting experimental research outside traditional laboratory settings. Free, open-source, and user-friendly, PoLA operates on Android devices to facilitate experiment flow, time management, self-report collection, and data logging. Its integration with PoLA's Helpful Assistant (HeLA), an artificial intelligence component, makes protocol creation and revision as simple as providing natural language instructions.

### Installation
1. Visit [https://lkacz.github.io/pocketlabapp/](https://lkacz.github.io/pocketlabapp/) to download the PoLA .apk file.
2. Allow installation from unknown sources in your device's settings.
3. Install the .apk file on your Android device.

### Getting Started
Upon installation, launch PoLA and familiarize yourself with the interface running demo and tutorial protocols. The app requires uploading a study-specific protocol formatted as a .txt file to guide participants through the experiment.

### Creating a Protocol
A protocol is a sequence of instructions stored in a .txt file. Each line in the protocol corresponds to a command that PoLA executes. Protocols can be created in any text editor and easily revised or fully developed using natural language with HeLA.

#### An example protocol for a bogus study: 

```
// This is the name of my study
STUDY_ID;MindfulWalkStudy
// This scale will ask about mood and provide five buttons for the response between stressed and relaxed
SCALE;Pre-Walk Mood;How do you feel right now?;Your Current Mood;Very Stressed;Stressed;Neutral;Relaxed;Very Relaxed
// The participant receives instructions on walking mindfully and start the session by pressing Begin Walk
INSTRUCTION;Mindful Walk Instructions;During your walk, focus on your surroundings. Observe the colors, sounds, and scents around you. If your mind wanders, gently bring your attention back to the present moment.;BEGIN WALK
// Now the participant continues to walk mindfully for 15 minutes
TIMER;Walk Time;Enjoy your mindful walk. Press 'Continue' when you finish.;Continue;900
// Here we check again the mood
SCALE;Post-Walk Mood;How do you feel right now?;Your Current Mood;Very Stressed;Stressed;Neutral;Relaxed;Very Relaxed
// We finish with asking for feedback
INPUTFIELD;Feedback;We value your feedback. Please share your thoughts about the mindful walk.;SUBMIT;Your Experience
```

### Commands Overview
PoLA uses a concise set of commands for various functionalities, including displaying instructions (INSTRUCTION), setting timers (TIMER), collecting self-reports (SCALE, MULTISCALE, INPUTFIELD), and logging data (LOG). The syntax for each command is straightforward, with semicolon-separated elements.

### Uploading the Protocol
1. Connect your device to a computer via USB.
2. Transfer the .txt protocol file to the device's storage.
3. In PoLA, select 'Load Protocol' and navigate to the file.

### Running an Experiment
1. Start the Session: Launch PoLA on the laboratory smartphone and load the protocol.
2. Participant Interaction: Hand the device to the participant to follow the instructions.
3. Data Collection: PoLA will automatically save responses and log data as the participant progresses through the protocol.
4. End of Session: Retrieve the device, and the collected data will be available for analysis in specified output formats.

### Output and Data Analysis
Upon completion of the protocol, PoLA generates output files in both .csv and .xlsx formats, stored under PoLA_Data and PoLA_Backup folders in the device's Documents directory. These files contain time-stamped participant responses and additional information, facilitating easy and quick data analysis.

### Limitations and Considerations
- Requires direct participant-researcher interaction for protocol initiation.
- Designed for moderate complexity experiments; may not support highly specialized tasks.
- Data aggregation across participants requires manual editing.
- The interface and documentation are currently available only in English.
- The AI functionality is experimental and may produce false suggestions that seem correct. 

### Future Directions
The development team is committed to continuous improvement, with future updates potentially including more complex instructions, diverse self-report options, branching logic, graphical customization, and leveraging smartphone sensors. Community involvement is highly encouraged to ensure PoLA evolves in ways that most benefit the research community.

### Developer Information
Lukasz Kaczmarek, PhD., Adam Mickiewicz University, AMU Psychophysiology Laboratory: Gaming & Streaming, Email: lkacz@amu.edu.pl
For inquiries, collaborations, or feedback regarding PoLA and HeLA, please contact Dr. Kaczmarek directly via email.

## Commands

### 1. STUDY_ID
- **Format:** `STUDY_ID;STUDY_ID_TEXT`
- **Function:** This command assigns an identifier to the study, which will be used as a prefix for filenames to help organize data.
- **Example:** `STUDY_ID;MyFirstStudy`
- **Usage:** It helps distinguish data files from different studies or protocols, especially useful when conducting multiple studies on the same device.

### 2. SCALE
- **Format:** `SCALE;HEADER_TEXT;BODY_TEXT;ITEM_TEXT;RESPONSE1_TEXT;...;RESPONSE9_TEXT`
- **Function:** Displays a single question with up to nine response options.
- **Example:** `SCALE;Emotions;Rate your current emotional state;Calmness;Very Low;Low;Moderate;High;Very High`
- **Usage:** It's ideal for collecting single-item self-report measures from participants like mood, satisfaction, or symptom severity.

### 3. MULTISCALE
- **Format:** `MULTISCALE;HEADER_TEXT;BODY_TEXT;[ITEM1_TEXT;ITEM2_TEXT;...];RESPONSE1_TEXT;...;RESPONSE9_TEXT`
- **Function:** Similar to SCALE, but allows presenting multiple items with the same set of response options.
- **Example:** `MULTISCALE;Daily Activities;How satisfied are you with the following activities today?;[Exercise;Work;Social Interaction];Not at all;Somewhat;Moderately;Very;Extremely`
- **Usage:** Useful for evaluating multiple aspects of a participant's experience or behavior using a consistent rating scale.

### 4. RANDOMIZED_MULTISCALE
- **Format:** `RANDOMIZED_MULTISCALE;HEADER_TEXT;BODY_TEXT;[ITEM1_TEXT;ITEM2_TEXT;...];RESPONSE1_TEXT;...;RESPONSE9_TEXT`
- **Function:** Functions like MULTISCALE, but items are presented in a random order each time.
- **Example:** `RANDOMIZED_MULTISCALE;Dietary Choices;Rate how frequently you consumed the following items this week;[Fruits;Vegetables;Fast Food];Never;Rarely;Sometimes;Often;Always`
- **Usage:** Employed to reduce order effects when presenting multiple items to participants.

### 5. TIMER
- **Format:** `TIMER;HEADER_TEXT;BODY_TEXT;BUTTON_TEXT;NumberOfSeconds`
- **Function:** Displays a message and a countdown timer. When the timer expires, a button appears to continue.
- **Example:** `TIMER;Break Time;Take a short break. The next session will start shortly.;Continue;300`
- **Usage:** Useful for scheduling breaks or controlled time intervals between experiment phases.

### 6. INPUTFIELD
- **Format:** `INPUTFIELD;HEADER_TEXT;BODY_TEXT;BUTTON_TEXT;INPUTFIELD#1;...;INPUTFIELD#10`
- **Function:** Displays a form with up to 10 input fields for open-ended responses.
- **Example:** `INPUTFIELD;Demographics;Please provide the following information.;Submit;Age;Gender;Education Level;Employment Status`
- **Usage:** Collects textual data from participants, including demographic information or open feedback.

### 7. INSTRUCTION
- **Format:** `INSTRUCTION;HEADER_TEXT;BODY_TEXT;BUTTON_TEXT`
- **Function:** Shows static instruction or information message with a button to proceed.
- **Example:** `INSTRUCTION;Welcome!;Thank you for participating in this study. When you're ready, please press 'Continue' to start.;Continue`
- **Usage:** Presents instructions, consent information, or other text to participants before or during the experiment.

### 8. TAP_INSTRUCTION
- **Format:** `TAP_INSTRUCTION;HEADER_TEXT;BODY_TEXT;BUTTON_TEXT`
- **Function:** Same as INSTRUCTION, but requires a triple tap to reveal the continue button.
- **Example:** `TAP_INSTRUCTION;Attention Check;To ensure you're paying attention, triple-tap anywhere on the screen to continue.;Continue`
- **Usage:** Ensures participants are actively engaged by requiring a specific interaction before proceeding.

### 9. LOG
- **Format:** `LOG;Text`
- **Function:** Logs a text note in the output file when reached in the protocol.
- **Example:** `LOG;Starting Section 2`
- **Usage:** Useful for marking different phases or sections of the study within the output data for easier analysis.

### 10. END
- **Format:** `END`
- **Function:** Signals the end of the protocol. Commands placed after this command will not be executed.
- **Example:** `END`
- **Usage:** Can be used to prematurely terminate the protocol or to ensure that a specific set of commands is the last to run.

### Comments 
- **Format and Function:** Text not starting with a command identifier is ignored, serving as comments within the protocol file.
- **Examples:** `// This is a comment`, `--- Section divider`
- **Usage:** Organizes and annotates the protocol file for better readability and documentation purposes. HeLA can create or verify commands based on a natural language description of desired functionality. Thus, commenting is a good practice for protocol reuse, modification, or AI-assisted verification.
