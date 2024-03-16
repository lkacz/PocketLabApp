package com.lkacz.pola

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.Html
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class ProtocolContentDisplayer(private val context: Context) {

    fun showProtocolContent(protocolName: String, fileContent: String) {
        val spannableContent = SpannableString(fileContent)
        styleWords(spannableContent, fileContent)
        styleSemicolons(spannableContent, fileContent)
        styleNonCommandLinesGrey(spannableContent, fileContent)
        showFileContentDialog(spannableContent)
    }

    private fun styleWords(spannableContent: SpannableString, fileContent: String) {
        val commandList = "SCALE|MULTISCALE|RANDOMIZED_MULTISCALE|TIMER|LOG|END|STUDY_ID|INSTRUCTION|TAP_INSTRUCTION|INPUTFIELD|RANDOMIZE_ON|RANDOMIZE_OFF"
        val commandPattern = "(?m)^($commandList)(;([^;\\n]*)?(;([^;\\n]*)?)?)?".toRegex()

        commandPattern.findAll(fileContent).forEach { matchResult ->
            val commandStart = matchResult.range.first
            val commandEnd = matchResult.groups[1]!!.range.last + 1
            spannableContent.setSpan(ForegroundColorSpan(Color.rgb(100, 100, 180)), commandStart, commandEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableContent.setSpan(StyleSpan(Typeface.BOLD), commandStart, commandEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableContent.setSpan(RelativeSizeSpan(1.10f), commandStart, commandEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            matchResult.groups[3]?.let {
                val phraseStart = it.range.first
                val phraseEnd = it.range.last + 1
                spannableContent.setSpan(StyleSpan(Typeface.BOLD), phraseStart, phraseEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            matchResult.groups[5]?.let {
                val phraseStart = it.range.first
                val phraseEnd = it.range.last + 1
                spannableContent.setSpan(StyleSpan(Typeface.ITALIC), phraseStart, phraseEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun styleSemicolons(spannableContent: SpannableString, fileContent: String) {
        var semicolonIndex = 0
        while (semicolonIndex != -1) {
            semicolonIndex = fileContent.indexOf(";", semicolonIndex)
            if (semicolonIndex != -1) {
                spannableContent.setSpan(StyleSpan(Typeface.BOLD), semicolonIndex, semicolonIndex + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                semicolonIndex++
            }
        }
    }

    private fun styleNonCommandLinesGrey(spannableContent: SpannableString, fileContent: String) {
        val commandList = listOf("SCALE", "MULTISCALE", "RANDOMIZED_MULTISCALE", "TIMER", "LOG", "END", "STUDY_ID", "INSTRUCTION", "TAP_INSTRUCTION", "INPUTFIELD", "RANDOMIZE_ON", "RANDOMIZE_OFF")
        val lines = fileContent.split("\n")
        var currentIndex = 0

        lines.forEach { line ->
            var isCommandLine = false
            for (command in commandList) {
                if (line.startsWith(command)) {
                    isCommandLine = true
                    break
                }
            }
            if (!isCommandLine) {
                val lineStart = currentIndex
                val lineEnd = currentIndex + line.length
                spannableContent.setSpan(ForegroundColorSpan(Color.GRAY), lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            currentIndex += line.length + 1 // +1 for the newline character
        }
    }

    private fun showFileContentDialog(spannableContent: SpannableString) {
        val textView = TextView(context).apply {
            text = spannableContent
            isVerticalScrollBarEnabled = true
        }

        AlertDialog.Builder(context).setTitle("File Content")
            .setView(ScrollView(context).apply { addView(textView) })
            .setPositiveButton("OK", null)
            .show()
    }

    fun showHtmlContent(title: String, htmlContent: String) {
        val formattedContent = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT)
        showFormattedContentDialog(title, formattedContent)
    }

    private fun showFormattedContentDialog(title: String, formattedContent: Spanned) {
        val textView = TextView(context).apply {
            text = formattedContent
            isVerticalScrollBarEnabled = true
        }

        AlertDialog.Builder(context).apply {
            setTitle(title)
            setView(ScrollView(context).apply { addView(textView) })
            setPositiveButton("OK", null)
            show()
        }
    }
}