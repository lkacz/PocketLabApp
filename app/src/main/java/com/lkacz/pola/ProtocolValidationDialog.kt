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

    // Simple undo stack of previous protocol line states
    private val undoStack: ArrayDeque<List<String>> = ArrayDeque()
    private fun pushUndoState() {
        // Limit stack size to avoid memory bloat
        if (undoStack.size > 50) undoStack.removeFirst()
        undoStack.addLast(allLines.toList())
    }
    private fun performUndo() {
        if (undoStack.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.toast_undo_none), Toast.LENGTH_SHORT).show()
            return
        }
        allLines = undoStack.removeLast().toMutableList()
        hasUnsavedChanges = true
        revalidateAndRefreshUI()
        Toast.makeText(requireContext(), getString(R.string.toast_undo_done), Toast.LENGTH_SHORT).show()
    }

    private lateinit var btnLoad: View
    private lateinit var btnSave: View
    private lateinit var btnAdd: View
    private lateinit var btnNew: View

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
        fun materialButton(text: String, styleAttr: Int, iconRes: Int? = null, onClick: () -> Unit): com.google.android.material.button.MaterialButton =
            com.google.android.material.button.MaterialButton(requireContext(), null, styleAttr).apply {
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

        fun iconOnlyButton(styleAttr: Int, iconRes: Int, cd: String, onClick: () -> Unit): com.google.android.material.button.MaterialButton =
            com.google.android.material.button.MaterialButton(requireContext(), null, styleAttr).apply {
                text = "" // icon only
                icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), iconRes)
                // Fallback gravity
                iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
                // Enforce square-ish size
                val size = (48 * resources.displayMetrics.density).toInt()
                minWidth = size
                minimumHeight = size
                iconPadding = 0
                contentDescription = cd
                isAllCaps = false
                setOnClickListener { onClick() }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 12 }
            }

        // Minimal top action bar with icon-only ImageButtons (no card, no button boxes)
        val actionBar = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(12,12,12,4) }
        }

    fun barIcon(@androidx.annotation.DrawableRes iconRes: Int, cd: String, onClick: () -> Unit): ImageButton =
            ImageButton(requireContext()).apply {
                setImageDrawable(androidx.core.content.ContextCompat.getDrawable(requireContext(), iconRes))
                // Use system selectable ripple borderless
                val attrs = intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
                val typed = requireContext().obtainStyledAttributes(attrs)
                background = typed.getDrawable(0)
                typed.recycle()
                contentDescription = cd
                scaleType = ImageView.ScaleType.CENTER_INSIDE
        val size = (48 * resources.displayMetrics.density).toInt()
        layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(8,0,8,0) }
        setPadding(8,8,8,8)
                // Ensure icon is visible (tint to on-surface color if available)
                try {
                    val tv = androidx.appcompat.view.ContextThemeWrapper(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3)
                    val colorAttr = intArrayOf(com.google.android.material.R.attr.colorOnSurface)
                    val a = tv.obtainStyledAttributes(colorAttr)
                    val color = a.getColor(0, 0xFF444444.toInt())
                    a.recycle()
                    imageTintList = android.content.res.ColorStateList.valueOf(color)
                } catch (_: Exception) { }
                setOnClickListener { onClick() }
            }

    btnLoad = barIcon(R.drawable.ic_folder_open, getString(R.string.cd_load_protocol)) { confirmLoadProtocol() }
        btnSave = barIcon(R.drawable.ic_save, getString(R.string.cd_save_protocol)) {
            // Always ask for a name (acts like Save As) for safety
            val defaultName = getSuggestedFileName()
            createDocumentLauncher.launch(defaultName)
        }
    btnAdd = barIcon(R.drawable.ic_add, getString(R.string.cd_add_command)) { showInsertCommandDialog(insertAfterLine = null) }
    btnNew = barIcon(R.drawable.ic_new_file, getString(R.string.cd_new_protocol)) { confirmNewProtocol() }
    val btnUndo = barIcon(R.drawable.ic_undo, getString(R.string.cd_undo)) { performUndo() }
        val btnClose = barIcon(R.drawable.ic_close, getString(R.string.cd_close_dialog)) { confirmCloseDialog() }

    actionBar.addView(btnLoad)
    actionBar.addView(btnSave)
    actionBar.addView(btnNew)
    actionBar.addView(btnAdd)
    actionBar.addView(btnUndo)
        actionBar.addView(btnClose)
        rootLayout.addView(actionBar)

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
                hint = getString(R.string.hint_search_keyword)
            }

    val searchButton = materialButton(getString(R.string.action_search_text), com.google.android.material.R.attr.materialButtonStyle, R.drawable.ic_search) {
            searchQuery = searchEditText.text?.toString()?.trim().takeIf { it?.isNotEmpty() == true }
            revalidateAndRefreshUI()
        }

    val clearButton = materialButton(getString(R.string.action_clear_text), com.google.android.material.R.attr.materialButtonOutlinedStyle, R.drawable.ic_close) {
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
    val btnPrev = iconOnlyButton(com.google.android.material.R.attr.materialButtonOutlinedStyle, R.drawable.ic_prev, getString(R.string.cd_prev_issue)) { navigateIssue(-1) }
    val btnNext = iconOnlyButton(com.google.android.material.R.attr.materialButtonOutlinedStyle, R.drawable.ic_next, getString(R.string.cd_next_issue)) { navigateIssue(1) }
    val btnExport = iconOnlyButton(com.google.android.material.R.attr.materialButtonOutlinedStyle, R.drawable.ic_export, getString(R.string.cd_export_report)) { exportReportLauncher.launch("protocol_validation_report.txt") }
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

    private fun confirmNewProtocol() {
        fun createNew() {
            pushUndoState()
            allLines.clear()
            allLines.add("INSTRUCTION;Header;Body;Continue")
            hasUnsavedChanges = true
            revalidateAndRefreshUI()
            Toast.makeText(requireContext(), getString(R.string.toast_new_protocol_created), Toast.LENGTH_SHORT).show()
        }
        if (hasUnsavedChanges) {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_title_new_protocol))
                .setMessage(getString(R.string.dialog_message_new_protocol_unsaved))
                .setPositiveButton(R.string.action_save) { _, _ ->
                    saveProtocol { createNew() }
                }
                .setNegativeButton(R.string.action_new_protocol) { _, _ -> createNew() }
                .setNeutralButton(android.R.string.cancel, null)
                .show()
        } else {
            createNew()
        }
    }

    private fun saveProtocol(onSuccess: (() -> Unit)? = null) {
        val prefs = requireContext().getSharedPreferences(Prefs.NAME, 0)
        val customUriString = prefs.getString(Prefs.KEY_PROTOCOL_URI, null)
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
            text = getString(R.string.label_summary_counts, total, errorCount, warningCount)
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
                    setOnLongClickListener {
                        showInsertCommandDialog(insertAfterLine = originalLineNumber - 1)
                        true
                    }
                }

            row.addView(createLineNumberCell(originalLineNumber))

            val commandCell = createBodyCell(lineContent, 2.0f)
            commandCell.setOnClickListener {
                val raw = allLines[originalLineNumber - 1]
                val firstToken = raw.split(';').firstOrNull()?.trim().orEmpty()
                if (!recognizedCommands.contains(firstToken.uppercase()) && !firstToken.equals("END", true)) {
                    val lineIndex = originalLineNumber - 1
                    val options = listOf(
                        getString(R.string.action_edit_raw_line),
                        getString(R.string.action_delete_command),
                        getString(R.string.action_move_up),
                        getString(R.string.action_move_down),
                        getString(android.R.string.cancel)
                    )
                    AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.error_unrecognized_command_title))
                        .setItems(options.toTypedArray()) { dialog, which ->
                            when (which) {
                                0 -> { // Edit
                                    showEditLineDialog(lineIndex)
                                }
                                1 -> { // Delete
                                    if (lineIndex in allLines.indices) {
                                        pushUndoState()
                                        allLines.removeAt(lineIndex)
                                        hasUnsavedChanges = true
                                        revalidateAndRefreshUI()
                                        Toast.makeText(requireContext(), getString(R.string.toast_command_deleted), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                2 -> { // Move Up
                                    if (lineIndex > 0) {
                                        pushUndoState()
                                        java.util.Collections.swap(allLines, lineIndex, lineIndex - 1)
                                        hasUnsavedChanges = true
                                        revalidateAndRefreshUI()
                                        Toast.makeText(requireContext(), getString(R.string.toast_command_moved), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(requireContext(), getString(R.string.toast_move_blocked), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                3 -> { // Move Down
                                    if (lineIndex < allLines.lastIndex) {
                                        pushUndoState()
                                        java.util.Collections.swap(allLines, lineIndex, lineIndex + 1)
                                        hasUnsavedChanges = true
                                        revalidateAndRefreshUI()
                                        Toast.makeText(requireContext(), getString(R.string.toast_command_moved), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(requireContext(), getString(R.string.toast_move_blocked), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                else -> dialog.dismiss()
                            }
                        }
                        .show()
                } else {
                    showInsertCommandDialog(insertAfterLine = originalLineNumber - 1, editLineIndex = originalLineNumber - 1)
                }
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
            Toast.makeText(requireContext(), getString(R.string.toast_no_issues), Toast.LENGTH_SHORT).show()
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
            pushUndoState()
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

    private fun showInsertCommandDialog(insertAfterLine: Int?, editLineIndex: Int? = null) {
        val ctx = requireContext()
        // Container layout
        val container = ScrollView(ctx)
        val inner = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(32,24,32,8) }
        container.addView(inner)

        val commands = arrayOf(
            "INSTRUCTION", "TIMER", "SCALE", "SCALE[RANDOMIZED]", "INPUTFIELD", "INPUTFIELD[RANDOMIZED]",
            "LABEL", "GOTO", "HTML", "TIMER_SOUND", "LOG", "END"
        )
        val spinner = Spinner(ctx).apply {
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, commands)
        }
        inner.addView(TextView(ctx).apply { text = getString(R.string.label_select_command); setTypeface(null, Typeface.BOLD) })
        inner.addView(spinner)

        fun edit(hintRes: Int): EditText = EditText(ctx).apply { hint = getString(hintRes); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }

        val header = edit(R.string.hint_header)
        val body = edit(R.string.hint_body)
        val items = edit(R.string.hint_items)
        val cont = edit(R.string.hint_continue)
        val time = edit(R.string.hint_time_seconds).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val labelName = edit(R.string.hint_label_name)
        val gotoLabel = edit(R.string.hint_goto_label)
        val filename = edit(R.string.hint_filename)
        val message = edit(R.string.hint_message)
        val inputFields = edit(R.string.hint_input_fields)

        val paramGroup = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(0,16,0,0) }
        inner.addView(TextView(ctx).apply { text = getString(R.string.label_parameters); setTypeface(null, Typeface.BOLD) })
        inner.addView(paramGroup)

        fun refreshParams() {
            paramGroup.removeAllViews()
            when (spinner.selectedItem as String) {
                "INSTRUCTION" -> { paramGroup.addView(header); paramGroup.addView(body); paramGroup.addView(cont) }
                "TIMER" -> { paramGroup.addView(header); paramGroup.addView(body); paramGroup.addView(time); paramGroup.addView(cont) }
                "SCALE", "SCALE[RANDOMIZED]" -> { paramGroup.addView(header); paramGroup.addView(body); paramGroup.addView(items); paramGroup.addView(cont) }
                "INPUTFIELD", "INPUTFIELD[RANDOMIZED]" -> { paramGroup.addView(header); paramGroup.addView(body); paramGroup.addView(inputFields); paramGroup.addView(cont) }
                "LABEL" -> { paramGroup.addView(labelName) }
                "GOTO" -> { paramGroup.addView(gotoLabel) }
                "HTML" -> { paramGroup.addView(filename) }
                "TIMER_SOUND" -> { paramGroup.addView(filename) }
                "LOG" -> { paramGroup.addView(message) }
                else -> { /* END has no params */ }
            }
        }
        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { refreshParams() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        refreshParams()

        // Prefill if editing existing line
        if (editLineIndex != null && editLineIndex in allLines.indices) {
            val existing = allLines[editLineIndex]
            val parts = existing.split(';')
            val cmd = parts.firstOrNull()?.trim().orEmpty()
            val spinnerIndex = commands.indexOf(cmd).takeIf { it >= 0 } ?: 0
            spinner.setSelection(spinnerIndex)
            // Delay param refresh until after selection applied
            spinner.post {
                when (cmd) {
                    "INSTRUCTION" -> { header.setText(parts.getOrNull(1)); body.setText(parts.getOrNull(2)); cont.setText(parts.getOrNull(3)) }
                    "TIMER" -> { header.setText(parts.getOrNull(1)); body.setText(parts.getOrNull(2)); time.setText(parts.getOrNull(3)); cont.setText(parts.getOrNull(4)) }
                    "SCALE", "SCALE[RANDOMIZED]" -> { header.setText(parts.getOrNull(1)); body.setText(parts.getOrNull(2));
                        val itemSlice = if (parts.size > 4) parts.subList(3, parts.size -1) else emptyList()
                        items.setText(itemSlice.joinToString(", "))
                        cont.setText(parts.lastOrNull()) }
                    "INPUTFIELD", "INPUTFIELD[RANDOMIZED]" -> { header.setText(parts.getOrNull(1)); body.setText(parts.getOrNull(2));
                        val fieldSlice = if (parts.size > 4) parts.subList(3, parts.size -1) else emptyList()
                        inputFields.setText(fieldSlice.joinToString(", "))
                        cont.setText(parts.lastOrNull()) }
                    "LABEL" -> { labelName.setText(parts.getOrNull(1)) }
                    "GOTO" -> { gotoLabel.setText(parts.getOrNull(1)) }
                    "HTML", "TIMER_SOUND" -> { filename.setText(parts.getOrNull(1)) }
                    "LOG" -> { message.setText(parts.getOrNull(1)) }
                }
            }
        }

        val isEdit = editLineIndex != null
        val builder = AlertDialog.Builder(ctx)
            .setTitle(getString(R.string.dialog_title_insert_command))
            .setView(container)
            .setPositiveButton(if (isEdit) R.string.action_update_command else R.string.action_add_command) { _, _ ->
                val cmd = spinner.selectedItem as String
                fun def(et: EditText, fallback: String) = et.text.toString().takeIf { it.isNotBlank() } ?: fallback
                val newLine = when (cmd) {
                    "INSTRUCTION" -> "$cmd;${def(header,"Header")};${def(body,"Body")};${def(cont,"Continue")}" 
                    "TIMER" -> "$cmd;${def(header,"Header")};${def(body,"Body")};${def(time,"60")};${def(cont,"Continue")}" 
                    "SCALE", "SCALE[RANDOMIZED]" -> {
                        val itemTokens = items.text.toString().split(',').map { it.trim() }.filter { it.isNotEmpty() }
                            .ifEmpty { listOf("1","2") }
                        val itemsPart = itemTokens.joinToString(";")
                        "$cmd;${def(header,"Header")};${def(body,"Body")};$itemsPart;${def(cont,"Continue")}" 
                    }
                    "INPUTFIELD", "INPUTFIELD[RANDOMIZED]" -> {
                        val fieldTokens = inputFields.text.toString().split(',').map { it.trim() }.filter { it.isNotEmpty() }
                            .ifEmpty { listOf("field1","field2") }
                        val fieldsPart = fieldTokens.joinToString(";")
                        "$cmd;${def(header,"Header")};${def(body,"Body")};$fieldsPart;${def(cont,"Continue")}" 
                    }
                    "LABEL" -> {
                        if (labelName.text.isBlank()) return@setPositiveButton Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                        "$cmd;${labelName.text}" 
                    }
                    "GOTO" -> {
                        if (gotoLabel.text.isBlank()) return@setPositiveButton Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                        "$cmd;${gotoLabel.text}" 
                    }
                    "HTML", "TIMER_SOUND" -> {
                        if (filename.text.isBlank()) return@setPositiveButton Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                        "$cmd;${filename.text}" 
                    }
                    "LOG" -> "$cmd;${def(message,"Log message")}" 
                    "END" -> "END"
                    else -> return@setPositiveButton Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                }
                // proceed add or update
                    if (isEdit) {
                        pushUndoState()
                        allLines[editLineIndex!!] = newLine
                        Toast.makeText(ctx, getString(R.string.toast_command_updated), Toast.LENGTH_SHORT).show()
                    } else {
                        val idx = insertAfterLine?.plus(1) ?: allLines.size
                        pushUndoState()
                        allLines.add(idx, newLine)
                        Toast.makeText(ctx, getString(R.string.toast_command_inserted), Toast.LENGTH_SHORT).show()
                    }
                    hasUnsavedChanges = true
                    revalidateAndRefreshUI()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> Toast.makeText(ctx, getString(R.string.toast_insert_cancelled), Toast.LENGTH_SHORT).show() }

        if (isEdit) {
            builder.setNeutralButton(R.string.action_delete_command) { _, _ ->
                if (editLineIndex != null && editLineIndex in allLines.indices) {
                    pushUndoState()
                    allLines.removeAt(editLineIndex)
                    hasUnsavedChanges = true
                    revalidateAndRefreshUI()
                    Toast.makeText(ctx, getString(R.string.toast_command_deleted), Toast.LENGTH_SHORT).show()
                }
            }
        }
        val dialog = builder.create()
        dialog.setOnShowListener {
            if (isEdit) {
                // Add move buttons below form dynamically
                val parent = container.parent as? ViewGroup ?: return@setOnShowListener
                val moveRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0,24,0,8) }
                val btnUp = Button(ctx).apply { text = getString(R.string.action_move_up) }
                val btnDown = Button(ctx).apply { text = getString(R.string.action_move_down) }
                moveRow.addView(btnUp, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                moveRow.addView(btnDown, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                inner.addView(moveRow)
                btnUp.setOnClickListener {
                    if (editLineIndex != null && editLineIndex > 0) {
                        pushUndoState()
                        java.util.Collections.swap(allLines, editLineIndex, editLineIndex - 1)
                        hasUnsavedChanges = true
                        revalidateAndRefreshUI()
                        Toast.makeText(ctx, getString(R.string.toast_command_moved), Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(ctx, getString(R.string.toast_move_blocked), Toast.LENGTH_SHORT).show()
                    }
                }
                btnDown.setOnClickListener {
                    if (editLineIndex != null && editLineIndex < allLines.lastIndex) {
                        pushUndoState()
                        java.util.Collections.swap(allLines, editLineIndex, editLineIndex + 1)
                        hasUnsavedChanges = true
                        revalidateAndRefreshUI()
                        Toast.makeText(ctx, getString(R.string.toast_command_moved), Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(ctx, getString(R.string.toast_move_blocked), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun getSuggestedFileName(): String {
        val prefs = requireContext().getSharedPreferences(Prefs.NAME, 0)
        val current = prefs.getString(Prefs.KEY_CURRENT_PROTOCOL_NAME, null)
        return if (!current.isNullOrBlank()) {
            // Append _copy if same name already in prefs (acts like save as safeguard)
            if (current.lowercase().endsWith(".txt")) {
                val base = current.removeSuffix(".txt")
                base + "_copy.txt"
            } else {
                current + "_copy.txt"
            }
        } else {
            "protocol_${System.currentTimeMillis()}.txt"
        }
    }
}
