package com.lkacz.pola

import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

/**
 * Compose-based confirmation dialogs.
 * Instead of directly calling .show(), you control whether these dialogs are displayed
 * by toggling a Boolean in your composable screens.
 */
object ConfirmationDialogManager {

    /**
     * Composable for protocol-change confirmation.
     * @param onConfirm Action taken when user clicks "Yes".
     * @param onDismiss Action taken when user clicks "No" or outside the dialog.
     */
    @Composable
    fun ChangeProtocolConfirmationDialog(
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("No")
                }
            },
            title = { Text("Confirm Protocol Change") },
            text = { Text("Are you sure you want to change the current protocol?") },
            properties = DialogProperties(dismissOnClickOutside = true)
        )
    }

    /**
     * Composable for starting the study.
     * @param protocolUri The URI of the protocol, if selected.
     * @param getFileName Lambda to resolve the file name from a Uri.
     * @param onConfirm Action taken when user clicks "Yes".
     * @param onDismiss Action taken when user clicks "No" or outside the dialog.
     */
    @Composable
    fun StartStudyConfirmationDialog(
        protocolUri: Uri?,
        getFileName: (Uri) -> String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        // Check if the protocol is a demo or tutorial, and set the protocolName accordingly
        val protocolName = when (protocolUri?.toString()) {
            "file:///android_asset/tutorial_protocol.txt" -> "Tutorial Protocol"
            null -> "Demo Protocol" // Null Uri indicates the demo protocol
            else -> getFileName(protocolUri)
        }

        val message = if (protocolName.isNotBlank() && protocolName != "No Protocol Selected") {
            "Start the study with protocol: $protocolName?"
        } else {
            "No protocol is selected. Are you sure you want to start the study?"
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("No")
                }
            },
            title = { Text("Confirm Start") },
            text = { Text(message) },
            properties = DialogProperties(dismissOnClickOutside = true)
        )
    }
}
