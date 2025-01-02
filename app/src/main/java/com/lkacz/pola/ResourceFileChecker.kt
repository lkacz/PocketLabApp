// Filename: ResourceFileChecker.kt
package com.lkacz.pola

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * Utility class to check if a given filename exists within the user-selected
 * resources folder. Also provides regex scanning for angled-bracket references.
 */
object ResourceFileChecker {

    private val angledFilePattern = Regex("<([^>]+\\.(?:mp3|wav|jpg|png|mp4|html)(?:,[^>]+)?)>", RegexOption.IGNORE_CASE)

    /**
     * Checks whether the provided [fileName] exists within the resources folder
     * designated in the user's settings. Returns true if found, false otherwise.
     */
    fun fileExistsInResources(context: Context, fileName: String): Boolean {
        val folderUri: Uri = ResourcesFolderManager(context).getResourcesFolderUri() ?: return true
        val parentFolder = folderUri.let { DocumentFile.fromTreeUri(context, it) } ?: return true
        val fileDoc = parentFolder.findFile(fileName) ?: return false
        return fileDoc.exists() && fileDoc.isFile
    }

    /**
     * Finds all angled-bracket references (e.g., <filename.mp3>), extracting
     * each file name. E.g., returns ["filename.mp3","someAudio.wav",...].
     */
    fun findBracketedFiles(line: String): List<String> {
        return angledFilePattern.findAll(line).map { match ->
            val group = match.groupValues[1]
            // If there's a comma indicating volume or dimensions, the filename
            // is the first segment (e.g., "myfile.mp4,50" -> "myfile.mp4")
            val fileCandidate = group.split(",").firstOrNull()?.trim() ?: group
            fileCandidate
        }.toList()
    }
}
