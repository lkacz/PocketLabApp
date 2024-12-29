package com.lkacz.pola

import android.text.Editable
import android.text.Html
import org.xml.sax.XMLReader
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Parses custom tags for images, video, and audio. For images, we extract optional 'width'
 * and 'height' attributes. For video and audio, we insert a bracketed placeholder
 * (e.g., "[Video: x.mp4]"). To center images, each <img> is wrapped in
 * <p style="text-align:center;">...</p>.
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
        // We handle everything by string manipulation inside beforeFromHtml().
    }

    /**
     * Pre-processes the HTML string to:
     * 1) Wrap each <img> in <p style="text-align:center;">...</p>.
     * 2) Extract and store <img> width/height in [HtmlImageLoader.imageSizeMap].
     * 3) Replace <video> and <audio> with a placeholder: "[Video: filename]"/"[Audio: filename]".
     */
    fun beforeFromHtml(html: String): String {
        var processedHtml = replaceWithCallback(html, imageTagPattern) { matcher ->
            val entireTag = matcher.group(0) ?: ""
            val srcMatcher = Pattern.compile("src\\s*=\\s*\"([^\"]+)\"").matcher(entireTag)
            if (!srcMatcher.find()) return@replaceWithCallback entireTag
            val srcValue = srcMatcher.group(1) ?: return@replaceWithCallback entireTag

            // Attempt to find optional width/height
            val wMatcher = widthPattern.matcher(entireTag)
            val hMatcher = heightPattern.matcher(entireTag)
            val width = if (wMatcher.find()) wMatcher.group(1)?.toIntOrNull() ?: 0 else 0
            val height = if (hMatcher.find()) hMatcher.group(1)?.toIntOrNull() ?: 0 else 0
            if (width > 0 && height > 0) {
                HtmlImageLoader.imageSizeMap[srcValue] = SizeInfo(width, height)
            }
            // Wrap in <p style="text-align:center;">
            "<p style=\"text-align:center;\">$entireTag</p>"
        }

        processedHtml = replaceWithCallback(processedHtml, videoTagPattern) { matcher ->
            val entireTag = matcher.group(0) ?: ""
            val srcMatcher = Pattern.compile("src\\s*=\\s*\"([^\"]+)\"").matcher(entireTag)
            if (!srcMatcher.find()) return@replaceWithCallback entireTag
            val srcValue = srcMatcher.group(1) ?: entireTag
            "[Video: $srcValue]"
        }

        processedHtml = replaceWithCallback(processedHtml, audioTagPattern) { matcher ->
            val entireTag = matcher.group(0) ?: ""
            val srcMatcher = Pattern.compile("src\\s*=\\s*\"([^\"]+)\"").matcher(entireTag)
            if (!srcMatcher.find()) return@replaceWithCallback entireTag
            val srcValue = srcMatcher.group(1) ?: entireTag
            "[Audio: $srcValue]"
        }

        return processedHtml
    }

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
