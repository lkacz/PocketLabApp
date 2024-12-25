package com.lkacz.pola

import android.content.Context
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService

class AlarmHelper(context: Context) {

    private val vibrator: Vibrator? = context.getSystemService()
    private val mediaPlayer: MediaPlayer? = MediaPlayer.create(context, R.raw.alarm_sound)?.apply {
        isLooping = true
    }

    /**
     * Starts the alarm sound and vibration. If the vibrator or media player is unavailable,
     * the alarm still attempts to function with whichever component is available.
     */
    fun startAlarm() {
        try {
            if (mediaPlayer != null && !mediaPlayer.isPlaying) {
                mediaPlayer.start()
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }

        val pattern = longArrayOf(0, 1000, 1000)
        val amplitudes = intArrayOf(0, 255, 0)
        val vibrationEffect = VibrationEffect.createWaveform(pattern, amplitudes, 0)
        vibrator?.vibrate(vibrationEffect)
    }

    /**
     * Stops the alarm and prepares the media player for future use.
     */
    fun stopAlarm() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                mediaPlayer.prepare()
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
        vibrator?.cancel()
    }

    /**
     * Releases the MediaPlayer resources to avoid leaks.
     */
    fun release() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer?.release()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }
}
