// Filename: SpacingManager.kt
package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores and retrieves a user-selected padding value (in dp) for response buttons.
 */
object SpacingManager {

    private const val SPACING_PREFS = "SpacingPrefs"
    private const val RESPONSE_BUTTON_PADDING_KEY = "responseButtonPadding"

    // Default padding = 0 dp
    private const val DEFAULT_RESPONSE_BUTTON_PADDING = 0f

    fun getResponseButtonPadding(context: Context): Float {
        return getSharedPrefs(context).getFloat(RESPONSE_BUTTON_PADDING_KEY, DEFAULT_RESPONSE_BUTTON_PADDING)
    }

    fun setResponseButtonPadding(context: Context, newPadding: Float) {
        getSharedPrefs(context).edit().putFloat(RESPONSE_BUTTON_PADDING_KEY, newPadding).apply()
    }

    private fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(SPACING_PREFS, Context.MODE_PRIVATE)
    }
}
