package com.lkacz.pola

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.util.regex.Pattern

/**
 * Plays both audio and video files referenced in text. Markers take the form:
 *   <filename.mp3[,volume]> or <filename.mp4[,volume]>
 * Volume is optional and defaults to 1.0 (100%) if omitted. If volume is provided (e.g. 60),
 * it is interpreted as 60%.
 *
 * Example usage:
 *   val cleanedText = MediaPlaybackHelper.parseAndPlayMedia(
 *       context = requireContext(),
 *       rawText = someString,
 *       mediaFolderUri = mediaFolderUri,
 *       mediaPlayers = mediaPlayers
 *   )
 *   // 'cleanedText' is the original text with all <...> segments removed, suitable for UI display.
 *
 * This class reuses the same approach for audio and video. Video playback here uses MediaPlayer
 * without a visual surface, so it will only play audio unless you integrate a UI surface.
 * For actual video display, additional UI setup is required (e.g., a VideoView or TextureView).
 */
object MediaPlaybackHelper {

    // A pattern to find <...> content in the text.
    private val pattern = Pattern.compile("<([^>]+)>")

    /**
     * Parse [rawText] for <filename[,volume]> references (supports mp3 or mp4) and play them.
     * The [mediaFolderUri] points to a user-selected folder with DocumentFile access.
     * Returns the text without the <...> placeholders.
     */
    fun parseAndPlayMedia(
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

            when {
                fileName.endsWith(".mp3", ignoreCase = true) -> {
                    playSoundFile(context, fileName, volume, mediaFolderUri, mediaPlayers)
                }
                fileName.endsWith(".mp4", ignoreCase = true) -> {
                    playVideoFile(context, fileName, volume, mediaFolderUri, mediaPlayers)
                }
                // If it's not mp3 or mp4, ignore or handle other formats as needed
            }

            // Remove the <...> text from the returned string
            matcher.appendReplacement(buffer, "")
        }
        matcher.appendTail(buffer)

        return buffer.toString()
    }

    /**
     * Splits strings like "something.mp3,60" into ("something.mp3", 0.60f).
     * If volume is omitted, it defaults to 1.0f (100%).
     */
    private fun parseFileAndVolume(fullMatch: String): Pair<String, Float> {
        val segments = fullMatch.split(",")
        val fileName = segments[0].trim()
        val volume = if (segments.size > 1) {
            val vol = segments[1].trim().toFloatOrNull()
            if (vol != null && vol in 0.0..100.0) (vol / 100f) else 1.0f
        } else 1.0f
        return fileName to volume
    }

    /**
     * Locates [fileName] in [mediaFolderUri] and plays it as audio with [volume].
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
                pfd?.let {
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

    /**
     * Locates [fileName] in [mediaFolderUri] and plays it as video with [volume].
     * This uses MediaPlayer without a visual surface, so only the audio track is heard.
     * To show actual video, additional UI setup (e.g., a VideoView) is required.
     */
    private fun playVideoFile(
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
                pfd?.let {
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
