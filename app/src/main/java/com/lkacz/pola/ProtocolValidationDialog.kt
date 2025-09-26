@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.lkacz.pola

import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
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
import android.view.DragEvent
import android.widget.*
import androidx.appcompat.widget.TooltipCompat
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.regex.Pattern

class ProtocolValidationDialog : DialogFragment() {
    private val recognizedCommands = ProtocolValidator.RECOGNIZED_COMMANDS

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
    private var pendingAutoScrollToFirstIssue = false

    // Container that holds the dynamic (revalidated) table content; filled by revalidateAndRefreshUI
    private var dynamicContentContainer: ViewGroup? = null

    // Simple reentrancy guard to prevent nested refreshes causing IllegalStateException
    private var isRefreshing = false
    private var pendingLoadErrorMessage: String? = null
    private val loadErrorPlaceholder: String =
        listOf(
            "// Unable to load the previously opened protocol.",
            "// Use Load Protocol or New Protocol to continue.",
        ).joinToString("\n")

    private val resourcesFolderUri: Uri? by lazy {
        ResourcesFolderManager(requireContext()).getResourcesFolderUri()
    }

    private var searchQuery: String? = null
    private var filterOption: FilterOption = FilterOption.HIDE_COMMENTS

    private var hasUnsavedChanges = false

    private var coloringEnabled = true
    private var semicolonsAsBreaks = true

    // Dynamic UI text scaling factor (base 1.0f)
    private var textScale = 1.0f
    private val minScale = 0.6f
    private val maxScale = 1.6f

    private fun applyScale(baseSp: Float): Float = baseSp * textScale

    private var pendingSaveAsCallback: (() -> Unit)? = null

    // Simple undo stack of previous protocol line states
    private val undoStack: ArrayDeque<List<String>> = ArrayDeque()

    // Redo stack stores states undone (for reapplication)
    private val redoStack: ArrayDeque<List<String>> = ArrayDeque()

    private fun pushUndoState() {
        // Limit stack size to avoid memory bloat
        if (undoStack.size > 50) undoStack.removeFirst()
        // When a new action occurs, clear redo history (branch cut)
        if (redoStack.isNotEmpty()) redoStack.clear()
        undoStack.addLast(allLines.toList())
    }

    private fun performUndo() {
        if (undoStack.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.toast_undo_none), Toast.LENGTH_SHORT).show()
            return
        }
        // Push current state onto redo before reverting
        redoStack.addLast(allLines.toList())
        if (redoStack.size > 50) redoStack.removeFirst()
        allLines = undoStack.removeLast().toMutableList()
        hasUnsavedChanges = true
        revalidateAndRefreshUI()
        Toast.makeText(requireContext(), getString(R.string.toast_undo_done), Toast.LENGTH_SHORT).show()
    }

    private fun performRedo() {
        if (redoStack.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.toast_redo_none), Toast.LENGTH_SHORT).show()
            return
        }
        // Current state becomes an undo point
        if (undoStack.size > 50) undoStack.removeFirst()
        undoStack.addLast(allLines.toList())
        allLines = redoStack.removeLast().toMutableList()
        hasUnsavedChanges = true
        revalidateAndRefreshUI()
        Toast.makeText(requireContext(), getString(R.string.toast_redo_done), Toast.LENGTH_SHORT).show()
    }

    private lateinit var btnLoad: View
    private lateinit var btnSave: View
    private lateinit var btnAdd: View
    private lateinit var btnNew: View

    private val createDocumentLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri != null) {
                try {
                    try {
                        requireContext().contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    } catch (_: SecurityException) {
                        // Some providers may not support persistable permissions; ignore
                    }
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
                    pendingSaveAsCallback?.let { callback ->
                        pendingSaveAsCallback = null
                        callback()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Error saving file: ${e.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
                    pendingSaveAsCallback = null
                }
            } else {
                pendingSaveAsCallback = null
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
        fun materialButton(
            text: String,
            styleAttr: Int,
            iconRes: Int? = null,
            onClick: () -> Unit,
        ): com.google.android.material.button.MaterialButton =
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

        fun iconOnlyButton(
            styleAttr: Int,
            iconRes: Int,
            cd: String,
            onClick: () -> Unit,
        ): com.google.android.material.button.MaterialButton =
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
        val actionBar =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(12, 12, 12, 4)
                    }
            }

        fun barIcon(
            @androidx.annotation.DrawableRes iconRes: Int,
            cd: String,
            onClick: () -> Unit,
        ): ImageButton =
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
                layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(8, 0, 8, 0) }
                setPadding(8, 8, 8, 8)
                // Ensure icon is visible (tint to on-surface color if available)
                try {
                    val tv =
                        androidx.appcompat.view.ContextThemeWrapper(
                            requireContext(),
                            com.google.android.material.R.style.ThemeOverlay_Material3,
                        )
                    val colorAttr = intArrayOf(com.google.android.material.R.attr.colorOnSurface)
                    val a = tv.obtainStyledAttributes(colorAttr)
                    val color = a.getColor(0, 0xFF444444.toInt())
                    a.recycle()
                    imageTintList = android.content.res.ColorStateList.valueOf(color)
                } catch (_: Exception) {
                }
                setOnClickListener { onClick() }
            }

        // Load & Save actions moved to overflow menu (keep references for potential future reinstatement)
        btnLoad = barIcon(R.drawable.ic_folder_open, getString(R.string.cd_load_protocol)) { confirmLoadProtocol() }
        btnSave = barIcon(R.drawable.ic_save, getString(R.string.cd_save_protocol)) { confirmSaveDialog() }
        btnAdd = barIcon(R.drawable.ic_add, getString(R.string.cd_add_command)) { showInsertCommandDialog(insertAfterLine = null) }
        btnNew = barIcon(R.drawable.ic_new_file, getString(R.string.cd_new_protocol)) { confirmNewProtocol() }
        val btnUndo = barIcon(R.drawable.ic_undo, getString(R.string.cd_undo)) { performUndo() }
        val btnRedo = barIcon(R.drawable.ic_redo, getString(R.string.cd_redo)) { performRedo() }
        // Overflow menu button
        val btnMore =
            ImageButton(requireContext()).apply {
                setImageDrawable(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_more_vert))
                val attrs = intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
                val typed = requireContext().obtainStyledAttributes(attrs)
                background = typed.getDrawable(0)
                typed.recycle()
                contentDescription = getString(R.string.cd_overflow_menu)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                val size = (48 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(8, 0, 8, 0) }
                setPadding(8, 8, 8, 8)
                try {
                    val tv =
                        androidx.appcompat.view.ContextThemeWrapper(
                            requireContext(),
                            com.google.android.material.R.style.ThemeOverlay_Material3,
                        )
                    val colorAttr = intArrayOf(com.google.android.material.R.attr.colorOnSurface)
                    val a = tv.obtainStyledAttributes(colorAttr)
                    val color = a.getColor(0, 0xFF444444.toInt())
                    a.recycle()
                    imageTintList = android.content.res.ColorStateList.valueOf(color)
                } catch (_: Exception) {
                }
                setOnClickListener {
                    val popup =
                        PopupMenu(requireContext(), this).apply {
                            // Place New first for quick access
                            menu.add(0, 1, 0, getString(R.string.action_new_protocol))
                            menu.add(0, 2, 1, getString(R.string.action_load_protocol))
                            menu.add(0, 3, 2, getString(R.string.action_save_protocol))
                            menu.add(0, 4, 3, getString(R.string.action_save_as_protocol))
                            menu.add(0, 5, 4, getString(R.string.action_increase) + " +")
                            menu.add(0, 6, 5, getString(R.string.action_decrease) + " -")
                        }
                    popup.setOnMenuItemClickListener { mi ->
                        when (mi.itemId) {
                            1 -> confirmNewProtocol()
                            2 -> confirmLoadProtocol()
                            3 -> confirmSaveDialog()
                            4 -> {
                                val defaultName = getSuggestedFileName()
                                createDocumentLauncher.launch(defaultName)
                            }
                            5 ->
                                if (textScale < maxScale) {
                                    textScale = (textScale + 0.1f).coerceAtMost(maxScale)
                                    revalidateAndRefreshUI()
                                } else {
                                    Toast.makeText(requireContext(), getString(R.string.toast_text_size_limit), Toast.LENGTH_SHORT).show()
                                }
                            6 ->
                                if (textScale > minScale) {
                                    textScale = (textScale - 0.1f).coerceAtLeast(minScale)
                                    revalidateAndRefreshUI()
                                } else {
                                    Toast.makeText(requireContext(), getString(R.string.toast_text_size_limit), Toast.LENGTH_SHORT).show()
                                }
                        }
                        true
                    }
                    popup.show()
                }
            }
        val spacer =
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            }
        val btnClose = barIcon(R.drawable.ic_close, getString(R.string.cd_close_dialog)) { confirmCloseDialog() }

        // Slim toolbar: only core edit actions; load/save now under overflow menu
        actionBar.addView(btnAdd)
        actionBar.addView(btnUndo)
        actionBar.addView(btnRedo)
        actionBar.addView(btnMore)
        actionBar.addView(spacer)
        actionBar.addView(btnClose)
        // Wrap in horizontal scroll so all icons remain reachable on small screens
        val actionScroll =
            HorizontalScrollView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                isHorizontalScrollBarEnabled = false
                addView(actionBar)
            }
        rootLayout.addView(actionScroll)

        val searchRow =
            LinearLayout(requireContext()).apply {
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

        val searchButton =
            materialButton(
                getString(R.string.action_search_text),
                com.google.android.material.R.attr.materialButtonStyle,
                R.drawable.ic_search,
            ) {
                searchQuery = searchEditText.text?.toString()?.trim().takeIf { it?.isNotEmpty() == true }
                revalidateAndRefreshUI()
            }

        val clearButton =
            materialButton(
                getString(R.string.action_clear_text),
                com.google.android.material.R.attr.materialButtonOutlinedStyle,
                R.drawable.ic_close,
            ) {
                searchQuery = null
                searchEditText.setText("")
                revalidateAndRefreshUI()
            }
        searchRow.addView(searchEditText)
        searchRow.addView(searchButton)
        searchRow.addView(clearButton)
        val searchCard =
            com.google.android.material.card.MaterialCardView(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(16, 8, 16, 8)
                    }
                radius = 12f
                strokeWidth = 1
                setContentPadding(24, 24, 24, 16)
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
        val filterCard =
            com.google.android.material.card.MaterialCardView(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(16, 8, 16, 4)
                    }
                radius = 12f
                strokeWidth = 1
                setContentPadding(24, 16, 24, 8)
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
        val togglesCard =
            com.google.android.material.card.MaterialCardView(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(16, 4, 16, 8)
                    }
                radius = 12f
                strokeWidth = 1
                setContentPadding(24, 16, 24, 16)
                addView(togglesContainer)
            }
        rootLayout.addView(togglesCard)

        // Placeholder for dynamic validated content (tables, quick-fixes, etc.)
        dynamicContentContainer =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                // Tag helps future defensive lookups if needed
                tag = "protocol_dynamic_container"
            }
        rootLayout.addView(dynamicContentContainer)

        // Navigation row (placed after dynamic content so content replaces properly)
        val navRow =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

        fun tinyButton(
            text: String,
            iconRes: Int,
            onClick: () -> Unit,
        ): com.google.android.material.button.MaterialButton =
            com.google.android.material.button.MaterialButton(
                requireContext(),
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle,
            ).apply {
                this.text = text
                isAllCaps = false
                icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), iconRes)
                iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
                iconPadding = 12
                setOnClickListener { onClick() }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 12 }
            }
        val btnPrev =
            iconOnlyButton(
                com.google.android.material.R.attr.materialButtonOutlinedStyle,
                R.drawable.ic_prev,
                getString(R.string.cd_prev_issue),
            ) {
                navigateIssue(-1)
            }
        val btnNext =
            iconOnlyButton(
                com.google.android.material.R.attr.materialButtonOutlinedStyle,
                R.drawable.ic_next,
                getString(R.string.cd_next_issue),
            ) {
                navigateIssue(1)
            }
        navRow.addView(btnPrev)
        navRow.addView(btnNext)
        val navCard =
            com.google.android.material.card.MaterialCardView(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(16, 8, 16, 8)
                    }
                radius = 12f
                strokeWidth = 1
                setContentPadding(24, 24, 24, 16)
                addView(navRow)
            }
        // Add navigation card after dynamic content container
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
                // Populate dynamic container instead of adding a second instance
                dynamicContentContainer?.removeAllViews()
                dynamicContentContainer?.addView(buildCompletedView())
                pendingLoadErrorMessage?.let { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    pendingLoadErrorMessage = null
                }
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
            // Reset protocol state completely
            undoStack.clear()
            redoStack.clear()
            allLines.clear()
            allLines.add("INSTRUCTION;Header;Body;Continue")
            hasUnsavedChanges = true
            pendingAutoScrollToFirstIssue = true
            // Clear any currently rendered dynamic content immediately to avoid visual duplication before rebuild
            dynamicContentContainer?.removeAllViews()
            revalidateAndRefreshUI()
            val prefs = requireContext().getSharedPreferences(Prefs.NAME, 0)
            val untitledName = getString(R.string.value_untitled_protocol)
            prefs
                .edit()
                .remove(Prefs.KEY_PROTOCOL_URI)
                .putString(Prefs.KEY_CURRENT_PROTOCOL_NAME, untitledName)
                .putString(Prefs.KEY_CURRENT_MODE, "custom")
                .apply()
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
            pendingSaveAsCallback = onSuccess
            val suggested = getSuggestedFileName()
            createDocumentLauncher.launch(suggested)
            return
        }
        val uri = Uri.parse(customUriString)
        try {
            // Re-acquire (persist) permissions just in case app process restarted since selection
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
                // ignore if already granted
            }

            // Use rwt (truncate) when possible; fall back to rw
            val mode =
                try {
                    requireContext().contentResolver.openFileDescriptor(uri, "rwt")?.close()
                    "rwt"
                } catch (_: Exception) {
                    "rw"
                }

            requireContext().contentResolver.openFileDescriptor(uri, mode)?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).use { fos ->
                    fos.write(allLines.joinToString("\n").toByteArray(Charsets.UTF_8))
                }
            } ?: throw FileNotFoundException("Descriptor null for URI: $uri")

            hasUnsavedChanges = false
            revalidateAndRefreshUI()
            onSuccess?.invoke()
        } catch (fnf: FileNotFoundException) {
            // Underlying document missing (ENOENT) – prompt user to pick a new location
            AlertDialog.Builder(requireContext())
                .setTitle("Original file missing")
                .setMessage("The previously selected file can't be found (it may have been moved or deleted). Save a new copy?")
                .setPositiveButton("Save As") { _, _ ->
                    val suggested = getSuggestedFileName()
                    createDocumentLauncher.launch(suggested)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } catch (e: Exception) {
            // If message hints ENOENT, offer same fallback; else simple toast
            val msg = e.message ?: "Unknown error"
            if (msg.contains("ENOENT", ignoreCase = true)) {
                AlertDialog.Builder(requireContext())
                    .setTitle("File not found")
                    .setMessage("Cannot open the existing file (ENOENT). Save to a new file instead?")
                    .setPositiveButton("Save As") { _, _ ->
                        val suggested = getSuggestedFileName()
                        createDocumentLauncher.launch(suggested)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } else {
                Toast.makeText(requireContext(), "Error saving file: $msg", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun revalidateAndRefreshUI() {
        // If fragment no longer attached, skip
        if (!isAdded) return
        // Prevent nested calls (e.g., triggered indirectly while already rebuilding)
        if (isRefreshing) return
        isRefreshing = true
        try {
            randomizationLevel = 0
            globalErrors.clear()
            lastCommand = null
            resourceExistenceMap.clear()

            val bracketedReferences = mutableSetOf<String>()
            for (line in allLines) {
                ResourceFileChecker.findBracketedFiles(line).forEach { bracketedReferences.add(it) }
            }
            if (resourcesFolderUri != null) {
                bracketedReferences.forEach { fileRef ->
                    val doesExist = ResourceFileChecker.fileExistsInResources(requireContext(), fileRef)
                    resourceExistenceMap[fileRef] = doesExist
                }
            }

            PerfTimer.track("ProtocolDialog.revalidate") { computeValidationCache() }

            dynamicContentContainer?.let { target ->
                target.removeAllViews()
                val built =
                    try {
                        buildCompletedView()
                    } catch (e: Exception) {
                        LinearLayout(requireContext()).apply {
                            setPadding(32, 32, 32, 32)
                            addView(
                                TextView(requireContext()).apply {
                                    text = "Error rebuilding view: ${e.message}"
                                    setTextColor(Color.RED)
                                },
                            )
                        }
                    }
                target.addView(built)
            }
            if (pendingAutoScrollToFirstIssue) {
                pendingAutoScrollToFirstIssue = false
                view?.postDelayed({
                    if (issueLineNumbers.isNotEmpty()) highlightAndScrollTo(issueLineNumbers.first())
                }, 60)
            }
        } finally {
            isRefreshing = false
        }
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
        val summaryLabel =
            TextView(requireContext()).apply {
                text = getString(R.string.label_summary_counts, total, errorCount, warningCount)
                setPadding(24, 8, 24, 4)
                textSize = applyScale(13f)
                setTypeface(null, Typeface.BOLD)
            }

        if (randomizationLevel > 0) {
            globalErrors.add("RANDOMIZE_ON not closed by matching RANDOMIZE_OFF")
        }

        addGlobalErrorsRow(contentTable)
        addStraySemicolonFixRow(contentTable)
        addDuplicateLabelFixRow(contentTable)
        addUndefinedGotoFixRow(contentTable)
        addTimerFixRow(contentTable)

        scrollView.addView(contentTable)
        containerLayout.addView(headerTable)
        containerLayout.addView(summaryLabel)
        containerLayout.addView(scrollView)

        // Build issue list for navigation (errors or warnings present)
        issueLineNumbers =
            validationCache.filter { it.error.isNotEmpty() || it.warning.isNotEmpty() }
                .map { it.lineNumber }
        issueIndex = if (issueLineNumbers.isNotEmpty()) -1 else -1
        return containerLayout
    }

    // --- Extracted helper sections (no functional change) ---
    private fun addGlobalErrorsRow(contentTable: TableLayout) {
        if (globalErrors.isEmpty()) return
        val row =
            TableRow(requireContext()).apply {
                setBackgroundColor(Color.parseColor("#FFEEEE"))
                setPadding(16, 8, 16, 8)
            }
        val innerLayout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val msgView =
            TextView(requireContext()).apply {
                text = globalErrors.joinToString("\n")
                setTextColor(Color.RED)
                setTypeface(null, Typeface.BOLD)
                textSize = applyScale(13f)
            }
        innerLayout.addView(msgView)
        if (globalErrors.any { it.contains("RANDOMIZE_ON not closed", ignoreCase = true) }) {
            val fixBtn =
                Button(requireContext()).apply {
                    text = "Insert RANDOMIZE_OFF"
                    setOnClickListener {
                        pushUndoState()
                        allLines.add("RANDOMIZE_OFF")
                        hasUnsavedChanges = true
                        revalidateAndRefreshUI()
                        Toast.makeText(requireContext(), "Inserted RANDOMIZE_OFF", Toast.LENGTH_SHORT).show()
                    }
                }
            innerLayout.addView(fixBtn)
        }
        if (globalErrors.any { it.contains("Duplicate STUDY_ID", ignoreCase = true) }) {
            val fixStudy =
                Button(requireContext()).apply {
                    text = getString(R.string.action_fix_duplicate_study_id)
                    setOnClickListener {
                        pushUndoState()
                        var seen = false
                        val iterator = allLines.listIterator()
                        while (iterator.hasNext()) {
                            val line = iterator.next()
                            if (line.trim().uppercase().startsWith("STUDY_ID;")) {
                                if (!seen) seen = true else iterator.remove()
                            }
                        }
                        hasUnsavedChanges = true
                        revalidateAndRefreshUI()
                        Toast.makeText(requireContext(), "Removed duplicate STUDY_ID", Toast.LENGTH_SHORT).show()
                    }
                }
            innerLayout.addView(fixStudy)
        }
        row.addView(innerLayout, TableRow.LayoutParams().apply { span = 3 })
        contentTable.addView(row)
    }

    private fun addStraySemicolonFixRow(contentTable: TableLayout) {
        val hasStraySemicolons = validationCache.any { it.error.contains("stray semicolon", ignoreCase = true) }
        if (!hasStraySemicolons) return
        val fixRow =
            TableRow(requireContext()).apply {
                setBackgroundColor(Color.parseColor("#FFF9E0"))
                setPadding(16, 8, 16, 8)
            }
        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val msg =
            TextView(requireContext()).apply {
                text = "Stray semicolons detected at line ends."
                setTextColor(Color.parseColor("#AA8800"))
                setTypeface(null, Typeface.BOLD)
                textSize = applyScale(13f)
            }
        val btn =
            Button(requireContext()).apply {
                text = "Fix stray semicolons"
                setOnClickListener {
                    pushUndoState()
                    var modified = false
                    for (i in allLines.indices) {
                        val raw = allLines[i]
                        if (raw.trim().endsWith(";")) {
                            val newLine = raw.replace(Regex(";\\s*$"), "")
                            if (newLine != raw) {
                                allLines[i] = newLine
                                modified = true
                            }
                        }
                    }
                    if (modified) {
                        hasUnsavedChanges = true
                        pendingAutoScrollToFirstIssue = true
                        revalidateAndRefreshUI()
                        Toast.makeText(requireContext(), "Removed stray semicolons", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "No trailing semicolons changed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        layout.addView(msg)
        layout.addView(btn)
        fixRow.addView(layout, TableRow.LayoutParams().apply { span = 3 })
        contentTable.addView(fixRow)
    }

    private fun addDuplicateLabelFixRow(contentTable: TableLayout) {
        val hasDuplicateLabelErrors = validationCache.any { it.error.contains("Label duplicated", ignoreCase = true) }
        if (!hasDuplicateLabelErrors) return
        val dupRow =
            TableRow(requireContext()).apply {
                setBackgroundColor(Color.parseColor("#FFEEEE"))
                setPadding(16, 8, 16, 8)
            }
        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val msg =
            TextView(requireContext()).apply {
                text = "Duplicate LABEL definitions found. Keep first occurrence and remove duplicates?"
                setTextColor(Color.parseColor("#BB0000"))
                setTypeface(null, Typeface.BOLD)
                textSize = applyScale(13f)
            }
        val btn =
            Button(requireContext()).apply {
                text = "Fix duplicate LABELs"
                setOnClickListener {
                    pushUndoState()
                    val seen = mutableSetOf<String>()
                    val iterator = allLines.listIterator()
                    var removed = 0
                    while (iterator.hasNext()) {
                        val line = iterator.next()
                        val t = line.trim()
                        if (t.uppercase().startsWith("LABEL;")) {
                            val parts = t.split(';')
                            val name = parts.getOrNull(1)?.trim().orEmpty()
                            if (name.isNotEmpty() && !seen.add(name)) {
                                iterator.remove()
                                removed++
                            }
                        }
                    }
                    if (removed > 0) {
                        hasUnsavedChanges = true
                        pendingAutoScrollToFirstIssue = true
                        revalidateAndRefreshUI()
                        Toast.makeText(requireContext(), "Removed $removed duplicate LABEL(s)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "No duplicate LABELs removed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        layout.addView(msg)
        layout.addView(btn)
        dupRow.addView(layout, TableRow.LayoutParams().apply { span = 3 })
        contentTable.addView(dupRow)
    }

    private fun addUndefinedGotoFixRow(contentTable: TableLayout) {
        val undefinedGotoTargets = mutableSetOf<String>()
        validationCache.forEach { entry ->
            val match = Regex("GOTO target label '([^']+)' not defined").find(entry.error)
            if (match != null) undefinedGotoTargets.add(match.groupValues[1])
        }
        if (undefinedGotoTargets.isEmpty()) return
        val gotoFixRow =
            TableRow(requireContext()).apply {
                setBackgroundColor(Color.parseColor("#FFF1E0"))
                setPadding(16, 8, 16, 8)
            }
        val lay = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val msg =
            TextView(requireContext()).apply {
                text = "Undefined GOTO target(s): ${undefinedGotoTargets.joinToString(", ")}. Insert missing LABEL lines?"
                setTextColor(Color.parseColor("#A65E00"))
                setTypeface(null, Typeface.BOLD)
                textSize = applyScale(13f)
            }
        val btn =
            Button(requireContext()).apply {
                text = "Insert missing LABELs"
                setOnClickListener {
                    pushUndoState()
                    val toInsert = mutableListOf<Pair<Int, String>>()
                    for (target in undefinedGotoTargets) {
                        val firstGotoLine =
                            validationCache.firstOrNull {
                                it.error.contains(
                                    "GOTO target label '$target' not defined",
                                )
                            }?.lineNumber
                        val insertionIndex = firstGotoLine ?: allLines.size
                        toInsert.add(insertionIndex to "LABEL;$target")
                    }
                    toInsert.sortedBy { it.first }.forEachIndexed { offset, pair ->
                        val (idx, line) = pair
                        val adj = idx + offset
                        if (adj <= allLines.size) allLines.add(adj, line) else allLines.add(line)
                    }
                    hasUnsavedChanges = true
                    pendingAutoScrollToFirstIssue = true
                    revalidateAndRefreshUI()
                    Toast.makeText(requireContext(), "Inserted ${undefinedGotoTargets.size} LABEL(s)", Toast.LENGTH_SHORT).show()
                }
            }
        lay.addView(msg)
        lay.addView(btn)
        gotoFixRow.addView(lay, TableRow.LayoutParams().apply { span = 3 })
        contentTable.addView(gotoFixRow)
    }

    private fun addTimerFixRow(contentTable: TableLayout) {
        val hasTimerErrors = validationCache.any { it.error.contains("TIMER must") }
        if (!hasTimerErrors) return
        val timerFixRow =
            TableRow(requireContext()).apply {
                setBackgroundColor(Color.parseColor("#E8F4FF"))
                setPadding(16, 8, 16, 8)
            }
        val lay = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val msg =
            TextView(requireContext()).apply {
                text = "Malformed TIMER command(s) detected. Normalize to TIMER;Header;Body;60;Continue?"
                setTextColor(Color.parseColor("#004B78"))
                setTypeface(null, Typeface.BOLD)
                textSize = applyScale(13f)
            }
        val btn =
            Button(requireContext()).apply {
                text = "Fix TIMER lines"
                setOnClickListener {
                    pushUndoState()
                    var fixed = 0
                    for (i in allLines.indices) {
                        val raw = allLines[i]
                        val t = raw.trim()
                        if (t.uppercase().startsWith("TIMER")) {
                            val parts = t.split(';').toMutableList()
                            if (parts.isNotEmpty() && parts[0].uppercase() == "TIMER") {
                                while (parts.size < 5) parts.add("")
                                val header = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "Header"
                                val body = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "Body"
                                val timeStrRaw = parts.getOrNull(3)?.trim().orEmpty()
                                val timeVal = timeStrRaw.toIntOrNull()?.takeIf { it >= 0 } ?: 60
                                val cont = parts.getOrNull(4)?.takeIf { it.isNotBlank() } ?: "Continue"
                                val normalized = "TIMER;$header;$body;$timeVal;$cont"
                                if (normalized != raw) {
                                    allLines[i] = normalized
                                    fixed++
                                }
                            }
                        }
                    }
                    if (fixed > 0) {
                        hasUnsavedChanges = true
                        pendingAutoScrollToFirstIssue = true
                        revalidateAndRefreshUI()
                        Toast.makeText(requireContext(), "Fixed $fixed TIMER line(s)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "No TIMER lines changed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        lay.addView(msg)
        lay.addView(btn)
        timerFixRow.addView(lay, TableRow.LayoutParams().apply { span = 3 })
        contentTable.addView(timerFixRow)
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
            // Columns: 0=drag handle,1=line#,2=command,3=issues
            setColumnStretchable(2, true)
            setColumnStretchable(3, true)
            setPadding(8, 8, 8, 8)

            val headerRow = TableRow(context)
            // Empty placeholder for drag handle column
            headerRow.addView(
                TextView(context).apply {
                    text = ""
                    width = (24 * resources.displayMetrics.density).toInt()
                },
            )
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
                // Columns: 0=drag handle,1=line#,2=command,3=issues
                setColumnStretchable(2, true)
                setColumnStretchable(3, true)
                setPadding(8, 8, 8, 8)
            }

        val filteredEntries =
            validationCache.filter { entry ->
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

            // Drag handle image
            val dragHandle =
                ImageView(context).apply {
                    setImageDrawable(androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_drag_handle))
                    contentDescription = getString(R.string.cd_drag_handle)
                    val sizePx = (32 * resources.displayMetrics.density).toInt()
                    layoutParams = TableRow.LayoutParams(sizePx, TableRow.LayoutParams.MATCH_PARENT)
                    setPadding(4, 4, 4, 4)
                    setOnLongClickListener {
                        if (!searchQuery.isNullOrBlank() || filterOption != FilterOption.HIDE_COMMENTS) {
                            Toast.makeText(context, getString(R.string.toast_drag_disabled), Toast.LENGTH_SHORT).show()
                            return@setOnLongClickListener true
                        }
                        val clip = ClipData.newPlainText("line", originalLineNumber.toString())
                        startDragAndDrop(clip, View.DragShadowBuilder(this), originalLineNumber, 0)
                        true
                    }
                }
            // Accept drop on row
            row.setOnDragListener { v, event ->
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> true
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        v.alpha = 0.6f
                        true
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        v.alpha = 1f
                        true
                    }
                    DragEvent.ACTION_DROP -> {
                        v.alpha = 1f
                        val fromLine = event.localState as? Int ?: return@setOnDragListener true
                        val toLine = originalLineNumber
                        if (fromLine != toLine) {
                            val fromIdx = fromLine - 1
                            val toIdx = toLine - 1
                            if (fromIdx in allLines.indices && toIdx in allLines.indices) {
                                pushUndoState()
                                val moving = allLines.removeAt(fromIdx)
                                val adjustedTo = if (fromIdx < toIdx) toIdx - 1 else toIdx
                                allLines.add(adjustedTo, moving)
                                hasUnsavedChanges = true
                                revalidateAndRefreshUI()
                                Toast.makeText(context, getString(R.string.toast_command_moved), Toast.LENGTH_SHORT).show()
                            }
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        v.alpha = 1f
                        true
                    }
                    else -> false
                }
            }
            row.addView(dragHandle)
            row.addView(createLineNumberCell(originalLineNumber))

            val commandCell = createBodyCell(lineContent, 2.0f)
            commandCell.setOnClickListener {
                val raw = allLines[originalLineNumber - 1]
                val firstToken = raw.split(';').firstOrNull()?.trim().orEmpty()
                if (!recognizedCommands.contains(firstToken.uppercase()) && !firstToken.equals("END", true)) {
                    showUnrecognizedCommandDialog(originalLineNumber - 1)
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
        // Hide synthetic EOF marker in UI while still surfacing its error globally
        validationCache =
            results
                .filter { it.raw != "<EOF>" }
                .map { ValidationEntry(it.lineNumber, it.raw, it.raw.trim(), it.error, it.warning) }
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
        issueIndex =
            when {
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
            textSize = applyScale(16f)
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
            textSize = applyScale(12f)
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
            textSize = applyScale(14f)
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
        val commandColor = "#AABBCC"
        val headerColor = "#CDEEFF"
        val bodyColor = "#FFFFE0"
        val itemColor = "#D1FCE3"
        val responseColor = "#DAF7A6"

        if (tokenIndex == 0) {
            return Color.parseColor(commandColor)
        }

        return when (commandUpper) {
            "INSTRUCTION" -> {
                when (tokenIndex) {
                    1 -> Color.parseColor(headerColor)
                    2 -> Color.parseColor(bodyColor)
                    3 -> Color.parseColor(responseColor)
                    else -> Color.TRANSPARENT
                }
            }
            "TIMER" -> {
                when (tokenIndex) {
                    1 -> Color.parseColor(headerColor)
                    2 -> Color.parseColor(bodyColor)
                    3 -> Color.parseColor(itemColor)
                    4 -> Color.parseColor(responseColor)
                    else -> Color.TRANSPARENT
                }
            }
            "SCALE", "SCALE[RANDOMIZED]" -> {
                when {
                    tokenIndex == 1 -> Color.parseColor(headerColor)
                    tokenIndex == 2 -> Color.parseColor(bodyColor)
                    tokenIndex == 3 -> Color.parseColor(itemColor)
                    tokenIndex > 3 -> Color.parseColor(responseColor)
                    else -> Color.TRANSPARENT
                }
            }
            "INPUTFIELD", "INPUTFIELD[RANDOMIZED]" -> {
                if (tokenIndex == 1) {
                    return Color.parseColor(headerColor)
                } else if (tokenIndex == 2) {
                    return Color.parseColor(bodyColor)
                } else if (tokenIndex == totalTokens - 1) {
                    return Color.parseColor(responseColor)
                } else if (tokenIndex >= 3) {
                    return Color.parseColor(itemColor)
                }
                return Color.TRANSPARENT
            }
            else -> {
                if (tokenIndex == 1) {
                    return Color.parseColor(itemColor)
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
        val reader = ProtocolReader()

        if (!customUriString.isNullOrEmpty()) {
            val uri = Uri.parse(customUriString)
            return try {
                val content = reader.readFileContent(requireContext(), uri)
                if (content.startsWith("Error reading file:")) {
                    // The ProtocolReader reported an error string instead of throwing
                    prefs.edit().remove(Prefs.KEY_PROTOCOL_URI).remove(Prefs.KEY_CURRENT_PROTOCOL_NAME).apply()
                    pendingLoadErrorMessage = content.replace("Error reading file:", "Failed to load protocol:").trim()
                    loadErrorPlaceholder
                } else if (content.startsWith("Error:")) {
                    prefs.edit().remove(Prefs.KEY_PROTOCOL_URI).remove(Prefs.KEY_CURRENT_PROTOCOL_NAME).apply()
                    pendingLoadErrorMessage = content.removePrefix("Error:").trim().ifEmpty { "Unknown error when loading protocol." }
                    loadErrorPlaceholder
                } else {
                    content
                }
            } catch (e: SecurityException) {
                // Permission lost (e.g., process death) or revoked: clear stored URI and fall back
                prefs.edit().remove(Prefs.KEY_PROTOCOL_URI).remove(Prefs.KEY_CURRENT_PROTOCOL_NAME).apply()
                pendingLoadErrorMessage = "Permission to access the saved protocol was revoked."
                loadErrorPlaceholder
            } catch (e: Exception) {
                pendingLoadErrorMessage = "Error reading protocol: ${e.message}"
                loadErrorPlaceholder
            }
        }
        return try {
            val assetContent =
                if (mode == "tutorial") {
                    reader.readFromAssets(requireContext(), "tutorial_protocol.txt")
                } else {
                    reader.readFromAssets(requireContext(), "demo_protocol.txt")
                }
            if (assetContent.startsWith("Error reading asset file:")) {
                pendingLoadErrorMessage = assetContent.replace("Error reading asset file:", "Unable to load bundled protocol:").trim()
                loadErrorPlaceholder
            } else {
                assetContent
            }
        } catch (e: Exception) {
            pendingLoadErrorMessage = "Error loading bundled protocol: ${e.message}"
            loadErrorPlaceholder
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

    private fun showInsertCommandDialog(
        insertAfterLine: Int?,
        editLineIndex: Int? = null,
    ) {
        val ctx = requireContext()
        // Container layout
        val container = ScrollView(ctx)
        val inner =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 24, 32, 8)
            }
        container.addView(inner)

        // Grouped command metadata
        data class CommandMeta(val name: String, val category: String)
        val commandMetaList =
            listOf(
                // Content / structural
                CommandMeta(
                    "INSTRUCTION",
                    "Content",
                ),
                CommandMeta("TIMER", "Content"), CommandMeta("SCALE", "Content"), CommandMeta("SCALE[RANDOMIZED]", "Content"),
                CommandMeta(
                    "INPUTFIELD",
                    "Content",
                ),
                CommandMeta("INPUTFIELD[RANDOMIZED]", "Content"), CommandMeta("LABEL", "Content"), CommandMeta("GOTO", "Content"),
                CommandMeta(
                    "HTML",
                    "Content",
                ),
                CommandMeta("TIMER_SOUND", "Content"), CommandMeta("LOG", "Content"), CommandMeta("END", "Content"),
                // Randomization
                CommandMeta("RANDOMIZE_ON", "Randomization"), CommandMeta("RANDOMIZE_OFF", "Randomization"),
                // Meta
                CommandMeta("STUDY_ID", "Meta"), CommandMeta("TRANSITIONS", "Meta"),
                // Style colors
                CommandMeta(
                    "HEADER_COLOR",
                    "Style",
                ),
                CommandMeta(
                    "BODY_COLOR",
                    "Style",
                ),
                CommandMeta("RESPONSE_TEXT_COLOR", "Style"), CommandMeta("RESPONSE_BACKGROUND_COLOR", "Style"),
                CommandMeta(
                    "SCREEN_BACKGROUND_COLOR",
                    "Style",
                ),
                CommandMeta(
                    "CONTINUE_TEXT_COLOR",
                    "Style",
                ),
                CommandMeta("CONTINUE_BACKGROUND_COLOR", "Style"), CommandMeta("TIMER_COLOR", "Style"),
                // Style sizes
                CommandMeta(
                    "HEADER_SIZE",
                    "Style",
                ),
                CommandMeta("BODY_SIZE", "Style"), CommandMeta("ITEM_SIZE", "Style"), CommandMeta("RESPONSE_SIZE", "Style"),
                CommandMeta("CONTINUE_SIZE", "Style"), CommandMeta("TIMER_SIZE", "Style"),
                // Style alignment
                CommandMeta(
                    "HEADER_ALIGNMENT",
                    "Style",
                ),
                CommandMeta(
                    "BODY_ALIGNMENT",
                    "Style",
                ),
                CommandMeta("CONTINUE_ALIGNMENT", "Style"), CommandMeta("TIMER_ALIGNMENT", "Style"),
            )
        val categories = listOf("All", "Content", "Randomization", "Meta", "Style")
        val isEdit = editLineIndex != null
        val existingParts =
            if (isEdit && editLineIndex != null && editLineIndex in allLines.indices) {
                ParsingUtils.customSplitSemicolons(allLines[editLineIndex]).map { it.trim() }
            } else {
                null
            }
        var selectedCommand = existingParts?.firstOrNull()?.trim().orEmpty()
        if (selectedCommand.isBlank()) {
            selectedCommand = commandMetaList.firstOrNull()?.name ?: ""
        }

        fun dp(value: Int): Int = (value * ctx.resources.displayMetrics.density).roundToInt()

        fun createRemoveButton(
            contentDescriptionText: String,
            onRemove: () -> Unit,
        ): ImageButton =
            ImageButton(ctx).apply {
                setImageDrawable(
                    androidx.core.content.ContextCompat.getDrawable(
                        ctx,
                        R.drawable.ic_close,
                    ),
                )
                val attrs = intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
                val typed = ctx.obtainStyledAttributes(attrs)
                background = typed.getDrawable(0)
                typed.recycle()
                contentDescription = contentDescriptionText
                scaleType = ImageView.ScaleType.CENTER
                val size = dp(40)
                layoutParams =
                    LinearLayout.LayoutParams(size, size).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        marginStart = dp(8)
                    }
                setPadding(dp(6), dp(6), dp(6), dp(6))
                try {
                    val themeWrapper =
                        androidx.appcompat.view.ContextThemeWrapper(
                            ctx,
                            com.google.android.material.R.style.ThemeOverlay_Material3,
                        )
                    val colorAttr = intArrayOf(com.google.android.material.R.attr.colorOnSurface)
                    val a = themeWrapper.obtainStyledAttributes(colorAttr)
                    val color = a.getColor(0, 0xFF444444.toInt())
                    a.recycle()
                    imageTintList = android.content.res.ColorStateList.valueOf(color)
                } catch (_: Exception) {
                }
                setOnClickListener { onRemove() }
            }
        val selectionCard =
            com.google.android.material.card.MaterialCardView(ctx).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        setMargins(0, 0, 0, dp(16))
                    }
                radius = 20f
                strokeWidth = 1
            }
        val selectionContainer =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(24), dp(20), dp(24), dp(20))
            }
        selectionContainer.addView(
            TextView(ctx).apply {
                text = getString(R.string.label_select_command)
                setTypeface(null, Typeface.BOLD)
                textSize = applyScale(15f)
            },
        )
        val categorySpinner =
            Spinner(ctx).apply {
                adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, categories)
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(18)
                    }
            }
        // Search + dynamic filtered spinner
        val searchBox =
            EditText(ctx).apply {
                hint = "Search commands"
                setSingleLine()
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(12)
                    }
            }
        val commandInputLayout =
            com.google.android.material.textfield.TextInputLayout(ctx, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox).apply {
                hint = getString(R.string.label_select_command)
                boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_DROPDOWN_MENU
                setPadding(0, dp(8), 0, 0)
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(12)
                    }
            }
        val commandDropdown =
            com.google.android.material.textfield.MaterialAutoCompleteTextView(commandInputLayout.context).apply {
                inputType = android.text.InputType.TYPE_NULL
                keyListener = null
                setOnClickListener { showDropDown() }
                setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDropDown() }
            }
        commandInputLayout.addView(commandDropdown)
        var lockedCommandLabel: TextView? = null
        if (isEdit) {
            val lockedLabel =
                TextView(ctx).apply {
                    text = selectedCommand.ifBlank { getString(R.string.value_none) }
                    setTypeface(null, Typeface.BOLD)
                    textSize = applyScale(16f)
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                    setTextColor(Color.parseColor("#1F2937"))
                    setBackgroundColor(Color.parseColor("#EEF2FF"))
                }
            lockedCommandLabel = lockedLabel
            selectionContainer.addView(lockedLabel)
            selectionContainer.addView(
                TextView(ctx).apply {
                    text = getString(R.string.insert_command_edit_locked_hint)
                    setTextColor(Color.parseColor("#6B7280"))
                    textSize = applyScale(12f)
                    setPadding(0, dp(12), 0, 0)
                },
            )
        } else {
            selectionContainer.addView(
                TextView(ctx).apply {
                    text = getString(R.string.insert_command_selection_hint)
                    setTextColor(Color.parseColor("#6B7280"))
                    textSize = applyScale(12f)
                    setPadding(0, dp(12), 0, 0)
                },
            )
            selectionContainer.addView(categorySpinner)
            selectionContainer.addView(searchBox)
            selectionContainer.addView(commandInputLayout)
        }
        selectionCard.addView(selectionContainer)
        inner.addView(selectionCard)

        var filtered: List<CommandMeta> = commandMetaList

        fun applyFilter(keepSelection: Boolean = true) {
            if (isEdit) return
            val query = searchBox.text.toString().trim().lowercase()
            val selectedCategory = categories.getOrNull(categorySpinner.selectedItemPosition) ?: "All"
            val base =
                if (query.isNotEmpty()) {
                    commandMetaList
                } else if (selectedCategory == "All") {
                    commandMetaList
                } else {
                    commandMetaList.filter { it.category == selectedCategory }
                }
            filtered =
                base.filter { meta ->
                    query.isEmpty() || meta.name.lowercase().contains(query)
                }
            val displayNames = filtered.map { it.name }
            commandDropdown.setAdapter(ArrayAdapter(ctx, android.R.layout.simple_dropdown_item_1line, displayNames))
            val previous = if (keepSelection) selectedCommand else null
            selectedCommand =
                when {
                    previous != null && displayNames.contains(previous) -> previous
                    displayNames.isNotEmpty() -> displayNames.first()
                    else -> ""
                }
            commandDropdown.setText(selectedCommand, false)
        }

        if (!isEdit) {
            applyFilter(keepSelection = false)
            if (selectedCommand.isNotBlank()) {
                commandDropdown.setText(selectedCommand, false)
            }
        }

        fun edit(hintRes: Int): EditText =
            EditText(ctx).apply {
                hint = getString(hintRes)
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(12)
                    }
            }

        val header = edit(R.string.hint_header)
        val body = edit(R.string.hint_body)
        val cont =
            edit(R.string.hint_continue).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f,
                    ).apply {
                        topMargin = 0
                    }
                setSingleLine()
            }
        val holdToggle =
            com.google.android.material.checkbox.MaterialCheckBox(ctx).apply {
                text = getString(R.string.action_hold_label)
                textSize = applyScale(13f)
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        marginStart = dp(12)
                    }
                visibility = View.GONE
                contentDescription = getString(R.string.cd_hold_toggle_off)
                setPadding(dp(4), dp(4), dp(4), dp(4))
            }
        TooltipCompat.setTooltipText(holdToggle, getString(R.string.hint_hold_to_confirm))
        holdToggle.setOnCheckedChangeListener { _, isChecked ->
            val cdRes =
                if (isChecked) {
                    R.string.cd_hold_toggle_on
                } else {
                    R.string.cd_hold_toggle_off
                }
            holdToggle.contentDescription = getString(cdRes)
        }
        val holdHelperText =
            TextView(ctx).apply {
                text = getString(R.string.hint_hold_to_confirm)
                setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        this,
                        com.google.android.material.R.attr.colorOnSurfaceVariant,
                        Color.parseColor("#6B7280"),
                    ),
                )
                textSize = applyScale(12f)
                visibility = View.GONE
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(4)
                    }
            }
        val continueRow =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(12)
                    }
                addView(cont)
                addView(holdToggle)
            }
        val continueSection =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                addView(continueRow)
                addView(holdHelperText)
            }
        val holdSupportedCommands =
            setOf(
                "INSTRUCTION",
                "TIMER",
                "INPUTFIELD",
                "INPUTFIELD[RANDOMIZED]",
                "HTML",
            )
        val holdDetectRegex = Regex("\\[HOLD]", RegexOption.IGNORE_CASE)
        val holdStripRegex = Regex("\\s*\\[HOLD]", RegexOption.IGNORE_CASE)
        var lastHoldCommand: String? = null

        val randomizableCommandPairs =
            listOf(
                "SCALE" to "SCALE[RANDOMIZED]",
                "INPUTFIELD" to "INPUTFIELD[RANDOMIZED]",
            )
        val randomizableCommandMap = randomizableCommandPairs.toMap()
        val randomizedToBaseMap = randomizableCommandPairs.associate { (base, randomized) -> randomized to base }

        fun baseCommandName(command: String): String = randomizedToBaseMap[command] ?: command

        fun commandWithRandomization(
            base: String,
            randomize: Boolean,
        ): String = if (randomize) randomizableCommandMap[base] ?: base else base

        fun isRandomizedCommandName(command: String): Boolean = randomizedToBaseMap.containsKey(command)

        val randomizeToggle =
            com.google.android.material.checkbox.MaterialCheckBox(ctx).apply {
                text = getString(R.string.label_randomize_order)
                textSize = applyScale(13f)
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                visibility = View.GONE
                contentDescription = getString(R.string.cd_randomize_toggle_off)
                setPadding(dp(4), dp(6), dp(4), dp(6))
            }
        var isProgrammaticRandomizeChange = false
        val randomizeHelperText =
            TextView(ctx).apply {
                text = getString(R.string.hint_randomize_scale)
                setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        this,
                        com.google.android.material.R.attr.colorOnSurfaceVariant,
                        Color.parseColor("#6B7280"),
                    ),
                )
                textSize = applyScale(12f)
                visibility = View.GONE
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(4)
                    }
            }
        val randomizeSection =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(8)
                    }
                visibility = View.GONE
                addView(randomizeToggle)
                addView(randomizeHelperText)
            }
        TooltipCompat.setTooltipText(randomizeToggle, getString(R.string.hint_randomize_scale))

        val commandsWithMedia =
            setOf(
                "INSTRUCTION",
                "TIMER",
                "SCALE",
                "SCALE[RANDOMIZED]",
                "INPUTFIELD",
                "INPUTFIELD[RANDOMIZED]",
            )
    var currentMediaCommand: String? = null
    var mediaPrefillApplied = false

        val mediaSection =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(12)
                    }
            }
        val mediaHeader =
            TextView(ctx).apply {
                text = getString(R.string.label_media_section)
                textSize = applyScale(14f)
                setTypeface(null, Typeface.BOLD)
            }
        val mediaHelperText =
            TextView(ctx).apply {
                text = getString(R.string.hint_media_section)
                setTextColor(Color.parseColor("#6B7280"))
                textSize = applyScale(12f)
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(4)
                    }
            }

        fun buildMediaInputLayout(hintRes: Int): Pair<com.google.android.material.textfield.TextInputLayout, TextInputEditText> {
            val inputLayout =
                com.google.android.material.textfield.TextInputLayout(
                    ctx,
                    null,
                    com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox,
                ).apply {
                    hint = getString(hintRes)
                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            topMargin = dp(12)
                        }
                    boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                }
            val editText =
                TextInputEditText(inputLayout.context).apply {
                    setSingleLine()
                }
            inputLayout.addView(editText)
            return inputLayout to editText
        }

        val (soundInputLayout, soundInput) = buildMediaInputLayout(R.string.hint_media_sound)
        val (videoInputLayout, videoInput) = buildMediaInputLayout(R.string.hint_media_video)
        val (customHtmlInputLayout, customHtmlInput) = buildMediaInputLayout(R.string.hint_media_custom_html)

        mediaSection.addView(mediaHeader)
        mediaSection.addView(mediaHelperText)
        mediaSection.addView(soundInputLayout)
        mediaSection.addView(videoInputLayout)
        mediaSection.addView(customHtmlInputLayout)

        fun resetMediaInputs() {
            if (soundInput.text?.isNotEmpty() == true) soundInput.setText("")
            if (videoInput.text?.isNotEmpty() == true) videoInput.setText("")
            if (customHtmlInput.text?.isNotEmpty() == true) customHtmlInput.setText("")
        }

        data class MediaPrefill(
            val sound: String = "",
            val video: String = "",
            val customHtml: String = "",
        )

        val trailingMediaRegex =
            Regex("<([^>]+\\.(?:mp3|wav|mp4|html)(?:,[^>]+)?)>\\s*$", RegexOption.IGNORE_CASE)

        fun extractMediaPlaceholders(raw: String): Pair<String, MediaPrefill> {
            var workingText = raw.trimEnd()
            var soundValue = ""
            var videoValue = ""
            var htmlValue = ""
            while (true) {
                val match = trailingMediaRegex.find(workingText) ?: break
                val value = match.groupValues[1]
                val filePart = value.substringBefore(',').trim()
                val ext = filePart.substringAfterLast('.', "").lowercase()
                val consumed =
                    when {
                        (ext == "mp3" || ext == "wav") && soundValue.isEmpty() -> {
                            soundValue = value
                            true
                        }
                        ext == "mp4" && videoValue.isEmpty() -> {
                            videoValue = value
                            true
                        }
                        ext == "html" && htmlValue.isEmpty() -> {
                            htmlValue = value
                            true
                        }
                        else -> false
                    }
                if (!consumed) {
                    break
                }
                workingText = workingText.removeRange(match.range).trimEnd()
            }
            return workingText to MediaPrefill(soundValue, videoValue, htmlValue)
        }

        fun populateMediaInputs(prefill: MediaPrefill) {
            resetMediaInputs()
            if (prefill.sound.isNotEmpty()) {
                soundInput.setText(prefill.sound)
            }
            if (prefill.video.isNotEmpty()) {
                videoInput.setText(prefill.video)
            }
            if (prefill.customHtml.isNotEmpty()) {
                customHtmlInput.setText(prefill.customHtml)
            }
        }

        fun normalizedMediaValue(editText: TextInputEditText): String {
            val raw = editText.text?.toString()?.trim().orEmpty()
            if (raw.isEmpty()) return ""
            return raw.removePrefix("<").removeSuffix(">").trim()
        }

        fun hasAllowedExtension(
            value: String,
            allowed: Set<String>,
        ): Boolean {
            val filePart = value.substringBefore(',').trim()
            if (!filePart.contains('.')) return false
            val ext = filePart.substringAfterLast('.', "").lowercase()
            return allowed.contains(ext)
        }

        fun appendMediaPlaceholders(
            base: String,
            applyMedia: Boolean,
            soundValue: String,
            videoValue: String,
            htmlValue: String,
        ): String {
            if (!applyMedia) return base
            var result = base.trimEnd()
            fun appendIfNeeded(value: String) {
                if (value.isBlank()) return
                val placeholder = "<$value>"
                if (!result.contains(placeholder)) {
                    result =
                        if (result.isEmpty()) {
                            placeholder
                        } else {
                            "$result $placeholder"
                        }
                }
            }
            appendIfNeeded(soundValue)
            appendIfNeeded(videoValue)
            appendIfNeeded(htmlValue)
            return result.trim()
        }

        randomizeToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isProgrammaticRandomizeChange) {
                return@setOnCheckedChangeListener
            }
            val base = baseCommandName(selectedCommand)
            if (!randomizableCommandMap.containsKey(base)) {
                isProgrammaticRandomizeChange = true
                randomizeToggle.isChecked = false
                isProgrammaticRandomizeChange = false
                randomizeToggle.contentDescription = getString(R.string.cd_randomize_toggle_off)
                return@setOnCheckedChangeListener
            }
            val updatedCommand = commandWithRandomization(base, isChecked)
            if (updatedCommand != selectedCommand) {
                selectedCommand = updatedCommand
                val cdRes = if (isChecked) R.string.cd_randomize_toggle_on else R.string.cd_randomize_toggle_off
                randomizeToggle.contentDescription = getString(cdRes)
                if (!isEdit) {
                    commandDropdown.setText(selectedCommand, false)
                } else {
                    lockedCommandLabel?.text = selectedCommand.ifBlank { getString(R.string.value_none) }
                }
            } else {
                val cdRes = if (isChecked) R.string.cd_randomize_toggle_on else R.string.cd_randomize_toggle_off
                randomizeToggle.contentDescription = getString(cdRes)
            }
        }

        fun stripHoldToken(raw: String?): Pair<String, Boolean> {
            if (raw.isNullOrBlank()) {
                return "" to false
            }
            val contains = holdDetectRegex.containsMatchIn(raw)
            val cleaned =
                if (contains) {
                    raw.replace(holdStripRegex, "").trim()
                } else {
                    raw.trim()
                }
            return cleaned to contains
        }

        fun applyHoldToken(base: String): String {
            val sanitized = holdStripRegex.replace(base, "").trim()
            return if (holdToggle.isChecked) {
                if (sanitized.isEmpty()) {
                    "[HOLD]"
                } else {
                    "$sanitized[HOLD]"
                }
            } else {
                sanitized
            }
        }

        val time =
            edit(R.string.hint_time_seconds).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
        val scaleItemFields = mutableListOf<TextInputEditText>()
        data class ScaleResponseEntry(
            val container: View,
            val textField: TextInputEditText,
            val labelField: TextInputEditText,
            val labelContainer: View,
            val branchButton: ImageButton,
            var branchingEnabled: Boolean,
        )
        val scaleResponseEntries = mutableListOf<ScaleResponseEntry>()

        val scaleItemsList =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
            }
        val scaleResponsesList =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
            }

        fun updateScaleItemHints() {
            scaleItemFields.forEachIndexed { index, editText ->
                (editText.parent as? com.google.android.material.textfield.TextInputLayout)?.hint =
                    getString(R.string.hint_scale_item_number, index + 1)
            }
        }

        fun updateScaleResponseHints() {
            scaleResponseEntries.forEachIndexed { index, entry ->
                (entry.textField.parent as? com.google.android.material.textfield.TextInputLayout)?.hint =
                    getString(R.string.hint_scale_response_number, index + 1)
                (entry.labelField.parent as? com.google.android.material.textfield.TextInputLayout)?.hint =
                    getString(R.string.hint_scale_response_label, index + 1)
            }
        }

        fun addScaleItemField(prefill: String = "") {
            val row =
                LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            topMargin = dp(8)
                        }
                    gravity = Gravity.CENTER_VERTICAL
                }
            val inputLayout =
                com.google.android.material.textfield.TextInputLayout(
                    ctx,
                    null,
                    com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox,
                ).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f,
                        )
                    boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                }
            val editText =
                TextInputEditText(inputLayout.context).apply {
                    setText(prefill)
                    setSingleLine()
                }
            inputLayout.addView(editText)
            val removeButton =
                createRemoveButton(getString(R.string.cd_remove_scale_item)) {
                    scaleItemsList.removeView(row)
                    scaleItemFields.remove(editText)
                    if (scaleItemFields.isEmpty()) {
                        addScaleItemField()
                    } else {
                        updateScaleItemHints()
                    }
                }
            row.addView(inputLayout)
            row.addView(removeButton)
            scaleItemsList.addView(row)
            scaleItemFields.add(editText)
            updateScaleItemHints()
            if (prefill.isNotEmpty()) {
                editText.setSelection(prefill.length)
            }
        }

        fun parseResponsePrefill(raw: String): Pair<String, String> {
            val trimmed = raw.trim()
            if (trimmed.endsWith(']') && trimmed.contains('[')) {
                val bracketStart = trimmed.lastIndexOf('[')
                val bracketEnd = trimmed.lastIndexOf(']')
                if (bracketStart in 0 until bracketEnd) {
                    val textPart = trimmed.substring(0, bracketStart).trim()
                    val labelPart = trimmed.substring(bracketStart + 1, bracketEnd).trim()
                    return textPart to labelPart
                }
            }
            return trimmed to ""
        }

        fun addScaleResponseField(prefillText: String = "", prefillLabel: String = "") {
            val branchingInitiallyEnabled = prefillLabel.isNotEmpty()
            val card =
                com.google.android.material.card.MaterialCardView(ctx).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            topMargin = dp(8)
                        }
                    radius = 18f
                    strokeWidth = 1
                    setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setContentPadding(dp(16), dp(16), dp(16), dp(16))
                }
            val column =
                LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                }
            val headerRow =
                LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
            val responseLayout =
                com.google.android.material.textfield.TextInputLayout(
                    ctx,
                    null,
                    com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox,
                ).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f,
                        )
                    boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                }
            val responseField =
                TextInputEditText(responseLayout.context).apply {
                    setText(prefillText)
                    setSingleLine()
                }
            responseLayout.addView(responseField)
            headerRow.addView(responseLayout)

            val branchButton =
                ImageButton(ctx).apply {
                    setImageDrawable(
                        androidx.core.content.ContextCompat.getDrawable(ctx, R.drawable.ic_branch),
                    )
                    val attrs = intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
                    val typed = ctx.obtainStyledAttributes(attrs)
                    background = typed.getDrawable(0)
                    typed.recycle()
                    contentDescription = getString(R.string.cd_toggle_scale_response_branch)
                    scaleType = ImageView.ScaleType.CENTER
                    val size = dp(40)
                    layoutParams =
                        LinearLayout.LayoutParams(size, size).apply {
                            marginStart = dp(8)
                        }
                    setPadding(dp(6), dp(6), dp(6), dp(6))
                }

            val removeButton =
                createRemoveButton(getString(R.string.cd_remove_scale_response)) {
                    scaleResponsesList.removeView(card)
                    scaleResponseEntries.removeAll { it.container == card }
                    if (scaleResponseEntries.isEmpty()) {
                        addScaleResponseField()
                    } else {
                        updateScaleResponseHints()
                    }
                }

            headerRow.addView(branchButton)
            headerRow.addView(removeButton)

            val labelLayout =
                com.google.android.material.textfield.TextInputLayout(
                    ctx,
                    null,
                    com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox,
                ).apply {
                    boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                }
            val labelField =
                TextInputEditText(labelLayout.context).apply {
                    setText(prefillLabel)
                    setSingleLine()
                }
            labelLayout.addView(labelField)

            val labelContainer =
                LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            topMargin = dp(12)
                        }
                    visibility = if (branchingInitiallyEnabled) View.VISIBLE else View.GONE
                }
            labelContainer.addView(
                TextView(ctx).apply {
                    text = getString(R.string.hint_scale_branching_helper)
                    textSize = applyScale(12f)
                    setPadding(0, 0, 0, dp(4))
                    setTextColor(
                        com.google.android.material.color.MaterialColors.getColor(
                            this,
                            com.google.android.material.R.attr.colorOnSurfaceVariant,
                            android.graphics.Color.parseColor("#5F6368"),
                        ),
                    )
                },
            )
            labelContainer.addView(labelLayout)

            column.addView(headerRow)
            column.addView(labelContainer)
            card.addView(column)
            scaleResponsesList.addView(card)

            val entry =
                ScaleResponseEntry(
                    card,
                    responseField,
                    labelField,
                    labelContainer,
                    branchButton,
                    branchingInitiallyEnabled,
                )
            scaleResponseEntries.add(entry)

            fun applyBranchState(enabled: Boolean) {
                entry.branchingEnabled = enabled
                labelContainer.visibility = if (enabled) View.VISIBLE else View.GONE
                labelLayout.isEnabled = enabled
                labelField.isEnabled = enabled
                val activeColor =
                    com.google.android.material.color.MaterialColors.getColor(
                        branchButton,
                        com.google.android.material.R.attr.colorPrimary,
                        android.graphics.Color.parseColor("#2962FF"),
                    )
                val inactiveColor =
                    com.google.android.material.color.MaterialColors.getColor(
                        branchButton,
                        com.google.android.material.R.attr.colorOnSurface,
                        android.graphics.Color.parseColor("#5F6368"),
                    )
                branchButton.imageTintList =
                    android.content.res.ColorStateList.valueOf(if (enabled) activeColor else inactiveColor)
                branchButton.alpha = if (enabled) 1f else 0.72f
                val descRes =
                    if (enabled) R.string.tip_scale_response_branch_remove else R.string.tip_scale_response_branch_add
                branchButton.contentDescription = getString(descRes)
                TooltipCompat.setTooltipText(branchButton, getString(descRes))
                if (enabled) {
                    if (labelField.text.isNullOrBlank()) {
                        labelField.requestFocus()
                    }
                } else {
                    labelField.text?.clear()
                }
                updateScaleResponseHints()
            }

            applyBranchState(branchingInitiallyEnabled)

            branchButton.setOnClickListener {
                applyBranchState(!entry.branchingEnabled)
            }

            updateScaleResponseHints()
            if (prefillText.isNotEmpty()) {
                responseField.setSelection(prefillText.length)
            }
            if (prefillLabel.isNotEmpty()) {
                labelField.setSelection(prefillLabel.length)
            }
        }

        fun rebuildScaleItemFields(values: List<String>) {
            scaleItemFields.clear()
            scaleItemsList.removeAllViews()
            val actual = values.ifEmpty { listOf("") }
            actual.forEach { addScaleItemField(it) }
        }

        fun rebuildScaleResponseFields(values: List<String>) {
            scaleResponseEntries.clear()
            scaleResponsesList.removeAllViews()
            val actual = values.ifEmpty { listOf("", "") }
            actual.forEach { value ->
                val (text, label) = parseResponsePrefill(value)
                addScaleResponseField(text, label)
            }
        }

        val addScaleItemButton =
            com.google.android.material.button.MaterialButton(
                ctx,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle,
            ).apply {
                text = getString(R.string.action_add_scale_item)
                isAllCaps = false
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(12)
                    }
                setOnClickListener {
                    addScaleItemField()
                    scaleItemFields.lastOrNull()?.requestFocus()
                }
            }

        val addScaleResponseButton =
            com.google.android.material.button.MaterialButton(
                ctx,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle,
            ).apply {
                text = getString(R.string.action_add_scale_response)
                isAllCaps = false
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(12)
                    }
                setOnClickListener {
                    addScaleResponseField()
                    scaleResponseEntries.lastOrNull()?.textField?.requestFocus()
                }
            }

        val scaleItemsSection =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(12)
                    }
                addView(
                    TextView(ctx).apply {
                        text = getString(R.string.label_scale_items)
                        textSize = applyScale(14f)
                        setTypeface(null, Typeface.BOLD)
                    },
                )
                addView(scaleItemsList)
                addView(addScaleItemButton)
            }

        val scaleResponsesSection =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(12)
                    }
                addView(
                    TextView(ctx).apply {
                        text = getString(R.string.label_scale_responses)
                        textSize = applyScale(14f)
                        setTypeface(null, Typeface.BOLD)
                    },
                )
                addView(scaleResponsesList)
                addView(addScaleResponseButton)
            }

        fun collectScaleItems(): List<String> =
            scaleItemFields.map { it.text.toString().trim() }.filter { it.isNotEmpty() }

        fun collectScaleResponses(): List<String> =
            scaleResponseEntries
                .mapNotNull { entry ->
                    val text = entry.textField.text.toString().trim()
                    if (text.isEmpty()) {
                        null
                    } else {
                        val label = entry.labelField.text.toString().trim()
                        if (entry.branchingEnabled && label.isNotEmpty()) "$text [$label]" else text
                    }
                }
        val labelName = edit(R.string.hint_label_name)
        val gotoLabel = edit(R.string.hint_goto_label)
        val filename = edit(R.string.hint_filename)
        val message = edit(R.string.hint_message)
        val colorValue = edit(R.string.hint_color_value)
        val sizeValue =
            edit(R.string.hint_size_value).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
        val inputFieldEntries = mutableListOf<TextInputEditText>()
        val inputFieldList =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
            }

        fun updateInputFieldHints() {
            inputFieldEntries.forEachIndexed { index, editText ->
                (editText.parent as? com.google.android.material.textfield.TextInputLayout)?.hint =
                    getString(R.string.hint_input_field_number, index + 1)
            }
        }

        fun addInputFieldEntry(prefill: String = "") {
            val row =
                LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            topMargin = dp(8)
                        }
                    gravity = Gravity.CENTER_VERTICAL
                }
            val inputLayout =
                com.google.android.material.textfield.TextInputLayout(
                    ctx,
                    null,
                    com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox,
                ).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f,
                        )
                    boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                }
            val editText =
                TextInputEditText(inputLayout.context).apply {
                    setText(prefill)
                    setSingleLine()
                }
            inputLayout.addView(editText)
            val removeButton =
                createRemoveButton(getString(R.string.cd_remove_input_field)) {
                    inputFieldList.removeView(row)
                    inputFieldEntries.remove(editText)
                    if (inputFieldEntries.isEmpty()) {
                        addInputFieldEntry()
                    } else {
                        updateInputFieldHints()
                    }
                }
            row.addView(inputLayout)
            row.addView(removeButton)
            inputFieldList.addView(row)
            inputFieldEntries.add(editText)
            updateInputFieldHints()
            if (prefill.isNotEmpty()) editText.setSelection(prefill.length)
        }

        fun rebuildInputFieldEntries(values: List<String>) {
            inputFieldEntries.clear()
            inputFieldList.removeAllViews()
            val actual = values.ifEmpty { listOf("") }
            actual.forEach { addInputFieldEntry(it) }
        }

        val addInputFieldButton =
            com.google.android.material.button.MaterialButton(
                ctx,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle,
            ).apply {
                text = getString(R.string.action_add_input_field)
                isAllCaps = false
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(12)
                    }
                setOnClickListener {
                    addInputFieldEntry()
                    inputFieldEntries.lastOrNull()?.requestFocus()
                }
            }

        val inputFieldSection =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(12)
                    }
                addView(
                    TextView(ctx).apply {
                        text = getString(R.string.label_inputfield_fields)
                        textSize = applyScale(14f)
                        setTypeface(null, Typeface.BOLD)
                    },
                )
                addView(inputFieldList)
                addView(addInputFieldButton)
            }

        fun collectInputFieldEntries(): List<String> =
            inputFieldEntries.map { it.text.toString().trim() }.filter { it.isNotEmpty() }
        val alignmentSpinner =
            Spinner(ctx).apply {
                adapter =
                    ArrayAdapter(
                        ctx,
                        android.R.layout.simple_spinner_dropdown_item,
                        arrayOf(
                            "LEFT",
                            "CENTER",
                            "RIGHT",
                        ),
                    )
            }
        val studyIdValue = edit(R.string.hint_study_id_value)
        val transitionsSpinner =
            Spinner(ctx).apply {
                adapter =
                    ArrayAdapter(
                        ctx,
                        android.R.layout.simple_spinner_dropdown_item,
                        arrayOf(
                            "off",
                            "slide",
                            "slideleft",
                            "fade",
                            "dissolve",
                        ),
                    )
            }

        val parameterCard =
            com.google.android.material.card.MaterialCardView(ctx).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        setMargins(0, 0, 0, dp(16))
                    }
                radius = 20f
                strokeWidth = 1
            }
        val parameterContainer =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(24), dp(20), dp(24), dp(20))
            }
        parameterContainer.addView(
            TextView(ctx).apply {
                text = getString(R.string.label_parameters)
                setTypeface(null, Typeface.BOLD)
                textSize = applyScale(15f)
            },
        )
        parameterContainer.addView(
            TextView(ctx).apply {
                text = getString(R.string.insert_command_parameters_hint)
                setTextColor(Color.parseColor("#6B7280"))
                textSize = applyScale(12f)
                setPadding(0, dp(12), 0, dp(4))
            },
        )
        val paramGroup =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
            }
        parameterContainer.addView(paramGroup)
        parameterCard.addView(parameterContainer)
        inner.addView(parameterCard)

        fun attachMediaSectionFor(
            command: String,
            resetIfChanged: Boolean,
        ) {
            if (resetIfChanged && currentMediaCommand != command) {
                resetMediaInputs()
                mediaPrefillApplied = false
            }
            (mediaSection.parent as? ViewGroup)?.removeView(mediaSection)
            mediaSection.visibility = View.VISIBLE
            paramGroup.addView(mediaSection)
            currentMediaCommand = command
        }

        fun detachMediaSection(reset: Boolean) {
            (mediaSection.parent as? ViewGroup)?.removeView(mediaSection)
            if (reset) {
                resetMediaInputs()
                mediaPrefillApplied = false
            }
            currentMediaCommand = null
        }

        fun detachRandomizeSection() {
            (randomizeSection.parent as? ViewGroup)?.removeView(randomizeSection)
            randomizeSection.visibility = View.GONE
            randomizeToggle.visibility = View.GONE
            randomizeHelperText.visibility = View.GONE
        }

        fun attachRandomizeSectionFor(command: String) {
            val base = baseCommandName(command)
            if (!randomizableCommandMap.containsKey(base)) {
                detachRandomizeSection()
                isProgrammaticRandomizeChange = true
                randomizeToggle.isChecked = false
                isProgrammaticRandomizeChange = false
                randomizeToggle.contentDescription = getString(R.string.cd_randomize_toggle_off)
                return
            }
            detachRandomizeSection()
            if (!isEdit) {
                mediaPrefillApplied = false
            }
            val helperRes = if (base == "SCALE") R.string.hint_randomize_scale else R.string.hint_randomize_inputfields
            randomizeHelperText.text = getString(helperRes)
            TooltipCompat.setTooltipText(randomizeToggle, getString(helperRes))
            val isRandom = isRandomizedCommandName(command)
            isProgrammaticRandomizeChange = true
            randomizeToggle.isChecked = isRandom
            isProgrammaticRandomizeChange = false
            val cdRes = if (isRandom) R.string.cd_randomize_toggle_on else R.string.cd_randomize_toggle_off
            randomizeToggle.contentDescription = getString(cdRes)
            randomizeSection.visibility = View.VISIBLE
            randomizeToggle.visibility = View.VISIBLE
            randomizeHelperText.visibility = View.VISIBLE
            paramGroup.addView(randomizeSection)
        }

        fun updateHoldToggleVisibility(
            command: String,
            continueIncluded: Boolean,
        ) {
            val supportsHold = continueIncluded && holdSupportedCommands.contains(command)
            if (supportsHold) {
                holdToggle.visibility = View.VISIBLE
                holdHelperText.visibility = View.VISIBLE
                if (!isEdit && lastHoldCommand != command) {
                    holdToggle.isChecked = false
                }
                lastHoldCommand = command
            } else {
                holdToggle.visibility = View.GONE
                holdHelperText.visibility = View.GONE
                if (!isEdit) {
                    holdToggle.isChecked = false
                }
                if (!holdSupportedCommands.contains(command)) {
                    lastHoldCommand = null
                }
            }
        }

        fun infoLabel(message: String, topMarginDp: Int = 16): TextView =
            TextView(ctx).apply {
                text = message
                setTextColor(Color.parseColor("#6B7280"))
                textSize = applyScale(13f)
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(topMarginDp)
                    }
            }

        fun refreshParams() {
            if (selectedCommand.isBlank()) {
                paramGroup.removeAllViews()
                paramGroup.addView(infoLabel(getString(R.string.insert_command_choose_command_hint)))
                updateHoldToggleVisibility("", continueIncluded = false)
                detachMediaSection(reset = true)
                mediaPrefillApplied = false
                return
            }
            paramGroup.removeAllViews()
            detachRandomizeSection()
            var continueIncluded = false
            var shouldAttachMedia = false
            when (selectedCommand) {
                "INSTRUCTION" -> {
                    paramGroup.addView(header)
                    paramGroup.addView(body)
                    paramGroup.addView(continueSection)
                    continueIncluded = true
                    shouldAttachMedia = true
                }
                "TIMER" -> {
                    paramGroup.addView(header)
                    paramGroup.addView(body)
                    paramGroup.addView(time)
                    paramGroup.addView(continueSection)
                    continueIncluded = true
                    shouldAttachMedia = true
                }
                "SCALE", "SCALE[RANDOMIZED]" -> {
                    paramGroup.addView(header)
                    paramGroup.addView(body)
                    attachRandomizeSectionFor(selectedCommand)
                    rebuildScaleItemFields(emptyList())
                    rebuildScaleResponseFields(emptyList())
                    paramGroup.addView(scaleItemsSection)
                    paramGroup.addView(scaleResponsesSection)
                    shouldAttachMedia = true
                }
                "INPUTFIELD", "INPUTFIELD[RANDOMIZED]" -> {
                    paramGroup.addView(header)
                    paramGroup.addView(body)
                    attachRandomizeSectionFor(selectedCommand)
                    rebuildInputFieldEntries(emptyList())
                    paramGroup.addView(inputFieldSection)
                    paramGroup.addView(continueSection)
                    continueIncluded = true
                    shouldAttachMedia = true
                }
                "LABEL" -> {
                    paramGroup.addView(labelName)
                }
                "GOTO" -> {
                    paramGroup.addView(gotoLabel)
                }
                "HTML" -> {
                    paramGroup.addView(filename)
                    paramGroup.addView(continueSection)
                    continueIncluded = true
                }
                "TIMER_SOUND" -> {
                    paramGroup.addView(filename)
                }
                "LOG" -> {
                    paramGroup.addView(message)
                }
                // Toggles & commands without params
                "RANDOMIZE_ON", "RANDOMIZE_OFF", "END" -> { /* no params */ }
                // Single-value meta
                "STUDY_ID" -> {
                    paramGroup.addView(studyIdValue)
                }
                "TRANSITIONS" -> {
                    paramGroup.addView(transitionsSpinner)
                }
                // Colors
                "HEADER_COLOR",
                "BODY_COLOR",
                "RESPONSE_TEXT_COLOR",
                "RESPONSE_BACKGROUND_COLOR",
                "SCREEN_BACKGROUND_COLOR",
                "CONTINUE_TEXT_COLOR",
                "CONTINUE_BACKGROUND_COLOR",
                "TIMER_COLOR",
                -> {
                    paramGroup.addView(colorValue)
                }
                // Sizes
                "HEADER_SIZE",
                "BODY_SIZE",
                "ITEM_SIZE",
                "RESPONSE_SIZE",
                "CONTINUE_SIZE",
                "TIMER_SIZE",
                -> {
                    paramGroup.addView(sizeValue)
                }
                // Alignments
                "HEADER_ALIGNMENT",
                "BODY_ALIGNMENT",
                "CONTINUE_ALIGNMENT",
                "TIMER_ALIGNMENT",
                -> {
                    paramGroup.addView(alignmentSpinner)
                }
                else -> { /* Unhandled commands fall through */ }
            }

            if (shouldAttachMedia) {
                val resetMedia = !isEdit && currentMediaCommand != selectedCommand
                attachMediaSectionFor(selectedCommand, resetIfChanged = resetMedia)
            } else {
                detachMediaSection(reset = true)
            }

            updateHoldToggleVisibility(selectedCommand, continueIncluded)

            if (paramGroup.childCount == 0) {
                paramGroup.addView(infoLabel(getString(R.string.insert_command_no_parameters_hint)))
            }
        }
        refreshParams()

        if (!isEdit) {
            commandDropdown.setOnItemClickListener { _, _, position, _ ->
                val meta = filtered.getOrNull(position)
                selectedCommand = meta?.name ?: ""
                if (meta != null) {
                    val desiredIndex = categories.indexOf(meta.category).takeIf { it >= 0 } ?: 0
                    if (categorySpinner.selectedItemPosition != desiredIndex) {
                        categorySpinner.setSelection(desiredIndex)
                    } else {
                        refreshParams()
                    }
                } else {
                    refreshParams()
                }
            }
            searchBox.addTextChangedListener(
                object : android.text.TextWatcher {
                    override fun afterTextChanged(s: android.text.Editable?) {}

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int,
                    ) {}

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int,
                    ) {
                        applyFilter()
                        refreshParams()
                    }
                },
            )
            categorySpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long,
                    ) {
                        applyFilter()
                        refreshParams()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
        }

        // Prefill if editing existing line
        if (isEdit && existingParts != null) {
            val cmd = existingParts.firstOrNull()?.trim().orEmpty()
            if (cmd.isNotEmpty()) {
                selectedCommand = cmd
                refreshParams()
                when (cmd) {
                    "INSTRUCTION" -> {
                        header.setText(existingParts.getOrNull(1))
                        val originalBody = existingParts.getOrNull(2).orEmpty()
                        val (bodyWithoutMedia, mediaPrefill) = extractMediaPlaceholders(originalBody)
                        body.setText(bodyWithoutMedia)
                        if (!mediaPrefillApplied) {
                            populateMediaInputs(mediaPrefill)
                            if (mediaPrefill.sound.isNotEmpty() || mediaPrefill.video.isNotEmpty() || mediaPrefill.customHtml.isNotEmpty()) {
                                mediaPrefillApplied = true
                            }
                        }
                        val (continueText, hadHold) = stripHoldToken(existingParts.getOrNull(3))
                        cont.setText(continueText)
                        if (holdSupportedCommands.contains(cmd)) {
                            holdToggle.isChecked = hadHold
                            lastHoldCommand = cmd
                        }
                    }
                    "TIMER" -> {
                        header.setText(existingParts.getOrNull(1))
                        val originalBody = existingParts.getOrNull(2).orEmpty()
                        val (bodyWithoutMedia, mediaPrefill) = extractMediaPlaceholders(originalBody)
                        body.setText(bodyWithoutMedia)
                        if (!mediaPrefillApplied) {
                            populateMediaInputs(mediaPrefill)
                            if (mediaPrefill.sound.isNotEmpty() || mediaPrefill.video.isNotEmpty() || mediaPrefill.customHtml.isNotEmpty()) {
                                mediaPrefillApplied = true
                            }
                        }
                        time.setText(existingParts.getOrNull(3))
                        val (continueText, hadHold) = stripHoldToken(existingParts.getOrNull(4))
                        cont.setText(continueText)
                        if (holdSupportedCommands.contains(cmd)) {
                            holdToggle.isChecked = hadHold
                            lastHoldCommand = cmd
                        }
                    }
                    "SCALE", "SCALE[RANDOMIZED]" -> {
                        header.setText(existingParts.getOrNull(1))
                        val originalBody = existingParts.getOrNull(2).orEmpty()
                        val (bodyWithoutMedia, mediaPrefill) = extractMediaPlaceholders(originalBody)
                        body.setText(bodyWithoutMedia)
                        if (!mediaPrefillApplied) {
                            populateMediaInputs(mediaPrefill)
                            if (mediaPrefill.sound.isNotEmpty() || mediaPrefill.video.isNotEmpty() || mediaPrefill.customHtml.isNotEmpty()) {
                                mediaPrefillApplied = true
                            }
                        }
                        val rawItems = existingParts.getOrNull(3).orEmpty()
                        val normalizedItems =
                            if (rawItems.startsWith("[") && rawItems.endsWith("]") && rawItems.length >= 2) {
                                rawItems.substring(1, rawItems.length - 1)
                            } else {
                                rawItems
                            }
                        val itemValues =
                            normalizedItems
                                .split(';')
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                        rebuildScaleItemFields(itemValues)
                        val responses =
                            existingParts.drop(4).map { it.trim() }.filter { it.isNotEmpty() }
                        rebuildScaleResponseFields(responses)
                    }
                    "INPUTFIELD", "INPUTFIELD[RANDOMIZED]" -> {
                        header.setText(existingParts.getOrNull(1))
                        val originalBody = existingParts.getOrNull(2).orEmpty()
                        val (bodyWithoutMedia, mediaPrefill) = extractMediaPlaceholders(originalBody)
                        body.setText(bodyWithoutMedia)
                        if (!mediaPrefillApplied) {
                            populateMediaInputs(mediaPrefill)
                            if (mediaPrefill.sound.isNotEmpty() || mediaPrefill.video.isNotEmpty() || mediaPrefill.customHtml.isNotEmpty()) {
                                mediaPrefillApplied = true
                            }
                        }
                        val fieldSlice =
                            when {
                                existingParts.size > 4 -> existingParts.subList(3, existingParts.size - 1)
                                existingParts.size >= 4 -> listOfNotNull(existingParts.getOrNull(3))
                                else -> emptyList()
                            }
                                .map { it.trim() }
                        rebuildInputFieldEntries(fieldSlice)
                        val (continueText, hadHold) = stripHoldToken(existingParts.lastOrNull())
                        cont.setText(continueText)
                        if (holdSupportedCommands.contains(cmd)) {
                            holdToggle.isChecked = hadHold
                            lastHoldCommand = cmd
                        }
                    }
                    "LABEL" -> {
                        labelName.setText(existingParts.getOrNull(1))
                    }
                    "GOTO" -> {
                        gotoLabel.setText(existingParts.getOrNull(1))
                    }
                    "HTML", "TIMER_SOUND" -> {
                        filename.setText(existingParts.getOrNull(1))
                        if (cmd == "HTML") {
                            val (continueText, hadHold) = stripHoldToken(existingParts.getOrNull(2))
                            cont.setText(continueText)
                            if (holdSupportedCommands.contains(cmd)) {
                                holdToggle.isChecked = hadHold
                                lastHoldCommand = cmd
                            }
                        }
                    }
                    "LOG" -> {
                        message.setText(existingParts.getOrNull(1))
                    }
                    // Single value commands
                    "STUDY_ID" -> {
                        studyIdValue.setText(existingParts.getOrNull(1))
                    }
                    "TRANSITIONS" -> {
                        val mode = existingParts.getOrNull(1)?.lowercase()
                        val idx = arrayOf("off", "slide", "slideleft", "fade", "dissolve").indexOf(mode)
                        if (idx >= 0) transitionsSpinner.setSelection(idx)
                    }
                    // Colors
                    "HEADER_COLOR",
                    "BODY_COLOR",
                    "RESPONSE_TEXT_COLOR",
                    "RESPONSE_BACKGROUND_COLOR",
                    "SCREEN_BACKGROUND_COLOR",
                    "CONTINUE_TEXT_COLOR",
                    "CONTINUE_BACKGROUND_COLOR",
                    "TIMER_COLOR",
                    -> {
                        colorValue.setText(existingParts.getOrNull(1))
                    }
                    // Sizes
                    "HEADER_SIZE",
                    "BODY_SIZE",
                    "ITEM_SIZE",
                    "RESPONSE_SIZE",
                    "CONTINUE_SIZE",
                    "TIMER_SIZE",
                    -> {
                        sizeValue.setText(existingParts.getOrNull(1))
                    }
                    // Alignments
                    "HEADER_ALIGNMENT",
                    "BODY_ALIGNMENT",
                    "CONTINUE_ALIGNMENT",
                    "TIMER_ALIGNMENT",
                    -> {
                        val valUpper = existingParts.getOrNull(1)?.uppercase()
                        val idx = arrayOf("LEFT", "CENTER", "RIGHT").indexOf(valUpper)
                        if (idx >= 0) alignmentSpinner.setSelection(idx)
                    }
                }
            }
        }
        val builder =
            AlertDialog.Builder(ctx)
                .setTitle(getString(R.string.dialog_title_insert_command))
                .setView(container)
                .setPositiveButton(if (isEdit) R.string.action_update_command else R.string.action_add_command) { _, _ ->
                    val cmd = selectedCommand.takeIf { it.isNotBlank() } ?: run {
                        Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    fun def(
                        et: EditText,
                        fallback: String,
                    ) = et.text.toString().takeIf { it.isNotBlank() } ?: fallback
                    val soundValue = normalizedMediaValue(soundInput)
                    val videoValue = normalizedMediaValue(videoInput)
                    val customHtmlValue = normalizedMediaValue(customHtmlInput)
                    val mediaValuesProvided =
                        soundValue.isNotEmpty() || videoValue.isNotEmpty() || customHtmlValue.isNotEmpty()
                    val commandSupportsMedia = commandsWithMedia.contains(cmd)
                    if (!commandSupportsMedia && mediaValuesProvided) {
                        Toast.makeText(ctx, getString(R.string.error_media_not_supported), Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (soundValue.isNotEmpty() && !hasAllowedExtension(soundValue, setOf("mp3", "wav"))) {
                        Toast.makeText(ctx, getString(R.string.error_media_sound_extension), Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (videoValue.isNotEmpty() && !hasAllowedExtension(videoValue, setOf("mp4"))) {
                        Toast.makeText(ctx, getString(R.string.error_media_video_extension), Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (customHtmlValue.isNotEmpty() && !hasAllowedExtension(customHtmlValue, setOf("html"))) {
                        Toast.makeText(ctx, getString(R.string.error_media_html_extension), Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    // Contextual validations
                    if (cmd == "STUDY_ID" && !isEdit) {
                        if (allLines.any { it.trim().uppercase().startsWith("STUDY_ID;") }) {
                            Toast.makeText(ctx, getString(R.string.error_duplicate_study_id), Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                    }
                    if (cmd == "RANDOMIZE_OFF") {
                        // Ensure a RANDOMIZE_ON appears above (and not already fully closed without open block)
                        var open = 0
                        for (line in allLines) {
                            val t = line.trim().uppercase()
                            if (t == "RANDOMIZE_ON") {
                                open++
                            } else if (t == "RANDOMIZE_OFF" && open > 0) {
                                open--
                            }
                        }
                        if (open <= 0) {
                            Toast.makeText(ctx, getString(R.string.error_randomize_off_without_on), Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                    }
                    if (cmd == "RANDOMIZE_ON") {
                        // disallow nested (any currently open block forbids new ON)
                        var open = 0
                        for (line in allLines) {
                            val t = line.trim().uppercase()
                            if (t == "RANDOMIZE_ON") {
                                open++
                            } else if (t == "RANDOMIZE_OFF" && open > 0) {
                                open--
                            }
                        }
                        if (open > 0) {
                            Toast.makeText(ctx, getString(R.string.error_randomize_on_nested), Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                    }
                    val newLine =
                        when (cmd) {
                            "INSTRUCTION" -> {
                                val headerText = def(header, "Header")
                                val rawBody = body.text.toString()
                                val baseBody =
                                    if (rawBody.isNotBlank()) {
                                        rawBody
                                    } else if (commandSupportsMedia && mediaValuesProvided) {
                                        ""
                                    } else {
                                        "Body"
                                    }
                                val bodyText =
                                    appendMediaPlaceholders(
                                        baseBody,
                                        commandSupportsMedia,
                                        soundValue,
                                        videoValue,
                                        customHtmlValue,
                                    ).ifBlank { "Body" }
                                val continueText = applyHoldToken(def(cont, "Continue"))
                                "$cmd;$headerText;$bodyText;$continueText"
                            }
                            "TIMER" -> {
                                val headerText = def(header, "Header")
                                val rawBody = body.text.toString()
                                val baseBody =
                                    if (rawBody.isNotBlank()) {
                                        rawBody
                                    } else if (commandSupportsMedia && mediaValuesProvided) {
                                        ""
                                    } else {
                                        "Body"
                                    }
                                val bodyText =
                                    appendMediaPlaceholders(
                                        baseBody,
                                        commandSupportsMedia,
                                        soundValue,
                                        videoValue,
                                        customHtmlValue,
                                    ).ifBlank { "Body" }
                                val continueText = applyHoldToken(def(cont, "Continue"))
                                "$cmd;$headerText;$bodyText;${def(time, "60")};$continueText"
                            }
                            "SCALE", "SCALE[RANDOMIZED]" -> {
                                val headerText = def(header, "Header")
                                val rawBody = body.text.toString()
                                val baseBody =
                                    if (rawBody.isNotBlank()) {
                                        rawBody
                                    } else if (commandSupportsMedia && mediaValuesProvided) {
                                        ""
                                    } else {
                                        "Body"
                                    }
                                val bodyText =
                                    appendMediaPlaceholders(
                                        baseBody,
                                        commandSupportsMedia,
                                        soundValue,
                                        videoValue,
                                        customHtmlValue,
                                    ).ifBlank { "Body" }
                                val itemTokens =
                                    collectScaleItems()
                                        .ifEmpty { listOf("Item 1") }
                                val itemsPart =
                                    if (itemTokens.size > 1) {
                                        "[" + itemTokens.joinToString(";") + "]"
                                    } else {
                                        itemTokens.first()
                                    }

                                val responseTokens = collectScaleResponses()
                                val responsesPart =
                                    if (responseTokens.isNotEmpty()) {
                                        responseTokens.joinToString(";")
                                    } else {
                                        "Response 1;Response 2"
                                    }

                                "$cmd;$headerText;$bodyText;$itemsPart;$responsesPart"
                            }
                            "INPUTFIELD", "INPUTFIELD[RANDOMIZED]" -> {
                                val headerText = def(header, "Header")
                                val rawBody = body.text.toString()
                                val baseBody =
                                    if (rawBody.isNotBlank()) {
                                        rawBody
                                    } else if (commandSupportsMedia && mediaValuesProvided) {
                                        ""
                                    } else {
                                        "Body"
                                    }
                                val bodyText =
                                    appendMediaPlaceholders(
                                        baseBody,
                                        commandSupportsMedia,
                                        soundValue,
                                        videoValue,
                                        customHtmlValue,
                                    ).ifBlank { "Body" }
                                val fieldTokens =
                                    collectInputFieldEntries()
                                        .ifEmpty { listOf("field1", "field2") }
                                val fieldsPart = fieldTokens.joinToString(";")
                                val continueText = applyHoldToken(def(cont, "Continue"))
                                "$cmd;$headerText;$bodyText;$fieldsPart;$continueText"
                            }
                            "LABEL" -> {
                                if (labelName.text.isBlank()) {
                                    Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }
                                "$cmd;${labelName.text}"
                            }
                            "GOTO" -> {
                                if (gotoLabel.text.isBlank()) {
                                    Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }
                                "$cmd;${gotoLabel.text}"
                            }
                            "HTML" -> {
                                if (filename.text.isBlank()) {
                                    Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }
                                val rawContinue = cont.text?.toString().orEmpty()
                                val sanitizedInput = holdStripRegex.replace(rawContinue, "").trim()
                                val shouldIncludeContinue = holdToggle.isChecked || sanitizedInput.isNotEmpty()
                                val baseContinue =
                                    when {
                                        sanitizedInput.isNotEmpty() -> sanitizedInput
                                        holdToggle.isChecked -> "Continue"
                                        else -> ""
                                    }
                                if (shouldIncludeContinue && baseContinue.isNotEmpty()) {
                                    val continueText = applyHoldToken(baseContinue)
                                    "$cmd;${filename.text};$continueText"
                                } else {
                                    "$cmd;${filename.text}"
                                }
                            }
                            "TIMER_SOUND" -> {
                                if (filename.text.isBlank()) {
                                    Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }
                                "$cmd;${filename.text}"
                            }
                            "LOG" -> "$cmd;${def(message,"Log message")}"
                            "RANDOMIZE_ON", "RANDOMIZE_OFF" -> cmd
                            "STUDY_ID" -> {
                                if (studyIdValue.text.isBlank()) {
                                    Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }
                                "$cmd;${studyIdValue.text}"
                            }
                            "TRANSITIONS" -> {
                                val mode = transitionsSpinner.selectedItem?.toString()?.trim().orEmpty()
                                if (mode.isEmpty()) {
                                    Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }
                                "$cmd;$mode"
                            }
                            // Colors
                            "HEADER_COLOR",
                            "BODY_COLOR",
                            "RESPONSE_TEXT_COLOR",
                            "RESPONSE_BACKGROUND_COLOR",
                            "SCREEN_BACKGROUND_COLOR",
                            "CONTINUE_TEXT_COLOR",
                            "CONTINUE_BACKGROUND_COLOR",
                            "TIMER_COLOR",
                            -> {
                                val cv = colorValue.text.toString().trim()
                                if (cv.isBlank()) {
                                    Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }
                                // quick validation
                                val ok =
                                    try {
                                        android.graphics.Color.parseColor(cv)
                                        true
                                    } catch (e: Exception) {
                                        false
                                    }
                                if (!ok) {
                                    Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }
                                "$cmd;$cv"
                            }
                            // Sizes
                            "HEADER_SIZE",
                            "BODY_SIZE",
                            "ITEM_SIZE",
                            "RESPONSE_SIZE",
                            "CONTINUE_SIZE",
                            "TIMER_SIZE",
                            -> {
                                val sv = sizeValue.text.toString().trim()
                                if (sv.isBlank()) {
                                    Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }
                                if (sv.toIntOrNull() == null) {
                                    Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }
                                "$cmd;$sv"
                            }
                            // Alignments
                            "HEADER_ALIGNMENT", "BODY_ALIGNMENT", "CONTINUE_ALIGNMENT", "TIMER_ALIGNMENT" -> {
                                val av = alignmentSpinner.selectedItem?.toString()?.uppercase()?.trim().orEmpty()
                                if (av.isBlank()) {
                                    Toast.makeText(ctx, getString(R.string.error_required_field), Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }
                                "$cmd;$av"
                            }
                            "END" -> "END"
                            else -> return@setPositiveButton Toast.makeText(
                                ctx,
                                getString(R.string.error_required_field),
                                Toast.LENGTH_SHORT,
                            ).show()
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
                    pendingAutoScrollToFirstIssue = true
                    revalidateAndRefreshUI()
                }
                .setNegativeButton(
                    android.R.string.cancel,
                ) { _, _ -> Toast.makeText(ctx, getString(R.string.toast_insert_cancelled), Toast.LENGTH_SHORT).show() }

        if (isEdit) {
            builder.setNeutralButton(R.string.action_delete_command) { _, _ ->
                if (editLineIndex != null && editLineIndex in allLines.indices) {
                    pushUndoState()
                    allLines.removeAt(editLineIndex)
                    hasUnsavedChanges = true
                    pendingAutoScrollToFirstIssue = true
                    revalidateAndRefreshUI()
                    Toast.makeText(ctx, getString(R.string.toast_command_deleted), Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.create().show()
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

    // Presents an unrecognized command in the same boxed layout used for recognized command editing
    private fun showUnrecognizedCommandDialog(lineIndex: Int) {
        if (lineIndex !in allLines.indices) return
        val ctx = requireContext()
        val original = allLines[lineIndex]

        val container = ScrollView(ctx)
        val inner =
            LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 24, 32, 8)
            }
        container.addView(inner)

        inner.addView(
            TextView(ctx).apply {
                text = getString(R.string.error_unrecognized_command_title)
                setTypeface(null, Typeface.BOLD)
                textSize = applyScale(16f)
            },
        )

        inner.addView(
            TextView(ctx).apply {
                text = getString(R.string.action_edit_raw_line)
                setPadding(0, 16, 0, 8)
                setTypeface(null, Typeface.BOLD)
                textSize = applyScale(14f)
            },
        )

        val editText =
            EditText(ctx).apply {
                setText(original)
                isSingleLine = false
                setPadding(16, 16, 16, 16)
            }
        inner.addView(editText)

        val dialog =
            AlertDialog.Builder(ctx)
                .setTitle(getString(R.string.error_unrecognized_command_title))
                .setView(container)
                .setPositiveButton(R.string.action_update_command) { _, _ ->
                    val newLine = editText.text.toString()
                    if (newLine != original) {
                        pushUndoState()
                        allLines[lineIndex] = newLine
                        hasUnsavedChanges = true
                        pendingAutoScrollToFirstIssue = true
                        Toast.makeText(ctx, getString(R.string.toast_command_updated), Toast.LENGTH_SHORT).show()
                    }
                    revalidateAndRefreshUI()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.action_delete_command) { _, _ ->
                    pushUndoState()
                    allLines.removeAt(lineIndex)
                    hasUnsavedChanges = true
                    pendingAutoScrollToFirstIssue = true
                    Toast.makeText(ctx, getString(R.string.toast_command_deleted), Toast.LENGTH_SHORT).show()
                    revalidateAndRefreshUI()
                }
                .create()
        dialog.show()
    }
}
