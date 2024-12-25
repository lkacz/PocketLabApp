package com.lkacz.pola

import android.content.Context
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService

class AlarmHelper(context: Context) {
    private val mediaPlayer: MediaPlayer = MediaPlayer.create(context, R.raw.alarm_sound)
    private val vibrator: Vibrator? = context.getSystemService()

    init {
        mediaPlayer.isLooping = true
    }

    fun startAlarm() {
        mediaPlayer.start()

        val pattern = longArrayOf(0, 1000, 1000)
        val amplitudes = intArrayOf(0, 255, 0) // Adjust pattern/amplitude as needed
        val vibrationEffect = VibrationEffect.createWaveform(pattern, amplitudes, 0)

        vibrator?.vibrate(vibrationEffect)
    }

    fun stopAlarm() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.prepare()
        }
        vibrator?.cancel()
    }

    fun release() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.release()
    }
}
