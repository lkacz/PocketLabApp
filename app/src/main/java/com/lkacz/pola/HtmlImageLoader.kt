package com.lkacz.pola

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Html
import android.text.Spanned
import androidx.core.text.HtmlCompat
import androidx.documentfile.provider.DocumentFile
import java.util.regex.Pattern

class HtmlImageLoader private constructor(
    private val context: Context,
    private val parentFolderUri: Uri?
) : Html.ImageGetter {

    override fun getDrawable(source: String?): Drawable? {
        if (source.isNullOrBlank()) return null
        val sizeInfo = imageSizeMap.remove(source)
        val parentFolder = parentFolderUri?.let { DocumentFile.fromTreeUri(context, it) } ?: return null
        val imageFile = parentFolder.findFile(source) ?: return null
        if (!imageFile.exists() || !imageFile.isFile) return null

        return try {
            context.contentResolver.openInputStream(imageFile.uri).use { inputStream ->
                val drawable = Drawable.createFromStream(inputStream, source)
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
        internal val imageSizeMap = mutableMapOf<String, SizeInfo>()

        fun getSpannedFromHtml(
            context: Context,
            parentFolderUri: Uri?,
            htmlText: String?
        ): Spanned {
            if (htmlText.isNullOrEmpty()) {
                return HtmlCompat.fromHtml("", HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
            return HtmlCompat.fromHtml(
                htmlText,
                HtmlCompat.FROM_HTML_MODE_LEGACY,
                HtmlImageLoader(context, parentFolderUri),
                MediaTagHandler()
            )
        }
    }
}

data class SizeInfo(val width: Int, val height: Int)