package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.Uri
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
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class StartFragment : Fragment() {
    interface OnProtocolSelectedListener {
        fun onProtocolSelected(protocolUri: Uri?)
    }

    private lateinit var listener: OnProtocolSelectedListener
    private lateinit var tvSelectedProtocolName: TextView
    private var protocolUri: Uri? = null
    private lateinit var sharedPref: SharedPreferences
    private var devTapCount = 0
    private val devTapThreshold = 7
    private var devButtonsAdded = false
    private val fileUriUtils = FileUriUtils()
    private val protocolReader by lazy { ProtocolReader() }
    private val confirmationDialogManager by lazy { ConfirmationDialogManager(requireContext()) }
    private lateinit var resourcesFolderManager: ResourcesFolderManager

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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnProtocolSelectedListener
            ?: throw IllegalStateException("Host activity must implement OnProtocolSelectedListener")
        sharedPref = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        protocolUri = sharedPref.getString(Prefs.KEY_PROTOCOL_URI, null)?.let(Uri::parse)
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
            }
        titleSection.addView(tvAppName)

        val tvAppVersion =
            TextView(requireContext()).apply {
                text = "(v${BuildConfig.APP_VERSION})"
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(4), 0, dpToPx(8))
                setOnClickListener { handleDeveloperTap(rootLayout) }
                setOnLongClickListener { handleDeveloperLongPress(rootLayout) }
            }
        titleSection.addView(tvAppVersion)

        rootLayout.addView(createDivider())

        val protocolSection =
            sectionCard(getString(R.string.title_current_protocol)) { section ->
                val tvCurrentProtocolLabel =
                    TextView(requireContext()).apply {
                        text = getString(R.string.label_selected_file)
                        textSize = 12f
                        setPadding(0, 0, 0, dpToPx(4))
                    }
                section.addView(tvCurrentProtocolLabel)

                tvSelectedProtocolName =
                    TextView(requireContext()).apply {
                        textSize = 16f
                        setTypeface(typeface, Typeface.BOLD)
                        gravity = Gravity.CENTER
                        setPadding(0, dpToPx(4), 0, dpToPx(8))
                    }
                val currentFileName =
                    protocolUri?.let { fileUriUtils.getFileName(requireContext(), it) }
                        ?: getString(R.string.value_none)
                updateProtocolNameDisplay(currentFileName)
                section.addView(tvSelectedProtocolName)

                section.addView(
                    createPrimaryButton(
                        text = getString(R.string.action_start_study),
                        icon = R.drawable.ic_play,
                    ) {
                        showStartStudyConfirmation()
                    },
                )
            }
        rootLayout.addView(protocolSection)

        val fileOpsSection =
            sectionCard(getString(R.string.section_protocol_files)) { section ->
                section.addView(
                    createSecondaryButton(
                        text = getString(R.string.action_load_protocol),
                        icon = R.drawable.ic_folder_open,
                    ) {
                        showChangeProtocolConfirmation { filePicker.launch(arrayOf("text/plain")) }
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
                section.addView(
                    createSecondaryButton(
                        text = getString(R.string.action_review_protocol),
                        icon = R.drawable.ic_review,
                    ) {
                        ProtocolValidationDialog().show(parentFragmentManager, "ProtocolValidationDialog")
                    },
                )
                section.addView(
                    createSecondaryButton(
                        text = getString(R.string.action_preview_protocol),
                        icon = R.drawable.ic_preview,
                    ) {
                        ProtocolPreviewDialog.newInstance().show(parentFragmentManager, "ProtocolPreviewDialog")
                    },
                )
                section.addView(
                    createTonalButton(
                        text = getString(R.string.action_use_tutorial_protocol),
                        icon = R.drawable.ic_tutorial,
                    ) {
                        showChangeProtocolConfirmation {
                            val assetUriString = "file:///android_asset/tutorial_protocol.txt"
                            sharedPref
                                .edit()
                                .putString(Prefs.KEY_PROTOCOL_URI, assetUriString)
                                .putString(Prefs.KEY_CURRENT_MODE, "tutorial")
                                .apply()
                            protocolUri = Uri.parse(assetUriString)
                            updateProtocolNameDisplay("Tutorial Protocol")
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
                            sharedPref
                                .edit()
                                .putString(Prefs.KEY_PROTOCOL_URI, assetUriString)
                                .putString(Prefs.KEY_CURRENT_MODE, "demo")
                                .apply()
                            protocolUri = Uri.parse(assetUriString)
                            updateProtocolNameDisplay("Demo Protocol")
                        }
                    },
                )
            }
        rootLayout.addView(fileOpsSection)

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

        val spacerView =
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(32),
                    )
            }
        rootLayout.addView(spacerView)

        val aboutSection =
            sectionCard(null) { section ->
                section.addView(
                    createSecondaryButton("About") {
                        val aboutHtmlContent = protocolReader.readFromAssets(requireContext(), "about.txt")
                        HtmlDialogHelper.showHtmlContent(requireContext(), "About", aboutHtmlContent)
                    },
                )
            }
        rootLayout.addView(aboutSection)

        FeatureFlags.load(requireContext())
        if (DeveloperModeManager.isEnabled(requireContext())) addDeveloperButtons(rootLayout)

        return scrollView
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
