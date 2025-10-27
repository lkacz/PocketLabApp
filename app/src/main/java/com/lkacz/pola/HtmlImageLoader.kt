package com.lkacz.pola

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Html
import android.text.Spanned
import androidx.core.text.HtmlCompat
import androidx.documentfile.provider.DocumentFile

class HtmlImageLoader private constructor(
    private val context: Context,
    private val parentFolderUri: Uri?,
) : Html.ImageGetter {
    override fun getDrawable(source: String?): Drawable? {
        if (source.isNullOrBlank()) return null

        // See if we have saved width/height from <img width="..."> or <img height="...">
        val sizeInfo = imageSizeMap.remove(source)

        // Try loading from resources folder if available
        if (parentFolderUri != null) {
            val parentFolder = DocumentFile.fromTreeUri(context, parentFolderUri)
            val imageFile = parentFolder?.findFile(source)
            if (imageFile != null && imageFile.exists() && imageFile.isFile) {
                return try {
                    context.contentResolver.openInputStream(imageFile.uri).use { stream ->
                        val drawable = Drawable.createFromStream(stream, source)
                        drawable?.apply {
                            if (sizeInfo != null && sizeInfo.width > 0 && sizeInfo.height > 0) {
                                setBounds(0, 0, sizeInfo.width, sizeInfo.height)
                            } else {
                                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                            }
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }

        // Fallback: try loading from assets
        return try {
            context.assets.open(source).use { stream ->
                val drawable = Drawable.createFromStream(stream, source)
                drawable?.apply {
                    if (sizeInfo != null && sizeInfo.width > 0 && sizeInfo.height > 0) {
                        setBounds(0, 0, sizeInfo.width, sizeInfo.height)
                    } else {
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
            htmlText: String?,
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
                MediaTagHandler(),
            )
        }
    }
}

/**
 * Holds optional width/height for an image.
 */
data class SizeInfo(val width: Int, val height: Int)
