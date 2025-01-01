package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences

object FontSizeManager {

    private const val FONT_PREFS = "FontPrefs"

    private const val HEADER_SIZE_KEY = "headerSize"
    private const val BODY_SIZE_KEY = "bodySize"
    private const val ITEM_SIZE_KEY = "itemSize"
    private const val RESPONSE_SIZE_KEY = "responseSize"
    private const val CONTINUE_SIZE_KEY = "continueSize"

    private const val DEFAULT_HEADER_SIZE = 60f
    private const val DEFAULT_BODY_SIZE = 24f
    private const val DEFAULT_ITEM_SIZE = 50f
    private const val DEFAULT_RESPONSE_SIZE = 8f
    private const val DEFAULT_CONTINUE_SIZE = 18f

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

    fun getContinueSize(context: Context): Float {
        return getSharedPrefs(context).getFloat(CONTINUE_SIZE_KEY, DEFAULT_CONTINUE_SIZE)
    }

    fun setContinueSize(context: Context, newSize: Float) {
        getSharedPrefs(context).edit().putFloat(CONTINUE_SIZE_KEY, newSize).apply()
    }

    private fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(FONT_PREFS, Context.MODE_PRIVATE)
    }
}
