// Filename: AudioPlaybackHelper.kt
package com.lkacz.pola

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.regex.Pattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Parses text for media placeholders (e.g., <filename.mp3>) and plays them if found,
 * removing the placeholder text from the displayed text.
 */
object AudioPlaybackHelper {
    private val pattern =
        Pattern.compile(
            "<([^>]+\\.(?:mp3|wav|jpg|png)(?:,[^>]+)?)>",
            Pattern.CASE_INSENSITIVE,
        )

    fun parseAndPlayAudio(
        context: Context,
        rawText: String,
        mediaFolderUri: Uri?,
        scope: CoroutineScope,
        mediaPlayers: MutableList<MediaPlayer>,
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
                    playSoundFile(context, fileName, volume, mediaFolderUri, scope, mediaPlayers)
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
        val volume =
            if (segments.size > 1) {
                val vol = segments[1].trim().toFloatOrNull()
                if (vol != null && vol in 0f..100f) vol / 100f else 1.0f
            } else {
                1.0f
            }
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

    private fun buildImageTag(
        fileName: String,
        width: Int,
        height: Int,
    ): String {
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
        scope: CoroutineScope,
        mediaPlayers: MutableList<MediaPlayer>,
    ) {
        scope.launch(Dispatchers.IO) {
            val target = ResourceFileCache.getFile(context, mediaFolderUri, fileName)
            if (target != null && target.exists() && target.isFile) {
                try {
                    context.contentResolver.openFileDescriptor(target.uri, "r")?.use { pfd ->
                        val mediaPlayer = MediaPlayer()
                        mediaPlayer.setOnCompletionListener { mp ->
                            mp.release()
                            mainHandler.post { mediaPlayers.remove(mp) }
                        }
                        mediaPlayer.setOnErrorListener { mp, _, _ ->
                            mp.release()
                            mainHandler.post { mediaPlayers.remove(mp) }
                            true
                        }
                        mediaPlayer.setDataSource(pfd.fileDescriptor)
                        mediaPlayer.prepare()
                        mediaPlayer.setVolume(volume, volume)
                        withContext(Dispatchers.Main) {
                            mediaPlayers.add(mediaPlayer)
                        }
                        mediaPlayer.start()
                    }
                    return@launch
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to play audio from resources: $fileName", e)
                }
            }

            // Fallback: try loading from assets
            try {
                context.assets.openFd(fileName).use { afd ->
                    val mediaPlayer = MediaPlayer()
                    mediaPlayer.setOnCompletionListener { mp ->
                        mp.release()
                        mainHandler.post { mediaPlayers.remove(mp) }
                    }
                    mediaPlayer.setOnErrorListener { mp, _, _ ->
                        mp.release()
                        mainHandler.post { mediaPlayers.remove(mp) }
                        true
                    }
                    mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    mediaPlayer.prepare()
                    mediaPlayer.setVolume(volume, volume)
                    withContext(Dispatchers.Main) {
                        mediaPlayers.add(mediaPlayer)
                    }
                    mediaPlayer.start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play audio from assets: $fileName", e)
            }
        }
    }

    private const val TAG = "AudioPlaybackHelper"
    private val mainHandler = Handler(Looper.getMainLooper())
}
