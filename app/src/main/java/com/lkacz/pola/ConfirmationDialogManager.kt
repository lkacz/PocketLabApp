package com.lkacz.pola

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AlertDialog

class ConfirmationDialogManager(private val context: Context) {
    fun showChangeProtocolConfirmation(onConfirm: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Confirm Protocol Change")
            .setMessage("Are you sure you want to change the current protocol?")
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("No", null)
            .show()
    }

    fun showStartStudyConfirmation(
        protocolUri: Uri?,
        getFileName: (Uri) -> String,
        onConfirm: () -> Unit,
    ) {
        // Check if the protocol is a demo or tutorial, and set the protocolName accordingly
        val protocolName =
            when (protocolUri?.toString()) {
                "file:///android_asset/tutorial_protocol.txt" -> context.getString(R.string.protocol_name_tutorial)
                null -> context.getString(R.string.protocol_name_demo) // Assuming null Uri indicates the demo protocol
                else -> protocolUri.let(getFileName)
            }

        val message =
            if (protocolName.isNotBlank() && protocolName != "No Protocol Selected") {
                "Start the study with protocol: $protocolName?"
            } else {
                "No protocol is selected. Are you sure you want to start the study?"
            }

        AlertDialog.Builder(context)
            .setTitle("Confirm Start")
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("No", null)
            .show()
    }
}
