// Filename: AudioPlaybackHelper.kt
package com.lkacz.pola

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.util.regex.Pattern

/**
 * Parses text for media placeholders (e.g., <filename.mp3>) and plays them if found,
 * removing the placeholder text from the displayed text.
 */
object AudioPlaybackHelper {

    private val pattern = Pattern.compile(
        "<([^>]+\\.(?:mp3|wav|jpg|png)(?:,[^>]+)?)>",
        Pattern.CASE_INSENSITIVE
    )

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
            val extension = getFileExtension(fullMatch).lowercase()

            when {
                extension == "mp3" || extension == "wav" -> {
                    val (fileName, volume) = parseAudioParams(fullMatch)
                    playSoundFile(context, fileName, volume, mediaFolderUri, mediaPlayers)
                    // Remove audio placeholders entirely from displayed text
                    matcher.appendReplacement(buffer, "")
                }
                extension == "jpg" || extension == "png" -> {
                    val (fileName, width, height) = parseImageParams(fullMatch)
                    val imgTag = buildImageTag(fileName, width, height)
                    matcher.appendReplacement(buffer, imgTag)
                }
                else -> {
                    matcher.appendReplacement(buffer, "")
                }
            }
        }
        matcher.appendTail(buffer)
        return buffer.toString()
    }

    private fun getFileExtension(fullMatch: String): String {
        val dotIndex = fullMatch.lastIndexOf('.')
        return if (dotIndex >= 0 && dotIndex < fullMatch.length - 1) {
            fullMatch.substring(dotIndex + 1).split(",")[0].trim()
        } else {
            ""
        }
    }

    private fun parseAudioParams(fullMatch: String): Pair<String, Float> {
        val segments = fullMatch.split(",")
        val fileName = segments[0].trim()
        val volume = if (segments.size > 1) {
            val vol = segments[1].trim().toFloatOrNull()
            if (vol != null && vol in 0f..100f) vol / 100f else 1.0f
        } else 1.0f
        return fileName to volume
    }

    private fun parseImageParams(fullMatch: String): Triple<String, Int, Int> {
        val segments = fullMatch.split(",")
        val fileName = segments[0].trim()
        var width = 0
        var height = 0

        if (segments.size >= 2) {
            width = segments[1].trim().toIntOrNull() ?: 0
        }
        if (segments.size >= 3) {
            height = segments[2].trim().toIntOrNull() ?: 0
        }
        return Triple(fileName, width, height)
    }

    private fun buildImageTag(fileName: String, width: Int, height: Int): String {
        val sb = StringBuilder("<img src=\"")
        sb.append(fileName).append("\"")
        if (width > 0) {
            sb.append(" width=\"").append(width).append("\"")
        }
        if (height > 0) {
            sb.append(" height=\"").append(height).append("\"")
        }
        sb.append(">")
        return sb.toString()
    }

    private fun playSoundFile(
        context: Context,
        fileName: String,
        volume: Float,
        mediaFolderUri: Uri?,
        mediaPlayers: MutableList<MediaPlayer>
    ) {
        if (mediaFolderUri == null) return
        val parentFolder = DocumentFile.fromTreeUri(context, mediaFolderUri) ?: return
        val mediaFile = parentFolder.findFile(fileName) ?: return
        if (!mediaFile.exists() || !mediaFile.isFile) return

        try {
            val mediaPlayer = MediaPlayer().apply {
                val pfd = context.contentResolver.openFileDescriptor(mediaFile.uri, "r")
                pfd?.use {
                    setDataSource(it.fileDescriptor)
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
}
