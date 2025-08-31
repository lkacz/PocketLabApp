// Filename: SpacingManager.kt
package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences

object SpacingManager {
    private const val SPACING_PREFS = "SpacingPrefs"

    private const val RESPONSE_BUTTON_MARGIN_KEY = "responseButtonMargin"
    private const val RESPONSE_BUTTON_PADDING_H_KEY = "responseButtonPaddingH"
    private const val RESPONSE_BUTTON_PADDING_V_KEY = "responseButtonPaddingV"
    private const val CONTINUE_BUTTON_PADDING_H_KEY = "continueButtonPaddingH"
    private const val CONTINUE_BUTTON_PADDING_V_KEY = "continueButtonPaddingV"
    private const val RESPONSE_SPACING_KEY = "responseSpacing"

    // Added these keys for timer padding
    private const val TIMER_PADDING_H_KEY = "timerPaddingH"
    private const val TIMER_PADDING_V_KEY = "timerPaddingV"

    private const val DEFAULT_BUTTON_MARGIN = 0f
    private const val DEFAULT_BUTTON_PADDING_H = 0f
    private const val DEFAULT_BUTTON_PADDING_V = 0f
    private const val DEFAULT_RESPONSE_SPACING = 0f
    private const val DEFAULT_TIMER_PADDING_H = 0f
    private const val DEFAULT_TIMER_PADDING_V = 0f

    fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(SPACING_PREFS, Context.MODE_PRIVATE)
    }

    fun getResponseButtonMargin(context: Context): Float {
        return getSharedPrefs(context).getFloat(RESPONSE_BUTTON_MARGIN_KEY, DEFAULT_BUTTON_MARGIN)
    }

    fun setResponseButtonMargin(
        context: Context,
        marginDp: Float,
    ) {
        getSharedPrefs(context).edit().putFloat(RESPONSE_BUTTON_MARGIN_KEY, marginDp).apply()
    }

    fun getResponseButtonPaddingHorizontal(context: Context): Float {
        return getSharedPrefs(context).getFloat(RESPONSE_BUTTON_PADDING_H_KEY, DEFAULT_BUTTON_PADDING_H)
    }

    fun setResponseButtonPaddingHorizontal(
        context: Context,
        paddingDp: Float,
    ) {
        getSharedPrefs(context).edit().putFloat(RESPONSE_BUTTON_PADDING_H_KEY, paddingDp).apply()
    }

    fun getResponseButtonPaddingVertical(context: Context): Float {
        return getSharedPrefs(context).getFloat(RESPONSE_BUTTON_PADDING_V_KEY, DEFAULT_BUTTON_PADDING_V)
    }

    fun setResponseButtonPaddingVertical(
        context: Context,
        paddingDp: Float,
    ) {
        getSharedPrefs(context).edit().putFloat(RESPONSE_BUTTON_PADDING_V_KEY, paddingDp).apply()
    }

    fun getContinueButtonPaddingHorizontal(context: Context): Float {
        return getSharedPrefs(context).getFloat(CONTINUE_BUTTON_PADDING_H_KEY, DEFAULT_BUTTON_PADDING_H)
    }

    fun setContinueButtonPaddingHorizontal(
        context: Context,
        paddingDp: Float,
    ) {
        getSharedPrefs(context).edit().putFloat(CONTINUE_BUTTON_PADDING_H_KEY, paddingDp).apply()
    }

    fun getContinueButtonPaddingVertical(context: Context): Float {
        return getSharedPrefs(context).getFloat(CONTINUE_BUTTON_PADDING_V_KEY, DEFAULT_BUTTON_PADDING_V)
    }

    fun setContinueButtonPaddingVertical(
        context: Context,
        paddingDp: Float,
    ) {
        getSharedPrefs(context).edit().putFloat(CONTINUE_BUTTON_PADDING_V_KEY, paddingDp).apply()
    }

    fun setResponseSpacing(
        context: Context,
        spacingDp: Float,
    ) {
        getSharedPrefs(context).edit().putFloat(RESPONSE_SPACING_KEY, spacingDp).apply()
    }

    fun getResponseSpacing(context: Context): Float {
        return getSharedPrefs(context).getFloat(RESPONSE_SPACING_KEY, DEFAULT_RESPONSE_SPACING)
    }

    // Newly added for Timer Padding:
    fun setTimerPaddingHorizontal(
        context: Context,
        paddingDp: Float,
    ) {
        getSharedPrefs(context).edit().putFloat(TIMER_PADDING_H_KEY, paddingDp).apply()
    }

    fun setTimerPaddingVertical(
        context: Context,
        paddingDp: Float,
    ) {
        getSharedPrefs(context).edit().putFloat(TIMER_PADDING_V_KEY, paddingDp).apply()
    }

    fun getTimerPaddingHorizontal(context: Context): Float {
        return getSharedPrefs(context).getFloat(TIMER_PADDING_H_KEY, DEFAULT_TIMER_PADDING_H)
    }

    fun getTimerPaddingVertical(context: Context): Float {
        return getSharedPrefs(context).getFloat(TIMER_PADDING_V_KEY, DEFAULT_TIMER_PADDING_V)
    }
}
