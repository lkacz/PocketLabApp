package com.lkacz.pola

import android.content.Context

/**
 * Scans protocol lines for custom timer sound directives in the form:
 *   TIMER_SOUND;filename.mp3
 * and stores them in SharedPreferences (to be read later by AlarmHelper).
 */
object TimerSoundUpdater {

    private val timerSoundPattern = Regex("^TIMER_SOUND;(.+)$", RegexOption.IGNORE_CASE)

    /**
     * Checks if [line] contains a custom timer sound directive.
     * If so, stores the filename in SharedPreferences for retrieval by [AlarmHelper].
     * Returns true if this line was a timer sound directive, false otherwise.
     */
    fun updateTimerSoundFromLine(context: Context, line: String): Boolean {
        val match = timerSoundPattern.find(line.trim())
        if (match != null) {
            val soundFile = match.groupValues[1].trim()
            val pref = context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
            pref.edit().putString("CUSTOM_TIMER_SOUND", soundFile).apply()
            return true
        }
        return false
    }
}
