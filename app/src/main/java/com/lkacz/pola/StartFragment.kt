// Filename: StartFragment.kt
package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class StartFragment : Fragment() {
    interface OnProtocolSelectedListener {
        fun onProtocolSelected(protocolUri: Uri?)
    }

    private lateinit var listener: OnProtocolSelectedListener
    private lateinit var tvSelectedProtocolName: TextView
    private var protocolUri: Uri? = null
    private lateinit var sharedPref: SharedPreferences
    private var devTapCount = 0
    private var devButtonsAdded = false
    private val fileUriUtils = FileUriUtils()
    private val protocolReader by lazy { ProtocolReader() }
    private val confirmationDialogManager by lazy { ConfirmationDialogManager(requireContext()) }
    private lateinit var resourcesFolderManager: ResourcesFolderManager

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { handleFileUri(it) }
                ?: showToast("File selection was cancelled")
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as OnProtocolSelectedListener
        sharedPref = context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        protocolUri = sharedPref.getString("PROTOCOL_URI", null)?.let(Uri::parse)
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

        val rootLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
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

        val tvAppName = TextView(requireContext()).apply {
            text = "Pocket Lab App"
            textSize = 32f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        titleSection.addView(tvAppName)

        val tvAppVersion =
            TextView(requireContext()).apply {
                text = "(v${BuildConfig.APP_VERSION})"
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(4), 0, dpToPx(8))
                // Secret tap to enable developer mode (7 taps)
                setOnClickListener {
                    if (!DeveloperModeManager.isEnabled(requireContext())) {
                        devTapCount++
                        val tapsRemaining = 7 - devTapCount
                        if (tapsRemaining in 1..5) {
                            showToast("$tapsRemaining more taps to enable developer mode")
                        }
                        if (devTapCount >= 7) {
                            DeveloperModeManager.enable(requireContext())
                            showToast("Developer mode enabled")
                            addDeveloperButtons(rootLayout)
                        }
                    }
                }
                // Long press disables
                setOnLongClickListener {
                    if (DeveloperModeManager.isEnabled(requireContext())) {
                        DeveloperModeManager.disable(requireContext())
                        showToast("Developer mode disabled")
                        removeDeveloperButtons(rootLayout)
                        devTapCount = 0
                    }
                    true
                }
            }
        titleSection.addView(tvAppVersion)

        rootLayout.addView(createDivider())

        // Helper to build a Material card section with vertical linear layout content
    fun sectionCard(title: String?, buildContent: (LinearLayout) -> Unit): View {
            val card = MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(16) }
                strokeWidth = dpToPx(1)
                radius = dpToPx(12).toFloat()
                setContentPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            }
            val inner = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            if (!title.isNullOrBlank()) {
                inner.addView(TextView(requireContext()).apply {
                    text = title
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0,0,0,dpToPx(8))
                })
            }
            buildContent(inner)
            card.addView(inner)
            return card
        }

        val protocolSection = sectionCard("CURRENT PROTOCOL") { section ->
            val tvCurrentProtocolLabel = TextView(requireContext()).apply {
                text = "Selected file"
                textSize = 12f
                setPadding(0,0,0,dpToPx(4))
            }
            section.addView(tvCurrentProtocolLabel)

        tvSelectedProtocolName =
            TextView(requireContext()).apply {
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(4), 0, dpToPx(8))
            }
        val currentFileName =
            protocolUri?.let {
                fileUriUtils.getFileName(requireContext(), it)
            } ?: "None"
        updateProtocolNameDisplay(currentFileName)
            section.addView(tvSelectedProtocolName)

            section.addView(createPrimaryButton(text = "Start Study", icon = R.drawable.ic_play, onClick = { showStartStudyConfirmation() }))
        }
        rootLayout.addView(protocolSection)
        val fileOpsSection = sectionCard("PROTOCOL & FILES") { section ->
            section.addView(createSecondaryButton(text = "Load Protocol File", icon = R.drawable.ic_folder_open, onClick = {
                showChangeProtocolConfirmation { filePicker.launch(arrayOf("text/plain")) }
            }))
            section.addView(createSecondaryButton(text = "Select Resources Folder", icon = R.drawable.ic_folder_open, onClick = {
                showChangeResourcesFolderConfirmation { resourcesFolderManager.pickResourcesFolder(folderPicker) }
            }))
            section.addView(createSecondaryButton(text = "Review Protocol", icon = R.drawable.ic_review, onClick = {
                ProtocolValidationDialog().show(parentFragmentManager, "ProtocolValidationDialog")
            }))
            section.addView(createSecondaryButton(text = "Preview Protocol", icon = R.drawable.ic_preview, onClick = {
                ProtocolPreviewDialog.newInstance().show(parentFragmentManager, "ProtocolPreviewDialog")
            }))
            section.addView(createTonalButton(text = "Use Tutorial Protocol", icon = R.drawable.ic_tutorial, onClick = {
                showChangeProtocolConfirmation {
                    val assetUriString = "file:///android_asset/tutorial_protocol.txt"
                    sharedPref.edit().putString("PROTOCOL_URI", assetUriString).putString("CURRENT_MODE", "tutorial").apply()
                    protocolUri = Uri.parse(assetUriString)
                    updateProtocolNameDisplay("Tutorial Protocol")
                }
            }))
            section.addView(createTonalButton(text = "Use Demo Protocol", icon = R.drawable.ic_tutorial, onClick = {
                showChangeProtocolConfirmation {
                    val assetUriString = "file:///android_asset/demo_protocol.txt"
                    sharedPref.edit().putString("PROTOCOL_URI", assetUriString).putString("CURRENT_MODE", "demo").apply()
                    protocolUri = Uri.parse(assetUriString)
                    updateProtocolNameDisplay("Demo Protocol")
                }
            }))
        }
        rootLayout.addView(fileOpsSection)

        val customizationSection = sectionCard("CUSTOMIZATION") { section ->
            section.addView(createSecondaryButton("Layout") {
                AppearanceCustomizationDialog().show(parentFragmentManager, "AppearanceCustomizationDialog")
            })
            section.addView(createSecondaryButton("Sounds") {
                AlarmCustomizationDialog().show(parentFragmentManager, "AlarmCustomizationDialog")
            })
        }
        rootLayout.addView(customizationSection)

        val spacerView =
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(32),
                    )
            }
        rootLayout.addView(spacerView)

        val aboutSection = sectionCard(null) { section ->
            section.addView(createSecondaryButton("About") {
                val aboutHtmlContent = protocolReader.readFromAssets(requireContext(), "about.txt")
                HtmlDialogHelper.showHtmlContent(requireContext(), "About", aboutHtmlContent)
            })
        }
        rootLayout.addView(aboutSection)

    // Load flags; only show dev items if developer mode or debug build
    FeatureFlags.load(requireContext())
    val isDev = DeveloperModeManager.isEnabled(requireContext())
    if (isDev) addDeveloperButtons(rootLayout)

        return scrollView
    }

    private fun baseButton(textValue: String, defStyleAttr: Int, onClick: () -> Unit, iconRes: Int? = null): MaterialButton {
        val btn = MaterialButton(requireContext(), null, defStyleAttr).apply {
            text = textValue
            if (iconRes != null) {
                icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), iconRes)
                iconPadding = dpToPx(8)
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            }
        }
        btn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dpToPx(8) }
        btn.setOnClickListener { onClick() }
        return btn
    }
    private fun createPrimaryButton(text: String, icon: Int? = null, onClick: () -> Unit) =
        baseButton(text, com.google.android.material.R.attr.materialButtonStyle, onClick, icon).apply { isAllCaps = false }
    private fun createSecondaryButton(text: String, icon: Int? = null, onClick: () -> Unit) =
        baseButton(text, com.google.android.material.R.attr.materialButtonOutlinedStyle, onClick, icon).apply { isAllCaps = false }
    private fun createTonalButton(text: String, icon: Int? = null, onClick: () -> Unit) =
        baseButton(text, com.google.android.material.R.attr.materialButtonOutlinedStyle, onClick, icon).apply { isAllCaps = false }

    private fun createDivider(): View {
        return View(requireContext()).apply {
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
    }

    private fun handleFileUri(uri: Uri) {
        context?.let { ctx ->
            val fileName = fileUriUtils.getFileName(ctx, uri)
            if (fileName.endsWith(".txt")) {
                fileUriUtils.handleFileUri(ctx, uri, sharedPref)
                protocolUri = uri
                updateProtocolNameDisplay(fileName)
            } else {
                showToast("Select a .txt file for the protocol")
            }
        }
    }

    private fun showStartStudyConfirmation() {
        confirmationDialogManager.showStartStudyConfirmation(
            protocolUri,
            { uri -> fileUriUtils.getFileName(requireContext(), uri) },
        ) {
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

    private fun updateProtocolNameDisplay(protocolName: String) {
        tvSelectedProtocolName.text = protocolName
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun addDeveloperButtons(rootLayout: LinearLayout) {
        if (devButtonsAdded) return
        devButtonsAdded = true
        if (FeatureFlags.NEW_FEATURE_ONE) {
            rootLayout.addView(createSecondaryButton("Dev Info") { DevInfoDialog().show(parentFragmentManager, "DevInfoDialog") })
        }
        // Feature flags dialog only in debug or explicit developer mode
        if (DeveloperModeManager.isEnabled(requireContext())) {
            rootLayout.addView(createSecondaryButton("Feature Flags") { FeatureFlagsDialog().show(parentFragmentManager, "FeatureFlagsDialog") })
        }
    }
    private fun removeDeveloperButtons(rootLayout: LinearLayout) {
        val toRemove = mutableListOf<View>()
        for (i in 0 until rootLayout.childCount) {
            val v = rootLayout.getChildAt(i)
            if (v is Button) {
                val t = v.text?.toString() ?: ""
                if (t == "Dev Info" || t == "Feature Flags") {
                    toRemove.add(v)
                }
            }
        }
        toRemove.forEach { rootLayout.removeView(it) }
        devButtonsAdded = false
    }
}
