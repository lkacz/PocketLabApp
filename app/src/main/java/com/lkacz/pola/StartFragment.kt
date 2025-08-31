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
                setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24))
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
                text = "Pocket Labb App"
                textSize = 34f
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
            }
        titleSection.addView(tvAppVersion)

        rootLayout.addView(createDivider())

        val currentProtocolLayout =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                gravity = Gravity.CENTER_HORIZONTAL
            }
        rootLayout.addView(currentProtocolLayout)

        val tvCurrentProtocolLabel =
            TextView(requireContext()).apply {
                text = "Current Protocol:"
                textSize = 16f
                gravity = Gravity.CENTER
            }
        currentProtocolLayout.addView(tvCurrentProtocolLabel)

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
        currentProtocolLayout.addView(tvSelectedProtocolName)

        val btnStart =
            createMenuButton("START") {
                showStartStudyConfirmation()
            }.apply {
                textSize = 22f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dpToPx(8), 0, dpToPx(24))
            }
        rootLayout.addView(btnStart)

        val tvFilesHeading =
            TextView(requireContext()).apply {
                text = "FILES"
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(16), 0, dpToPx(8))
            }
        rootLayout.addView(tvFilesHeading)

        rootLayout.addView(
            createMenuButton("Load protocol file") {
                showChangeProtocolConfirmation {
                    filePicker.launch(arrayOf("text/plain"))
                }
            },
        )

        rootLayout.addView(
            createMenuButton("Select Resources Folder") {
                showChangeResourcesFolderConfirmation {
                    resourcesFolderManager.pickResourcesFolder(folderPicker)
                }
            },
        )

        rootLayout.addView(
            createMenuButton("Review Protocol") {
                ProtocolValidationDialog().show(parentFragmentManager, "ProtocolValidationDialog")
            },
        )

        rootLayout.addView(
            createMenuButton("Use Tutorial Protocol") {
                showChangeProtocolConfirmation {
                    val assetUriString = "file:///android_asset/tutorial_protocol.txt"
                    sharedPref.edit()
                        .putString("PROTOCOL_URI", assetUriString)
                        .putString("CURRENT_MODE", "tutorial")
                        .apply()
                    protocolUri = Uri.parse(assetUriString)
                    updateProtocolNameDisplay("Tutorial Protocol")
                }
            },
        )

        rootLayout.addView(
            createMenuButton("Use Demo Protocol") {
                showChangeProtocolConfirmation {
                    val assetUriString = "file:///android_asset/demo_protocol.txt"
                    sharedPref.edit()
                        .putString("PROTOCOL_URI", assetUriString)
                        .putString("CURRENT_MODE", "demo")
                        .apply()
                    protocolUri = Uri.parse(assetUriString)
                    updateProtocolNameDisplay("Demo Protocol")
                }
            },
        )

        val tvCustomizationHeading =
            TextView(requireContext()).apply {
                text = "CUSTOMIZATION"
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(16), 0, dpToPx(8))
            }
        rootLayout.addView(tvCustomizationHeading)

        rootLayout.addView(
            createMenuButton("Layout") {
                AppearanceCustomizationDialog().show(parentFragmentManager, "AppearanceCustomizationDialog")
            },
        )

        rootLayout.addView(
            createMenuButton("Sounds") {
                AlarmCustomizationDialog().show(parentFragmentManager, "AlarmCustomizationDialog")
            },
        )

        val spacerView =
            View(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(32),
                    )
            }
        rootLayout.addView(spacerView)

        val btnAbout =
            createMenuButton("About") {
                val aboutHtmlContent = protocolReader.readFromAssets(requireContext(), "about.txt")
                HtmlDialogHelper.showHtmlContent(requireContext(), "About", aboutHtmlContent)
            }
        rootLayout.addView(btnAbout)

        // Feature Flagged developer info panel
        if (FeatureFlags.NEW_FEATURE_ONE) {
            rootLayout.addView(
                createMenuButton("Dev Info") {
                    DevInfoDialog().show(parentFragmentManager, "DevInfoDialog")
                },
            )
        }

        if (FeatureFlags.NEW_FEATURE_TWO) {
            rootLayout.addView(
                createMenuButton("Preview Protocol") {
                    ProtocolPreviewDialog.newInstance().show(parentFragmentManager, "ProtocolPreviewDialog")
                },
            )
        }

        return scrollView
    }

    private fun createMenuButton(
        textValue: String,
        onClick: () -> Unit,
    ): Button {
        return Button(requireContext()).apply {
            text = textValue
            setOnClickListener { onClick() }
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = dpToPx(8)
                }
        }
    }

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
}
