package com.lkacz.pola

import android.content.Context
import android.net.Uri
import android.text.Spanned
import androidx.core.text.HtmlCompat

/**
 * Static utility to handle media placeholders (<video>, <audio>) and <img> sizing+centering
 * by pre-processing HTML strings before passing them to [HtmlCompat.fromHtml].
 */
object HtmlMediaHelper {
    /**
     * Pre-processes:
     *  - <img width="X" height="Y"> (centered, size stored in a map),
     *  - <video> (placeholder inserted),
     *  - <audio> (placeholder inserted).
     *
     * Then loads the processed HTML into a Spanned with [HtmlImageLoader].
     */
    fun toSpannedHtml(
        context: Context,
        parentFolderUri: Uri?,
        rawHtml: String?,
    ): Spanned {
        if (rawHtml.isNullOrEmpty()) {
            return HtmlCompat.fromHtml("", HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
        val handler = MediaTagHandler()
        val refinedHtml = handler.beforeFromHtml(rawHtml)
        return HtmlImageLoader.getSpannedFromHtml(context, parentFolderUri, refinedHtml)
    }
}
