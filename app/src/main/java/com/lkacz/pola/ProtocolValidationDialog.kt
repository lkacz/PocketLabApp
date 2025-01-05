// Filename: ProtocolValidationDialog.kt
package com.lkacz.pola

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.*
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
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
        "TIMER",
        "TIMER_SOUND",
        "TIMER_SIZE",
        "TIMER_COLOR",
        "TIMER_ALIGNMENT",
        "TRANSITIONS"
    )

    private val allowedMediaCommands = setOf(
        "INPUTFIELD",
        "INPUTFIELD[RANDOMIZED]",
        "INSTRUCTION",
        "SCALE",
        "SCALE[RANDOMIZED]",
        "TIMER"
    )

    private var randomizationLevel = 0
    private val globalErrors = mutableListOf<String>()
    private var lastCommand: String? = null

    private var allLines: MutableList<String> = mutableListOf()
    private val resourceExistenceMap = mutableMapOf<String, Boolean>()

    private val resourcesFolderUri: Uri? by lazy {
        ResourcesFolderManager(requireContext()).getResourcesFolderUri()
    }

    private var searchQuery: String? = null
    private var filterOption: FilterOption = FilterOption.HIDE_COMMENTS

    private var hasUnsavedChanges = false
        set(value) {
            field = value
            btnSave.isEnabled = value
        }

    private var coloringEnabled = true
    private var semicolonsAsBreaks = false

    private lateinit var btnSave: Button
    private lateinit var btnSaveAs: Button

    private val createDocumentLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri != null) {
                try {
                    requireContext().contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                        FileOutputStream(pfd.fileDescriptor).use { fos ->
                            fos.write(allLines.joinToString("\n").toByteArray(Charsets.UTF_8))
                        }
                    }
                    val prefs = requireContext().getSharedPreferences("ProtocolPrefs", 0)
                    prefs.edit().putString("PROTOCOL_URI", uri.toString()).apply()
                    hasUnsavedChanges = false
                    revalidateAndRefreshUI()
                    Toast.makeText(requireContext(), "Saved as new file.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Error saving file: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(requireContext(), "Save As was cancelled.", Toast.LENGTH_SHORT).show()
            }
        }

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

        val searchContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val searchEditText = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            hint = "Keyword"
        }

        val searchButton = Button(requireContext()).apply {
            text = "Search"
            setOnClickListener {
                searchQuery =
                    searchEditText.text?.toString()?.trim().takeIf { it?.isNotEmpty() == true }
                revalidateAndRefreshUI()
            }
        }

        val clearButton = Button(requireContext()).apply {
            text = "Clear"
            setOnClickListener {
                searchQuery = null
                searchEditText.setText("")
                revalidateAndRefreshUI()
            }
        }

        searchContainer.addView(searchEditText)
        searchContainer.addView(searchButton)
        searchContainer.addView(clearButton)
        rootLayout.addView(searchContainer)

        val filterContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 0, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val spinnerFilter = Spinner(requireContext()).apply {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                FilterOption.values().map { it.displayName }
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter
            setSelection(FilterOption.HIDE_COMMENTS.ordinal, false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selected = FilterOption.values()[position]
                    if (selected != filterOption) {
                        filterOption = selected
                        revalidateAndRefreshUI()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        filterContainer.addView(spinnerFilter)
        rootLayout.addView(filterContainer)

        val togglesContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 0, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val cbColoring = CheckBox(requireContext()).apply {
            text = "Coloring"
            isChecked = coloringEnabled
            setOnCheckedChangeListener { _, isChecked ->
                coloringEnabled = isChecked
                revalidateAndRefreshUI()
            }
        }

        val cbSemicolonsBreak = CheckBox(requireContext()).apply {
            text = "Semicolons as breaks"
            isChecked = semicolonsAsBreaks
            setOnCheckedChangeListener { _, isChecked ->
                semicolonsAsBreaks = isChecked
                revalidateAndRefreshUI()
            }
        }

        togglesContainer.addView(cbColoring)
        togglesContainer.addView(cbSemicolonsBreak)
        rootLayout.addView(togglesContainer)

        val buttonRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 0, 16, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.END
        }

        val btnOk = Button(requireContext()).apply {
            text = "OK"
            setOnClickListener {
                dismiss()
            }
        }

        btnSave = Button(requireContext()).apply {
            text = "SAVE"
            isEnabled = false
            setOnClickListener {
                saveProtocol()
            }
        }

        btnSaveAs = Button(requireContext()).apply {
            text = "SAVE AS"
            setOnClickListener {
                createDocumentLauncher.launch("protocol_modified.txt")
            }
        }

        buttonRow.addView(btnOk)
        buttonRow.addView(btnSave)
        buttonRow.addView(btnSaveAs)
        rootLayout.addView(buttonRow)

        val progressBar = ProgressBar(requireContext()).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }

        rootLayout.addView(progressBar)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val fileContent = getProtocolContent()
            allLines = fileContent.split("\n").toMutableList()

            val bracketedReferences = mutableSetOf<String>()
            for (line in allLines) {
                ResourceFileChecker.findBracketedFiles(line).forEach {
                    bracketedReferences.add(it)
                }
            }
            if (resourcesFolderUri != null) {
                for (fileRef in bracketedReferences) {
                    val doesExist = ResourceFileChecker.fileExistsInResources(
                        requireContext(),
                        fileRef
                    )
                    resourceExistenceMap[fileRef] = doesExist
                }
            }

            withContext(Dispatchers.Main) {
                rootLayout.removeView(progressBar)
                val finalView = buildCompletedView()
                rootLayout.addView(finalView)
            }
        }

        return rootLayout
    }

    private fun revalidateAndRefreshUI() {
        randomizationLevel = 0
        globalErrors.clear()
        lastCommand = null
        resourceExistenceMap.clear()

        val bracketedReferences = mutableSetOf<String>()
        for (line in allLines) {
            ResourceFileChecker.findBracketedFiles(line).forEach {
                bracketedReferences.add(it)
            }
        }
        if (resourcesFolderUri != null) {
            bracketedReferences.forEach { fileRef ->
                val doesExist = ResourceFileChecker.fileExistsInResources(requireContext(), fileRef)
                resourceExistenceMap[fileRef] = doesExist
            }
        }

        val containerLayout = view as? LinearLayout ?: return
        while (containerLayout.childCount > 4) {
            containerLayout.removeViewAt(4)
        }
        containerLayout.addView(buildCompletedView())
    }

    private fun buildCompletedView(): View {
        val containerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val headerTable = buildHeaderTable()
        val scrollView = ScrollView(requireContext())
        val contentTable = buildContentTable()

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
        containerLayout.addView(headerTable)
        containerLayout.addView(scrollView)
        return containerLayout
    }

    private fun buildHeaderTable(): TableLayout {
        val context = requireContext()
        return TableLayout(context).apply {
            layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            // Let columns be stretchable
            isStretchAllColumns = false
            setColumnStretchable(1, true)
            setColumnStretchable(2, true)
            setPadding(8, 8, 8, 8)

            val headerRow = TableRow(context)
            headerRow.addView(createHeaderCell("Line", Gravity.END))
            headerRow.addView(createHeaderCell("Command", Gravity.START))
            headerRow.addView(createHeaderCell("Error(s)", Gravity.START))
            addView(headerRow)
        }
    }

    private fun buildContentTable(): TableLayout {
        val context = requireContext()
        val tableLayout = TableLayout(context).apply {
            layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            isStretchAllColumns = false
            setColumnStretchable(1, true)
            setColumnStretchable(2, true)
            setPadding(8, 8, 8, 8)
        }

        val linesWithIndices = allLines.mapIndexed { index, line ->
            (index + 1) to line
        }

        val labelOccurrences = findLabelOccurrences(allLines)

        val filteredLines = linesWithIndices.filter { (originalIndex, rawLine) ->
            lineMatchesCurrentFilter(originalIndex - 1, rawLine, labelOccurrences)
        }

        filteredLines.forEach { (originalLineNumber, rawLine) ->
            val trimmedLine = rawLine.trim()
            val (errorMessage, warningMessage) =
                validateLine(originalLineNumber, trimmedLine, labelOccurrences)

            val lineContent =
                if (coloringEnabled) highlightLine(trimmedLine, semicolonsAsBreaks)
                else {
                    if (semicolonsAsBreaks) {
                        SpannableString(trimmedLine.replace(";", "\n"))
                    } else {
                        SpannableString(trimmedLine)
                    }
                }

            val combinedIssuesSpannable = colorizeIssues(errorMessage, warningMessage)

            val row = TableRow(context).apply {
                val backgroundColor = if ((originalLineNumber % 2) == 0) {
                    Color.parseColor("#FFFFFF")
                } else {
                    Color.parseColor("#EEEEEE")
                }
                setBackgroundColor(backgroundColor)
                setPadding(16, 8, 16, 8)
            }

            // Line number cell
            row.addView(createLineNumberCell(originalLineNumber))

            // Command column
            val commandCell = createBodyCell(lineContent, 2.0f)
            commandCell.setOnClickListener {
                showEditLineDialog(originalLineNumber - 1)
            }
            row.addView(commandCell)

            // Error(s) column
            row.addView(createBodyCell(combinedIssuesSpannable, 1.0f))

            tableLayout.addView(row)
        }

        return tableLayout
    }

    private fun lineMatchesCurrentFilter(
        zeroBasedIndex: Int,
        rawLine: String,
        labelOccurrences: Map<String, List<Int>>
    ): Boolean {
        if (!searchQuery.isNullOrBlank()) {
            if (!rawLine.contains(searchQuery!!, ignoreCase = true)) {
                return false
            }
        }
        val trimmedLine = rawLine.trim()
        val lineNumber = zeroBasedIndex + 1
        val (errMsg, warnMsg) = validateLine(lineNumber, trimmedLine, labelOccurrences)
        val hasErrors = errMsg.isNotEmpty()
        val hasWarnings = warnMsg.isNotEmpty()

        return when (filterOption) {
            FilterOption.EVERYTHING -> true
            FilterOption.HIDE_COMMENTS -> {
                if (trimmedLine.isBlank() || trimmedLine.startsWith("//")) {
                    false
                } else {
                    true
                }
            }
            FilterOption.ERRORS_WARNINGS_ONLY -> (hasErrors || hasWarnings)
            FilterOption.ERRORS_ONLY -> hasErrors
        }
    }

    private fun showEditLineDialog(lineIndex: Int) {
        val context = requireContext()
        val originalLine = allLines[lineIndex]

        val editText = EditText(context).apply {
            setText(originalLine)
            setSingleLine(false)
            isFocusable = true
            isFocusableInTouchMode = true
            setPadding(16, 16, 16, 16)
        }

        AlertDialog.Builder(context)
            .setTitle("Edit Line #${lineIndex + 1}")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newLine = editText.text.toString()
                if (newLine != originalLine) {
                    allLines[lineIndex] = newLine
                    hasUnsavedChanges = true
                }
                revalidateAndRefreshUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createHeaderCell(headerText: String, gravity: Int): TextView {
        return TextView(requireContext()).apply {
            text = headerText
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            this.gravity = gravity
            setPadding(24, 16, 24, 16)
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun createLineNumberCell(lineNumber: Int): TextView {
        return TextView(requireContext()).apply {
            text = lineNumber.toString()
            textSize = 12f
            setSingleLine(true)
            gravity = Gravity.END
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            setPadding(4, 4, 4, 4)
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
            gravity = Gravity.START
            setPadding(24, 8, 24, 8)
            layoutParams = TableRow.LayoutParams(
                /* width = */ 0,
                /* height = */ TableRow.LayoutParams.WRAP_CONTENT,
                /* weight = */ weight
            )
        }
    }

    private fun validateLine(
        lineNumber: Int,
        line: String,
        labelOccurrences: Map<String, List<Int>>
    ): Pair<String, String> {
        var errorMessage = ""
        var warningMessage = ""

        if (line.startsWith("//") || line.isEmpty()) {
            lastCommand = null
            return Pair(errorMessage, warningMessage)
        }
        if (line.endsWith(";")) {
            errorMessage = appendError(errorMessage, "Line ends with stray semicolon")
        }

        val parts = line.split(";")
        val commandRaw = parts[0].uppercase()
        val commandRecognized = recognizedCommands.contains(commandRaw)

        when (commandRaw) {
            "RANDOMIZE_ON" -> {
                if (lastCommand?.uppercase() == "RANDOMIZE_ON") {
                    errorMessage = appendError(
                        errorMessage,
                        "RANDOMIZE_ON cannot be followed by another RANDOMIZE_ON without an intervening RANDOMIZE_OFF"
                    )
                }
                randomizationLevel++
            }
            "RANDOMIZE_OFF" -> {
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
        }

        if (!commandRecognized) {
            errorMessage = appendError(errorMessage, "Unrecognized command")
            return Pair(errorMessage, warningMessage)
        } else {
            val result = handleKnownCommandValidations(
                commandRaw,
                parts,
                lineNumber,
                line,
                labelOccurrences,
                errorMessage,
                warningMessage
            )
            errorMessage = result.first
            warningMessage = result.second
        }

        val bracketedFiles = ResourceFileChecker.findBracketedFiles(line)
        if (bracketedFiles.isNotEmpty()) {
            for (fileRef in bracketedFiles) {
                if (resourcesFolderUri != null && resourceExistenceMap[fileRef] == false) {
                    errorMessage =
                        appendError(errorMessage, "File '$fileRef' not found in resources folder.")
                }
                val fileError = checkFileUsageRules(commandRaw, fileRef)
                if (fileError.isNotEmpty()) {
                    errorMessage = appendError(errorMessage, fileError)
                }
            }
        }

        val foundEmptyTags = findEmptyHtmlTags(line)
        if (foundEmptyTags.isNotEmpty()) {
            foundEmptyTags.forEach { tag ->
                warningMessage = appendWarning(warningMessage, "Empty HTML tag: $tag")
            }
        }

        lastCommand = commandRaw
        return Pair(errorMessage, warningMessage)
    }

    private fun handleKnownCommandValidations(
        commandRaw: String,
        parts: List<String>,
        lineNumber: Int,
        line: String,
        labelOccurrences: Map<String, List<Int>>,
        existingError: String,
        existingWarning: String
    ): Pair<String, String> {
        var errorMessage = existingError
        var warningMessage = existingWarning

        when (commandRaw) {
            "LABEL" -> {
                labelValidation(lineNumber, line, labelOccurrences).forEach {
                    errorMessage = appendError(errorMessage, it)
                }
            }
            "TIMER_SOUND" -> {
                timerSoundValidation(parts).forEach {
                    errorMessage = appendError(errorMessage, it)
                }
                if (parts.size > 1 && parts[1].isNotBlank()) {
                    val fileRef = parts[1].trim()
                    if (resourcesFolderUri != null) {
                        val found =
                            ResourceFileChecker.fileExistsInResources(requireContext(), fileRef)
                        if (!found) {
                            errorMessage = appendError(
                                errorMessage,
                                "Sound file '$fileRef' not found in resources folder."
                            )
                        }
                    }
                }
            }
            "CUSTOM_HTML" -> {
                customHtmlValidation(parts).forEach {
                    errorMessage = appendError(errorMessage, it)
                }
            }
            "HEADER_SIZE", "BODY_SIZE", "ITEM_SIZE", "RESPONSE_SIZE", "CONTINUE_SIZE", "TIMER_SIZE" -> {
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
            "INSTRUCTION" -> {
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
                        "$commandRaw must have at least 4 segments: e.g. INPUTFIELD;HEADER;BODY;[field1;field2;...];CONTINUE_TEXT"
                    )
                }
            }
            "TIMER" -> {
                val (err, warn) = timerValidation(parts)
                if (err.isNotEmpty()) errorMessage = appendError(errorMessage, err)
                if (warn.isNotEmpty()) warningMessage = appendWarning(warningMessage, warn)
            }
            "HEADER_COLOR", "BODY_COLOR", "RESPONSE_TEXT_COLOR",
            "RESPONSE_BACKGROUND_COLOR", "SCREEN_BACKGROUND_COLOR",
            "CONTINUE_TEXT_COLOR", "CONTINUE_BACKGROUND_COLOR", "TIMER_COLOR" -> {
                if (parts.size < 2 || parts[1].isBlank()) {
                    errorMessage = appendError(errorMessage, "$commandRaw missing color value")
                } else {
                    val colorStr = parts[1].trim()
                    if (!isValidColor(colorStr)) {
                        errorMessage =
                            appendError(errorMessage, "$commandRaw has invalid color format")
                    }
                }
            }
            "HEADER_ALIGNMENT", "BODY_ALIGNMENT", "CONTINUE_ALIGNMENT", "TIMER_ALIGNMENT" -> {
                if (parts.size < 2 || parts[1].isBlank()) {
                    errorMessage =
                        appendError(errorMessage, "$commandRaw missing alignment value")
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
                if (parts.size < 2 || parts[1].isBlank()) {
                    errorMessage = appendError(errorMessage, "STUDY_ID missing required value")
                }
            }
            "GOTO" -> {
                if (parts.size < 2 || parts[1].isBlank()) {
                    errorMessage = appendError(errorMessage, "GOTO missing label name")
                }
            }
            "LOG" -> {
                if (parts.size < 2 || parts[1].isBlank()) {
                    errorMessage = appendError(errorMessage, "LOG requires a message or parameter")
                }
            }
            "END" -> {
                if (parts.size > 1 && parts[1].isNotBlank()) {
                    warningMessage =
                        appendWarning(warningMessage, "END command should not have parameters")
                }
            }
            "TRANSITIONS" -> {
                val mode = parts.getOrNull(1)?.lowercase()?.trim()
                if (mode.isNullOrEmpty()) {
                    errorMessage =
                        appendError(
                            errorMessage,
                            "TRANSITIONS missing mode (e.g. off or slide or dissolve)"
                        )
                } else if (mode !in listOf("off", "slide", "slideleft", "dissolve", "fade")) {
                    errorMessage = appendError(
                        errorMessage,
                        "TRANSITIONS mode must be either 'off', 'slide', 'slideleft', 'dissolve', or 'fade'"
                    )
                }
            }
        }
        return errorMessage to warningMessage
    }

    private fun checkFileUsageRules(command: String, fileRef: String): String {
        val lowerFile = fileRef.lowercase()
        val knownExtensions = listOf(".mp3", ".wav", ".mp4", ".jpg", ".png", ".html")
        val matchedExt = knownExtensions.firstOrNull { lowerFile.endsWith(it) } ?: return ""

        val mainAllowed = allowedMediaCommands.contains(command)
        val isCustomHtml = (command == "CUSTOM_HTML")
        val isTimerSound = (command == "TIMER_SOUND")

        if (!mainAllowed && !isCustomHtml && !isTimerSound) {
            return "Command '$command' cannot reference media or .html files, found <$fileRef>"
        }
        if (isCustomHtml && matchedExt != ".html") {
            return "CUSTOM_HTML only accepts .html files, found <$fileRef>"
        }
        if (isTimerSound && (matchedExt != ".mp3" && matchedExt != ".wav")) {
            return "TIMER_SOUND only accepts .mp3 or .wav files, found <$fileRef>"
        }
        return ""
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
        return err to warn
    }

    private fun timerValidation(parts: List<String>): Pair<String, String> {
        var err = ""
        var warn = ""
        if (parts.size != 5) {
            err = "TIMER must have exactly 4 semicolons (5 segments): TIMER;HEADER;BODY;TIME_IN_SECONDS;CONTINUE_TEXT"
            return err to warn
        }

        val timeVal = parts[3].trim().toIntOrNull()
        if (timeVal == null || timeVal < 0) {
            err = appendError(err, "TIMER must have a non-negative integer in the 4th segment")
        } else if (timeVal > 3600) {
            warn = appendWarning(warn, "TIMER = $timeVal (over 3600s, is that intentional?)")
        }
        return err to warn
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

    private fun highlightLine(
        line: String,
        treatSemicolonsAsLineBreaks: Boolean
    ): SpannableStringBuilder {
        val tokens = line.split(";")
        val spannableBuilder = SpannableStringBuilder()

        if (line.startsWith("//") || line.isBlank()) {
            spannableBuilder.append(line)
            return spannableBuilder
        }

        for ((index, token) in tokens.withIndex()) {
            val start = spannableBuilder.length
            spannableBuilder.append(token)
            val end = spannableBuilder.length

            val commandUpper = tokens.firstOrNull()?.uppercase().orEmpty()
            val colorSpan = BackgroundColorSpan(
                chooseTokenBackgroundColor(commandUpper, index, tokens.size)
            )
            spannableBuilder.setSpan(colorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            if (index == 0) {
                spannableBuilder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            if (index < tokens.size - 1) {
                if (treatSemicolonsAsLineBreaks) {
                    spannableBuilder.append("\n")
                } else {
                    spannableBuilder.append(";")
                }
            }
        }
        return spannableBuilder
    }

    private fun chooseTokenBackgroundColor(
        commandUpper: String,
        tokenIndex: Int,
        totalTokens: Int
    ): Int {
        val BG_COMMAND = "#CCFFCC"
        val BG_HEADER = "#ADD8E6"
        val BG_BODY = "#FFFFE0"
        val BG_RESPONSES = "#40E0D0"
        val BG_CONTINUE = "#80FF80"

        if (tokenIndex == 0) {
            return Color.parseColor(BG_COMMAND)
        }

        return when (commandUpper) {
            "INSTRUCTION" -> {
                when (tokenIndex) {
                    1 -> Color.parseColor(BG_HEADER)
                    2 -> Color.parseColor(BG_BODY)
                    3 -> Color.parseColor(BG_CONTINUE)
                    else -> Color.TRANSPARENT
                }
            }
            "TIMER" -> {
                when (tokenIndex) {
                    1 -> Color.parseColor(BG_HEADER)
                    2 -> Color.parseColor(BG_BODY)
                    3 -> Color.parseColor(BG_RESPONSES)
                    4 -> Color.parseColor(BG_CONTINUE)
                    else -> Color.TRANSPARENT
                }
            }
            "SCALE", "SCALE[RANDOMIZED]" -> {
                when {
                    tokenIndex == 1 -> Color.parseColor(BG_HEADER)
                    tokenIndex == 2 -> Color.parseColor(BG_BODY)
                    tokenIndex >= 3 -> Color.parseColor(BG_RESPONSES)
                    else -> Color.TRANSPARENT
                }
            }
            "INPUTFIELD", "INPUTFIELD[RANDOMIZED]" -> {
                when {
                    tokenIndex == 1 -> Color.parseColor(BG_HEADER)
                    tokenIndex == 2 -> Color.parseColor(BG_BODY)
                    tokenIndex == totalTokens - 1 -> Color.parseColor(BG_CONTINUE)
                    tokenIndex >= 3 -> Color.parseColor(BG_RESPONSES)
                    else -> Color.TRANSPARENT
                }
            }
            else -> {
                if (tokenIndex == 1) {
                    Color.parseColor(BG_RESPONSES)
                } else {
                    Color.TRANSPARENT
                }
            }
        }
    }

    private fun colorizeIssues(errorMessage: String, warningMessage: String): SpannableString {
        val combinedText = buildString {
            if (errorMessage.isNotEmpty()) append(errorMessage)
            if (errorMessage.isNotEmpty() && warningMessage.isNotEmpty()) append("\n")
            if (warningMessage.isNotEmpty()) append(warningMessage)
        }

        val spannable = SpannableString(combinedText)

        if (errorMessage.isNotEmpty()) {
            val errorLength = errorMessage.length
            spannable.setSpan(
                ForegroundColorSpan(Color.RED),
                0,
                errorLength,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (warningMessage.isNotEmpty()) {
            val startIndex = errorMessage.length + if (errorMessage.isNotEmpty()) 1 else 0
            val endIndex = startIndex + warningMessage.length
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#FFA500")),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    private fun isValidColor(colorStr: String): Boolean {
        return try {
            Color.parseColor(colorStr)
            true
        } catch (_: Exception) {
            false
        }
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
            val uri = Uri.parse(customUriString)
            return ProtocolReader().readFileContent(requireContext(), uri)
        }
        return if (mode == "tutorial") {
            ProtocolReader().readFromAssets(requireContext(), "tutorial_protocol.txt")
        } else {
            ProtocolReader().readFromAssets(requireContext(), "demo_protocol.txt")
        }
    }

    private enum class FilterOption(val displayName: String) {
        EVERYTHING("Everything"),
        HIDE_COMMENTS("Hide comments"),
        ERRORS_WARNINGS_ONLY("Errors & Warnings"),
        ERRORS_ONLY("Only Errors")
    }

    private fun saveProtocol() {
        val prefs = requireContext().getSharedPreferences("ProtocolPrefs", 0)
        val customUriString = prefs.getString("PROTOCOL_URI", null)
        if (customUriString.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No file to save into.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = Uri.parse(customUriString)
        try {
            requireContext().contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).use { fos ->
                    fos.write(allLines.joinToString("\n").toByteArray(Charsets.UTF_8))
                }
            }
            hasUnsavedChanges = false
            revalidateAndRefreshUI()
            Toast.makeText(requireContext(), "Protocol saved.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error saving file: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
