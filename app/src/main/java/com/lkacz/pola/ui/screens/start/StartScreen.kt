package com.lkacz.pola.ui.screens.start

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

/**
 * Replaces StartFragment with Compose.
 * - Launches file/folder pickers for protocol and media directory.
 * - Confirms start of study and triggers the protocol reading.
 */
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
    val confirmationDialogManager = remember { ConfirmationDialogManager(context) }
    val mediaFolderManager = remember { MediaFolderManager(context) }
    val themeManager = remember { ThemeManager(context) }

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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Selected Protocol: ${
                mainActivity.protocolUri?.let { fileUriUtils.getFileName(context, it) }
                    ?: getProtocolDisplayName(sharedPref)
            }"
        )

        Button(onClick = {
            confirmationDialogManager.showStartStudyConfirmation(
                mainActivity.protocolUri,
                { uri -> fileUriUtils.getFileName(context, uri) }
            ) {
                mainActivity.protocolManager = com.lkacz.pola.ProtocolManager(context)
                mainActivity.protocolManager.readOriginalProtocol(mainActivity.protocolUri)
                navController.navigate("instruction")
            }
        }, modifier = Modifier.padding(4.dp)) {
            Text("Start Study")
        }

        OutlinedButton(
            onClick = {
                confirmationDialogManager.showChangeProtocolConfirmation {
                    filePickerLauncher.launch(arrayOf("text/plain"))
                }
            },
            modifier = Modifier.padding(4.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            Text("Select Protocol File")
        }

        OutlinedButton(
            onClick = {
                confirmationDialogManager.showChangeProtocolConfirmation {
                    mainActivity.protocolUri = null
                    sharedPref.edit()
                        .remove("PROTOCOL_URI")
                        .putString("CURRENT_MODE", "demo")
                        .apply()
                }
            },
            modifier = Modifier.padding(4.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            Text("Use Demo Protocol")
        }

        OutlinedButton(
            onClick = {
                confirmationDialogManager.showChangeProtocolConfirmation {
                    mainActivity.protocolUri = null
                    sharedPref.edit()
                        .remove("PROTOCOL_URI")
                        .putString("CURRENT_MODE", "tutorial")
                        .apply()
                }
            },
            modifier = Modifier.padding(4.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            Text("Use Tutorial Protocol")
        }

        OutlinedButton(
            onClick = {
                themeManager.toggleTheme()
                mainActivity.recreate()
            },
            modifier = Modifier.padding(4.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            Text("Toggle Theme")
        }

        OutlinedButton(
            onClick = {
                val protocolName = mainActivity.protocolUri?.let { fileUriUtils.getFileName(context, it) }
                    ?: getProtocolDisplayName(sharedPref)
                val fileContent = when (protocolName) {
                    "Demo Protocol" -> protocolReader.readFromAssets(context, "demo_protocol.txt")
                    "Tutorial Protocol" -> protocolReader.readFromAssets(context, "tutorial_protocol.txt")
                    else -> mainActivity.protocolUri?.let { uri ->
                        protocolReader.readFileContent(context, uri)
                    } ?: "File content not available"
                }
                ProtocolContentDisplayer(context).showProtocolContent(protocolName, fileContent)
            },
            modifier = Modifier.padding(4.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            Text("Show Protocol Content")
        }

        OutlinedButton(
            onClick = {
                val aboutHtmlContent = protocolReader.readFromAssets(context, "about.txt")
                ProtocolContentDisplayer(context).showHtmlContent("About", aboutHtmlContent)
            },
            modifier = Modifier.padding(4.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            Text("Show About")
        }

        OutlinedButton(
            onClick = {
                confirmationDialogManager.showChangeProtocolConfirmation {
                    folderPickerLauncher.launch(null)
                }
            },
            modifier = Modifier.padding(4.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            Text("Select Media Folder")
        }
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
