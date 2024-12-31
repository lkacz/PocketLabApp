// Filename: ProtocolValidationDialog.kt
package com.lkacz.pola

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
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
        "END"
    )

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
            setStretchAllColumns(true)
            setPadding(8, 8, 8, 8)

            // Single header row
            val headerRow = TableRow(context).apply {
                // Remove vertical dividers:
                // showDividers = TableRow.SHOW_DIVIDERS_MIDDLE
                // dividerDrawable = ColorDrawable(Color.DKGRAY)
                // dividerPadding = 2
            }

            // Instead of "Line #", use "Nr"
            headerRow.addView(createHeaderCell("Nr", 0.1f))
            // Instead of "Line Content", use "Command"
            headerRow.addView(createHeaderCell("Command", 0.6f))
            // Keep "Error(s)" label for error/warning messages
            headerRow.addView(createHeaderCell("Error(s)", 0.3f))

            addView(headerRow)
        }
    }

    /**
     * Builds the main (scrollable) table rows (excluding header).
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

        // Pre-calculate label occurrences
        val lines = fileContent.split("\n")
        val labelOccurrences = findLabelOccurrences(lines)

        lines.forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            val trimmedLine = rawLine.trim()

            // Validate line => get (errorMessage, warningMessage)
            val (errorMessage, warningMessage) = validateLine(lineNumber, trimmedLine, labelOccurrences)

            // Highlight recognized/unrecognized commands + empty HTML tags
            val highlightedLine = highlightLine(trimmedLine, errorMessage, warningMessage)

            // Merge both error & warning messages into a single Spannable
            val combinedIssuesSpannable = combineIssues(errorMessage, warningMessage)

            val row = TableRow(context).apply {
                // Remove vertical dividers:
                // showDividers = TableRow.SHOW_DIVIDERS_MIDDLE
                // dividerDrawable = ColorDrawable(Color.DKGRAY)
                // dividerPadding = 2

                // Alternate row background color
                val backgroundColor = if (index % 2 == 0) {
                    Color.parseColor("#EEEEEE") // white
                } else {
                    Color.parseColor("#DDDDDD") // light gray
                }
                setBackgroundColor(backgroundColor)

                // Padding around the row
                setPadding(16, 8, 16, 8)
            }

            // Column: line number (now labeled "Nr")
            row.addView(createBodyCell(lineNumber.toString(), 0.1f))

            // Column: highlighted line (now labeled "Command")
            row.addView(createBodyCell(highlightedLine, 0.6f))

            // Column: combined error/warning messages
            row.addView(createBodyCell(
                text = combinedIssuesSpannable,
                weight = 0.3f
            ))

            tableLayout.addView(row)
        }

        return tableLayout
    }

    /**
     * Creates a header cell with centered, bold text and specified weight.
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

    /**
     * Finds all LABEL occurrences for duplicate checks.
     */
    private fun findLabelOccurrences(lines: List<String>): MutableMap<String, MutableList<Int>> {
        val labelOccurrences = mutableMapOf<String, MutableList<Int>>()
        lines.forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            val trimmedLine = rawLine.trim()
            if (trimmedLine.startsWith("LABEL;")) {
                val parts = trimmedLine.split(";")
                val labelName = parts.getOrNull(1)?.trim().orEmpty()
                if (labelName.isNotEmpty()) {
                    labelOccurrences
                        .computeIfAbsent(labelName) { mutableListOf() }
                        .add(lineNumber)
                }
            }
        }
        return labelOccurrences
    }

    /**
     * Validate each line. Return a pair (errorMessage, warningMessage).
     */
    private fun validateLine(
        lineNumber: Int,
        line: String,
        labelOccurrences: Map<String, List<Int>>
    ): Pair<String, String> {
        var errorMessage = ""
        var warningMessage = ""

        if (line.isNotBlank() && !line.startsWith("//")) {
            val semiIndex = line.indexOf(";")
            val command = if (semiIndex >= 0) {
                line.substring(0, semiIndex).uppercase()
            } else {
                line.uppercase()
            }

            // Command logic
            if (semiIndex >= 0) {
                // We have something like "COMMAND; arguments..."
                if (!recognizedCommands.contains(command)) {
                    errorMessage = appendError(errorMessage, "Unrecognized command")
                } else if (command == "LABEL") {
                    // Check label validity
                    val labelName = line.split(";").getOrNull(1)?.trim().orEmpty()
                    if (labelName.contains("\\s".toRegex())) {
                        errorMessage = appendError(errorMessage, "Label is not a single word")
                    }
                    labelOccurrences[labelName]?.let { linesUsed ->
                        if (linesUsed.size > 1 && lineNumber in linesUsed) {
                            val duplicatesExcludingSelf = linesUsed.filter { it != lineNumber }
                            if (duplicatesExcludingSelf.isNotEmpty()) {
                                errorMessage = appendError(
                                    errorMessage,
                                    "Label duplicated with line(s) ${duplicatesExcludingSelf.joinToString(", ")}"
                                )
                            }
                        }
                    }
                }
            } else {
                // Possibly a single-word command
                if (!recognizedCommands.contains(command)) {
                    errorMessage = appendError(errorMessage, "Unrecognized command")
                }
            }
        }

        // Check for empty HTML tags => warnings
        val foundEmptyTags = findEmptyHtmlTags(line)
        if (foundEmptyTags.isNotEmpty()) {
            foundEmptyTags.forEach { tag ->
                warningMessage = appendWarning(warningMessage, "Empty HTML tag: $tag")
            }
        }

        return Pair(errorMessage, warningMessage)
    }

    /**
     * Regex approach to finding empty HTML tags of form <tag></tag>, or just <>.
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
     * Highlight recognized commands (green), unrecognized (red), empty HTML tags (orange).
     */
    private fun highlightLine(
        line: String,
        errorMessage: String,
        warningMessage: String
    ): SpannableString {
        if (line.isBlank() || line.startsWith("//")) {
            return SpannableString(line)
        }

        val semiIndex = line.indexOf(";")
        val combined = SpannableString(line)

        // Color recognized/unrecognized command
        if (semiIndex == -1) {
            // Single command
            val command = line.uppercase()
            if (recognizedCommands.contains(command)) {
                setSpanColor(combined, 0, line.length, Color.GREEN)
            } else {
                setSpanColor(combined, 0, line.length, Color.RED)
            }
        } else {
            // There's a command part + arguments
            val commandPart = line.substring(0, semiIndex)
            val cmdIsRecognized = recognizedCommands.contains(commandPart.uppercase())
            val cmdColor = if (cmdIsRecognized) Color.GREEN else Color.RED

            // Color the command portion
            setSpanColor(combined, 0, semiIndex, cmdColor)

            // If LABEL is invalid or duplicated => color the label portion in red
            if (commandPart.uppercase() == "LABEL") {
                if (errorMessage.contains("duplicated") || errorMessage.contains("Label is not a single word")) {
                    setSpanColor(combined, semiIndex, line.length, Color.RED)
                }
            }
        }

        // Color empty HTML tags => orange
        val emptyTags = findEmptyHtmlTags(line)
        for (tag in emptyTags) {
            val startIndex = line.indexOf(tag)
            if (startIndex != -1) {
                val endIndex = startIndex + tag.length
                setSpanColor(combined, startIndex, endIndex, Color.rgb(255,165,0))
            }
        }

        return combined
    }

    /**
     * Combine errors (red) and warnings (orange) in a single Spannable string.
     */
    private fun combineIssues(errorMessage: String, warningMessage: String): SpannableString {
        val combinedText = buildString {
            if (errorMessage.isNotEmpty()) append(errorMessage)
            if (errorMessage.isNotEmpty() && warningMessage.isNotEmpty()) append("\n")
            if (warningMessage.isNotEmpty()) append(warningMessage)
        }

        val spannable = SpannableString(combinedText)
        // Color the error portion in red
        if (errorMessage.isNotEmpty()) {
            val errorStart = 0
            val errorEnd = errorStart + errorMessage.length
            setSpanColor(spannable, errorStart, errorEnd, Color.RED)
            setSpanBold(spannable, errorStart, errorEnd)
        }

        // Color the warning portion in orange
        if (warningMessage.isNotEmpty()) {
            val warnStart = combinedText.indexOf(warningMessage)
            val warnEnd = warnStart + warningMessage.length
            setSpanColor(spannable, warnStart, warnEnd, Color.rgb(255,165,0))
            setSpanBold(spannable, warnStart, warnEnd)
        }

        return spannable
    }

    /**
     * Utility to color a substring (start..end) in a given color.
     */
    private fun setSpanColor(spannable: SpannableString, start: Int, end: Int, color: Int) {
        spannable.setSpan(
            ForegroundColorSpan(color),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    /**
     * Utility to set text bold on (start..end).
     */
    private fun setSpanBold(spannable: SpannableString, start: Int, end: Int) {
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    /**
     * Utility to append an error with a separator if needed.
     */
    private fun appendError(current: String, newError: String): String {
        return if (current.isEmpty()) newError else "$current; $newError"
    }

    /**
     * Utility to append a warning with a separator if needed.
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
