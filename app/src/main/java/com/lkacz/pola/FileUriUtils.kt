// Filename: FileUriUtils.kt
package com.lkacz.pola

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

class FileUriUtils {
    fun handleFileUri(
        context: Context,
        uri: Uri,
        sharedPref: SharedPreferences,
    ) {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        sharedPref
            .edit()
            .putString(Prefs.KEY_PROTOCOL_URI, uri.toString())
            .remove(Prefs.KEY_PROTOCOL_PROGRESS_INDEX)
            .putBoolean(Prefs.KEY_PROTOCOL_IN_PROGRESS, false)
            .apply()
    }

    fun getFileName(
        context: Context,
        uri: Uri,
    ): String {
        val scheme = uri.scheme.orEmpty()
        if (scheme == "file") {
            val path = uri.path.orEmpty()
            if (path.startsWith("/android_asset/")) {
                val assetName = path.substringAfterLast('/')
                return when (assetName) {
                    "demo_protocol.txt" -> context.getString(R.string.protocol_name_demo)
                    "tutorial_protocol.txt" -> context.getString(R.string.protocol_name_tutorial)
                    else -> assetName.ifBlank { "Unknown" }
                }
            }
        }

        val docFile = DocumentFile.fromSingleUri(context, uri)
        return docFile?.name ?: uri.lastPathSegment ?: "Unknown"
    }
}
