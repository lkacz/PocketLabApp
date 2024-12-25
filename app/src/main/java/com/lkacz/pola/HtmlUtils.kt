package com.lkacz.pola

import android.os.Build
import android.text.Html
import android.text.Spanned

/**
 * Utility object to convert HTML strings into styled text.
 */
object HtmlUtils {
    fun parseHtml(text: String?): Spanned {
        val safeText = text ?: ""
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(safeText, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(safeText)
        }
    }
}
