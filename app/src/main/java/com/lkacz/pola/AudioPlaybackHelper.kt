package com.lkacz.pola

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.util.regex.Pattern

/**
 * Parses text for audio markers in the form <filename.mp3[,volume]> or <filename.wav[,volume]>.
 * Volume is optional and defaults to 1.0 if omitted. If volume is, for example, 60,
 * it is interpreted as 60% (0.6f).
 *
 * Usage example:
 *   val cleanedText = AudioPlaybackHelper.parseAndPlayAudio(
 *       context = requireContext(),
 *       rawText = "Some text <clip.mp3,50> more text",
 *       mediaFolderUri = yourMediaFolderUri,
 *       mediaPlayers = yourMediaPlayersList
 *   )
 *   // 'cleanedText' will not contain the <clip.mp3,50> portion.
 *   // Meanwhile, "clip.mp3" will start playing at 50% volume if found.
 */
object AudioPlaybackHelper {

    // Revised to only target mp3/wav placeholders and avoid removing <img> or other HTML tags.
    private val pattern = Pattern.compile("<([^>]+\\.(?:mp3|wav)(?:,[^>]+)?)>", Pattern.CASE_INSENSITIVE)

    /**
     * Scans [rawText] for <filename.mp3[,volume]> or <filename.wav[,volume]> markers,
     * attempts to locate each file under [mediaFolderUri], and plays the file if found.
     * Returns [rawText] with the <...> segments removed for display purposes.
     *
     * [mediaPlayers] is a shared list of MediaPlayer instances that you manage, ensuring
     * you can stop or release them as needed (e.g., in onDestroyView()).
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

            // Split by comma to get file name and volume
            val (fileName, volume) = parseFileAndVolume(fullMatch)

            // Play if it's .mp3 or .wav
            playSoundFile(context, fileName, volume, mediaFolderUri, mediaPlayers)

            // Remove the <...> audio placeholder from the final displayed string
            matcher.appendReplacement(buffer, "")
        }

        matcher.appendTail(buffer)
        return buffer.toString()
    }

    /**
     * Splits something like "song.mp3,60" into ("song.mp3", 0.60f).
     * If volume is missing, defaults to 1.0 (i.e., 100%).
     */
    private fun parseFileAndVolume(fullMatch: String): Pair<String, Float> {
        val segments = fullMatch.split(",")
        val fileName = segments[0].trim()
        val volume = if (segments.size > 1) {
            val vol = segments[1].trim().toFloatOrNull()
            if (vol != null && vol in 0f..100f) vol / 100f else 1.0f
        } else 1.0f
        return fileName to volume
    }

    /**
     * Attempts to locate [fileName] (MP3/WAV) inside [mediaFolderUri]. Then plays it with [volume].
     * If file not found, no action is taken.
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
