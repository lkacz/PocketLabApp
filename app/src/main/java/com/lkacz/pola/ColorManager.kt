// Filename: ColorManager.kt
package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

object ColorManager {

    private const val COLOR_PREFS = "ColorPrefs"

    private const val HEADER_TEXT_COLOR_KEY = "headerTextColor"
    private const val BODY_TEXT_COLOR_KEY = "bodyTextColor"
    private const val BUTTON_TEXT_COLOR_KEY = "buttonTextColor"
    private const val BUTTON_BACKGROUND_COLOR_KEY = "buttonBackgroundColor"
    private const val ITEM_TEXT_COLOR_KEY = "itemTextColor"
    private const val RESPONSE_TEXT_COLOR_KEY = "responseTextColor"
    private const val SCREEN_BACKGROUND_COLOR_KEY = "screenBackgroundColor"
    private const val CONTINUE_TEXT_COLOR_KEY = "continueTextColor"
    private const val CONTINUE_BACKGROUND_COLOR_KEY = "continueBackgroundColor"
    private const val TIMER_TEXT_COLOR_KEY = "timerTextColor"

    private const val DEFAULT_HEADER_TEXT_COLOR = Color.BLACK
    private const val DEFAULT_BODY_TEXT_COLOR = Color.DKGRAY
    private const val DEFAULT_BUTTON_TEXT_COLOR = Color.WHITE
    private const val DEFAULT_BUTTON_BACKGROUND_COLOR = Color.BLUE
    private const val DEFAULT_ITEM_TEXT_COLOR = Color.BLUE
    private const val DEFAULT_RESPONSE_TEXT_COLOR = Color.RED
    private const val DEFAULT_SCREEN_BACKGROUND_COLOR = Color.WHITE
    private const val DEFAULT_CONTINUE_TEXT_COLOR = Color.WHITE
    private const val DEFAULT_CONTINUE_BACKGROUND_COLOR = Color.DKGRAY
    private const val DEFAULT_TIMER_TEXT_COLOR = Color.BLACK

    fun getHeaderTextColor(context: Context): Int {
        return getSharedPrefs(context).getInt(HEADER_TEXT_COLOR_KEY, DEFAULT_HEADER_TEXT_COLOR)
    }

    fun setHeaderTextColor(context: Context, color: Int) {
        getSharedPrefs(context).edit().putInt(HEADER_TEXT_COLOR_KEY, color).apply()
    }

    fun getBodyTextColor(context: Context): Int {
        return getSharedPrefs(context).getInt(BODY_TEXT_COLOR_KEY, DEFAULT_BODY_TEXT_COLOR)
    }

    fun setBodyTextColor(context: Context, color: Int) {
        getSharedPrefs(context).edit().putInt(BODY_TEXT_COLOR_KEY, color).apply()
    }

    fun getButtonTextColor(context: Context): Int {
        return getSharedPrefs(context).getInt(BUTTON_TEXT_COLOR_KEY, DEFAULT_BUTTON_TEXT_COLOR)
    }

    fun setButtonTextColor(context: Context, color: Int) {
        getSharedPrefs(context).edit().putInt(BUTTON_TEXT_COLOR_KEY, color).apply()
    }

    fun getButtonBackgroundColor(context: Context): Int {
        return getSharedPrefs(context).getInt(BUTTON_BACKGROUND_COLOR_KEY, DEFAULT_BUTTON_BACKGROUND_COLOR)
    }

    fun setButtonBackgroundColor(context: Context, color: Int) {
        getSharedPrefs(context).edit().putInt(BUTTON_BACKGROUND_COLOR_KEY, color).apply()
    }

    fun getItemTextColor(context: Context): Int {
        return getSharedPrefs(context).getInt(ITEM_TEXT_COLOR_KEY, DEFAULT_ITEM_TEXT_COLOR)
    }

    fun setItemTextColor(context: Context, color: Int) {
        getSharedPrefs(context).edit().putInt(ITEM_TEXT_COLOR_KEY, color).apply()
    }

    fun getResponseTextColor(context: Context): Int {
        return getSharedPrefs(context).getInt(RESPONSE_TEXT_COLOR_KEY, DEFAULT_RESPONSE_TEXT_COLOR)
    }

    fun setResponseTextColor(context: Context, color: Int) {
        getSharedPrefs(context).edit().putInt(RESPONSE_TEXT_COLOR_KEY, color).apply()
    }

    fun getScreenBackgroundColor(context: Context): Int {
        return getSharedPrefs(context).getInt(SCREEN_BACKGROUND_COLOR_KEY, DEFAULT_SCREEN_BACKGROUND_COLOR)
    }

    fun setScreenBackgroundColor(context: Context, color: Int) {
        getSharedPrefs(context).edit().putInt(SCREEN_BACKGROUND_COLOR_KEY, color).apply()
    }

    fun getContinueTextColor(context: Context): Int {
        return getSharedPrefs(context).getInt(CONTINUE_TEXT_COLOR_KEY, DEFAULT_CONTINUE_TEXT_COLOR)
    }

    fun setContinueTextColor(context: Context, color: Int) {
        getSharedPrefs(context).edit().putInt(CONTINUE_TEXT_COLOR_KEY, color).apply()
    }

    fun getContinueBackgroundColor(context: Context): Int {
        return getSharedPrefs(context).getInt(CONTINUE_BACKGROUND_COLOR_KEY, DEFAULT_CONTINUE_BACKGROUND_COLOR)
    }

    fun setContinueBackgroundColor(context: Context, color: Int) {
        getSharedPrefs(context).edit().putInt(CONTINUE_BACKGROUND_COLOR_KEY, color).apply()
    }

    /**
     * Timer text color
     */
    fun getTimerTextColor(context: Context): Int {
        return getSharedPrefs(context).getInt(TIMER_TEXT_COLOR_KEY, DEFAULT_TIMER_TEXT_COLOR)
    }

    fun setTimerTextColor(context: Context, color: Int) {
        getSharedPrefs(context).edit().putInt(TIMER_TEXT_COLOR_KEY, color).apply()
    }

    private fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(COLOR_PREFS, Context.MODE_PRIVATE)
    }
}
