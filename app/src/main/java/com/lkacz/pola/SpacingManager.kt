// Filename: SpacingManager.kt
package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores and retrieves the user-selected margin and padding values (in dp) for response buttons,
 * and now also for continue buttons.
 */
object SpacingManager {

    private const val SPACING_PREFS = "SpacingPrefs"

    private const val RESPONSE_BUTTON_MARGIN_KEY = "responseButtonMargin"
    private const val RESPONSE_BUTTON_PADDING_H_KEY = "responseButtonPaddingH"
    private const val RESPONSE_BUTTON_PADDING_V_KEY = "responseButtonPaddingV"

    // New keys for CONTINUE button padding
    private const val CONTINUE_BUTTON_PADDING_H_KEY = "continueButtonPaddingH"
    private const val CONTINUE_BUTTON_PADDING_V_KEY = "continueButtonPaddingV"

    // Default values (in dp)
    private const val DEFAULT_BUTTON_MARGIN = 0f
    private const val DEFAULT_BUTTON_PADDING_H = 0f
    private const val DEFAULT_BUTTON_PADDING_V = 0f

    fun getResponseButtonMargin(context: Context): Float {
        return getSharedPrefs(context).getFloat(RESPONSE_BUTTON_MARGIN_KEY, DEFAULT_BUTTON_MARGIN)
    }

    fun setResponseButtonMargin(context: Context, marginDp: Float) {
        getSharedPrefs(context).edit()
            .putFloat(RESPONSE_BUTTON_MARGIN_KEY, marginDp)
            .apply()
    }

    fun getResponseButtonPaddingHorizontal(context: Context): Float {
        return getSharedPrefs(context).getFloat(RESPONSE_BUTTON_PADDING_H_KEY, DEFAULT_BUTTON_PADDING_H)
    }

    fun setResponseButtonPaddingHorizontal(context: Context, paddingDp: Float) {
        getSharedPrefs(context).edit()
            .putFloat(RESPONSE_BUTTON_PADDING_H_KEY, paddingDp)
            .apply()
    }

    fun getResponseButtonPaddingVertical(context: Context): Float {
        return getSharedPrefs(context).getFloat(RESPONSE_BUTTON_PADDING_V_KEY, DEFAULT_BUTTON_PADDING_V)
    }

    fun setResponseButtonPaddingVertical(context: Context, paddingDp: Float) {
        getSharedPrefs(context).edit()
            .putFloat(RESPONSE_BUTTON_PADDING_V_KEY, paddingDp)
            .apply()
    }

    // CONTINUE button padding
    fun getContinueButtonPaddingHorizontal(context: Context): Float {
        return getSharedPrefs(context).getFloat(CONTINUE_BUTTON_PADDING_H_KEY, DEFAULT_BUTTON_PADDING_H)
    }

    fun setContinueButtonPaddingHorizontal(context: Context, paddingDp: Float) {
        getSharedPrefs(context).edit()
            .putFloat(CONTINUE_BUTTON_PADDING_H_KEY, paddingDp)
            .apply()
    }

    fun getContinueButtonPaddingVertical(context: Context): Float {
        return getSharedPrefs(context).getFloat(CONTINUE_BUTTON_PADDING_V_KEY, DEFAULT_BUTTON_PADDING_V)
    }

    fun setContinueButtonPaddingVertical(context: Context, paddingDp: Float) {
        getSharedPrefs(context).edit()
            .putFloat(CONTINUE_BUTTON_PADDING_V_KEY, paddingDp)
            .apply()
    }

    private fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(SPACING_PREFS, Context.MODE_PRIVATE)
    }
}
