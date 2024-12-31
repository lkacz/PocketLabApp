// Filename: TransitionManager.kt
package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences

/**
 * Handles reading and writing the user's chosen transition mode ("off" or "slide").
 */
object TransitionManager {

    private const val PREFS_NAME = "ProtocolPrefs"
    private const val KEY_TRANSITION_MODE = "TRANSITION_MODE"

    fun setTransitionMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_TRANSITION_MODE, mode).apply()
    }

    fun getTransitionMode(context: Context): String {
        return getPrefs(context).getString(KEY_TRANSITION_MODE, "slide").orEmpty()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
