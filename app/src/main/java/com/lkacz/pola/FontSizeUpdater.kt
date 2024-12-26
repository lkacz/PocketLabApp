package com.lkacz.pola

import android.content.Context

/**
 * Checks protocol lines for font size directives (HEADER_SIZE, BODY_SIZE, etc.)
 * and updates persistent font size values accordingly. Returns true if the line
 * is a recognized size directive (and thus should be excluded from normal protocol
 * instructions).
 */
object FontSizeUpdater {
    private val sizePattern = Regex("^(HEADER_SIZE|BODY_SIZE|BUTTON_SIZE|ITEM_SIZE|RESPONSE_SIZE);(\\d+(\\.\\d+)?)")

    fun updateFontSizesFromLine(context: Context, line: String): Boolean {
        val match = sizePattern.find(line.trim())
        match?.let {
            val directive = it.groupValues[1]
            val sizeValue = it.groupValues[2].toFloat()
            when (directive) {
                "HEADER_SIZE" -> FontSizeManager.setHeaderSize(context, sizeValue)
                "BODY_SIZE" -> FontSizeManager.setBodySize(context, sizeValue)
                "BUTTON_SIZE" -> FontSizeManager.setButtonSize(context, sizeValue)
                "ITEM_SIZE" -> FontSizeManager.setItemSize(context, sizeValue)
                "RESPONSE_SIZE" -> FontSizeManager.setResponseSize(context, sizeValue)
            }
            return true
        }
        return false
    }
}
