package com.lkacz.pola.ui.screens.start

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.lkacz.pola.ConfirmationDialogManager
import com.lkacz.pola.FileUriUtils
import com.lkacz.pola.Logger
import com.lkacz.pola.MediaFolderManager
import com.lkacz.pola.ProtocolContentDisplayer
import com.lkacz.pola.ProtocolReader
import com.lkacz.pola.ThemeManager
import com.lkacz.pola.ui.MainActivity

@Composable
fun StartScreen(
    navController: NavHostController,
    mainActivity: MainActivity,
    logger: Logger
) {
    val context = mainActivity.applicationContext
    val sharedPref = context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
    val fileUriUtils = remember { FileUriUtils() }
    val protocolReader = remember { ProtocolReader() }
    val mediaFolderManager = remember { MediaFolderManager(context) }
    val themeManager = remember { ThemeManager(context) }

    // Track when to show the dialogs
    var showChangeProtocolDialog by remember { mutableStateOf(false) }
    var showStartStudyDialog by remember { mutableStateOf(false) }

    // File picker (for text protocols)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                val fileName = fileUriUtils.getFileName(context, uri)
                if (fileName.endsWith(".txt")) {
                    fileUriUtils.handleFileUri(context, uri, sharedPref)
                    mainActivity.protocolUri = uri
                } else {
                    Toast.makeText(context, "Select a .txt file for the protocol", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // Folder picker (for media folder)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                mediaFolderManager.storeMediaFolderUri(uri)
                Toast.makeText(context, "Media folder set: $uri", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Show the selected or default protocol name
    val protocolName = mainActivity.protocolUri?.let { fileUriUtils.getFileName(context, it) }
        ?: getProtocolDisplayName(sharedPref)

    // Compose UI
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Selected Protocol: $protocolName")

        Button(
            onClick = { showStartStudyDialog = true },
            modifier = Modifier.padding(4.dp)
        ) {
            Text("Start Study")
        }

        OutlinedButton(
            onClick = { showChangeProtocolDialog = true },
            modifier = Modifier.padding(4.dp)
        ) {
            Text("Select Protocol File")
        }

        OutlinedButton(
            onClick = {
                // Clearing the existing protocol and switching to demo
                showChangeProtocolDialog = true
            },
            modifier = Modifier.padding(4.dp)
        ) {
            Text("Use Demo Protocol")
        }

        OutlinedButton(
            onClick = {
                // Clearing the existing protocol and switching to tutorial
                showChangeProtocolDialog = true
            },
            modifier = Modifier.padding(4.dp)
        ) {
            Text("Use Tutorial Protocol")
        }

        OutlinedButton(
            onClick = {
                themeManager.toggleTheme()
                mainActivity.recreate()
            },
            modifier = Modifier.padding(4.dp)
        ) {
            Text("Toggle Theme")
        }

        OutlinedButton(
            onClick = {
                val fullContent = when (protocolName) {
                    "Demo Protocol" -> protocolReader.readFromAssets(context, "demo_protocol.txt")
                    "Tutorial Protocol" -> protocolReader.readFromAssets(context, "tutorial_protocol.txt")
                    else -> mainActivity.protocolUri?.let { uri ->
                        protocolReader.readFileContent(context, uri)
                    } ?: "File content not available"
                }
                ProtocolContentDisplayer(context).showProtocolContent(protocolName, fullContent)
            },
            modifier = Modifier.padding(4.dp)
        ) {
            Text("Show Protocol Content")
        }

        OutlinedButton(
            onClick = {
                val aboutHtmlContent = protocolReader.readFromAssets(context, "about.txt")
                ProtocolContentDisplayer(context).showHtmlContent("About", aboutHtmlContent)
            },
            modifier = Modifier.padding(4.dp)
        ) {
            Text("Show About")
        }

        OutlinedButton(
            onClick = { showChangeProtocolDialog = true },
            modifier = Modifier.padding(4.dp)
        ) {
            Text("Select Media Folder")
        }
    }

    // -- Compose-based dialogs below --

    // 1) Change Protocol Confirmation
    if (showChangeProtocolDialog) {
        ConfirmationDialogManager.ChangeProtocolConfirmationDialog(
            onConfirm = {
                showChangeProtocolDialog = false
                // Perform protocol/folder change actions here
                // This block is triggered when user taps "Yes"
                filePickerLauncher.launch(arrayOf("text/plain"))
            },
            onDismiss = {
                showChangeProtocolDialog = false
            }
        )
    }

    // 2) Start Study Confirmation
    if (showStartStudyDialog) {
        ConfirmationDialogManager.StartStudyConfirmationDialog(
            protocolUri = mainActivity.protocolUri,
            getFileName = { uri -> fileUriUtils.getFileName(context, uri) },
            onConfirm = {
                showStartStudyDialog = false
                mainActivity.protocolManager = com.lkacz.pola.ProtocolManager(context)
                mainActivity.protocolManager.readOriginalProtocol(mainActivity.protocolUri)
                navController.navigate("instruction")
            },
            onDismiss = {
                showStartStudyDialog = false
            }
        )
    }
}

/**
 * Returns a display name for the selected or default protocol.
 */
private fun getProtocolDisplayName(sharedPref: android.content.SharedPreferences): String {
    return when (sharedPref.getString("CURRENT_MODE", "demo")) {
        "tutorial" -> "Tutorial Protocol"
        "demo" -> "Demo Protocol"
        else -> "No Protocol Selected"
    }
}
