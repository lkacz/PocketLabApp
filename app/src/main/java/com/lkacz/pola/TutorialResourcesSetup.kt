// Filename: TutorialResourcesSetup.kt
package com.lkacz.pola

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles automatic setup of resources folder for tutorial and demo protocols.
 * Copies bundled assets to user-selected folder on first run.
 */
class TutorialResourcesSetup(private val context: Context) {
    
    private val assetFiles = listOf(
        "pola_sound.mp3",
        "pola_video.mp4",
        "pola_pic.jpg",
        "pola_reaction_time.html",
        "psnakev2.html",
        "wiki.html"
    )
    
    /**
     * Copies tutorial/demo assets to the specified resources folder.
     * Runs on a background thread to avoid blocking the UI.
     * Returns true if successful, false otherwise.
     */
    suspend fun copyAssetsToResourcesFolder(resourcesFolderUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val parentFolder = DocumentFile.fromTreeUri(context, resourcesFolderUri) ?: return@withContext false
                
                var allSuccessful = true
                assetFiles.forEach { fileName ->
                    // Check if file already exists
                    val existingFile = parentFolder.findFile(fileName)
                    if (existingFile != null && existingFile.exists()) {
                        // Skip if already exists
                        return@forEach
                    }
                    
                    // Create new file and copy content
                    val mimeType = getMimeType(fileName)
                    val newFile = parentFolder.createFile(mimeType, fileName)
                    if (newFile == null) {
                        allSuccessful = false
                        return@forEach
                    }
                    
                    try {
                        context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                            context.assets.open(fileName).use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        } ?: run {
                            // Output stream was null - delete the empty file and mark as failed
                            newFile.delete()
                            allSuccessful = false
                        }
                    } catch (e: Exception) {
                        // Copy failed - delete the partial file and mark as failed
                        newFile.delete()
                        allSuccessful = false
                        e.printStackTrace()
                    }
                }
                allSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    /**
     * Checks if resources have already been set up in the specified folder.
     */
    fun areResourcesSetup(resourcesFolderUri: Uri?): Boolean {
        if (resourcesFolderUri == null) return false
        
        return try {
            val parentFolder = DocumentFile.fromTreeUri(context, resourcesFolderUri) ?: return false
            // Check if at least some key files exist
            assetFiles.take(3).all { fileName ->
                val file = parentFolder.findFile(fileName)
                file != null && file.exists()
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".mp3") -> "audio/mpeg"
            fileName.endsWith(".mp4") -> "video/mp4"
            fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> "image/jpeg"
            fileName.endsWith(".png") -> "image/png"
            fileName.endsWith(".html") -> "text/html"
            else -> "*/*"
        }
    }
}
