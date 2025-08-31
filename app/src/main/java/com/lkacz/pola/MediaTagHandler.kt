package com.lkacz.pola

import android.text.Editable
import android.text.Html
import org.xml.sax.XMLReader
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Restored a simpler version of the tag handler that:
 * 1) Captures width/height in <img width="..." height="..."> and puts it into [HtmlImageLoader.imageSizeMap].
 * 2) Leaves the <img src="..."> tag alone (no forced centering or fancy short syntax).
 * 3) Converts <video ...> and <audio ...> tags into placeholders [Video: ...] or [Audio: ...].
 *
 * Reasoning:
 * - The prior forced-centering or short-image conversions occasionally broke normal <img> usage.
 *   This returns to the older logic that was known to display images properly.
 */
class MediaTagHandler : Html.TagHandler {
    // Regex to detect <img src="..."> for optional width/height extraction:
    private val imageTagPattern =
        Pattern.compile(
            "<img\\s+[^>]*src\\s*=\\s*\"([^\"]+)\"[^>]*>",
            Pattern.CASE_INSENSITIVE,
        )
    private val widthPattern = Pattern.compile("width\\s*=\\s*\"(\\d+)\"", Pattern.CASE_INSENSITIVE)
    private val heightPattern = Pattern.compile("height\\s*=\\s*\"(\\d+)\"", Pattern.CASE_INSENSITIVE)

    // Regex for <video> and <audio>, replaced with placeholders:
    private val videoTagPattern =
        Pattern.compile(
            "<video\\s+[^>]*src\\s*=\\s*\"([^\"]+)\"[^>]*>",
            Pattern.CASE_INSENSITIVE,
        )
    private val audioTagPattern =
        Pattern.compile(
            "<audio\\s+[^>]*src\\s*=\\s*\"([^\"]+)\"[^>]*>",
            Pattern.CASE_INSENSITIVE,
        )

    override fun handleTag(
        opening: Boolean,
        tag: String,
        output: Editable,
        xmlReader: XMLReader,
    ) {
        // No direct in-processor logic here, everything is done via beforeFromHtml pre-processing.
    }

    /**
     * Called before Html.fromHtml(...) to parse <img> width/height, as well as
     * turn <video>/<audio> into text placeholders. Leaves the rest unchanged.
     */
    fun beforeFromHtml(html: String): String {
        var modifiedHtml = html

        // 1) Replace <video> with placeholders:
        modifiedHtml =
            replaceWithCallback(modifiedHtml, videoTagPattern) { match ->
                val entireTag = match.group(0) ?: return@replaceWithCallback ""
                val srcValue = extractSrcAttribute(entireTag)
                "[Video: $srcValue]"
            }

        // 2) Replace <audio> with placeholders:
        modifiedHtml =
            replaceWithCallback(modifiedHtml, audioTagPattern) { match ->
                val entireTag = match.group(0) ?: return@replaceWithCallback ""
                val srcValue = extractSrcAttribute(entireTag)
                "[Audio: $srcValue]"
            }

        // 3) Parse <img> for optional width/height; store in [HtmlImageLoader.imageSizeMap].
        modifiedHtml =
            replaceWithCallback(modifiedHtml, imageTagPattern) { match ->
                val fullImgTag = match.group(0) ?: return@replaceWithCallback ""
                val srcValue = extractSrcAttribute(fullImgTag) ?: return@replaceWithCallback fullImgTag

                val w = extractDimension(fullImgTag, widthPattern)
                val h = extractDimension(fullImgTag, heightPattern)
                if (w > 0 && h > 0) {
                    HtmlImageLoader.imageSizeMap[srcValue] = SizeInfo(w, h)
                }
                // Return the original <img> tag unmodified, so it displays in normal flow.
                fullImgTag
            }

        return modifiedHtml
    }

    private fun extractDimension(
        tag: String,
        pattern: Pattern,
    ): Int {
        val matcher = pattern.matcher(tag)
        return if (matcher.find()) matcher.group(1)?.toIntOrNull() ?: 0 else 0
    }

    private val srcAttrPattern = Pattern.compile("src\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)
    private fun extractSrcAttribute(tag: String): String {
        val srcMatcher = srcAttrPattern.matcher(tag)
        return if (srcMatcher.find()) srcMatcher.group(1)!! else ""
    }

    private fun replaceWithCallback(
        original: String,
        pattern: Pattern,
        replacer: (Matcher) -> String,
    ): String {
        val matcher = pattern.matcher(original)
        val sb = StringBuffer()
        while (matcher.find()) {
            val replacement = java.util.regex.Matcher.quoteReplacement(replacer(matcher))
            matcher.appendReplacement(sb, replacement)
        }
        matcher.appendTail(sb)
        return sb.toString()
    }
}
