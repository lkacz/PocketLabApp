package com.lkacz.pola

import android.content.Context
import androidx.appcompat.app.AlertDialog

/**
 * Utility for building and showing confirmation dialogs without repeating code.
 */
object AlertDialogBuilderUtils {
    fun showConfirmation(
        context: Context,
        title: String,
        message: String,
        onConfirm: () -> Unit,
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("No", null)
            .show()
    }
}
