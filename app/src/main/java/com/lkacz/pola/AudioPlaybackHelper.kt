package com.lkacz.pola

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.util.regex.Pattern

/**
 * Parses text for media placeholders in the form:
 *   - <filename.mp3[,volume]>
 *   - <filename.wav[,volume]>
 *   - <filename.jpg[,width,height]>
 *   - <filename.png[,width,height]>
 *
 * For audio (mp3/wav), the optional numeric argument is volume in percent (0..100).
 * For images (jpg/png), the optional numeric arguments are width and height in pixels.
 * If width and/or height are omitted, the image is placed without explicit sizing.
 *
 * Usage example:
 *   val cleanedText = AudioPlaybackHelper.parseAndPlayAudio(
 *       context = requireContext(),
 *       rawText = "Sample <clip.mp3,50> and <photo.jpg,200,400> test",
 *       mediaFolderUri = yourMediaFolderUri,
 *       mediaPlayers = yourMediaPlayersList
 *   )
 *   // 'cleanedText' will remove audio placeholders entirely, and replace image placeholders
 *   // with <img src="photo.jpg" width="200" height="400"> so they can be displayed in HTML.
 *   // Meanwhile, "clip.mp3" will start playing at 50% volume if found.
 */
object AudioPlaybackHelper {

    /**
     * Updated pattern:
     *   - Only captures placeholders ending in .mp3, .wav, .jpg, .png (with optional params).
     *   - We do NOT match <img>, <video>, or other standard HTML tags to avoid stripping them.
     */
    private val pattern = Pattern.compile(
        "<([^>]+\\.(?:mp3|wav|jpg|png)(?:,[^>]+)?)>",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Scans [rawText] for recognized placeholders (mp3/wav or jpg/png), handles them, and
     * returns the text with those placeholders removed or replaced (for images).
     *
     * For audio:
     *  - We remove the placeholder from display text and play the file (if found) at specified volume.
     * For images:
     *  - We replace the placeholder with an <img> tag, optionally including width/height if provided.
     *
     * @param context Android [Context].
     * @param rawText The original text that may contain placeholders.
     * @param mediaFolderUri Points to the user-chosen folder of media files.
     * @param mediaPlayers A list managing all MediaPlayer instances for later release.
     * @return The modified text, ready for HTML rendering.
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
                    // Convert to <img src="..." [width="..."] [height="..."]>
                    val imgTag = buildImageTag(fileName, width, height)
                    // Replace the original placeholder with an <img> tag
                    matcher.appendReplacement(buffer, imgTag)
                }
                else -> {
                    // No action on unrecognized extension
                    matcher.appendReplacement(buffer, "")
                }
            }
        }
        matcher.appendTail(buffer)
        return buffer.toString()
    }

    /**
     * Checks the extension from the full placeholder content, e.g. "photo.jpg,200,300".
     */
    private fun getFileExtension(fullMatch: String): String {
        val dotIndex = fullMatch.lastIndexOf('.')
        return if (dotIndex >= 0 && dotIndex < fullMatch.length - 1) {
            fullMatch.substring(dotIndex + 1).split(",")[0].trim()
        } else {
            ""
        }
    }

    /**
     * For placeholders like "<sound.mp3,60>", splits into filename + volume in range 0.0..1.0.
     */
    private fun parseAudioParams(fullMatch: String): Pair<String, Float> {
        val segments = fullMatch.split(",")
        val fileName = segments[0].trim()
        val volume = if (segments.size > 1) {
            val vol = segments[1].trim().toFloatOrNull()
            if (vol != null && vol in 0f..100f) vol / 100f else 1.0f
        } else 1.0f
        return fileName to volume
    }

    /**
     * For placeholders like "<pic.jpg,200,300>" splits into filename + width + height.
     * Width/height default to zero if omitted or not numeric.
     */
    private fun parseImageParams(fullMatch: String): Triple<String, Int, Int> {
        val segments = fullMatch.split(",")
        val fileName = segments[0].trim()
        var width = 0
        var height = 0

        // If one or two numeric parameters are provided, parse them
        if (segments.size >= 2) {
            width = segments[1].trim().toIntOrNull() ?: 0
        }
        if (segments.size >= 3) {
            height = segments[2].trim().toIntOrNull() ?: 0
        }
        return Triple(fileName, width, height)
    }

    /**
     * Builds an <img> HTML tag given [fileName], optional [width] and [height].
     * If width/height <= 0, the attribute is omitted.
     */
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

    /**
     * If [fileName] is found (MP3/WAV) in [mediaFolderUri], plays it at [volume].
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
