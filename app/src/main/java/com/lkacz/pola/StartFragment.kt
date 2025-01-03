// Filename: StartFragment.kt
package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
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
        savedInstanceState: Bundle?
    ): View {
        val scrollView = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
        }
        val rootLinearLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }
        scrollView.addView(rootLinearLayout)

        rootLinearLayout.addView(createHeaderSection())
        rootLinearLayout.addView(createProtocolInfoSection())
        rootLinearLayout.addView(createProtocolActionsSection())
        rootLinearLayout.addView(createResourceFolderButton())
        rootLinearLayout.addView(createCustomizationSection())
        rootLinearLayout.addView(createValidationButton())
        rootLinearLayout.addView(createAboutSection())

        return scrollView
    }

    private fun createHeaderSection(): View {
        val headerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val tvAppName = TextView(requireContext()).apply {
            text = "Pocket Lab App"
            textSize = 34f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 4)
        }

        val tvAppVersion = TextView(requireContext()).apply {
            text = "Ver 0.2"
            textSize = 12f
            setPadding(0, 0, 0, 24)
        }

        headerLayout.addView(tvAppName)
        headerLayout.addView(tvAppVersion)
        return headerLayout
    }

    private fun createProtocolInfoSection(): View {
        val infoLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val tvSelectedLabel = TextView(requireContext()).apply {
            text = "Selected:"
            textSize = 16f
            gravity = Gravity.CENTER
        }
        tvSelectedProtocolName = TextView(requireContext()).apply {
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }
        val currentFileName = protocolUri?.let {
            fileUriUtils.getFileName(requireContext(), it)
        } ?: "None"
        updateProtocolNameDisplay(currentFileName)

        infoLayout.addView(tvSelectedLabel)
        infoLayout.addView(tvSelectedProtocolName)
        return infoLayout
    }

    private fun createProtocolActionsSection(): View {
        val actionLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val btnShowProtocolContent = Button(requireContext()).apply {
            text = "Show Protocol Content"
            setOnClickListener { showProtocolContentDialog() }
            setPadding(0, 0, 0, 16)
        }
        actionLayout.addView(btnShowProtocolContent)

        val btnStart = Button(requireContext()).apply {
            text = "Start"
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setOnClickListener { showStartStudyConfirmation() }
            setPadding(0, 0, 0, 36)
        }
        actionLayout.addView(btnStart)

        val btnSelectFile = Button(requireContext()).apply {
            text = "Select your protocol"
            setOnClickListener {
                showChangeProtocolConfirmation {
                    filePicker.launch(arrayOf("text/plain"))
                }
            }
            setPadding(0, 0, 0, 8)
        }
        actionLayout.addView(btnSelectFile)

        val btnUseDemo = Button(requireContext()).apply {
            text = "Load DEMO protocol"
            setOnClickListener { handleProtocolChange("demo", "Demo Protocol") }
            setPadding(0, 0, 0, 8)
        }
        actionLayout.addView(btnUseDemo)

        val btnUseTutorial = Button(requireContext()).apply {
            text = "Load Tutorial Protocol"
            setOnClickListener { handleProtocolChange("tutorial", "Tutorial Protocol") }
            setPadding(0, 0, 0, 16)
        }
        actionLayout.addView(btnUseTutorial)

        return actionLayout
    }

    private fun createResourceFolderButton(): View {
        val btnSelectResourcesFolder = Button(requireContext()).apply {
            text = "Select Resources Folder"
            setOnClickListener {
                showChangeResourcesFolderConfirmation {
                    resourcesFolderManager.pickResourcesFolder(folderPicker)
                }
            }
            setPadding(0, 0, 0, 32)
        }
        return btnSelectResourcesFolder
    }

    private fun createCustomizationSection(): View {
        val customizationLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val tvCustomizationLabel = TextView(requireContext()).apply {
            text = "-- Customization --"
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }
        customizationLayout.addView(tvCustomizationLabel)

        val btnColorsAndSizes = Button(requireContext()).apply {
            text = "Colors and sizes"
            setOnClickListener {
                AppearanceCustomizationDialog().show(parentFragmentManager, "AppearanceCustomizationDialog")
            }
            setPadding(0, 0, 0, 8)
        }
        customizationLayout.addView(btnColorsAndSizes)

        val btnAlarm = Button(requireContext()).apply {
            text = "Alarm"
            setOnClickListener {
                AlarmCustomizationDialog().show(parentFragmentManager, "AlarmCustomizationDialog")
            }
            setPadding(0, 0, 0, 36)
        }
        customizationLayout.addView(btnAlarm)

        return customizationLayout
    }

    private fun createValidationButton(): View {
        val btnProtocolValidation = Button(requireContext()).apply {
            text = "Protocol Validation"
            setOnClickListener {
                ProtocolValidationDialog().show(parentFragmentManager, "ProtocolValidationDialog")
            }
            setPadding(0, 0, 0, 36)
        }
        return btnProtocolValidation
    }

    private fun createAboutSection(): View {
        val btnShowAbout = Button(requireContext()).apply {
            text = "Show About"
            setOnClickListener { showAboutContentDialog() }
            setPadding(0, 0, 0, 16)
        }
        return btnShowAbout
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

    private fun handleProtocolChange(mode: String, protocolName: String) {
        showChangeProtocolConfirmation {
            protocolUri = null
            sharedPref.edit()
                .remove("PROTOCOL_URI")
                .putString("CURRENT_MODE", mode)
                .apply()
            updateProtocolNameDisplay(protocolName)
        }
    }

    private fun showProtocolContentDialog() {
        val protocolName = tvSelectedProtocolName.text.toString()
        val fileContent = when (protocolName) {
            "Demo Protocol" ->
                protocolReader.readFromAssets(requireContext(), "demo_protocol.txt")
            "Tutorial Protocol" ->
                protocolReader.readFromAssets(requireContext(), "tutorial_protocol.txt")
            else ->
                protocolUri?.let {
                    protocolReader.readFileContent(requireContext(), it)
                } ?: "File content not available"
        }
        ProtocolContentDisplayer(requireContext()).showProtocolContent(protocolName, fileContent)
    }

    private fun showAboutContentDialog() {
        val aboutHtmlContent = protocolReader.readFromAssets(requireContext(), "about.txt")
        ProtocolContentDisplayer(requireContext()).showHtmlContent("About", aboutHtmlContent)
    }

    private fun showStartStudyConfirmation() {
        confirmationDialogManager.showStartStudyConfirmation(
            protocolUri,
            { uri -> fileUriUtils.getFileName(requireContext(), uri) }
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
            onConfirm
        )
    }

    private fun updateProtocolNameDisplay(protocolName: String) {
        tvSelectedProtocolName.text = protocolName
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
