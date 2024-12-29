package com.lkacz.pola

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.util.regex.Pattern

/**
 * Scans a string for audio playback markers of the form <filename.mp3[,volume]> and plays them.
 * Volume is optional and defaults to 1.0 (100%).
 *
 * Usage:
 *   val cleanedText = AudioPlaybackHelper.parseAndPlayAudio(
 *       context = requireContext(),
 *       rawText = someStringContainingMarkers,
 *       mediaFolderUri = mediaFolderUri,
 *       mediaPlayers = mediaPlayers
 *   )
 *   // 'cleanedText' is 'someStringContainingMarkers' with all <...> segments removed or replaced
 *   // so you can safely show it in UI.
 */
object AudioPlaybackHelper {

    // A simple pattern to find any <...> substring.
    private val pattern = Pattern.compile("<([^>]+)>")

    /**
     * Parse and play audio references in the form <filename.mp3[,volume]> from the given raw text,
     * using files found in [mediaFolderUri]. Found audio references are played immediately and
     * removed from the returned string.
     *
     * @param context The Android context.
     * @param rawText The text possibly containing <filename.mp3[,volume]> markers.
     * @param mediaFolderUri The URI of the user-selected media folder; can be null if none selected.
     * @param mediaPlayers A collection of [MediaPlayer] references to track active players for release later.
     *
     * @return The input text with all <...> segments removed.
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

            // Expected format: "mySoundFile.mp3[,0-100]"
            // We'll parse out fileName and optionalVolume
            val (fileName, volume) = parseFileAndVolume(fullMatch)

            // Attempt to play the sound file if it ends with ".mp3"
            if (fileName.endsWith(".mp3", ignoreCase = true)) {
                playSoundFile(context, fileName, volume, mediaFolderUri, mediaPlayers)
            }

            // Remove the <...> content from the displayed text
            matcher.appendReplacement(buffer, "")
        }
        matcher.appendTail(buffer)

        return buffer.toString()
    }

    /**
     * If the text inside <> is "something.mp3,50", we parse out "something.mp3" and 0.50f for volume (50%).
     * If no comma is found, volume defaults to 1.0f.
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
     * Locates [fileName] in [mediaFolderUri] and plays it with [volume].
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
        val audioFile = parentFolder.findFile(fileName) ?: return
        if (!audioFile.exists() || !audioFile.isFile) return

        try {
            val mediaPlayer = MediaPlayer().apply {
                val afd = context.contentResolver.openFileDescriptor(audioFile.uri, "r")
                afd?.let {
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
