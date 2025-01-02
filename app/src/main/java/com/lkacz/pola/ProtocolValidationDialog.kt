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

    private val recognizedCommands = setOf(
        "BODY_ALIGNMENT",
        "BODY_COLOR",
        "BODY_SIZE",
        "CONTINUE_TEXT_COLOR",
        "CONTINUE_ALIGNMENT",
        "CONTINUE_BACKGROUND_COLOR",
        "CONTINUE_SIZE",
        "CUSTOM_HTML",
        "END",
        "GOTO",
        "HEADER_ALIGNMENT",
        "HEADER_COLOR",
        "HEADER_SIZE",
        "INPUTFIELD",
        "INPUTFIELD[RANDOMIZED]",
        "INSTRUCTION",
        "ITEM_SIZE",
        "LABEL",
        "LOG",
        "RANDOMIZE_OFF",
        "RANDOMIZE_ON",
        "RESPONSE_BACKGROUND_COLOR",
        "RESPONSE_SIZE",
        "RESPONSE_TEXT_COLOR",
        "SCALE",
        "SCALE[RANDOMIZED]",
        "SCREEN_BACKGROUND_COLOR",
        "STUDY_ID",
        "TAP_INSTRUCTION",
        "TIMER",
        "TIMER_SOUND",
        "TRANSITIONS"
    )

    private var randomizationLevel = 0
    private val globalErrors = mutableListOf<String>()
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
    ): View {
        val rootLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val headerTable = buildHeaderTable()
        val scrollView = ScrollView(requireContext())
        val contentTable = buildContentTable(getProtocolContent())

        if (randomizationLevel > 0) {
            globalErrors.add("RANDOMIZE_ON not closed by matching RANDOMIZE_OFF")
        }

        if (globalErrors.isNotEmpty()) {
            val row = TableRow(requireContext()).apply {
                setBackgroundColor(Color.parseColor("#FFEEEE"))
                setPadding(16, 8, 16, 8)
            }
            val cell = createBodyCell(
                text = globalErrors.joinToString("\n"),
                weight = 1.0f
            ).apply {
                setTextColor(Color.RED)
                setTypeface(null, Typeface.BOLD)
            }
            cell.layoutParams = TableRow.LayoutParams().apply { span = 3 }
            row.addView(cell)
            contentTable.addView(row)
        }

        scrollView.addView(contentTable)
        rootLayout.addView(headerTable)
        rootLayout.addView(scrollView)
        return rootLayout
    }

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
            headerRow.addView(createHeaderCell("Line", 0.1f))
            headerRow.addView(createHeaderCell("Command", 0.6f))
            headerRow.addView(createHeaderCell("Error(s)", 0.3f))

            addView(headerRow)
        }
    }

    private fun buildContentTable(fileContent: String): TableLayout {
        val context = requireContext()
        val tableLayout = TableLayout(context).apply {
            layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            isStretchAllColumns = true
            setPadding(8, 8, 8, 8)
        }

        val allLines = fileContent.split("\n")
        val labelOccurrences = findLabelOccurrences(allLines)

        allLines.forEachIndexed { index, rawLine ->
            val realLineNumber = index + 1
            val trimmedLine = rawLine.trim()
            val (errorMessage, warningMessage) = validateLine(realLineNumber, trimmedLine, labelOccurrences)

            if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("//")) {
                val highlightedLine = highlightLine(trimmedLine, errorMessage)
                val combinedIssuesSpannable = combineIssues(errorMessage, warningMessage)

                val row = TableRow(context).apply {
                    val backgroundColor = if (index % 2 == 0) {
                        Color.parseColor("#FFFFFF")
                    } else {
                        Color.parseColor("#EEEEEE")
                    }
                    setBackgroundColor(backgroundColor)
                    setPadding(16, 8, 16, 8)
                }
                row.addView(createBodyCell(realLineNumber.toString(), 0.1f))
                row.addView(createBodyCell(highlightedLine, 0.6f))
                row.addView(
                    createBodyCell(
                        text = combinedIssuesSpannable,
                        weight = 0.3f
                    )
                )
                tableLayout.addView(row)
            }
        }
        return tableLayout
    }

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

    private fun validateLine(
        lineNumber: Int,
        line: String,
        labelOccurrences: Map<String, List<Int>>
    ): Pair<String, String> {
        var errorMessage = ""
        var warningMessage = ""

        if (line.isEmpty() || line.startsWith("//")) {
            lastCommand = null
            return Pair(errorMessage, warningMessage)
        }

        // Check for trailing semicolons
        if (line.endsWith(";")) {
            errorMessage = appendError(errorMessage, "Line ends with stray semicolon")
        }

        val parts = line.split(";")
        val commandRaw = parts[0].uppercase()
        val commandRecognized = recognizedCommands.contains(commandRaw)

        // Manage randomization levels
        if (commandRaw == "RANDOMIZE_ON") {
            if (lastCommand?.uppercase() == "RANDOMIZE_ON") {
                errorMessage = appendError(
                    errorMessage,
                    "RANDOMIZE_ON cannot be followed by another RANDOMIZE_ON without an intervening RANDOMIZE_OFF"
                )
            }
            randomizationLevel++
        } else if (commandRaw == "RANDOMIZE_OFF") {
            if (lastCommand?.uppercase() == "RANDOMIZE_OFF") {
                errorMessage = appendError(
                    errorMessage,
                    "RANDOMIZE_OFF should be preceded by RANDOMIZE_ON (not another RANDOMIZE_OFF)"
                )
            }
            if (randomizationLevel <= 0) {
                errorMessage = appendError(
                    errorMessage,
                    "RANDOMIZE_OFF without matching RANDOMIZE_ON"
                )
            } else {
                randomizationLevel--
            }
        }

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
                "HEADER_SIZE", "BODY_SIZE", "ITEM_SIZE", "RESPONSE_SIZE", "CONTINUE_SIZE" -> {
                    val (err, warn) = sizeValidation(commandRaw, parts)
                    if (err.isNotEmpty()) errorMessage = appendError(errorMessage, err)
                    if (warn.isNotEmpty()) warningMessage = appendWarning(warningMessage, warn)
                }
                "SCALE", "SCALE[RANDOMIZED]" -> {
                    if (parts.size < 2) {
                        errorMessage = appendError(
                            errorMessage,
                            "$commandRaw must have at least one parameter after the command"
                        )
                    }
                }
                "INSTRUCTION", "TAP_INSTRUCTION" -> {
                    val semicolonCount = line.count { it == ';' }
                    if (semicolonCount != 3) {
                        errorMessage = appendError(
                            errorMessage,
                            "$commandRaw must have exactly 3 semicolons (4 segments)"
                        )
                    }
                }
                "INPUTFIELD", "INPUTFIELD[RANDOMIZED]" -> {
                    if (parts.size < 4) {
                        errorMessage = appendError(
                            errorMessage,
                            "$commandRaw must have at least 4 segments: e.g. INPUTFIELD;heading;body;button;field1..."
                        )
                    }
                }
                "TIMER" -> {
                    val (err, warn) = timerValidation(parts)
                    if (err.isNotEmpty()) errorMessage = appendError(errorMessage, err)
                    if (warn.isNotEmpty()) warningMessage = appendWarning(warningMessage, warn)
                }
                // Color-based commands
                "HEADER_COLOR", "BODY_COLOR", "RESPONSE_TEXT_COLOR",
                "RESPONSE_BACKGROUND_COLOR", "SCREEN_BACKGROUND_COLOR",
                "CONTINUE_TEXT_COLOR", "CONTINUE_BACKGROUND_COLOR" -> {
                    if (parts.size < 2 || parts[1].isBlank()) {
                        errorMessage = appendError(
                            errorMessage,
                            "$commandRaw missing color value"
                        )
                    } else {
                        val colorStr = parts[1].trim()
                        if (!isValidColor(colorStr)) {
                            errorMessage = appendError(
                                errorMessage,
                                "$commandRaw has invalid color format"
                            )
                        }
                    }
                }
                // Alignment commands
                "HEADER_ALIGNMENT", "BODY_ALIGNMENT", "CONTINUE_ALIGNMENT" -> {
                    if (parts.size < 2 || parts[1].isBlank()) {
                        errorMessage = appendError(errorMessage, "$commandRaw missing alignment value")
                    } else {
                        val alignValue = parts[1].uppercase().trim()
                        val allowedAlignments = setOf("LEFT", "CENTER", "RIGHT")
                        if (!allowedAlignments.contains(alignValue)) {
                            errorMessage = appendError(
                                errorMessage,
                                "$commandRaw must be one of: ${allowedAlignments.joinToString(", ")}"
                            )
                        }
                    }
                }
                "STUDY_ID" -> {
                    // Requires at least one param (the ID)
                    if (parts.size < 2 || parts[1].isBlank()) {
                        errorMessage = appendError(errorMessage, "STUDY_ID missing required value")
                    }
                }
                "GOTO" -> {
                    // Must reference a label
                    if (parts.size < 2 || parts[1].isBlank()) {
                        errorMessage = appendError(errorMessage, "GOTO missing label name")
                    }
                }
                "LOG" -> {
                    // Typically at least one param
                    if (parts.size < 2 || parts[1].isBlank()) {
                        errorMessage = appendError(errorMessage, "LOG requires a message or parameter")
                    }
                }
                "END" -> {
                    // Should be alone (no extra parts)
                    if (parts.size > 1 && parts[1].isNotBlank()) {
                        warningMessage = appendWarning(warningMessage, "END command should not have parameters")
                    }
                }
                "TRANSITIONS" -> {
                    // Off or slide (or maybe other modes if future expansions)
                    val mode = parts.getOrNull(1)?.lowercase()?.trim()
                    if (mode.isNullOrEmpty()) {
                        errorMessage = appendError(errorMessage, "TRANSITIONS missing mode (e.g. off or slide)")
                    } else {
                        if (mode != "off" && mode != "slide") {
                            errorMessage = appendError(
                                errorMessage,
                                "TRANSITIONS mode must be either 'off' or 'slide'"
                            )
                        }
                    }
                }
            }
        }

        // Check for empty HTML tags
        val foundEmptyTags = findEmptyHtmlTags(line)
        if (foundEmptyTags.isNotEmpty()) {
            foundEmptyTags.forEach { tag ->
                warningMessage = appendWarning(warningMessage, "Empty HTML tag: $tag")
            }
        }

        lastCommand = commandRaw
        return Pair(errorMessage, warningMessage)
    }

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
            if (
                !filename.endsWith(".wav", ignoreCase = true) &&
                !filename.endsWith(".mp3", ignoreCase = true)
            ) {
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

    private fun timerValidation(parts: List<String>): Pair<String, String> {
        var err = ""
        var warn = ""
        if (parts.size < 2) {
            err = "TIMER missing numeric value or parameters"
        } else {
            val timeVal = parts.last().trim().toIntOrNull()
            if (timeVal == null || timeVal < 0) {
                err = "TIMER must have a non-negative integer"
            } else if (timeVal > 3600) {
                warn = "TIMER = $timeVal (over 3600s, is that intentional?)"
            }
        }
        return Pair(err, warn)
    }

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
                        .add(index + 1)
                }
            }
        }
        return labelOccurrences
    }

    private fun findEmptyHtmlTags(line: String): List<String> {
        val results = mutableListOf<String>()
        val pattern1 = Pattern.compile("<([a-zA-Z]+)>\\s*</\\1>")
        val matcher1 = pattern1.matcher(line)
        while (matcher1.find()) {
            results.add(matcher1.group() ?: "")
        }
        val pattern2 = Pattern.compile("<\\s*>")
        val matcher2 = pattern2.matcher(line)
        while (matcher2.find()) {
            results.add(matcher2.group() ?: "")
        }
        return results
    }

    private fun setSpanColor(spannable: SpannableString, start: Int, end: Int, color: Int) {
        if (start < 0 || end <= start || end > spannable.length) return
        spannable.setSpan(
            ForegroundColorSpan(color),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun setSpanBold(spannable: SpannableString, start: Int, end: Int) {
        if (start < 0 || end <= start || end > spannable.length) return
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun highlightLine(
        line: String,
        errorMessage: String
    ): SpannableString {
        val combined = SpannableString(line)
        val parts = line.split(";")
        val commandPart = parts[0]
        val cmdIsRecognized = recognizedCommands.contains(commandPart.uppercase())

        val startCmd = 0
        val endCmd = commandPart.length
        if (cmdIsRecognized) {
            setSpanColor(combined, startCmd, endCmd, Color.rgb(0, 100, 0))
        } else {
            setSpanColor(combined, startCmd, endCmd, Color.RED)
        }

        if (commandPart.uppercase() == "LABEL") {
            if (errorMessage.contains("duplicated") || errorMessage.contains("Label is not a single word")) {
                val offset = commandPart.length
                if (offset < line.length) {
                    setSpanColor(combined, offset, line.length, Color.RED)
                }
            }
        }

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

    private fun combineIssues(errorMessage: String, warningMessage: String): SpannableString {
        val combinedText = buildString {
            if (errorMessage.isNotEmpty()) append(errorMessage)
            if (errorMessage.isNotEmpty() && warningMessage.isNotEmpty()) append("\n")
            if (warningMessage.isNotEmpty()) append(warningMessage)
        }

        val spannable = SpannableString(combinedText)
        if (errorMessage.isNotEmpty()) {
            val errorStart = 0
            val errorEnd = errorStart + errorMessage.length
            setSpanColor(spannable, errorStart, errorEnd, Color.RED)
            setSpanBold(spannable, errorStart, errorEnd)
        }
        if (warningMessage.isNotEmpty()) {
            val warnStart = combinedText.indexOf(warningMessage)
            val warnEnd = warnStart + warningMessage.length
            setSpanColor(spannable, warnStart, warnEnd, Color.rgb(255, 165, 0))
            setSpanBold(spannable, warnStart, warnEnd)
        }

        return spannable
    }

    private fun appendError(current: String, newError: String): String {
        return if (current.isEmpty()) newError else "$current; $newError"
    }

    private fun appendWarning(current: String, newWarning: String): String {
        return if (current.isEmpty()) newWarning else "$current; $newWarning"
    }

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

    private fun isValidColor(colorStr: String): Boolean {
        return try {
            Color.parseColor(colorStr)
            true
        } catch (_: Exception) {
            false
        }
    }
}
