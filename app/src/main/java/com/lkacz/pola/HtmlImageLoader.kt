package com.lkacz.pola

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Editable
import android.text.Html
import android.text.Spanned
import androidx.core.text.HtmlCompat
import androidx.documentfile.provider.DocumentFile
import org.xml.sax.XMLReader
import java.util.regex.Matcher
import java.util.regex.Pattern

class HtmlImageLoader private constructor(
    private val context: Context,
    private val parentFolderUri: Uri?
) : Html.ImageGetter {

    override fun getDrawable(source: String?): Drawable? {
        if (source.isNullOrBlank()) return null

        // Retrieve and remove any stored width/height for this source.
        val sizeInfo = imageSizeMap.remove(source)

        val parentFolder = parentFolderUri?.let { DocumentFile.fromTreeUri(context, it) }
            ?: return null
        val imageFile = parentFolder.findFile(source)
        if (imageFile == null || !imageFile.isFile || !imageFile.exists()) return null

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
        /**
         * Internal map used by [MediaTagHandler] to temporarily store <img> width/height,
         * keyed by the image 'src' attribute. This map is read and cleared in [getDrawable].
         */
        internal val imageSizeMap = mutableMapOf<String, SizeInfo>()

        /**
         * Produces a [Spanned] from HTML that handles:
         *   - <img> with optional width/height (centered).
         *   - <video> and <audio> placeholders.
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

/**
 * Data class for storing optional width/height attributes from <img>.
 */
internal data class SizeInfo(val width: Int, val height: Int)

/**
 * Parses custom tags for images, video, and audio. For images, we extract optional 'width' and
 * 'height' attributes. For video and audio, we insert a bracketed placeholder (e.g., "[Video: x.mp4]").
 * To center images, each <img> is wrapped in <p style="text-align:center;">...</p>.
 *
 * If inline video/audio is required, use a WebView instead.
 */
class MediaTagHandler : Html.TagHandler {

    private val imageTagPattern = Pattern.compile(
        "<img\\s+[^>]*src\\s*=\\s*\"([^\"]+)\"[^>]*>",
        Pattern.CASE_INSENSITIVE
    )
    private val widthPattern = Pattern.compile("width\\s*=\\s*\"(\\d+)\"", Pattern.CASE_INSENSITIVE)
    private val heightPattern = Pattern.compile("height\\s*=\\s*\"(\\d+)\"", Pattern.CASE_INSENSITIVE)

    private val videoTagPattern = Pattern.compile(
        "<video\\s+[^>]*src\\s*=\\s*\"([^\"]+)\"[^>]*>",
        Pattern.CASE_INSENSITIVE
    )
    private val audioTagPattern = Pattern.compile(
        "<audio\\s+[^>]*src\\s*=\\s*\"([^\"]+)\"[^>]*>",
        Pattern.CASE_INSENSITIVE
    )

    override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
        // No operation here. We'll process <img>, <video>, and <audio> below by string-level manipulation.
    }

    /**
     * Pre-processes the HTML string to:
     * 1) Wrap each <img> in <p style="text-align:center;">...</p>.
     * 2) Extract and store <img> width/height in [HtmlImageLoader.imageSizeMap].
     * 3) Replace <video> and <audio> with a placeholder, e.g., "[Video: xyz.mp4]".
     *
     * We do not rely on newer Java APIs like String::replaceAll with a lambda.
     * Instead, we use [Matcher.appendReplacement]/[Matcher.appendTail].
     *
     * We also handle potential null returns from [Matcher.group].
     */
    fun beforeFromHtml(html: String): String {
        // Handle <img> tags
        var processedHtml = replaceWithCallback(html, imageTagPattern) { matcher ->
            val entireTag = matcher.group(0)
            if (entireTag.isNullOrBlank()) {
                // If group(0) is null or empty, safely return empty
                return@replaceWithCallback ""
            }

            // Attempt to find the 'src' attribute
            val srcMatcher = Pattern.compile("src\\s*=\\s*\"([^\"]+)\"").matcher(entireTag)
            if (!srcMatcher.find()) {
                // If no 'src' is found, just return the entire tag
                return@replaceWithCallback entireTag
            }
            val srcValue = srcMatcher.group(1) ?: return@replaceWithCallback entireTag

            // Extract optional width/height
            val widthMatcher = widthPattern.matcher(entireTag)
            val heightMatcher = heightPattern.matcher(entireTag)
            val width = if (widthMatcher.find()) widthMatcher.group(1)?.toIntOrNull() ?: 0 else 0
            val height = if (heightMatcher.find()) heightMatcher.group(1)?.toIntOrNull() ?: 0 else 0
            if (width > 0 && height > 0) {
                HtmlImageLoader.imageSizeMap[srcValue] = SizeInfo(width, height)
            }

            // Wrap the final <img> in <p style="text-align:center;">
            "<p style=\"text-align:center;\"><img src=\"$srcValue\"/></p>"
        }

        // Replace <video> tags
        processedHtml = replaceWithCallback(processedHtml, videoTagPattern) { matcher ->
            val entireTag = matcher.group(0)
            if (entireTag.isNullOrEmpty()) return@replaceWithCallback ""
            val srcMatcher = Pattern.compile("src\\s*=\\s*\"([^\"]+)\"").matcher(entireTag)
            if (!srcMatcher.find()) return@replaceWithCallback entireTag
            val srcValue = srcMatcher.group(1) ?: return@replaceWithCallback entireTag
            "[Video: $srcValue]"
        }

        // Replace <audio> tags
        processedHtml = replaceWithCallback(processedHtml, audioTagPattern) { matcher ->
            val entireTag = matcher.group(0)
            if (entireTag.isNullOrEmpty()) return@replaceWithCallback ""
            val srcMatcher = Pattern.compile("src\\s*=\\s*\"([^\"]+)\"").matcher(entireTag)
            if (!srcMatcher.find()) return@replaceWithCallback entireTag
            val srcValue = srcMatcher.group(1) ?: return@replaceWithCallback entireTag
            "[Audio: $srcValue]"
        }

        return processedHtml
    }

    /**
     * Helper to mimic Java 8's String::replaceAll using a callback for each match.
     * This is compatible with older APIs. It appends the replaced content into a buffer
     * and returns the new string at the end.
     *
     * We also explicitly check for null or empty matches when calling [Matcher.group].
     */
    private fun replaceWithCallback(
        original: String,
        pattern: Pattern,
        replacer: (Matcher) -> String
    ): String {
        val matcher = pattern.matcher(original)
        val sb = StringBuffer()
        while (matcher.find()) {
            val replacement = replacer(matcher)
            matcher.appendReplacement(sb, replacement)
        }
        matcher.appendTail(sb)
        return sb.toString()
    }
}

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
        rawHtml: String?
    ): Spanned {
        if (rawHtml.isNullOrEmpty()) {
            return HtmlCompat.fromHtml("", HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
        val handler = MediaTagHandler()
        val refinedHtml = handler.beforeFromHtml(rawHtml)
        return HtmlImageLoader.getSpannedFromHtml(context, parentFolderUri, refinedHtml)
    }
}
