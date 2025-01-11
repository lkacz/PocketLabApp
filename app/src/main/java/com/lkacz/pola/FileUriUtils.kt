// Filename: FileUriUtils.kt
package com.lkacz.pola

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

class FileUriUtils {

    fun handleFileUri(context: Context, uri: Uri, sharedPref: SharedPreferences) {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        sharedPref.edit().putString("PROTOCOL_URI", uri.toString()).apply()
    }

    fun getFileName(context: Context, uri: Uri): String {
        val docFile = DocumentFile.fromSingleUri(context, uri)
        return docFile?.name ?: "Unknown"
    }
}
