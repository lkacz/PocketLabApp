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
            setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24))
        }
        scrollView.addView(rootLinearLayout)

        val titleSection = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_HORIZONTAL
        }
        rootLinearLayout.addView(titleSection)

        val tvAppName = TextView(requireContext()).apply {
            text = "Pocket Labb App"
            textSize = 34f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        titleSection.addView(tvAppName)

        val tvAppVersion = TextView(requireContext()).apply {
            text = "Version 0.5.0"
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(4), 0, dpToPx(8))
        }
        titleSection.addView(tvAppVersion)

        rootLinearLayout.addView(createDivider())

        val protocolDisplaySection = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_HORIZONTAL
        }
        rootLinearLayout.addView(protocolDisplaySection)

        val tvSelectedLabel = TextView(requireContext()).apply {
            text = "Current Protocol:"
            textSize = 16f
            gravity = Gravity.CENTER
        }
        protocolDisplaySection.addView(tvSelectedLabel)

        tvSelectedProtocolName = TextView(requireContext()).apply {
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(4), 0, dpToPx(8))
        }
        val currentFileName = protocolUri?.let {
            fileUriUtils.getFileName(requireContext(), it)
        } ?: "None"
        updateProtocolNameDisplay(currentFileName)
        protocolDisplaySection.addView(tvSelectedProtocolName)

        rootLinearLayout.addView(createDivider())

        val mainActionsLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootLinearLayout.addView(mainActionsLayout)

        val btnStart = createMenuButton("START") {
            showStartStudyConfirmation()
        }.apply {
            textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dpToPx(8), 0, dpToPx(24))
        }
        mainActionsLayout.addView(btnStart)

        mainActionsLayout.addView(
            createMenuButton("Load protocol file") {
                showChangeProtocolConfirmation {
                    filePicker.launch(arrayOf("text/plain"))
                }
            }
        )

        mainActionsLayout.addView(
            createMenuButton("Review Protocol") {
                ProtocolValidationDialog().show(parentFragmentManager, "ProtocolValidationDialog")
            }
        )

        mainActionsLayout.addView(
            createMenuButton("Load Tutorial Protocol") {
                handleProtocolChange("tutorial", "Tutorial Protocol")
            }
        )

        mainActionsLayout.addView(
            createMenuButton("Select Resources Folder") {
                showChangeResourcesFolderConfirmation {
                    resourcesFolderManager.pickResourcesFolder(folderPicker)
                }
            }
        )

        rootLinearLayout.addView(createDivider())

        val customizationLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootLinearLayout.addView(customizationLayout)

        val tvCustomizationLabel = TextView(requireContext()).apply {
            text = "Customization"
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dpToPx(8))
        }
        customizationLayout.addView(tvCustomizationLabel)

        customizationLayout.addView(
            createMenuButton("Layout") {
                AppearanceCustomizationDialog().show(parentFragmentManager, "AppearanceCustomizationDialog")
            }
        )

        customizationLayout.addView(
            createMenuButton("Sounds") {
                AlarmCustomizationDialog().show(parentFragmentManager, "AlarmCustomizationDialog")
            }
        )

        rootLinearLayout.addView(createDivider())

        val btnAbout = createMenuButton("About") {
            showAboutContentDialog()
        }
        rootLinearLayout.addView(btnAbout)

        return scrollView
    }

    private fun createMenuButton(textValue: String, onClick: () -> Unit): Button {
        return Button(requireContext()).apply {
            text = textValue
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8)
            }
        }
    }

    private fun createDivider(): View {
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
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

    private fun showAboutContentDialog() {
        val aboutHtmlContent = protocolReader.readFromAssets(requireContext(), "about.txt")
        ProtocolContentDisplayer(requireContext()).showHtmlContent("About", aboutHtmlContent)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }
}
