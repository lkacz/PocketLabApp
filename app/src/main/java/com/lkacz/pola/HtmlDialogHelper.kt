// Filename: HtmlDialogHelper.kt
package com.lkacz.pola

import android.content.Context
import android.text.Html
import android.text.Spanned
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

object HtmlDialogHelper {
    fun showHtmlContent(context: Context, title: String, htmlContent: String) {
        val formattedContent = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT)
        showFormattedContentDialog(context, title, formattedContent)
    }

    private fun showFormattedContentDialog(context: Context, title: String, formattedContent: Spanned) {
        val textView = TextView(context).apply {
            text = formattedContent
        }

        AlertDialog.Builder(context).apply {
            setTitle(title)
            setView(ScrollView(context).apply { addView(textView) })
            setPositiveButton("OK", null)
            show()
        }
    }
}
