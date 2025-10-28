package com.lkacz.pola

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Html
import android.text.Spanned
import androidx.core.text.HtmlCompat

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
            val imageFile = ResourceFileCache.getFile(context, parentFolderUri, source)
            if (imageFile != null && imageFile.exists() && imageFile.isFile) {
                return try {
                    context.contentResolver.openInputStream(imageFile.uri).use { stream ->
                        val drawable = Drawable.createFromStream(stream, source)
                        drawable?.apply {
                            applyBounds(this, sizeInfo)
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
                    applyBounds(this, sizeInfo)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun applyBounds(drawable: Drawable, sizeInfo: SizeInfo?) {
        if (sizeInfo == null) {
            // No size specified - use intrinsic dimensions
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            return
        }

        // Get screen width in pixels for responsive sizing
        val screenWidthPx = context.resources.displayMetrics.widthPixels
        val density = context.resources.displayMetrics.density
        
        when {
            sizeInfo.width > 0 && sizeInfo.height > 0 -> {
                // Both width and height specified - treat as max width in dp
                val maxWidthPx = (sizeInfo.width * density).toInt()
                val requestedWidth = minOf(maxWidthPx, screenWidthPx - (32 * density).toInt()) // Account for padding
                val aspectRatio = drawable.intrinsicHeight.toFloat() / drawable.intrinsicWidth.toFloat()
                val calculatedHeight = (requestedWidth * aspectRatio).toInt()
                drawable.setBounds(0, 0, requestedWidth, calculatedHeight)
            }
            sizeInfo.width > 0 -> {
                // Only width specified - treat as max width in dp, maintain aspect ratio
                val maxWidthPx = (sizeInfo.width * density).toInt()
                val requestedWidth = minOf(maxWidthPx, screenWidthPx - (32 * density).toInt()) // Account for padding
                val aspectRatio = drawable.intrinsicHeight.toFloat() / drawable.intrinsicWidth.toFloat()
                val calculatedHeight = (requestedWidth * aspectRatio).toInt()
                drawable.setBounds(0, 0, requestedWidth, calculatedHeight)
            }
            sizeInfo.height > 0 -> {
                // Only height specified - maintain aspect ratio
                val scaledHeight = (sizeInfo.height * density).toInt()
                val aspectRatio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
                val scaledWidth = (scaledHeight * aspectRatio).toInt()
                drawable.setBounds(0, 0, scaledWidth, scaledHeight)
            }
            else -> {
                // Shouldn't happen, but fallback to intrinsic
                drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            }
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
