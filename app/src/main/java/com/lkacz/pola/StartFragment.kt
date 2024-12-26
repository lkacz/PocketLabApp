package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class StartFragment : Fragment() {

    private lateinit var listener: OnProtocolSelectedListener
    private lateinit var tvSelectedProtocolName: TextView
    private var protocolUri: Uri? = null
    private lateinit var sharedPref: SharedPreferences
    private val fileUriUtils = FileUriUtils()
    private val protocolReader by lazy { ProtocolReader() }
    private lateinit var themeManager: ThemeManager
    private val confirmationDialogManager by lazy { ConfirmationDialogManager(requireContext()) }

    // Newly added MediaFolderManager to handle folder URI storage
    private lateinit var mediaFolderManager: MediaFolderManager

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { handleFileUri(it) } ?: showToast("File selection was cancelled")
        }

    /**
     * Folder picker for selecting a media directory, not a single file.
     * We use [ActivityResultContracts.OpenDocumentTree] to allow the user
     * to select an entire folder.
     */
    private val folderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                mediaFolderManager.storeMediaFolderUri(uri)
                showToast("Media folder set: ${uri.lastPathSegment ?: uri.toString()}")
            } else {
                showToast("Folder selection was cancelled")
            }
        }

    interface OnProtocolSelectedListener {
        fun onProtocolSelected(protocolUri: Uri?)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as OnProtocolSelectedListener
        sharedPref = context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        protocolUri = sharedPref.getString("PROTOCOL_URI", null)?.let(Uri::parse)
        themeManager = ThemeManager(context)
        mediaFolderManager = MediaFolderManager(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_start, container, false)
        tvSelectedProtocolName = view.findViewById(R.id.tvSelectedProtocolName)
        updateProtocolNameDisplay(protocolUri?.let { fileUriUtils.getFileName(requireContext(), it) } ?: "None")
        setupButtons(view)
        return view
    }

    override fun onResume() {
        super.onResume()
        themeManager.applyTheme()
    }

    private fun setupButtons(view: View) {
        view.findViewById<Button>(R.id.btnStart).setOnClickListener { showStartStudyConfirmation() }
        view.findViewById<Button>(R.id.btnSelectFile).setOnClickListener {
            showChangeProtocolConfirmation {
                filePicker.launch(arrayOf("text/plain"))
            }
        }
        view.findViewById<Button>(R.id.btnUseDemo).setOnClickListener {
            handleProtocolChange("demo", "Demo Protocol")
        }
        view.findViewById<Button>(R.id.btnUseTutorial).setOnClickListener {
            handleProtocolChange("tutorial", "Tutorial Protocol")
        }
        view.findViewById<Button>(R.id.btnToggleTheme).setOnClickListener {
            themeManager.toggleTheme()
            activity?.recreate()
        }
        view.findViewById<Button>(R.id.btnShowProtocolContent).setOnClickListener {
            showProtocolContentDialog()
        }
        view.findViewById<Button>(R.id.btnShowAbout).setOnClickListener {
            showAboutContentDialog()
        }

        // Newly added button to pick the media folder
        view.findViewById<Button>(R.id.btnSelectMediaFolder).setOnClickListener {
            showChangeMediaFolderConfirmation {
                mediaFolderManager.pickMediaFolder(folderPicker)
            }
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
            else -> protocolUri?.let { protocolReader.readFileContent(requireContext(), it) }
                ?: "File content not available"
        }
        ProtocolContentDisplayer(requireContext()).showProtocolContent(protocolName, fileContent)
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

    /**
     * For changing the media folder with a confirmation prompt.
     */
    private fun showChangeMediaFolderConfirmation(onConfirm: () -> Unit) {
        ConfirmationDialogManager(requireContext())
            .apply {
                context?.let {
                    AlertDialogBuilderUtils.showConfirmation(
                        it,
                        title = "Confirm Media Folder",
                        message = "Are you sure you want to change the media folder?",
                        onConfirm = onConfirm
                    )
                }
            }
    }

    private fun showAboutContentDialog() {
        val aboutHtmlContent = protocolReader.readFromAssets(requireContext(), "about.txt")
        ProtocolContentDisplayer(requireContext()).showHtmlContent("About", aboutHtmlContent)
    }
}
