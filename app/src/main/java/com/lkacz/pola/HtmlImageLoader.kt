package com.lkacz.pola

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Html
import android.text.Spanned
import androidx.core.text.HtmlCompat
import androidx.documentfile.provider.DocumentFile

/**
 * Revised HtmlImageLoader with recursive file lookup to ensure images are found even if
 * they reside in nested subdirectories. Now, paths like "folder/pic.jpg" or "assets\\pic.jpg"
 * are handled properly by splitting on '/' or '\' and descending subfolders.
 */
class HtmlImageLoader private constructor(
    private val context: Context,
    private val parentFolderUri: Uri?
) : Html.ImageGetter {

    override fun getDrawable(source: String?): Drawable? {
        if (source.isNullOrBlank()) return null

        // Retrieve any stored dimensions for this source
        val sizeInfo = imageSizeMap.remove(source)

        // Try to find the file in the top-level or subfolders
        val parentFolder = parentFolderUri?.let { DocumentFile.fromTreeUri(context, it) } ?: return null
        val imageFile = findFileRecursive(parentFolder, source) ?: return null
        if (!imageFile.exists() || !imageFile.isFile) return null

        return try {
            context.contentResolver.openInputStream(imageFile.uri).use { inputStream ->
                val drawable = Drawable.createFromStream(inputStream, source)
                drawable?.apply {
                    if (sizeInfo != null && sizeInfo.width > 0 && sizeInfo.height > 0) {
                        setBounds(0, 0, sizeInfo.width, sizeInfo.height)
                    } else {
                        // Use intrinsic sizes if custom dimensions are not specified
                        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Splits [filePath] by '/' or '\' and descends into subfolders to locate the DocumentFile.
     */
    private fun findFileRecursive(folder: DocumentFile, filePath: String): DocumentFile? {
        val segments = filePath.split('/', '\\').filter { it.isNotBlank() }
        return findFileBySegments(folder, segments)
    }

    private fun findFileBySegments(folder: DocumentFile, segments: List<String>): DocumentFile? {
        if (segments.isEmpty()) return null
        val firstSegment = segments.first()
        val child = folder.listFiles().firstOrNull { it.name == firstSegment } ?: return null
        return if (segments.size == 1) {
            child
        } else {
            if (child.isDirectory) {
                findFileBySegments(child, segments.drop(1))
            } else null
        }
    }

    companion object {
        internal val imageSizeMap = mutableMapOf<String, SizeInfo>()

        /**
         * Converts raw HTML into a Spanned object using our revised [HtmlImageLoader].
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
                MediaTagHandler()
            )
        }
    }
}

data class SizeInfo(val width: Int, val height: Int)
