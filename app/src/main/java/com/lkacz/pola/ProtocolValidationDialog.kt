// Filename: ProtocolValidationDialog.kt
package com.lkacz.pola

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import java.util.regex.Pattern

class ProtocolValidationDialog : DialogFragment() {

    /**
     * Commands recognized by default.
     */
    private val recognizedCommands = setOf(
        "BRANCH_SCALE",
        "INSTRUCTION",
        "TIMER",
        "TAP_INSTRUCTION",
        "SCALE",
        "MULTISCALE",
        "RANDOMIZED_MULTISCALE",
        "BRANCH_SCALE",
        "INPUTFIELD",
        "CUSTOM_HTML",
        "LABEL",
        "GOTO",
        "TRANSITIONS",
        "TIMER_SOUND",
        "HEADER_SIZE",
        "BODY_SIZE",
        "BUTTON_SIZE",
        "ITEM_SIZE",
        "RESPONSE_SIZE",
        "LOG",
        "END",
        "RANDOMIZATION_ON",
        "RANDOMIZATION_OFF",
        "STUDY_ID"
    )

    // Tracks unclosed randomization blocks
    private var randomizationLevel = 0

    // Collect global errors (e.g., unclosed randomization blocks)
    private val globalErrors = mutableListOf<String>()

    // Keep track of previous command for randomization checks
    private var lastCommand: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Root layout for pinned header + scrollable content
        val rootLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Build the header-only table
        val headerTable = buildHeaderTable()

        // Build the scrollable content table
        val scrollView = ScrollView(requireContext())
        val contentTable = buildContentTable(getProtocolContent())

        // After processing lines, check if randomization is unclosed
        if (randomizationLevel > 0) {
            globalErrors.add("RANDOMIZATION_ON not closed by matching RANDOMIZATION_OFF")
        }

        // If global errors exist, append them as a final row
        if (globalErrors.isNotEmpty()) {
            val row = TableRow(requireContext()).apply {
                setBackgroundColor(Color.parseColor("#FFEEEE")) // slight red background
                setPadding(16, 8, 16, 8)
            }
            val cell = createBodyCell(
                text = globalErrors.joinToString("\n"),
                weight = 1.0f
            ).apply {
                setTextColor(Color.RED)
                setTypeface(null, Typeface.BOLD)
            }
            // Span across all columns (3 columns total)
            cell.layoutParams = TableRow.LayoutParams().apply { span = 3 }
            row.addView(cell)
            contentTable.addView(row)
        }

        // Add the content table to the scroll view
        scrollView.addView(contentTable)

        // Add header table and scrollable content to the root layout
        rootLayout.addView(headerTable)
        rootLayout.addView(scrollView)

        return rootLayout
    }

    /**
     * Builds the pinned header (one row) in its own TableLayout.
     */
    private fun buildHeaderTable(): TableLayout {
        val context = requireContext()
        return TableLayout(context).apply {
            layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            isStretchAllColumns = true
            setPadding(8, 8, 8, 8)

            val headerRow = TableRow(context)
            // Three header cells: "Line", "Command", "Error(s)"
            headerRow.addView(createHeaderCell("Line", 0.1f))
            headerRow.addView(createHeaderCell("Command", 0.6f))
            headerRow.addView(createHeaderCell("Error(s)", 0.3f))

            addView(headerRow)
        }
    }

    /**
     * Builds the main (scrollable) table rows (excluding header).
     *
     * NOTE: We now keep the *entire* file's lines (including empty and comment lines),
     * to preserve their numbering for display in "Line" column.
     */
    private fun buildContentTable(fileContent: String): TableLayout {
        val context = requireContext()
        val tableLayout = TableLayout(context).apply {
            layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            setStretchAllColumns(true)
            setPadding(8, 8, 8, 8)
        }

        // 1) Split all lines, do *not* skip anything at first
        val allLines = fileContent.split("\n")

        // 2) Pre-calculate label occurrences among all lines
        val labelOccurrences = findLabelOccurrences(allLines)

        // 3) Iterate over each line with real lineNumber
        allLines.forEachIndexed { index, rawLine ->
            val realLineNumber = index + 1
            val trimmedLine = rawLine.trim()

            // Validate the line (even if empty or comment) so randomization checks remain correct
            val (errorMessage, warningMessage) = validateLine(realLineNumber, trimmedLine, labelOccurrences)

            // We *only* add a row to the table if the line is not empty/comment
            if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("//")) {
                // Highlight recognized/unrecognized commands + empty HTML tags
                val highlightedLine = highlightLine(trimmedLine, errorMessage, warningMessage)

                // Combine errors & warnings
                val combinedIssuesSpannable = combineIssues(errorMessage, warningMessage)

                // Create row
                val row = TableRow(context).apply {
                    val backgroundColor = if (index % 2 == 0) {
                        Color.parseColor("#FFFFFF")
                    } else {
                        Color.parseColor("#EEEEEE")
                    }
                    setBackgroundColor(backgroundColor)
                    setPadding(16, 8, 16, 8)
                }

                // Column: real line number
                row.addView(createBodyCell(realLineNumber.toString(), 0.1f))
                // Column: command content
                row.addView(createBodyCell(highlightedLine, 0.6f))
                // Column: combined errors/warnings
                row.addView(createBodyCell(
                    text = combinedIssuesSpannable,
                    weight = 0.3f
                ))

                tableLayout.addView(row)
            }
        }

        return tableLayout
    }

    /**
     * Creates a header cell (centered, bold) with specified weight.
     */
    private fun createHeaderCell(headerText: String, weight: Float): TextView {
        return TextView(requireContext()).apply {
            text = headerText
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(24, 16, 24, 16)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weight)
        }
    }

    /**
     * Creates a body cell. If text is a Spannable, uses that. Otherwise uses plain string.
     */
    private fun createBodyCell(
        text: CharSequence,
        weight: Float
    ): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 14f
            isSingleLine = false
            setHorizontallyScrolling(false)
            ellipsize = null
            gravity = Gravity.START
            setPadding(24, 8, 24, 8)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weight)
        }
    }

    // =====================================================================================
    //                             VALIDATION LOGIC
    // =====================================================================================
    /**
     * Validate each line => returns a pair (errorMessage, warningMessage).
     * We do this for all lines, even if they're empty or comment lines,
     * so that randomization checks remain consistent.
     */
    private fun validateLine(
        lineNumber: Int,
        line: String,
        labelOccurrences: Map<String, List<Int>>
    ): Pair<String, String> {
        var errorMessage = ""
        var warningMessage = ""

        // If line is empty or comment, skip standard checks but still do randomization logic if we want
        // Typically we won't parse commands from empty or comment lines, but let's see:
        // "We do not skip the logic entirely because randomization state might get updated if the user typed something"
        // However, if line is empty/comment, it won't have a recognized command. So let's handle that.
        if (line.isEmpty() || line.startsWith("//")) {
            // There's no command to parse, but let's finalize lastCommand if needed
            // Actually we won't do anything except set lastCommand to null if line is blank or comment.
            lastCommand = null
            return Pair(errorMessage, warningMessage)
        }

        // 1) No trailing semicolon
        if (line.endsWith(";")) {
            errorMessage = appendError(errorMessage, "Line ends with stray semicolon")
        }

        // 2) Split at semicolons, identify command
        val parts = line.split(";")
        val commandRaw = parts[0].uppercase()
        val commandRecognized = recognizedCommands.contains(commandRaw)

        // 3) Randomization logic
        if (commandRaw == "RANDOMIZATION_ON") {
            // Check if lastCommand was also RANDOMIZATION_ON => not allowed
            if (lastCommand?.uppercase() == "RANDOMIZATION_ON") {
                errorMessage = appendError(
                    errorMessage,
                    "RANDOMIZATION_ON cannot be followed by another RANDOMIZATION_ON without an intervening RANDOMIZATION_OFF"
                )
            }
            randomizationLevel++
        } else if (commandRaw == "RANDOMIZATION_OFF") {
            // Must be preceded by an ON (and not another OFF)
            if (lastCommand?.uppercase() == "RANDOMIZATION_OFF") {
                errorMessage = appendError(
                    errorMessage,
                    "RANDOMIZATION_OFF should be preceded by RANDOMIZATION_ON (not another RANDOMIZATION_OFF)"
                )
            }
            if (randomizationLevel <= 0) {
                errorMessage = appendError(
                    errorMessage,
                    "RANDOMIZATION_OFF without matching RANDOMIZATION_ON"
                )
            } else {
                randomizationLevel--
            }
        }

        // 4) Command recognition
        if (!commandRecognized) {
            errorMessage = appendError(errorMessage, "Unrecognized command")
        } else {
            when (commandRaw) {
                "LABEL" -> {
                    labelValidation(lineNumber, line, labelOccurrences).forEach {
                        errorMessage = appendError(errorMessage, it)
                    }
                }
                "TIMER_SOUND" -> timerSoundValidation(parts).forEach {
                    errorMessage = appendError(errorMessage, it)
                }
                "CUSTOM_HTML" -> customHtmlValidation(parts).forEach {
                    errorMessage = appendError(errorMessage, it)
                }
                "HEADER_SIZE", "BODY_SIZE", "BUTTON_SIZE", "ITEM_SIZE", "RESPONSE_SIZE" -> {
                    val (err, warn) = sizeValidation(commandRaw, parts)
                    if (err.isNotEmpty()) errorMessage = appendError(errorMessage, err)
                    if (warn.isNotEmpty()) warningMessage = appendWarning(warningMessage, warn)
                }
                "SCALE" -> scaleValidation(line).forEach {
                    errorMessage = appendError(errorMessage, it)
                }
                "INSTRUCTION" -> instructionValidation(line).forEach {
                    errorMessage = appendError(errorMessage, it)
                }
                "TIMER" -> {
                    val (err, warn) = timerValidation(parts)
                    if (err.isNotEmpty()) errorMessage = appendError(errorMessage, err)
                    if (warn.isNotEmpty()) warningMessage = appendWarning(warningMessage, warn)
                }
                "MULTISCALE" -> multiScaleValidation(line).forEach { pair ->
                    // pair can be error or warning => let's assume we prefix them with "ERR:" or "WARN:"
                    // or we can just store them separately
                    if (pair.first == "ERROR") {
                        errorMessage = appendError(errorMessage, pair.second)
                    } else {
                        warningMessage = appendWarning(warningMessage, pair.second)
                    }
                }
            }
        }

        // 5) Check for empty HTML tags => warnings
        val foundEmptyTags = findEmptyHtmlTags(line)
        if (foundEmptyTags.isNotEmpty()) {
            foundEmptyTags.forEach { tag ->
                warningMessage = appendWarning(warningMessage, "Empty HTML tag: $tag")
            }
        }

        // Update last command
        lastCommand = commandRaw

        return Pair(errorMessage, warningMessage)
    }

    /**
     * Validate label duplication and single-word constraints.
     */
    private fun labelValidation(
        lineNumber: Int,
        line: String,
        labelOccurrences: Map<String, List<Int>>
    ): List<String> {
        val errors = mutableListOf<String>()
        val parts = line.split(";")
        val labelName = parts.getOrNull(1)?.trim().orEmpty()
        if (labelName.contains("\\s".toRegex())) {
            errors.add("Label is not a single word")
        }
        val linesUsed = labelOccurrences[labelName]
        if (linesUsed != null && linesUsed.size > 1 && lineNumber in linesUsed) {
            val duplicatesExcludingSelf = linesUsed.filter { it != lineNumber }
            if (duplicatesExcludingSelf.isNotEmpty()) {
                errors.add("Label duplicated with line(s) ${duplicatesExcludingSelf.joinToString(", ")}")
            }
        }
        return errors
    }

    private fun timerSoundValidation(parts: List<String>): List<String> {
        val errors = mutableListOf<String>()
        if (parts.size < 2 || parts[1].isBlank()) {
            errors.add("TIMER_SOUND missing filename")
        } else {
            val filename = parts[1].trim()
            if (!(filename.endsWith(".wav", ignoreCase = true) ||
                        filename.endsWith(".mp3", ignoreCase = true))) {
                errors.add("TIMER_SOUND must be *.wav or *.mp3")
            }
        }
        return errors
    }

    private fun customHtmlValidation(parts: List<String>): List<String> {
        val errors = mutableListOf<String>()
        if (parts.size < 2 || parts[1].isBlank()) {
            errors.add("CUSTOM_HTML missing filename")
        } else {
            val filename = parts[1].trim()
            if (!filename.endsWith(".html", ignoreCase = true)) {
                errors.add("CUSTOM_HTML must be followed by *.html")
            }
        }
        return errors
    }

    private fun sizeValidation(command: String, parts: List<String>): Pair<String, String> {
        var err = ""
        var warn = ""
        if (parts.size < 2 || parts[1].isBlank()) {
            err = "$command missing number"
        } else {
            val num = parts[1].trim().toIntOrNull()
            if (num == null || num <= 0) {
                err = "$command must be a positive integer > 5"
            } else if (num <= 5) {
                err = "$command must be > 5"
            } else if (num > 120) {
                warn = "$command = $num (is that intentional?)"
            }
        }
        return Pair(err, warn)
    }

    private fun scaleValidation(line: String): List<String> {
        val errors = mutableListOf<String>()
        // Must contain at least 4 semicolons => 5 segments
        val semicolonCount = line.count { it == ';' }
        if (semicolonCount < 4) {
            errors.add("SCALE must have at least 4 semicolons (5 segments)")
        }
        return errors
    }

    private fun instructionValidation(line: String): List<String> {
        val errors = mutableListOf<String>()
        // Must contain exactly 3 semicolons => 4 segments
        val semicolonCount = line.count { it == ';' }
        if (semicolonCount != 3) {
            errors.add("INSTRUCTION must have exactly 3 semicolons (4 segments)")
        }
        return errors
    }

    private fun timerValidation(parts: List<String>): Pair<String, String> {
        var err = ""
        var warn = ""
        if (parts.size < 2) {
            err = "TIMER missing numeric value"
        } else {
            val timeVal = parts.last().trim().toIntOrNull()
            if (timeVal == null || timeVal < 0) {
                err = "TIMER must have non-negative integer"
            } else if (timeVal > 3600) {
                warn = "TIMER = $timeVal (over 3600s, is that intentional?)"
            }
        }
        return Pair(err, warn)
    }

    private fun multiScaleValidation(line: String): List<Pair<String, String>> {
        // Return list of (type, message) => type in {"ERROR","WARNING"}
        val output = mutableListOf<Pair<String, String>>()

        // Must contain bracketed items => e.g. MULTISCALE;Header;Body;[Item1;Item2];Resp1;...
        val bracketStart = line.indexOf("[")
        val bracketEnd = line.indexOf("]")
        if (bracketStart < 0 || bracketEnd < 0 || bracketEnd < bracketStart) {
            output.add("ERROR" to "MULTISCALE is missing bracketed items [ITEM1;ITEM2;...]")
        } else {
            val bracketContent = line.substring(bracketStart + 1, bracketEnd).trim()
            if (bracketContent.isEmpty()) {
                output.add("ERROR" to "MULTISCALE has empty bracketed items []")
            } else {
                if (bracketContent.contains(",")) {
                    output.add("ERROR" to "Items in brackets must be separated by semicolons only (no commas)")
                }
                val items = bracketContent.split(";").filter { it.isNotBlank() }
                if (items.size == 1) {
                    output.add("WARNING" to "Only one item in MULTISCALE bracket. Consider using SCALE instead")
                }
            }
        }
        return output
    }

    // =====================================================================================
    //                           HIGHLIGHTING & OUTPUT
    // =====================================================================================
    /**
     * Partially highlight recognized commands (dark green), unrecognized (red),
     * empty HTML tags (orange), etc.
     */
    private fun highlightLine(
        line: String,
        errorMessage: String,
        warningMessage: String
    ): SpannableString {
        val combined = SpannableString(line)
        val parts = line.split(";")
        val commandPart = parts[0]
        val cmdIsRecognized = recognizedCommands.contains(commandPart.uppercase())

        // 1) Color recognized vs unrecognized command
        val startCmd = 0
        val endCmd = commandPart.length
        if (cmdIsRecognized) {
            // Dark green
            setSpanColor(combined, startCmd, endCmd, Color.rgb(0, 100, 0))
        } else {
            // Red
            setSpanColor(combined, startCmd, endCmd, Color.RED)
        }

        // 2) If LABEL is invalid => highlight label portion in red
        if (commandPart.uppercase() == "LABEL") {
            if (errorMessage.contains("duplicated") || errorMessage.contains("Label is not a single word")) {
                // highlight everything after "LABEL;" in red
                val offset = commandPart.length
                if (offset < line.length) {
                    setSpanColor(combined, offset, line.length, Color.RED)
                }
            }
        }

        // 3) Color empty HTML tags => orange
        val emptyTags = findEmptyHtmlTags(line)
        for (tag in emptyTags) {
            val startIndex = line.indexOf(tag)
            if (startIndex != -1) {
                val endIndex = startIndex + tag.length
                setSpanColor(combined, startIndex, endIndex, Color.rgb(255, 165, 0))
            }
        }

        return combined
    }

    /**
     * Combine errors (red) and warnings (orange) in one Spannable string.
     */
    private fun combineIssues(errorMessage: String, warningMessage: String): SpannableString {
        val combinedText = buildString {
            if (errorMessage.isNotEmpty()) append(errorMessage)
            if (errorMessage.isNotEmpty() && warningMessage.isNotEmpty()) append("\n")
            if (warningMessage.isNotEmpty()) append(warningMessage)
        }

        val spannable = SpannableString(combinedText)
        // Color errors in red
        if (errorMessage.isNotEmpty()) {
            val errorStart = 0
            val errorEnd = errorStart + errorMessage.length
            setSpanColor(spannable, errorStart, errorEnd, Color.RED)
            setSpanBold(spannable, errorStart, errorEnd)
        }
        // Color warnings in orange
        if (warningMessage.isNotEmpty()) {
            val warnStart = combinedText.indexOf(warningMessage)
            val warnEnd = warnStart + warningMessage.length
            setSpanColor(spannable, warnStart, warnEnd, Color.rgb(255,165,0))
            setSpanBold(spannable, warnStart, warnEnd)
        }

        return spannable
    }

    // =====================================================================================
    //                           HELPER FUNCTIONS
    // =====================================================================================
    /**
     * Finds label occurrences for duplicates, among *all* lines (including empty & comment).
     */
    private fun findLabelOccurrences(lines: List<String>): MutableMap<String, MutableList<Int>> {
        val labelOccurrences = mutableMapOf<String, MutableList<Int>>()
        lines.forEachIndexed { index, rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.uppercase().startsWith("LABEL;")) {
                val parts = trimmed.split(";")
                val labelName = parts.getOrNull(1)?.trim().orEmpty()
                if (labelName.isNotEmpty()) {
                    labelOccurrences
                        .computeIfAbsent(labelName) { mutableListOf() }
                        .add(index + 1) // real line number
                }
            }
        }
        return labelOccurrences
    }

    /**
     * Regex approach to finding empty HTML tags: <tag></tag> or <>
     */
    private fun findEmptyHtmlTags(line: String): List<String> {
        val results = mutableListOf<String>()
        // Pattern 1: <tag></tag> with no content
        val pattern1 = Pattern.compile("<([a-zA-Z]+)>\\s*</\\1>")
        val matcher1 = pattern1.matcher(line)
        while (matcher1.find()) {
            results.add(matcher1.group() ?: "")
        }
        // Pattern 2: <>
        val pattern2 = Pattern.compile("<\\s*>")
        val matcher2 = pattern2.matcher(line)
        while (matcher2.find()) {
            results.add(matcher2.group() ?: "")
        }
        return results
    }

    /**
     * Sets color on (start..end) in a SpannableString.
     */
    private fun setSpanColor(spannable: SpannableString, start: Int, end: Int, color: Int) {
        if (start < 0 || end <= start || end > spannable.length) return
        spannable.setSpan(
            ForegroundColorSpan(color),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    /**
     * Sets text bold on (start..end).
     */
    private fun setSpanBold(spannable: SpannableString, start: Int, end: Int) {
        if (start < 0 || end <= start || end > spannable.length) return
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    /**
     * Appends a new error with semicolon separation if needed.
     */
    private fun appendError(current: String, newError: String): String {
        return if (current.isEmpty()) newError else "$current; $newError"
    }

    /**
     * Appends a new warning with semicolon separation if needed.
     */
    private fun appendWarning(current: String, newWarning: String): String {
        return if (current.isEmpty()) newWarning else "$current; $newWarning"
    }

    /**
     * Retrieve content from either shared prefs or assets.
     */
    private fun getProtocolContent(): String {
        val prefs = requireContext().getSharedPreferences("ProtocolPrefs", 0)
        val mode = prefs.getString("CURRENT_MODE", null)
        val customUriString = prefs.getString("PROTOCOL_URI", null)

        if (!customUriString.isNullOrEmpty()) {
            val uri = android.net.Uri.parse(customUriString)
            return ProtocolReader().readFileContent(requireContext(), uri)
        }

        return if (mode == "tutorial") {
            ProtocolReader().readFromAssets(requireContext(), "tutorial_protocol.txt")
        } else {
            ProtocolReader().readFromAssets(requireContext(), "demo_protocol.txt")
        }
    }
}
