// Filename: ResourcesFolderManager.kt
package com.lkacz.pola

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher

/**
 * Handles storing and retrieving the user-selected resources folder URI.
 * Formerly named MediaFolderManager.
 */
class ResourcesFolderManager(private val context: Context) {

    private val sharedPref: SharedPreferences = context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)

    fun storeResourcesFolderUri(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        sharedPref.edit().putString(RESOURCES_FOLDER_URI_KEY, uri.toString()).apply()
    }

    fun getResourcesFolderUri(): Uri? {
        val uriString = sharedPref.getString(RESOURCES_FOLDER_URI_KEY, null) ?: return null
        return Uri.parse(uriString)
    }

    fun clearResourcesFolderUri() {
        sharedPref.edit().remove(RESOURCES_FOLDER_URI_KEY).apply()
    }

    /**
     * Launches the folder picker for selecting a resources folder.
     */
    fun pickResourcesFolder(folderPickerLauncher: ActivityResultLauncher<Uri?>) {
        folderPickerLauncher.launch(null)
    }

    companion object {
        const val RESOURCES_FOLDER_URI_KEY = "RESOURCES_FOLDER_URI"
    }
}
