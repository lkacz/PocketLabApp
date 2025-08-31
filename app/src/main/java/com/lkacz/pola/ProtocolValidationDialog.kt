// Filename: ProtocolValidationDialog.kt
package com.lkacz.pola

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
    private val recognizedCommands =
        setOf(
            "BODY_ALIGNMENT",
            "BODY_COLOR",
            "BODY_SIZE",
            "CONTINUE_TEXT_COLOR",
            "CONTINUE_ALIGNMENT",
            "CONTINUE_BACKGROUND_COLOR",
            "CONTINUE_SIZE",
            "HTML", // Replaced CUSTOM_HTML with HTML
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
            "TRANSITIONS",
        )

    private val allowedMediaCommands =
        setOf(
            "INPUTFIELD",
            "INPUTFIELD[RANDOMIZED]",
            "INSTRUCTION",
            "SCALE",
            "SCALE[RANDOMIZED]",
            "TIMER",
        )

    private var randomizationLevel = 0
    private val globalErrors = mutableListOf<String>()
    private var lastCommand: String? = null

    private var allLines: MutableList<String> = mutableListOf()
    private val resourceExistenceMap = mutableMapOf<String, Boolean>()
    private data class ValidationEntry(
        val lineNumber: Int,
        val rawLine: String,
        val trimmedLine: String,
        val error: String,
        val warning: String,
    )
    private var validationCache: List<ValidationEntry> = emptyList()
    private val pureValidator = ProtocolValidator()
    private var issueLineNumbers: List<Int> = emptyList()
    private var issueIndex: Int = -1
    private val lineRowMap = mutableMapOf<Int, TableRow>()
    private var scrollViewRef: ScrollView? = null

    // Launcher for exporting validation report
    private val exportReportLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri != null) {
                try {
                    requireContext().contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                        FileOutputStream(pfd.fileDescriptor).use { fos ->
                            fos.write(generateReportText().toByteArray(Charsets.UTF_8))
                        }
                    }
                    Toast.makeText(requireContext(), "Report exported", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Export cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    private val resourcesFolderUri: Uri? by lazy {
        ResourcesFolderManager(requireContext()).getResourcesFolderUri()
    }

    private var searchQuery: String? = null
    private var filterOption: FilterOption = FilterOption.HIDE_COMMENTS

    private var hasUnsavedChanges = false

    private var coloringEnabled = true
    private var semicolonsAsBreaks = true

    private lateinit var btnLoad: Button
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
                    val prefs = requireContext().getSharedPreferences(Prefs.NAME, 0)
                    prefs.edit()
                        .putString(Prefs.KEY_PROTOCOL_URI, uri.toString())
                        .putString(Prefs.KEY_CURRENT_PROTOCOL_NAME, getFileName(uri) ?: "Untitled")
                        .apply()
                    hasUnsavedChanges = false
                    revalidateAndRefreshUI()
                    Toast.makeText(requireContext(), "Saved as new file.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Error saving file: ${e.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            } else {
                Toast.makeText(requireContext(), "Save As was cancelled.", Toast.LENGTH_SHORT).show()
            }
        }

    private val openDocumentLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (_: SecurityException) {
                }
                val prefs = requireContext().getSharedPreferences(Prefs.NAME, 0)
                prefs.edit()
                    .putString(Prefs.KEY_PROTOCOL_URI, uri.toString())
                    .putString(Prefs.KEY_CURRENT_PROTOCOL_NAME, getFileName(uri) ?: "Untitled")
                    .apply()
                revalidateAndRefreshUI()
                Toast.makeText(requireContext(), "Protocol loaded.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Loading was cancelled.", Toast.LENGTH_SHORT).show()
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
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val rootLayout =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }

    // (Removed old topButtonRow layout; replaced with card-based action layout)

        // Helper to create a horizontal flow of buttons with spacing
        fun materialButton(text: String, styleAttr: Int, iconRes: Int? = null, onClick: () -> Unit): com.google.android.material.button.MaterialButton {
            return com.google.android.material.button.MaterialButton(requireContext(), null, styleAttr).apply {
                this.text = text
                isAllCaps = false
                if (iconRes != null) {
                    icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), iconRes)
                    iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
                    iconPadding = 16
                }
                setOnClickListener { onClick() }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 12 }
            }
        }

        val actionRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

    btnLoad = materialButton(getString(R.string.action_load), com.google.android.material.R.attr.materialButtonOutlinedStyle, R.drawable.ic_folder_open) { confirmLoadProtocol() }.apply { contentDescription = getString(R.string.cd_load_protocol) }
    btnSave = materialButton(getString(R.string.action_save), com.google.android.material.R.attr.materialButtonStyle, R.drawable.ic_save) { confirmSaveDialog() }.apply { contentDescription = getString(R.string.cd_save_protocol) }
    btnSaveAs = materialButton(getString(R.string.action_save_as), com.google.android.material.R.attr.materialButtonOutlinedStyle, R.drawable.ic_save_as) { createDocumentLauncher.launch("protocol_modified.txt") }.apply { contentDescription = getString(R.string.cd_save_as_protocol) }
    val btnClose = materialButton(getString(R.string.action_close), com.google.android.material.R.attr.materialButtonOutlinedStyle, R.drawable.ic_close) { confirmCloseDialog() }.apply { contentDescription = getString(R.string.cd_close_dialog) }

        actionRow.addView(btnLoad)
        actionRow.addView(btnSave)
        actionRow.addView(btnSaveAs)
        actionRow.addView(btnClose.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })

        // Wrap in a MaterialCardView for consistency with Start screen sections
        val actionsCard = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(16,16,16,8) }
            radius = 12f
            strokeWidth = 1
            setContentPadding(24,24,24,16)
            addView(actionRow)
        }
        rootLayout.addView(actionsCard)

        val searchRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val searchEditText =
            EditText(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    )
                hint = "Keyword"
            }

    val searchButton = materialButton("Search", com.google.android.material.R.attr.materialButtonStyle, R.drawable.ic_search) {
            searchQuery = searchEditText.text?.toString()?.trim().takeIf { it?.isNotEmpty() == true }
            revalidateAndRefreshUI()
        }

    val clearButton = materialButton("Clear", com.google.android.material.R.attr.materialButtonOutlinedStyle, R.drawable.ic_close) {
            searchQuery = null
            searchEditText.setText("")
            revalidateAndRefreshUI()
        }
        searchRow.addView(searchEditText)
        searchRow.addView(searchButton)
        searchRow.addView(clearButton)
        val searchCard = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(16,8,16,8) }
            radius = 12f
            strokeWidth = 1
            setContentPadding(24,24,24,16)
            addView(searchRow)
        }
        rootLayout.addView(searchCard)

        val filterContainer =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 0, 16, 16)
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }

        val spinnerFilter =
            Spinner(requireContext()).apply {
                val adapter =
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        FilterOption.values().map { getString(it.labelRes) },
                    )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                this.adapter = adapter
                setSelection(FilterOption.HIDE_COMMENTS.ordinal, false)
                onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long,
                        ) {
                            val selected = FilterOption.values()[position]
                            if (selected != filterOption) {
                                filterOption = selected
                                revalidateAndRefreshUI()
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }

        filterContainer.addView(spinnerFilter)
        // Wrap filter in a card for visual consistency
        val filterCard = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(16,8,16,4) }
            radius = 12f
            strokeWidth = 1
            setContentPadding(24,16,24,8)
            addView(filterContainer)
        }
        rootLayout.addView(filterCard)

        val togglesContainer =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 0, 16, 16)
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }

        val cbColoring =
            CheckBox(requireContext()).apply {
                text = getString(R.string.toggle_highlight_commands)
                isChecked = coloringEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    coloringEnabled = isChecked
                    revalidateAndRefreshUI()
                }
            }

        val cbSemicolonsBreak =
            CheckBox(requireContext()).apply {
                text = getString(R.string.toggle_split_commands)
                isChecked = semicolonsAsBreaks
                setOnCheckedChangeListener { _, isChecked ->
                    semicolonsAsBreaks = isChecked
                    revalidateAndRefreshUI()
                }
            }

        togglesContainer.addView(cbColoring)
        togglesContainer.addView(cbSemicolonsBreak)
        val togglesCard = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(16,4,16,8) }
            radius = 12f
            strokeWidth = 1
            setContentPadding(24,16,24,16)
            addView(togglesContainer)
        }
        rootLayout.addView(togglesCard)

        // Navigation + Export row
        val navRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        fun tinyButton(text: String, iconRes: Int, onClick: () -> Unit): com.google.android.material.button.MaterialButton =
            com.google.android.material.button.MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                this.text = text
                isAllCaps = false
                icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), iconRes)
                iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
                iconPadding = 12
                setOnClickListener { onClick() }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 12 }
            }
    val btnPrev = tinyButton(getString(R.string.action_prev), R.drawable.ic_prev) { navigateIssue(-1) }.apply { contentDescription = getString(R.string.cd_prev_issue) }
    val btnNext = tinyButton(getString(R.string.action_next), R.drawable.ic_next) { navigateIssue(1) }.apply { contentDescription = getString(R.string.cd_next_issue) }
    val btnExport = tinyButton(getString(R.string.action_export), R.drawable.ic_export) { exportReportLauncher.launch("protocol_validation_report.txt") }.apply { contentDescription = getString(R.string.cd_export_report) }
        navRow.addView(btnPrev)
        navRow.addView(btnNext)
        navRow.addView(btnExport)
        val navCard = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(16,8,16,8) }
            radius = 12f
            strokeWidth = 1
            setContentPadding(24,24,24,16)
            addView(navRow)
        }
        rootLayout.addView(navCard)

        val progressBar =
            ProgressBar(requireContext()).apply {
                isIndeterminate = true
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { gravity = Gravity.CENTER_HORIZONTAL }
            }
        rootLayout.addView(progressBar)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val fileContent = getProtocolContent()
            val lines = fileContent.split("\n")
            allLines = mergeLongFormatCommands(lines).toMutableList()

            val bracketedReferences = mutableSetOf<String>()
            for (line in allLines) {
                ResourceFileChecker.findBracketedFiles(line).forEach {
                    bracketedReferences.add(it)
                }
            }
            if (resourcesFolderUri != null) {
                for (fileRef in bracketedReferences) {
                    val doesExist =
                        ResourceFileChecker.fileExistsInResources(
                            requireContext(),
                            fileRef,
                        )
                    resourceExistenceMap[fileRef] = doesExist
                }
            }

            // Build validation cache (expensive part) once off main thread
            PerfTimer.track("ProtocolDialog.initialValidate") { computeValidationCache() }

            withContext(Dispatchers.Main) {
                rootLayout.removeView(progressBar)
                val finalView = buildCompletedView()
                rootLayout.addView(finalView)
            }
        }

        return rootLayout
    }

    private fun confirmLoadProtocol() {
        if (!hasUnsavedChanges) {
            openDocumentLauncher.launch(arrayOf("text/plain", "text/*"))
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Would you like to save before loading another protocol?")
            .setPositiveButton("Yes") { _, _ ->
                saveProtocol {
                    Toast.makeText(requireContext(), "Saved current protocol.", Toast.LENGTH_SHORT).show()
                    openDocumentLauncher.launch(arrayOf("text/plain", "text/*"))
                }
            }
            .setNegativeButton("No") { _, _ ->
                Toast.makeText(requireContext(), "Discarding unsaved changes.", Toast.LENGTH_SHORT).show()
                hasUnsavedChanges = false
                openDocumentLauncher.launch(arrayOf("text/plain", "text/*"))
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun confirmCloseDialog() {
        if (!hasUnsavedChanges) {
            dismiss()
            Toast.makeText(requireContext(), "Dialog closed.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Would you like to save before closing?")
            .setPositiveButton("Yes") { _, _ ->
                saveProtocol {
                    Toast.makeText(requireContext(), "Saved current protocol.", Toast.LENGTH_SHORT).show()
                    dismiss()
                    Toast.makeText(requireContext(), "Dialog closed.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No") { _, _ ->
                Toast.makeText(requireContext(), "Discarding unsaved changes.", Toast.LENGTH_SHORT).show()
                hasUnsavedChanges = false
                dismiss()
                Toast.makeText(requireContext(), "Dialog closed.", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun confirmSaveDialog() {
        if (!hasUnsavedChanges) {
            AlertDialog.Builder(requireContext())
                .setTitle("No Changes Detected")
                .setMessage("No changes have been made. Do you still want to save the file?")
                .setPositiveButton("Yes") { _, _ ->
                    saveProtocol {
                        Toast.makeText(requireContext(), "Protocol saved.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("No") { _, _ ->
                    Toast.makeText(requireContext(), "Save cancelled.", Toast.LENGTH_SHORT).show()
                }
                .show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Save")
            .setMessage("Are you sure you want to save?")
            .setPositiveButton("Yes") { _, _ ->
                saveProtocol {
                    Toast.makeText(requireContext(), "Protocol saved.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No") { _, _ ->
                Toast.makeText(requireContext(), "Save cancelled.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun saveProtocol(onSuccess: (() -> Unit)? = null) {
    val prefs = requireContext().getSharedPreferences(Prefs.NAME, 0)
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
            onSuccess?.invoke()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error saving file: ${e.message}",
                Toast.LENGTH_SHORT,
            ).show()
        }
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

    PerfTimer.track("ProtocolDialog.revalidate") { computeValidationCache() }

        val containerLayout = view as? LinearLayout ?: return
        while (containerLayout.childCount > 4) {
            containerLayout.removeViewAt(4)
        }
        containerLayout.addView(buildCompletedView())
    }

    private fun buildCompletedView(): View {
        val containerLayout =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }

        val headerTable = buildHeaderTable()
    val scrollView = ScrollView(requireContext())
    scrollViewRef = scrollView
        val contentTable = buildContentTable()

        // Summary counts
        val errorCount = validationCache.count { it.error.isNotEmpty() }
        val warningCount = validationCache.count { it.warning.isNotEmpty() }
        val total = validationCache.size
        val summaryLabel = TextView(requireContext()).apply {
            text = "Lines: $total  |  Errors: $errorCount  |  Warnings: $warningCount"
            setPadding(24, 8, 24, 4)
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
        }

        if (randomizationLevel > 0) {
            globalErrors.add("RANDOMIZE_ON not closed by matching RANDOMIZE_OFF")
        }

        if (globalErrors.isNotEmpty()) {
            val row =
                TableRow(requireContext()).apply {
                    setBackgroundColor(Color.parseColor("#FFEEEE"))
                    setPadding(16, 8, 16, 8)
                }
            val cell =
                createBodyCell(
                    text = globalErrors.joinToString("\n"),
                    weight = 1.0f,
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
    containerLayout.addView(summaryLabel)
        containerLayout.addView(scrollView)

        // Build issue list for navigation (errors or warnings present)
        issueLineNumbers = validationCache.filter { it.error.isNotEmpty() || it.warning.isNotEmpty() }
            .map { it.lineNumber }
        issueIndex = if (issueLineNumbers.isNotEmpty()) -1 else -1
        return containerLayout
    }

    private fun buildHeaderTable(): TableLayout {
        val context = requireContext()
        return TableLayout(context).apply {
            layoutParams =
                TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT,
                )
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
        val tableLayout =
            TableLayout(context).apply {
                layoutParams =
                    TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT,
                    )
                isStretchAllColumns = false
                setColumnStretchable(1, true)
                setColumnStretchable(2, true)
                setPadding(8, 8, 8, 8)
            }

        val filteredEntries = validationCache.filter { entry ->
            lineMatchesCurrentFilter(entry)
        }

    lineRowMap.clear()
    filteredEntries.forEach { entry ->
            val originalLineNumber = entry.lineNumber
            val trimmedLine = entry.trimmedLine
            val errorMessage = entry.error
            val warningMessage = entry.warning

            val lineContent =
                if (coloringEnabled) {
                    highlightLine(trimmedLine, semicolonsAsBreaks)
                } else {
                    if (semicolonsAsBreaks) {
                        SpannableString(trimmedLine.replace(";", ";\n"))
                    } else {
                        SpannableString(trimmedLine)
                    }
                }

            val combinedIssuesSpannable = colorizeIssues(errorMessage, warningMessage)

            val row =
                TableRow(context).apply {
                    val backgroundColor =
                        if ((originalLineNumber % 2) == 0) {
                            Color.parseColor("#FFFFFF")
                        } else {
                            Color.parseColor("#EEEEEE")
                        }
                    setBackgroundColor(backgroundColor)
                    setPadding(16, 8, 16, 8)
                }

            row.addView(createLineNumberCell(originalLineNumber))

            val commandCell = createBodyCell(lineContent, 2.0f)
            commandCell.setOnClickListener {
                showEditLineDialog(originalLineNumber - 1)
            }
            row.addView(commandCell)
            row.addView(createBodyCell(combinedIssuesSpannable, 1.0f))
            tableLayout.addView(row)
            lineRowMap[originalLineNumber] = row
        }

        return tableLayout
    }

    private fun lineMatchesCurrentFilter(entry: ValidationEntry): Boolean {
        if (!searchQuery.isNullOrBlank()) {
            if (!entry.rawLine.contains(searchQuery!!, ignoreCase = true)) return false
        }
        val hasErrors = entry.error.isNotEmpty()
        val hasWarnings = entry.warning.isNotEmpty()

        return when (filterOption) {
            FilterOption.EVERYTHING -> true
            FilterOption.HIDE_COMMENTS -> {
                if (entry.trimmedLine.isBlank() || entry.trimmedLine.startsWith("//")) {
                    false
                } else {
                    true
                }
            }
            FilterOption.ERRORS_WARNINGS_ONLY -> (hasErrors || hasWarnings)
            FilterOption.ERRORS_ONLY -> hasErrors
        }
    }

    private fun computeValidationCache() {
        // Use pure validator for base validations then map into existing cache structure
        val results = pureValidator.validate(allLines)
        validationCache = results.map { ValidationEntry(it.lineNumber, it.raw, it.raw.trim(), it.error, it.warning) }
        // Preserve randomizationLevel side-effect detection (EOF unmatched) using last synthetic entry if present
        val eofEntry = results.lastOrNull()?.takeIf { it.raw == "<EOF>" && it.error.isNotEmpty() }
        if (eofEntry != null) {
            globalErrors.clear()
            globalErrors.add(eofEntry.error)
        }
    }

    private fun navigateIssue(direction: Int) {
        if (issueLineNumbers.isEmpty()) {
            Toast.makeText(requireContext(), "No issues", Toast.LENGTH_SHORT).show()
            return
        }
        // Move index
        issueIndex = when {
            issueIndex == -1 && direction > 0 -> 0
            issueIndex == -1 && direction < 0 -> issueLineNumbers.lastIndex
            else -> (issueIndex + direction + issueLineNumbers.size) % issueLineNumbers.size
        }
        val lineNumber = issueLineNumbers[issueIndex]
        highlightAndScrollTo(lineNumber)
    }

    private fun highlightAndScrollTo(lineNumber: Int) {
        // Reset previous highlight
        lineRowMap.forEach { (_, row) ->
            val ln = (row.getChildAt(0) as? TextView)?.text?.toString()?.toIntOrNull()
            if (ln != null) {
                val baseColor = if ((ln % 2) == 0) Color.parseColor("#FFFFFF") else Color.parseColor("#EEEEEE")
                row.setBackgroundColor(baseColor)
            }
        }
        val row = lineRowMap[lineNumber] ?: return
        row.setBackgroundColor(Color.parseColor("#FFF7CC"))
        scrollViewRef?.post { scrollViewRef?.smoothScrollTo(0, row.top) }
    }

    private fun generateReportText(): String {
        val sb = StringBuilder()
        sb.appendLine("Protocol Validation Report")
        sb.appendLine("==========================")
        val issues = validationCache.filter { it.error.isNotEmpty() || it.warning.isNotEmpty() }
        issues.forEach { entry ->
            sb.append("Line ").append(entry.lineNumber).append(':').append(' ')
            if (entry.error.isNotEmpty()) sb.append("ERROR: ").append(entry.error)
            if (entry.error.isNotEmpty() && entry.warning.isNotEmpty()) sb.append(" | ")
            if (entry.warning.isNotEmpty()) sb.append("WARN: ").append(entry.warning)
            sb.appendLine()
        }
        if (issues.isEmpty()) sb.appendLine("No issues found.")
        return sb.toString()
    }

    private fun showEditLineDialog(lineIndex: Int) {
        val context = requireContext()
        val originalLine = allLines[lineIndex]

        val editText =
            EditText(context).apply {
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

    private fun createHeaderCell(
        headerText: String,
        gravity: Int,
    ): TextView {
        return TextView(requireContext()).apply {
            text = headerText
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            this.gravity = gravity
            setPadding(24, 16, 24, 16)
            layoutParams =
                TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT,
                )
        }
    }

    private fun createLineNumberCell(lineNumber: Int): TextView {
        return TextView(requireContext()).apply {
            text = lineNumber.toString()
            textSize = 12f
            setSingleLine(true)
            gravity = Gravity.END
            layoutParams =
                TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT,
                )
            setPadding(4, 4, 4, 4)
        }
    }

    private fun createBodyCell(
        text: CharSequence,
        weight: Float,
    ): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 14f
            isSingleLine = false
            setHorizontallyScrolling(false)
            gravity = Gravity.START
            setPadding(24, 8, 24, 8)
            layoutParams =
                TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    weight,
                )
        }
    }

    private fun validateLine(
        lineNumber: Int,
        line: String,
        labelOccurrences: Map<String, List<Int>>,
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
                    errorMessage =
                        appendError(
                            errorMessage,
                            "RANDOMIZE_ON cannot be followed by another RANDOMIZE_ON without an intervening RANDOMIZE_OFF",
                        )
                }
                randomizationLevel++
            }
            "RANDOMIZE_OFF" -> {
                if (lastCommand?.uppercase() == "RANDOMIZE_OFF") {
                    errorMessage =
                        appendError(
                            errorMessage,
                            "RANDOMIZE_OFF should be preceded by RANDOMIZE_ON (not another RANDOMIZE_OFF)",
                        )
                }
                if (randomizationLevel <= 0) {
                    errorMessage =
                        appendError(
                            errorMessage,
                            "RANDOMIZE_OFF without matching RANDOMIZE_ON",
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
            val result =
                handleKnownCommandValidations(
                    commandRaw,
                    parts,
                    lineNumber,
                    line,
                    labelOccurrences,
                    errorMessage,
                    warningMessage,
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
        existingWarning: String,
    ): Pair<String, String> {
        var errorMessage = existingError
        var warningMessage = existingWarning

        when (commandRaw) {
            "HTML" -> {
                htmlValidation(parts).forEach {
                    errorMessage = appendError(errorMessage, it)
                }
            }
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
                            errorMessage =
                                appendError(
                                    errorMessage,
                                    "Sound file '$fileRef' not found in resources folder.",
                                )
                        }
                    }
                }
            }
            "HEADER_SIZE", "BODY_SIZE", "ITEM_SIZE", "RESPONSE_SIZE", "CONTINUE_SIZE", "TIMER_SIZE" -> {
                val (err, warn) = sizeValidation(commandRaw, parts)
                if (err.isNotEmpty()) errorMessage = appendError(errorMessage, err)
                if (warn.isNotEmpty()) warningMessage = appendWarning(warningMessage, warn)
            }
            "SCALE", "SCALE[RANDOMIZED]" -> {
                if (parts.size < 2) {
                    errorMessage =
                        appendError(
                            errorMessage,
                            "$commandRaw must have at least one parameter after the command",
                        )
                }
            }
            "INSTRUCTION" -> {
                val semicolonCount = line.count { it == ';' }
                if (semicolonCount != 3) {
                    errorMessage =
                        appendError(
                            errorMessage,
                            "$commandRaw must have exactly 3 semicolons (4 segments)",
                        )
                }
            }
            "INPUTFIELD", "INPUTFIELD[RANDOMIZED]" -> {
                if (parts.size < 4) {
                    errorMessage =
                        appendError(
                            errorMessage,
                            "$commandRaw must have at least 4 segments: e.g. INPUTFIELD;HEADER;BODY;[field1;field2;...];CONTINUE_TEXT",
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
            "CONTINUE_TEXT_COLOR", "CONTINUE_BACKGROUND_COLOR", "TIMER_COLOR",
            -> {
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
                        errorMessage =
                            appendError(
                                errorMessage,
                                "$commandRaw must be one of: ${allowedAlignments.joinToString(", ")}",
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
                            "TRANSITIONS missing mode (e.g. off or slide or dissolve)",
                        )
                } else if (mode !in listOf("off", "slide", "slideleft", "dissolve", "fade")) {
                    errorMessage =
                        appendError(
                            errorMessage,
                            "TRANSITIONS mode must be either 'off', 'slide', 'slideleft', or 'fade' or 'dissolve'",
                        )
                }
            }
        }
        return errorMessage to warningMessage
    }

    private fun checkFileUsageRules(
        command: String,
        fileRef: String,
    ): String {
        val lowerFile = fileRef.lowercase()
        val knownExtensions = listOf(".mp3", ".wav", ".mp4", ".jpg", ".png", ".html")
        val matchedExt = knownExtensions.firstOrNull { lowerFile.endsWith(it) } ?: return ""

        val mainAllowed = allowedMediaCommands.contains(command)
        val isHtml = (command == "HTML")
        val isTimerSound = (command == "TIMER_SOUND")

        if (!mainAllowed && !isHtml && !isTimerSound) {
            return "Command '$command' cannot reference media or .html files, found <$fileRef>"
        }
        if (isHtml && matchedExt != ".html") {
            return "HTML only accepts .html files, found <$fileRef>"
        }
        if (isTimerSound && (matchedExt != ".mp3" && matchedExt != ".wav")) {
            return "TIMER_SOUND only accepts .mp3 or .wav files, found <$fileRef>"
        }
        return ""
    }

    private fun labelValidation(
        lineNumber: Int,
        line: String,
        labelOccurrences: Map<String, List<Int>>,
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

    private fun htmlValidation(parts: List<String>): List<String> {
        val errors = mutableListOf<String>()
        if (parts.size < 2 || parts[1].isBlank()) {
            errors.add("HTML missing filename")
        } else {
            val filename = parts[1].trim()
            if (!filename.endsWith(".html", ignoreCase = true)) {
                errors.add("HTML must be followed by *.html")
            }
        }
        return errors
    }

    private fun sizeValidation(
        command: String,
        parts: List<String>,
    ): Pair<String, String> {
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
        treatSemicolonsAsLineBreaks: Boolean,
    ): SpannableStringBuilder {
        if (line.startsWith("//")) {
            val greyComment = SpannableString(line)
            greyComment.setSpan(
                ForegroundColorSpan(Color.GRAY),
                0,
                line.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            return SpannableStringBuilder(greyComment)
        }

        val tokens =
            if (
                line.uppercase().startsWith("SCALE") &&
                (
                    line.uppercase().startsWith("SCALE[RANDOMIZED]") ||
                        line.uppercase() == "SCALE" ||
                        line.uppercase().startsWith("SCALE;")
                )
            ) {
                ParsingUtils.customSplitSemicolons(line)
            } else {
                line.split(";")
            }

        val spannableBuilder = SpannableStringBuilder()
        val commandUpper = tokens.firstOrNull()?.uppercase().orEmpty()
        val isRecognized = recognizedCommands.contains(commandUpper)

        for ((index, token) in tokens.withIndex()) {
            val start = spannableBuilder.length
            spannableBuilder.append(token)
            val end = spannableBuilder.length

            val colorForToken =
                when {
                    !isRecognized && index == 0 -> Color.parseColor("#ffaaaa")
                    !isRecognized -> Color.TRANSPARENT
                    else -> chooseTokenBackgroundColor(commandUpper, index, tokens.size)
                }
            spannableBuilder.setSpan(
                BackgroundColorSpan(colorForToken),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )

            if (index == 0) {
                spannableBuilder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }

            if (index < tokens.size - 1) {
                if (treatSemicolonsAsLineBreaks) {
                    spannableBuilder.append(";")
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
        totalTokens: Int,
    ): Int {
        val BG_COMMAND = "#AABBCC"
        val BG_HEADER = "#CDEEFF"
        val BG_BODY = "#FFFFE0"
        val BG_TEAL_FOR_ITEMS = "#D1FCE3"
        val BG_GREEN_FOR_RESPONSES = "#DAF7A6"

        if (tokenIndex == 0) {
            return Color.parseColor(BG_COMMAND)
        }

        return when (commandUpper) {
            "INSTRUCTION" -> {
                when (tokenIndex) {
                    1 -> Color.parseColor(BG_HEADER)
                    2 -> Color.parseColor(BG_BODY)
                    3 -> Color.parseColor(BG_GREEN_FOR_RESPONSES)
                    else -> Color.TRANSPARENT
                }
            }
            "TIMER" -> {
                when (tokenIndex) {
                    1 -> Color.parseColor(BG_HEADER)
                    2 -> Color.parseColor(BG_BODY)
                    3 -> Color.parseColor(BG_TEAL_FOR_ITEMS)
                    4 -> Color.parseColor(BG_GREEN_FOR_RESPONSES)
                    else -> Color.TRANSPARENT
                }
            }
            "SCALE", "SCALE[RANDOMIZED]" -> {
                when {
                    tokenIndex == 1 -> Color.parseColor(BG_HEADER)
                    tokenIndex == 2 -> Color.parseColor(BG_BODY)
                    tokenIndex == 3 -> Color.parseColor(BG_TEAL_FOR_ITEMS)
                    tokenIndex > 3 -> Color.parseColor(BG_GREEN_FOR_RESPONSES)
                    else -> Color.TRANSPARENT
                }
            }
            "INPUTFIELD", "INPUTFIELD[RANDOMIZED]" -> {
                if (tokenIndex == 1) {
                    return Color.parseColor(BG_HEADER)
                } else if (tokenIndex == 2) {
                    return Color.parseColor(BG_BODY)
                } else if (tokenIndex == totalTokens - 1) {
                    return Color.parseColor(BG_GREEN_FOR_RESPONSES)
                } else if (tokenIndex >= 3) {
                    return Color.parseColor(BG_TEAL_FOR_ITEMS)
                }
                return Color.TRANSPARENT
            }
            else -> {
                if (tokenIndex == 1) {
                    return Color.parseColor(BG_TEAL_FOR_ITEMS)
                }
                return Color.TRANSPARENT
            }
        }
    }

    private fun colorizeIssues(
        errorMessage: String,
        warningMessage: String,
    ): SpannableString {
        val combinedText =
            buildString {
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
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        if (warningMessage.isNotEmpty()) {
            val startIndex = errorMessage.length + if (errorMessage.isNotEmpty()) 1 else 0
            val endIndex = startIndex + warningMessage.length
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#FFA500")),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
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

    private fun appendError(
        current: String,
        newError: String,
    ): String {
        return if (current.isEmpty()) newError else "$current; $newError"
    }

    private fun appendWarning(
        current: String,
        newWarning: String,
    ): String {
        return if (current.isEmpty()) newWarning else "$current; $newWarning"
    }

    private fun getProtocolContent(): String {
    val prefs = requireContext().getSharedPreferences(Prefs.NAME, 0)
    val mode = prefs.getString(Prefs.KEY_CURRENT_MODE, null)
    val customUriString = prefs.getString(Prefs.KEY_PROTOCOL_URI, null)

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

    private enum class FilterOption(val labelRes: Int) {
        EVERYTHING(R.string.filter_everything),
        HIDE_COMMENTS(R.string.filter_hide_comments),
        ERRORS_WARNINGS_ONLY(R.string.filter_errors_warnings),
        ERRORS_ONLY(R.string.filter_errors_only),
    }

    private fun mergeLongFormatCommands(lines: List<String>): List<String> {
        val mergedLines = mutableListOf<String>()
        var buffer = StringBuilder()
        var isLongFormat = false

        fun flushBufferIfNeeded() {
            if (buffer.isNotEmpty()) {
                mergedLines.add(buffer.toString())
                buffer = StringBuilder()
                isLongFormat = false
            }
        }

        for (lineRaw in lines) {
            val line = lineRaw.trim()
            if (line.isEmpty()) {
                if (!isLongFormat) {
                    mergedLines.add(lineRaw)
                } else {
                    buffer.append(" ")
                }
                continue
            }

            val tokens = line.split(";").map { it.trim() }
            val firstToken = tokens.firstOrNull()?.uppercase().orEmpty()

            if (!isLongFormat) {
                if (recognizedCommands.contains(firstToken)) {
                    val hasTrailingSemicolon = line.endsWith(";")
                    val contentAfterCommand = tokens.drop(1).joinToString("").isBlank()

                    if (hasTrailingSemicolon && contentAfterCommand) {
                        buffer.append(line.removeSuffix(";"))
                        isLongFormat = true
                        continue
                    } else {
                        mergedLines.add(lineRaw)
                    }
                } else {
                    mergedLines.add(lineRaw)
                }
            } else {
                val endsWithSemicolon = line.endsWith(";")
                val content = if (endsWithSemicolon) line.removeSuffix(";") else line
                buffer.append(";").append(content)

                if (!endsWithSemicolon) {
                    flushBufferIfNeeded()
                }
            }
        }
        flushBufferIfNeeded()
        return mergedLines
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor: Cursor? = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                name = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return name
    }
}
