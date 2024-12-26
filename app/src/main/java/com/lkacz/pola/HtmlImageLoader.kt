package com.lkacz.pola

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Html
import android.text.Spanned
import androidx.core.text.HtmlCompat
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Loads images referenced by <img> tags in HTML strings.
 * Images are assumed to be in the same folder as the protocol file.
 *
 * Usage:
 *   val spannedText = HtmlImageLoader.getSpannedFromHtml(context, ProtocolManager.protocolParentUri, htmlText)
 *   textView.text = spannedText
 */
class HtmlImageLoader private constructor(
    private val context: Context,
    private val parentFolderUri: Uri?
) : Html.ImageGetter {

    override fun getDrawable(source: String?): Drawable? {
        if (source.isNullOrBlank()) return null
        // Attempt to locate the parent folder (DocumentFile) to resolve the image
        val parentFolder = parentFolderUri?.let { DocumentFile.fromTreeUri(context, it) }
            ?: return null

        // Find file matching 'source' under the parent folder
        val imageFile = parentFolder.findFile(source)
        if (imageFile == null || !imageFile.isFile || !imageFile.exists()) {
            return null
        }

        val filePath = imageFile.uri
        return try {
            context.contentResolver.openInputStream(filePath).use { inputStream ->
                val drawable = Drawable.createFromStream(inputStream, source)
                // Set bounds or you won't see the image if no width/height are specified
                drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                drawable
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /**
         * Converts an HTML string to a [Spanned] object capable of displaying inline images.
         * @param context The context used to open streams.
         * @param parentFolderUri The Uri of the folder containing the images (e.g., the protocol file's parent).
         * @param htmlText The HTML text.
         */
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
                null
            )
        }
    }
}
