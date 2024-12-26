package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages font sizes for various UI elements. Sizes persist in SharedPreferences
 * and can be updated at runtime if desired.
 *
 * Default sizes are set according to the user's specification:
 * HEADER_SIZE = 60
 * BODY_SIZE = 24
 * BUTTON_SIZE = 20
 * ITEM_SIZE = 50
 * RESPONSE_SIZE = 8
 */
object FontSizeManager {

    private const val FONT_PREFS = "FontPrefs"

    private const val HEADER_SIZE_KEY = "headerSize"
    private const val BODY_SIZE_KEY = "bodySize"
    private const val BUTTON_SIZE_KEY = "buttonSize"
    private const val ITEM_SIZE_KEY = "itemSize"
    private const val RESPONSE_SIZE_KEY = "responseSize"

    // Defaults per user specification:
    private const val DEFAULT_HEADER_SIZE = 60f
    private const val DEFAULT_BODY_SIZE = 24f
    private const val DEFAULT_BUTTON_SIZE = 20f
    private const val DEFAULT_ITEM_SIZE = 50f
    private const val DEFAULT_RESPONSE_SIZE = 8f

    fun getHeaderSize(context: Context): Float {
        return getSharedPrefs(context).getFloat(HEADER_SIZE_KEY, DEFAULT_HEADER_SIZE)
    }

    fun setHeaderSize(context: Context, newSize: Float) {
        getSharedPrefs(context).edit().putFloat(HEADER_SIZE_KEY, newSize).apply()
    }

    fun getBodySize(context: Context): Float {
        return getSharedPrefs(context).getFloat(BODY_SIZE_KEY, DEFAULT_BODY_SIZE)
    }

    fun setBodySize(context: Context, newSize: Float) {
        getSharedPrefs(context).edit().putFloat(BODY_SIZE_KEY, newSize).apply()
    }

    fun getButtonSize(context: Context): Float {
        return getSharedPrefs(context).getFloat(BUTTON_SIZE_KEY, DEFAULT_BUTTON_SIZE)
    }

    fun setButtonSize(context: Context, newSize: Float) {
        getSharedPrefs(context).edit().putFloat(BUTTON_SIZE_KEY, newSize).apply()
    }

    fun getItemSize(context: Context): Float {
        return getSharedPrefs(context).getFloat(ITEM_SIZE_KEY, DEFAULT_ITEM_SIZE)
    }

    fun setItemSize(context: Context, newSize: Float) {
        getSharedPrefs(context).edit().putFloat(ITEM_SIZE_KEY, newSize).apply()
    }

    fun getResponseSize(context: Context): Float {
        return getSharedPrefs(context).getFloat(RESPONSE_SIZE_KEY, DEFAULT_RESPONSE_SIZE)
    }

    fun setResponseSize(context: Context, newSize: Float) {
        getSharedPrefs(context).edit().putFloat(RESPONSE_SIZE_KEY, newSize).apply()
    }

    private fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(FONT_PREFS, Context.MODE_PRIVATE)
    }
}
