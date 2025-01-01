// Filename: AlarmHelper.kt
package com.lkacz.pola

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import androidx.documentfile.provider.DocumentFile

class AlarmHelper(private val context: Context) {

    private val vibrator: Vibrator? = context.getSystemService()
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Starts the alarm sound (either a custom sound from the resources folder if specified,
     * or the default resource).
     * Vibration also starts, repeating until [stopAlarm] is called.
     */
    fun startAlarm() {
        stopAlarm()  // Ensure no old playback continues

        // Attempt to retrieve custom timer sound from SharedPreferences
        val prefs = context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        val soundFileName = prefs.getString("CUSTOM_TIMER_SOUND", null)

        // If we have a custom sound name, try to locate and play it
        val customSoundUri = if (!soundFileName.isNullOrBlank()) {
            findSoundUri(soundFileName)
        } else null

        // Create a MediaPlayer for either custom sound or the default alarm sound
        mediaPlayer = if (customSoundUri != null) {
            try {
                MediaPlayer().apply {
                    setDataSource(context, customSoundUri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                // Fallback to default if any error occurs
                MediaPlayer.create(context, R.raw.alarm_sound)?.apply {
                    isLooping = true
                    start()
                }
            }
        } else {
            // Default resource
            MediaPlayer.create(context, R.raw.alarm_sound)?.apply {
                isLooping = true
                start()
            }
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

    /**
     * Attempts to find a file named [fileName] in the user-selected resources folder
     * (document tree). Returns its [Uri] if found and valid, otherwise null.
     */
    private fun findSoundUri(fileName: String): Uri? {
        val resourcesFolderUri = ResourcesFolderManager(context).getResourcesFolderUri() ?: return null
        val parentFolder = DocumentFile.fromTreeUri(context, resourcesFolderUri) ?: return null
        val soundFile = parentFolder.findFile(fileName) ?: return null
        if (!soundFile.exists() || !soundFile.isFile) return null
        return soundFile.uri
    }
}
