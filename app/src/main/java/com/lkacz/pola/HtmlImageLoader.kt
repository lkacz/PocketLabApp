package com.lkacz.pola

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Html
import android.text.Spanned
import androidx.core.text.HtmlCompat
import androidx.documentfile.provider.DocumentFile

/**
 * Revised to restore the simpler, previously working image loading approach without
 * unwanted transformations or forced centering. Also preserves optional width/height
 * if specified as attributes within <img> tags.
 *
 * Changes Made (compared to the recent version):
 * 1) Removed subfolder recursion and fancy short-image pattern conversions; we just
 *    look for the file directly under the user-selected media folder.
 * 2) Retained the ability to parse width/height from <img width="X" height="Y"> and
 *    store them in [imageSizeMap], applying them if found.
 * 3) Simplified getDrawable() to behave like it did in the older version, ensuring
 *    images appear as they did previously.
 *
 * Reasoning:
 * - Some advanced logic (recursive lookups, special <filename.png,100,200> patterns)
 *   caused confusion or broke direct <img> usage. This reverts to simpler code that
 *   was known to work in the past, letting standard <img src="..."> load images.
 */
class HtmlImageLoader private constructor(
    private val context: Context,
    private val parentFolderUri: Uri?
) : Html.ImageGetter {

    override fun getDrawable(source: String?): Drawable? {
        if (source.isNullOrBlank()) return null

        // See if we have saved width/height from <img width="..."> or <img height="...">
        val sizeInfo = imageSizeMap.remove(source)

        // If we have a valid parent folder URI, try to locate the file by name.
        val parentFolder = parentFolderUri?.let { DocumentFile.fromTreeUri(context, it) } ?: return null
        val imageFile = parentFolder.findFile(source) ?: return null
        if (!imageFile.exists() || !imageFile.isFile) return null

        return try {
            context.contentResolver.openInputStream(imageFile.uri).use { stream ->
                val drawable = Drawable.createFromStream(stream, source)
                drawable?.apply {
                    if (sizeInfo != null && sizeInfo.width > 0 && sizeInfo.height > 0) {
                        // Apply explicit width/height if provided in <img>.
                        setBounds(0, 0, sizeInfo.width, sizeInfo.height)
                    } else {
                        // Otherwise fall back to intrinsic sizes.
                        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /**
         * Map storing custom width/height from <img width="W" height="H">.
         * Keyed by the exact src string, so the parser can apply them.
         */
        internal val imageSizeMap = mutableMapOf<String, SizeInfo>()

        /**
         * Renders HTML text containing <img> tags using our [HtmlImageLoader].
         * Any optional <img width="X" height="Y"> attributes are handled.
         */
        fun getSpannedFromHtml(
            context: Context,
            parentFolderUri: Uri?,
            htmlText: String?
        ): Spanned {
            if (htmlText.isNullOrEmpty()) {
                return HtmlCompat.fromHtml("", HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
            // Rely on MediaTagHandler for minimal <video>/<audio> placeholders,
            // plus capturing any width/height from <img>.
            return HtmlCompat.fromHtml(
                htmlText,
                HtmlCompat.FROM_HTML_MODE_LEGACY,
                HtmlImageLoader(context, parentFolderUri),
                MediaTagHandler()
            )
        }
    }
}

/**
 * Holds optional width/height for an image.
 */
data class SizeInfo(val width: Int, val height: Int)
