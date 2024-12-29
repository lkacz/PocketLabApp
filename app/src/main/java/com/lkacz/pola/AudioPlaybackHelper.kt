package com.lkacz.pola

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.util.regex.Pattern

/**
 * Updated to locate audio files (.mp3, .wav) recursively within the selected media folder
 * or any of its subfolders. This ensures that audio markers still work if the user
 * organizes media files in subdirectories.
 */
object AudioPlaybackHelper {

    // Pattern to capture <...> segments in the text
    private val pattern = Pattern.compile("<([^>]+)>")

    /**
     * Parses [rawText] for <filename.mp3[,volume]> or <filename.wav[,volume]> markers,
     * tries to locate them recursively in [mediaFolderUri], then plays them.
     * Returns a version of [rawText] with all <...> segments removed, suitable for display.
     */
    fun parseAndPlayAudio(
        context: Context,
        rawText: String,
        mediaFolderUri: Uri?,
        mediaPlayers: MutableList<MediaPlayer>
    ): String {
        if (rawText.isBlank()) return rawText

        val matcher = pattern.matcher(rawText)
        val buffer = StringBuffer()

        while (matcher.find()) {
            val fullMatch = matcher.group(1)?.trim() ?: continue
            val (fileName, volume) = parseFileAndVolume(fullMatch)

            // Attempt to play the sound only if it ends with .mp3 or .wav
            if (
                fileName.endsWith(".mp3", ignoreCase = true) ||
                fileName.endsWith(".wav", ignoreCase = true)
            ) {
                playSoundFile(context, fileName, volume, mediaFolderUri, mediaPlayers)
            }

            // Remove the <filename> segment from the returned text
            matcher.appendReplacement(buffer, "")
        }
        matcher.appendTail(buffer)

        return buffer.toString()
    }

    /**
     * Splits the full match (e.g., "someFile.mp3,50") into (fileName, volume),
     * where volume is converted from 0–100% to 0.0–1.0f.
     */
    private fun parseFileAndVolume(fullMatch: String): Pair<String, Float> {
        val segments = fullMatch.split(",")
        val fileName = segments[0].trim()
        val volume = if (segments.size > 1) {
            val vol = segments[1].trim().toFloatOrNull()
            if (vol != null && vol in 0f..100f) (vol / 100f) else 1.0f
        } else 1.0f
        return fileName to volume
    }

    /**
     * Uses [findFileRecursive] to locate [fileName] within [mediaFolderUri] (including subfolders)
     * and play it with the requested [volume]. If not found or an error occurs, it is quietly ignored.
     */
    private fun playSoundFile(
        context: Context,
        fileName: String,
        volume: Float,
        mediaFolderUri: Uri?,
        mediaPlayers: MutableList<MediaPlayer>
    ) {
        if (mediaFolderUri == null) return
        val parentFolder = DocumentFile.fromTreeUri(context, mediaFolderUri) ?: return
        val audioFile = findFileRecursive(parentFolder, fileName) ?: return
        if (!audioFile.exists() || !audioFile.isFile) return

        try {
            val mediaPlayer = MediaPlayer().apply {
                context.contentResolver.openFileDescriptor(audioFile.uri, "r")?.use { pfd ->
                    setDataSource(pfd.fileDescriptor)
                    prepare()
                    setVolume(volume, volume)
                    start()
                }
            }
            mediaPlayers.add(mediaPlayer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Recursively searches [folder] and its subfolders for a file matching [fileName].
     * Returns the [DocumentFile] if found, else null.
     */
    private fun findFileRecursive(folder: DocumentFile, fileName: String): DocumentFile? {
        // First check if there's a direct child with this name
        folder.findFile(fileName)?.let { return it }

        // Otherwise, recurse into any subfolders
        folder.listFiles().forEach { docFile ->
            if (docFile.isDirectory) {
                findFileRecursive(docFile, fileName)?.let { return it }
            }
        }
        return null
    }
}
