// Filename: AlarmHelper.kt
package com.lkacz.pola

import android.content.Context
import android.media.MediaPlayer
import android.content.res.AssetFileDescriptor
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import androidx.documentfile.provider.DocumentFile
import java.io.IOException

class AlarmHelper(private val context: Context) {
    private val vibrator: Vibrator? = context.getSystemService()
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Starts the alarm sound (either a custom sound from the resources folder if specified,
     * or the default resource).
     * Vibration also starts, repeating until [stopAlarm] is called.
     */
    fun startAlarm() {
        stopAlarm() // Ensure no old playback continues

        // Attempt to retrieve custom timer sound from SharedPreferences
        val prefs = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        val soundFileName = prefs.getString("CUSTOM_TIMER_SOUND", null)

        // If we have a custom sound name, try to locate and play it
        mediaPlayer =
            createCustomMediaPlayer(soundFileName)?.also { player ->
                player.start()
            }
                ?: MediaPlayer.create(context, R.raw.alarm_sound)?.apply {
                    isLooping = true
                    start()
                }

        // Vibration pattern (waveform) repeated indefinitely
        val pattern = longArrayOf(0, 1000, 1000)
        val amplitudes = intArrayOf(0, 255, 0)
        val vibrationEffect = VibrationEffect.createWaveform(pattern, amplitudes, 0)
        vibrator?.vibrate(vibrationEffect)
    }

    /**
     * Stops the alarm sound and vibration, preparing for reuse.
     */
    fun stopAlarm() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
        mediaPlayer = null
        vibrator?.cancel()
    }

    /**
     * Must be called when disposing of AlarmHelper to avoid leaks.
     */
    fun release() {
        stopAlarm()
    }

    private fun createCustomMediaPlayer(fileName: String?): MediaPlayer? {
        if (fileName.isNullOrBlank()) {
            return null
        }

        return tryCreateFromResourcesFolder(fileName) ?: tryCreateFromAssets(fileName)
    }

    private fun tryCreateFromResourcesFolder(fileName: String): MediaPlayer? {
        val resourcesFolderUri = ResourcesFolderManager(context).getResourcesFolderUri() ?: return null
        val parentFolder = DocumentFile.fromTreeUri(context, resourcesFolderUri) ?: return null
        val soundFile = parentFolder.findFile(fileName) ?: return null
        if (!soundFile.exists() || !soundFile.isFile) return null

        return try {
            context.contentResolver.openFileDescriptor(soundFile.uri, "r")?.use { pfd ->
                MediaPlayer().apply {
                    setDataSource(pfd.fileDescriptor)
                    isLooping = true
                    prepare()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun tryCreateFromAssets(fileName: String): MediaPlayer? {
        return try {
            context.assets.openFd(fileName).use { assetFd ->
                createMediaPlayerFromAssetFd(assetFd)
            }
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun createMediaPlayerFromAssetFd(assetFd: AssetFileDescriptor): MediaPlayer {
        return MediaPlayer().apply {
            setDataSource(assetFd.fileDescriptor, assetFd.startOffset, assetFd.length)
            isLooping = true
            prepare()
        }
    }
}
