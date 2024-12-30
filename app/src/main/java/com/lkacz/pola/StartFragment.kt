// Filename: StartFragment.kt
package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
    private lateinit var mediaFolderManager: MediaFolderManager

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { handleFileUri(it) } ?: showToast("File selection was cancelled")
        }

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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_start, container, false)
        tvSelectedProtocolName = view.findViewById(R.id.tvSelectedProtocolName)

        val fileName = protocolUri?.let { fileUriUtils.getFileName(requireContext(), it) } ?: "None"
        updateProtocolNameDisplay(fileName)

        setupButtons(view)
        return view
    }

    private fun setupButtons(view: View) {
        // Show Protocol Content
        view.findViewById<Button>(R.id.btnShowProtocolContent).setOnClickListener {
            showProtocolContentDialog()
        }

        // Start the study
        view.findViewById<Button>(R.id.btnStart).setOnClickListener {
            showStartStudyConfirmation()
        }

        // Select protocol file
        view.findViewById<Button>(R.id.btnSelectFile).setOnClickListener {
            showChangeProtocolConfirmation {
                filePicker.launch(arrayOf("text/plain"))
            }
        }

        // Use Demo
        view.findViewById<Button>(R.id.btnUseDemo).setOnClickListener {
            handleProtocolChange("demo", "Demo Protocol")
        }

        // Use Tutorial
        view.findViewById<Button>(R.id.btnUseTutorial).setOnClickListener {
            handleProtocolChange("tutorial", "Tutorial Protocol")
        }

        // Select Media Folder (moved below tutorial)
        view.findViewById<Button>(R.id.btnSelectMediaFolder).setOnClickListener {
            showChangeMediaFolderConfirmation {
                mediaFolderManager.pickMediaFolder(folderPicker)
            }
        }

        // Customization > Colors and sizes
        view.findViewById<Button>(R.id.btnColorsAndSizes).setOnClickListener {
            AppearanceCustomizationDialog().show(parentFragmentManager, "AppearanceCustomizationDialog")
        }

        // Customization > Alarm
        view.findViewById<Button>(R.id.btnAlarm).setOnClickListener {
            AlarmCustomizationDialog().show(parentFragmentManager, "AlarmCustomizationDialog")
        }

        // Show About (moved to the bottom)
        view.findViewById<Button>(R.id.btnShowAbout).setOnClickListener {
            showAboutContentDialog()
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

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
}
