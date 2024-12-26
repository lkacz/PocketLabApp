package com.lkacz.pola

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher

/**
 * Handles storing and retrieving the user-selected media folder URI.
 */
class MediaFolderManager(private val context: Context) {

    private val sharedPref: SharedPreferences = context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)

    fun storeMediaFolderUri(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        sharedPref.edit().putString(MEDIA_FOLDER_URI_KEY, uri.toString()).apply()
    }

    fun getMediaFolderUri(): Uri? {
        val uriString = sharedPref.getString(MEDIA_FOLDER_URI_KEY, null) ?: return null
        return Uri.parse(uriString)
    }

    fun clearMediaFolderUri() {
        sharedPref.edit().remove(MEDIA_FOLDER_URI_KEY).apply()
    }

    /**
     * Launches the folder picker for selecting a media folder.
     */
    fun pickMediaFolder(folderPickerLauncher: ActivityResultLauncher<Uri?>) {
        folderPickerLauncher.launch(null)
    }

    companion object {
        const val MEDIA_FOLDER_URI_KEY = "MEDIA_FOLDER_URI"
    }
}
