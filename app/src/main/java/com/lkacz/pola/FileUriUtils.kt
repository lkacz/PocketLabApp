// Filename: FileUriUtils.kt
package com.lkacz.pola

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log

/**
 * Persists and retrieves file URIs for protocol or other resources.
 * Includes a safer query method to handle potential SecurityExceptions.
 */
class FileUriUtils {

    /**
     * Persists URI permission grants so we can reliably read/write the file
     * outside the immediate result callback. Call this right after selecting
     * the file via ACTION_OPEN_DOCUMENT.
     */
    fun handleFileUri(context: Context, uri: Uri, sharedPref: SharedPreferences) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            Log.e("FileUriUtils", "Failed to take persistable URI permission: ${e.message}")
        }
        sharedPref.edit().putString("PROTOCOL_URI", uri.toString()).apply()
    }

    /**
     * Retrieves the display name of the file from the system’s content provider.
     * First verifies we actually hold read permission for the URI. If not, we
     * throw a SecurityException with a more explicit message prompting the user
     * to pick the file again via ACTION_OPEN_DOCUMENT.
     */
    fun getFileName(context: Context, uri: Uri): String {
        // Check if we have a persisted read permission for this URI
        val hasReadPermission = context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }
        if (!hasReadPermission) {
            throw SecurityException(
                "No persisted read permission for $uri. " +
                        "Please re-select the file via ACTION_OPEN_DOCUMENT."
            )
        }

        // Attempt the query
        return try {
            var name: String? = null
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                    name = cursor.getString(nameIndex)
                }
            }
            name ?: "Unknown"
        } catch (e: SecurityException) {
            // Re-throw with a friendlier explanation
            throw SecurityException(
                "Reading $uri failed. Ensure you picked it with ACTION_OPEN_DOCUMENT and have " +
                        "persistable read permissions. Original error: ${e.message}",
                e
            )
        }
    }
}
