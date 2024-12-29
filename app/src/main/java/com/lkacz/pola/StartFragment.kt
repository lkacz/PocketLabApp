package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

/**
 * Updated StartFragment:
 * 1) Includes a PocketLabLogoView at the top for the new animated "logo."
 * 2) Existing logic for protocol selection remains intact.
 */
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
    private lateinit var mediaFolderManager: MediaFolderManager

    // Dropdown for protocol selection
    private lateinit var protocolMenuDropdown: ProtocolMenuDropdown

    // File Picker
    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { handleFileUri(it) } ?: showToast("File selection was cancelled")
        }

    // Folder Picker
    private val folderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                mediaFolderManager.storeMediaFolderUri(uri)
                showToast("Media folder set: ${uri.lastPathSegment ?: uri.toString()}")
            } else {
                showToast("Folder selection was cancelled")
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as OnProtocolSelectedListener
        sharedPref = context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        protocolUri = sharedPref.getString("PROTOCOL_URI", null)?.let(Uri::parse)
        mediaFolderManager = MediaFolderManager(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_start, container, false)

        // 1) Add the PocketLabLogoView at runtime near the top
        val containerLayout = view.findViewById<android.widget.LinearLayout>(R.id.linearLayout)
            ?: view as? android.widget.LinearLayout
            ?: return view

        // Create our new custom "logo" view and add it to the layout
        val logoView = PocketLabLogoView(requireContext()).apply {
            // Optionally set layout params or any extra config
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also {
                it.bottomMargin = 32
            }
        }
        // Insert the logo at index 0 for top placement
        containerLayout.addView(logoView, 0)

        // 2) Setup existing UI elements
        tvSelectedProtocolName = view.findViewById(R.id.tvSelectedProtocolName)
        val fileName = protocolUri?.let { fileUriUtils.getFileName(requireContext(), it) } ?: "None"
        updateProtocolNameDisplay(fileName)

        // About icon
        val aboutIcon = view.findViewById<ImageView>(R.id.imgAboutIcon)
        aboutIcon.setOnClickListener { showAboutContentDialog() }

        // Protocol menu dropdown
        protocolMenuDropdown = view.findViewById(R.id.protocolMenuDropdown)
        protocolMenuDropdown.setup(
            onFileSelectionClick = {
                showChangeProtocolConfirmation {
                    filePicker.launch(arrayOf("text/plain"))
                }
            },
            onSelectDemoClick = {
                showChangeProtocolConfirmation {
                    handleProtocolChange("demo", "Demo Protocol")
                }
            },
            onSelectTutorialClick = {
                showChangeProtocolConfirmation {
                    handleProtocolChange("tutorial", "Tutorial Protocol")
                }
            },
            onSelectMediaFolderClick = {
                showChangeMediaFolderConfirmation {
                    mediaFolderManager.pickMediaFolder(folderPicker)
                }
            },
            onShowProtocolContentClick = {
                showProtocolContentDialog()
            }
        )

        // Start button
        val btnStart = view.findViewById<View>(R.id.btnStart)
        btnStart.setOnClickListener { showStartStudyConfirmation() }

        // Customize App button
        val btnCustomizeApp = view.findViewById<View>(R.id.btnCustomizeApp)
        btnCustomizeApp.setOnClickListener {
            AppearanceCustomizationDialog().show(
                requireActivity().supportFragmentManager,
                "AppearanceCustomizationDialog"
            )
        }

        return view
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
        protocolUri = null
        sharedPref.edit()
            .remove("PROTOCOL_URI")
            .putString("CURRENT_MODE", mode)
            .apply()
        updateProtocolNameDisplay(protocolName)
    }

    private fun updateProtocolNameDisplay(protocolName: String) {
        tvSelectedProtocolName.text = protocolName
    }

    private fun showProtocolContentDialog() {
        val protocolName = tvSelectedProtocolName.text.toString()
        val fileContent = when (protocolName) {
            "Demo Protocol" -> protocolReader.readFromAssets(requireContext(), "demo_protocol.txt")
            "Tutorial Protocol" -> protocolReader.readFromAssets(requireContext(), "tutorial_protocol.txt")
            else -> protocolUri?.let {
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

    private fun showChangeMediaFolderConfirmation(onConfirm: () -> Unit) {
        AlertDialogBuilderUtils.showConfirmation(
            requireContext(),
            title = "Confirm Media Folder",
            message = "Are you sure you want to change the media folder?",
            onConfirm = onConfirm
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
