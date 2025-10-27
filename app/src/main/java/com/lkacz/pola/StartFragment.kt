package com.lkacz.pola

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class StartFragment : Fragment() {
    interface OnProtocolSelectedListener {
        fun onProtocolSelected(protocolUri: Uri?)
    }

    private lateinit var listener: OnProtocolSelectedListener
    private lateinit var tvSelectedProtocolName: TextView
    private lateinit var participantIdInput: TextInputEditText
    private var tvSelectedOutputFolder: TextView? = null
    private var protocolUri: Uri? = null
    private var currentProtocolName: String? = null
    private lateinit var sharedPref: SharedPreferences
    private var outputFolderUri: Uri? = null
    private var devTapCount = 0
    private val devTapThreshold = 7
    private var devButtonsAdded = false
    private val fileUriUtils = FileUriUtils()
    private val protocolReader by lazy { ProtocolReader() }
    private val confirmationDialogManager by lazy { ConfirmationDialogManager(requireContext()) }
    private lateinit var resourcesFolderManager: ResourcesFolderManager
    private val tutorialResourcesSetup by lazy { TutorialResourcesSetup(requireContext()) }

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { handleFileUri(it) } ?: showToast("File selection was cancelled")
        }

    private val folderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                resourcesFolderManager.storeResourcesFolderUri(uri)
                showToast("Resources folder set: ${uri.lastPathSegment ?: uri.toString()}")
            } else {
                showToast("Folder selection was cancelled")
            }
        }

    private val outputFolderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                handleOutputFolderSelection(uri)
            } else {
                showToast(getString(R.string.toast_output_folder_cancelled))
            }
        }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                showToast("Storage permissions granted")
            } else {
                showToast("Storage permissions denied - file operations may fail")
            }
        }

    private val tutorialResourcesFolderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                handleTutorialResourcesSetup(uri)
            } else {
                showToast("Tutorial setup cancelled - resources folder required")
            }
        }

    private val tutorialOutputFolderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                handleTutorialOutputSetup(uri)
            } else {
                showToast("Tutorial setup cancelled - output folder required")
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnProtocolSelectedListener
            ?: throw IllegalStateException("Host activity must implement OnProtocolSelectedListener")
        sharedPref = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        protocolUri = sharedPref.getString(Prefs.KEY_PROTOCOL_URI, null)?.let(Uri::parse)
        currentProtocolName = sharedPref.getString(Prefs.KEY_CURRENT_PROTOCOL_NAME, null)
        outputFolderUri = sharedPref.getString(Prefs.KEY_OUTPUT_FOLDER_URI, null)?.let(Uri::parse)
        resourcesFolderManager = ResourcesFolderManager(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val scrollView =
            ScrollView(requireContext()).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                isFillViewport = true
            }

        val rootLayout =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(32))
            }
        scrollView.addView(rootLayout)

        val titleSection =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                gravity = Gravity.CENTER_HORIZONTAL
            }
        rootLayout.addView(titleSection)

        val tvAppName =
            TextView(requireContext()).apply {
                text = getString(R.string.app_name)
                textSize = 32f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                setOnClickListener { handleDeveloperTap(rootLayout) }
                setOnLongClickListener { handleDeveloperLongPress(rootLayout) }
            }
        titleSection.addView(tvAppName)

        rootLayout.addView(createDivider())

        val studySection =
            sectionCard(null) { section ->
                val protocolRow =
                    LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                            )
                        setPadding(0, 0, 0, dpToPx(8))
                    }
                protocolRow.addView(
                    TextView(requireContext()).apply {
                        text = getString(R.string.label_protocol_prefix)
                        textSize = 16f
                        setTypeface(typeface, Typeface.BOLD)
                        layoutParams =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                            )
                    },
                )

                tvSelectedProtocolName =
                    TextView(requireContext()).apply {
                        textSize = 16f
                        setTypeface(typeface, Typeface.BOLD)
                        setPadding(dpToPx(4), 0, 0, 0)
                        layoutParams =
                            LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f,
                            )
                    }
                val currentFileName =
                    currentProtocolName?.takeIf { it.isNotBlank() }
                        ?: protocolUri?.let { fileUriUtils.getFileName(requireContext(), it) }
                updateProtocolNameDisplay(currentFileName)
                protocolRow.addView(tvSelectedProtocolName)
                section.addView(protocolRow)

                val participantIdLayout =
                    TextInputLayout(requireContext()).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                            ).apply {
                                bottomMargin = dpToPx(12)
                            }
                        hint = getString(R.string.label_participant_id)
                        endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
                        endIconContentDescription = getString(R.string.cd_clear_participant_id)
                    }
                participantIdInput =
                    TextInputEditText(participantIdLayout.context).apply {
                        maxLines = 1
                        setText(sharedPref.getString(Prefs.KEY_PARTICIPANT_ID, "")?.trim().orEmpty())
                        doAfterTextChanged { editable ->
                            persistParticipantId(editable?.toString())
                        }
                    }
                participantIdLayout.addView(participantIdInput)
                section.addView(participantIdLayout)

                section.addView(
                    createPrimaryButton(
                        text = getString(R.string.action_start_study),
                        icon = R.drawable.ic_play,
                    ) {
                        showStartStudyConfirmation()
                    },
                )
            }
        rootLayout.addView(studySection)

        rootLayout.addView(createDivider())

        val protocolsSection =
            sectionCard(getString(R.string.section_protocols)) { section ->
                section.addView(
                    createSecondaryButton(
                        text = getString(R.string.action_load_protocol),
                        icon = R.drawable.ic_folder_open,
                    ) {
                        showChangeProtocolConfirmation {
                            filePicker.launch(arrayOf("text/plain"))
                        }
                    },
                )
                section.addView(
                    createSecondaryButton(
                        text = getString(R.string.action_review_protocol),
                        icon = R.drawable.ic_review,
                    ) {
                        ProtocolValidationDialog().show(parentFragmentManager, "ProtocolValidationDialog")
                    },
                )
                section.addView(
                    createTonalButton(
                        text = getString(R.string.action_use_tutorial_protocol),
                        icon = R.drawable.ic_tutorial,
                    ) {
                        showChangeProtocolConfirmation {
                            val assetUriString = "file:///android_asset/tutorial_protocol.txt"
                            val tutorialName = getString(R.string.protocol_name_tutorial)
                            sharedPref
                                .edit()
                                .putString(Prefs.KEY_PROTOCOL_URI, assetUriString)
                                .putString(Prefs.KEY_CURRENT_MODE, "tutorial")
                                .putString(Prefs.KEY_CURRENT_PROTOCOL_NAME, tutorialName)
                                .apply()
                            clearStoredProgress()
                            protocolUri = Uri.parse(assetUriString)
                            updateProtocolNameDisplay(tutorialName)
                        }
                    },
                )
                section.addView(
                    createTonalButton(
                        text = getString(R.string.action_use_demo_protocol),
                        icon = R.drawable.ic_tutorial,
                    ) {
                        showChangeProtocolConfirmation {
                            val assetUriString = "file:///android_asset/demo_protocol.txt"
                            val demoName = getString(R.string.protocol_name_demo)
                            sharedPref
                                .edit()
                                .putString(Prefs.KEY_PROTOCOL_URI, assetUriString)
                                .putString(Prefs.KEY_CURRENT_MODE, "demo")
                                .putString(Prefs.KEY_CURRENT_PROTOCOL_NAME, demoName)
                                .apply()
                            clearStoredProgress()
                            protocolUri = Uri.parse(assetUriString)
                            updateProtocolNameDisplay(demoName)
                        }
                    },
                )
            }
        rootLayout.addView(protocolsSection)

        rootLayout.addView(createDivider())

        val fileSection =
            sectionCard(getString(R.string.section_file)) { section ->
                section.addView(
                    createSecondaryButton(
                        text = getString(R.string.action_select_output_folder),
                        icon = R.drawable.ic_folder_open,
                    ) {
                        showChangeOutputFolderConfirmation {
                            outputFolderPicker.launch(outputFolderUri)
                        }
                    },
                )
                section.addView(
                    createSecondaryButton(
                        text = getString(R.string.action_select_resources_folder),
                        icon = R.drawable.ic_folder_open,
                    ) {
                        showChangeResourcesFolderConfirmation {
                            resourcesFolderManager.pickResourcesFolder(folderPicker)
                        }
                    },
                )
            }
        rootLayout.addView(fileSection)

        rootLayout.addView(createDivider())

        val customizationSection =
            sectionCard(getString(R.string.section_customization)) { section ->
                section.addView(
                    createSecondaryButton(getString(R.string.action_layout)) {
                        AppearanceCustomizationDialog().show(parentFragmentManager, "AppearanceCustomizationDialog")
                    },
                )
                section.addView(
                    createSecondaryButton(getString(R.string.action_sounds)) {
                        AlarmCustomizationDialog().show(parentFragmentManager, "AlarmCustomizationDialog")
                    },
                )
            }
        rootLayout.addView(customizationSection)

        rootLayout.addView(createDivider())

        val aboutSection =
            sectionCard(getString(R.string.section_other)) { section ->
                section.addView(
                    createSecondaryButton(getString(R.string.action_about)) {
                        val aboutMarkdown = protocolReader.readFromAssets(requireContext(), "about.md")
                        val aboutHtmlContent = MarkdownRenderer.render(aboutMarkdown)
                        HtmlDialogHelper.showHtmlContent(
                            requireContext(),
                            getString(R.string.action_about),
                            aboutHtmlContent,
                        )
                    },
                )
                section.addView(
                    createSecondaryButton(getString(R.string.action_manual)) {
                        val manualMarkdown = protocolReader.readFromAssets(requireContext(), "manual.md")
                        val manualHtmlContent = MarkdownRenderer.render(manualMarkdown)
                        HtmlDialogHelper.showHtmlContent(
                            requireContext(),
                            getString(R.string.action_manual),
                            manualHtmlContent,
                        )
                    },
                )
            }
        rootLayout.addView(aboutSection)

        FeatureFlags.load(requireContext())
        if (DeveloperModeManager.isEnabled(requireContext())) addDeveloperButtons(rootLayout)

        // Request storage permissions if needed
        checkAndRequestStoragePermissions()

        return scrollView
    }

    override fun onResume() {
        super.onResume()
        protocolUri = sharedPref.getString(Prefs.KEY_PROTOCOL_URI, null)?.let(Uri::parse)
        currentProtocolName = sharedPref.getString(Prefs.KEY_CURRENT_PROTOCOL_NAME, currentProtocolName)
        outputFolderUri = sharedPref.getString(Prefs.KEY_OUTPUT_FOLDER_URI, null)?.let(Uri::parse)
        val resolvedName =
            currentProtocolName?.takeIf { it.isNotBlank() }
                ?: protocolUri?.let { fileUriUtils.getFileName(requireContext(), it) }
        updateProtocolNameDisplay(resolvedName)
        updateOutputFolderDisplay(outputFolderUri)
        if (::participantIdInput.isInitialized) {
            val storedValue = sharedPref.getString(Prefs.KEY_PARTICIPANT_ID, "")?.trim().orEmpty()
            if (participantIdInput.text?.toString()?.trim() != storedValue) {
                participantIdInput.setText(storedValue)
                participantIdInput.setSelection(storedValue.length)
            }
        }
    }

    private fun sectionCard(
        title: String?,
        buildContent: (LinearLayout) -> Unit,
    ): View {
        val card =
            MaterialCardView(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        bottomMargin = dpToPx(16)
                    }
                strokeWidth = dpToPx(1)
                radius = dpToPx(12).toFloat()
                setContentPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            }
        val inner =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }
        if (!title.isNullOrBlank()) {
            inner.addView(
                TextView(requireContext()).apply {
                    text = title
                    textSize = 14f
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, 0, 0, dpToPx(8))
                },
            )
        }
        buildContent(inner)
        card.addView(inner)
        return card
    }

    private fun handleDeveloperTap(rootLayout: LinearLayout) {
        if (DeveloperModeManager.isEnabled(requireContext())) return
        devTapCount++
        val tapsRemaining = devTapThreshold - devTapCount
        if (tapsRemaining in 1..5) {
            showToast("$tapsRemaining more taps to enable developer mode")
        }
        if (devTapCount >= devTapThreshold) {
            DeveloperModeManager.enable(requireContext())
            showToast("Developer mode enabled")
            addDeveloperButtons(rootLayout)
            devTapCount = 0
        }
    }

    private fun handleDeveloperLongPress(rootLayout: LinearLayout): Boolean {
        if (!DeveloperModeManager.isEnabled(requireContext())) return false
        DeveloperModeManager.disable(requireContext())
        showToast("Developer mode disabled")
        removeDeveloperButtons(rootLayout)
        devTapCount = 0
        return true
    }

    private fun baseButton(
        textValue: String,
        defStyleAttr: Int,
        onClick: () -> Unit,
        iconRes: Int? = null,
    ): MaterialButton {
        val button =
            MaterialButton(requireContext(), null, defStyleAttr).apply {
                text = textValue
                if (iconRes != null) {
                    icon = ContextCompat.getDrawable(requireContext(), iconRes)
                    iconPadding = dpToPx(8)
                    iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
                }
            }
        button.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dpToPx(8)
            }
        button.setOnClickListener { onClick() }
        return button
    }

    private fun createPrimaryButton(
        text: String,
        icon: Int? = null,
        onClick: () -> Unit,
    ): MaterialButton =
        baseButton(
            textValue = text,
            defStyleAttr = com.google.android.material.R.attr.materialButtonStyle,
            onClick = onClick,
            iconRes = icon,
        ).apply {
            isAllCaps = false
        }

    private fun createSecondaryButton(
        text: String,
        icon: Int? = null,
        onClick: () -> Unit,
    ): MaterialButton =
        baseButton(
            textValue = text,
            defStyleAttr = com.google.android.material.R.attr.materialButtonOutlinedStyle,
            onClick = onClick,
            iconRes = icon,
        ).apply {
            isAllCaps = false
        }

    private fun createTonalButton(
        text: String,
        icon: Int? = null,
        onClick: () -> Unit,
    ): MaterialButton =
        baseButton(
            textValue = text,
            defStyleAttr = com.google.android.material.R.attr.materialButtonOutlinedStyle,
            onClick = onClick,
            iconRes = icon,
        ).apply {
            isAllCaps = false
        }

    private fun createDivider(): View =
        View(requireContext()).apply {
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(1),
                ).also {
                    it.topMargin = dpToPx(8)
                    it.bottomMargin = dpToPx(8)
                }
            setBackgroundColor(android.graphics.Color.LTGRAY)
        }

    private fun handleFileUri(uri: Uri) {
        context?.let { ctx ->
            val fileName = fileUriUtils.getFileName(ctx, uri)
            if (fileName.endsWith(".txt")) {
                fileUriUtils.handleFileUri(ctx, uri, sharedPref)
                sharedPref
                    .edit()
                    .putString(Prefs.KEY_CURRENT_PROTOCOL_NAME, fileName)
                    .putString(Prefs.KEY_CURRENT_MODE, "custom")
                    .apply()
                clearStoredProgress()
                protocolUri = uri
                updateProtocolNameDisplay(fileName)
            } else {
                showToast("Select a .txt file for the protocol")
            }
        }
    }

    private fun ensureProtocolSelection() {
        if (protocolUri != null) return
        val rawMode = sharedPref.getString(Prefs.KEY_CURRENT_MODE, "demo") ?: "demo"
        val resolvedMode = if (rawMode == "tutorial") "tutorial" else "demo"
        val assetFileName =
            if (resolvedMode == "tutorial") {
                "tutorial_protocol.txt"
            } else {
                "demo_protocol.txt"
            }
        val assetUriString = "file:///android_asset/$assetFileName"
        val displayName =
            if (resolvedMode == "tutorial") {
                getString(R.string.protocol_name_tutorial)
            } else {
                getString(R.string.protocol_name_demo)
            }
        sharedPref
            .edit()
            .putString(Prefs.KEY_PROTOCOL_URI, assetUriString)
            .putString(Prefs.KEY_CURRENT_MODE, resolvedMode)
            .putString(Prefs.KEY_CURRENT_PROTOCOL_NAME, displayName)
            .apply()
        protocolUri = Uri.parse(assetUriString)
        updateProtocolNameDisplay(displayName)
    }

    private fun showStartStudyConfirmation() {
        // Check if we're starting tutorial/demo mode without resources folder set up
        val currentMode = sharedPref.getString(Prefs.KEY_CURRENT_MODE, null)
        val isTutorialOrDemo = currentMode == "tutorial" || currentMode == "demo"
        val resourcesFolderUri = resourcesFolderManager.getResourcesFolderUri()
        
        if (isTutorialOrDemo && !tutorialResourcesSetup.areResourcesSetup(resourcesFolderUri)) {
            // Show dialog explaining we need to set up resources folder
            AlertDialogBuilderUtils.showConfirmation(
                requireContext(),
                "Set up Tutorial Resources",
                "The tutorial protocol includes media files (audio, video, images). " +
                    "Please select a folder where these resources will be copied for easy access.\n\n" +
                    "This is a one-time setup that will help you learn how to manage protocol resources.",
                {
                    tutorialResourcesFolderPicker.launch(null)
                },
            )
            return
        }
        
        // Normal start study flow
        confirmationDialogManager.showStartStudyConfirmation(
            protocolUri,
            { uri -> fileUriUtils.getFileName(requireContext(), uri) },
        ) {
            val participantId = participantIdInput.text?.toString()?.trim().orEmpty()
            persistParticipantId(participantId)
            Logger.getInstance(requireContext()).updateParticipantId(participantId.takeIf { it.isNotBlank() })
            ensureProtocolSelection()
            listener.onProtocolSelected(protocolUri)
        }
    }

    private fun handleTutorialResourcesSetup(uri: Uri) {
        // Store the resources folder URI
        resourcesFolderManager.storeResourcesFolderUri(uri)
        
        // Copy assets to the folder
        val success = tutorialResourcesSetup.copyAssetsToResourcesFolder(uri)
        
        if (success) {
            showToast("Tutorial resources copied successfully!")
            
            // Now check if output folder is set up
            if (outputFolderUri == null) {
                AlertDialogBuilderUtils.showConfirmation(
                    requireContext(),
                    "Set up Output Folder",
                    "Now select a folder where protocol logs and data will be saved.\n\n" +
                        "This helps you learn how to collect and export study results.",
                    {
                        tutorialOutputFolderPicker.launch(null)
                    },
                )
            } else {
                // Both folders set up, proceed with study
                proceedWithStartStudy()
            }
        } else {
            showToast("Failed to copy tutorial resources. Please try again.")
        }
    }

    private fun handleTutorialOutputSetup(uri: Uri) {
        handleOutputFolderSelection(uri)
        showToast("Output folder set up successfully!")
        
        // Now proceed with starting the study
        proceedWithStartStudy()
    }

    private fun proceedWithStartStudy() {
        confirmationDialogManager.showStartStudyConfirmation(
            protocolUri,
            { uri -> fileUriUtils.getFileName(requireContext(), uri) },
        ) {
            val participantId = participantIdInput.text?.toString()?.trim().orEmpty()
            persistParticipantId(participantId)
            Logger.getInstance(requireContext()).updateParticipantId(participantId.takeIf { it.isNotBlank() })
            ensureProtocolSelection()
            listener.onProtocolSelected(protocolUri)
        }
    }

    private fun showChangeProtocolConfirmation(onConfirm: () -> Unit) {
        confirmationDialogManager.showChangeProtocolConfirmation(onConfirm)
    }

    private fun showChangeResourcesFolderConfirmation(onConfirm: () -> Unit) {
        AlertDialogBuilderUtils.showConfirmation(
            requireContext(),
            "Confirm Resources Folder",
            "Are you sure you want to change the resources folder?",
            onConfirm,
        )
    }

    private fun showChangeOutputFolderConfirmation(onConfirm: () -> Unit) {
        AlertDialogBuilderUtils.showConfirmation(
            requireContext(),
            getString(R.string.dialog_title_output_folder),
            getString(R.string.dialog_message_output_folder),
            onConfirm,
        )
    }

    private fun updateProtocolNameDisplay(protocolName: String?) {
        currentProtocolName = protocolName
        val displayName = protocolName?.takeIf { it.isNotBlank() } ?: getString(R.string.value_none)
        tvSelectedProtocolName.text = displayName
    }

    private fun persistParticipantId(participantId: String?) {
        val trimmed = participantId?.trim().orEmpty()
        with(sharedPref.edit()) {
            if (trimmed.isEmpty()) {
                remove(Prefs.KEY_PARTICIPANT_ID)
            } else {
                putString(Prefs.KEY_PARTICIPANT_ID, trimmed)
            }
            apply()
        }
    }

    private fun handleOutputFolderSelection(uri: Uri) {
        val resolver = requireContext().contentResolver
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            resolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            showToast(getString(R.string.toast_output_folder_permission_denied))
            return
        }
        outputFolderUri?.takeIf { it != uri }?.let {
            try {
                resolver.releasePersistableUriPermission(it, flags)
            } catch (_: SecurityException) {
                // Ignore if permission was already revoked
            } catch (_: IllegalArgumentException) {
                // Ignore if permission cannot be released
            }
        }
        outputFolderUri = uri
        sharedPref
            .edit()
            .putString(Prefs.KEY_OUTPUT_FOLDER_URI, uri.toString())
            .apply()
        updateOutputFolderDisplay(uri)
        Logger.getInstance(requireContext()).updateOutputFolderUri(uri)
        showToast(getString(R.string.toast_output_folder_set))
    }

    private fun updateOutputFolderDisplay(uri: Uri?) {
        val textView = tvSelectedOutputFolder ?: return
        val displayName =
            uri?.let {
                DocumentFile.fromTreeUri(requireContext(), it)?.name
                    ?: it.lastPathSegment
            } ?: getString(R.string.value_none)
        textView.text = displayName
    }

    private fun clearStoredProgress() {
        sharedPref
            .edit()
            .remove(Prefs.KEY_PROTOCOL_PROGRESS_INDEX)
            .putBoolean(Prefs.KEY_PROTOCOL_IN_PROGRESS, false)
            .apply()
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun checkAndRequestStoragePermissions() {
        // For Android 13+ (API 33+), we use scoped storage and don't need these permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return
        }

        // For Android 10-12, check if permissions are needed
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            storagePermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun addDeveloperButtons(rootLayout: LinearLayout) {
        if (devButtonsAdded) return
        devButtonsAdded = true
        if (FeatureFlags.newFeatureOne) {
            rootLayout.addView(
                createSecondaryButton("Dev Info") {
                    DevInfoDialog().show(parentFragmentManager, "DevInfoDialog")
                }.apply {
                    tag = DEV_BUTTON_TAG
                },
            )
        }
        if (DeveloperModeManager.isEnabled(requireContext())) {
            rootLayout.addView(
                createSecondaryButton("Feature Flags") {
                    FeatureFlagsDialog().show(parentFragmentManager, "FeatureFlagsDialog")
                }.apply {
                    tag = DEV_BUTTON_TAG
                },
            )
        }
    }

    private fun removeDeveloperButtons(rootLayout: LinearLayout) {
        val toRemove =
            (0 until rootLayout.childCount)
                .map(rootLayout::getChildAt)
                .filter { it.tag == DEV_BUTTON_TAG }
        toRemove.forEach(rootLayout::removeView)
        devButtonsAdded = false
    }

    private companion object {
        const val DEV_BUTTON_TAG = "DEV_BTN"
    }
}
