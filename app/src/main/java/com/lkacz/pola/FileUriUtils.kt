package com.lkacz.pola

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns

class FileUriUtils {

    fun handleFileUri(context: Context, uri: Uri, sharedPref: SharedPreferences) {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        sharedPref.edit().putString("PROTOCOL_URI", uri.toString()).apply()
    }

    fun getFileName(context: Context, uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        val nameIndex = cursor?.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
        cursor?.moveToFirst()
        val name = nameIndex?.let { cursor.getString(it) }
        cursor?.close()
        return name ?: "Unknown"
    }
}
