package com.lkacz.pola

import android.text.Editable
import android.text.Html
import org.xml.sax.XMLReader
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Parses custom tags for images (both the standard <img src="..."> form and the shorter <file.jpg,...> form),
 * as well as for <video> and <audio>. For images, we extract optional 'width' and 'height' attributes (or from
 * the short form). For video and audio, we insert a bracketed placeholder (e.g., "[Video: x.mp4]").
 * To center images, each <img> is wrapped in <p style="text-align:center;">...</p>.
 *
 * Revisions for short images:
 *  - Added [convertShortImageTags] to transform a short image reference like <map.jpg,200,300>
 *    into a proper HTML tag: <img src="map.jpg" width="200" height="300">.
 *  - The pattern also handles cases like <map.jpg> without dimensions, turning them into
 *    <img src="map.jpg"/>.
 *  - Width/height are stored in [HtmlImageLoader.imageSizeMap], so the loader can size them accordingly.
 */
class MediaTagHandler : Html.TagHandler {

    // Matches an <img ...> tag with src="...".
    private val imageTagPattern = Pattern.compile(
        "<img\\s+[^>]*src\\s*=\\s*\"([^\"]+)\"[^>]*>",
        Pattern.CASE_INSENSITIVE
    )

    // Matches width="123" or height="123" inside <img>.
    private val widthPattern = Pattern.compile("width\\s*=\\s*\"(\\d+)\"", Pattern.CASE_INSENSITIVE)
    private val heightPattern = Pattern.compile("height\\s*=\\s*\"(\\d+)\"", Pattern.CASE_INSENSITIVE)

    // Matches <video src="..."> tags (for later replacement with a bracketed placeholder).
    private val videoTagPattern = Pattern.compile(
        "<video\\s+[^>]*src\\s*=\\s*\"([^\"]+)\"[^>]*>",
        Pattern.CASE_INSENSITIVE
    )

    // Matches <audio src="..."> tags (for later replacement with a bracketed placeholder).
    private val audioTagPattern = Pattern.compile(
        "<audio\\s+[^>]*src\\s*=\\s*\"([^\"]+)\"[^>]*>",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Matches short-image tags within angle brackets, for example:
     *
     *   <picture.jpg>
     *   <picture.jpeg,200,300>
     *   <something.png,100,150>
     *
     * Captures the filename plus optional ",width,height" part in group(1).
     *
     * - This pattern currently handles .png / .jpg / .jpeg. You can extend it
     *   (e.g., add `|gif|bmp|webp`) if needed.
     */
    private val shortImagePattern = Pattern.compile(
        "<([^>]+\\.(?:png|jpe?g)(?:,[^>]+)?)>",
        Pattern.CASE_INSENSITIVE
    )

    override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
        // We do not parse the tags directly here.
        // All transformations happen in [beforeFromHtml].
    }

    /**
     * Pre-process the HTML string to:
     *  1) Convert short-image tags (e.g. <map.jpg,200,300>) into the standard <img src="map.jpg" width="200" height="300">
     *     wrapped in <p style="text-align:center;"> ... </p>.
     *  2) Wrap any existing <img src="..."> in a centered paragraph if not already.
     *  3) Replace <video> and <audio> tags with placeholders like "[Video: file.mp4]" or "[Audio: file.mp3]".
     */
    fun beforeFromHtml(html: String): String {
        // First convert short <file.jpg[,W,H]> forms into standard <img src="file.jpg" width="W" height="H">.
        var processedHtml = convertShortImageTags(html)

        // Then handle existing <img ...> tags: center them, and capture any explicit width/height for later.
        processedHtml = replaceWithCallback(processedHtml, imageTagPattern) { matcher ->
            val entireTag = matcher.group(0) ?: return@replaceWithCallback ""
            val srcValue = extractImgSrc(entireTag) ?: return@replaceWithCallback entireTag

            // Attempt to find optional width/height
            val w = extractWidth(entireTag)
            val h = extractHeight(entireTag)
            if (w > 0 && h > 0) {
                HtmlImageLoader.imageSizeMap[srcValue] = SizeInfo(w, h)
            }
            // Return the centered version
            "<p style=\"text-align:center;\">$entireTag</p>"
        }

        // Replace <video ...> with a bracketed placeholder, e.g., [Video: sample.mp4]
        processedHtml = replaceWithCallback(processedHtml, videoTagPattern) { matcher ->
            val entireTag = matcher.group(0) ?: ""
            val srcValue = extractSrcAttribute(entireTag) ?: entireTag
            "[Video: $srcValue]"
        }

        // Replace <audio ...> with a bracketed placeholder, e.g., [Audio: sample.mp3]
        processedHtml = replaceWithCallback(processedHtml, audioTagPattern) { matcher ->
            val entireTag = matcher.group(0) ?: ""
            val srcValue = extractSrcAttribute(entireTag) ?: entireTag
            "[Audio: $srcValue]"
        }

        return processedHtml
    }

    /**
     * Converts the short form <filename.jpg[,width,height]> into <p style="text-align:center;">
     * <img src="filename.jpg" width="width" height="height" />
     * </p>.
     *
     * If width/height are missing, we omit them from the <img> tag.
     */
    private fun convertShortImageTags(originalHtml: String): String {
        return replaceWithCallback(originalHtml, shortImagePattern) { matcher ->
            val fullMatch = matcher.group(1) ?: ""
            // Possible forms:
            //   "map.jpg"
            //   "map.jpg,400,300"
            //   "pic.jpeg"
            //   "pic.png,200,300"
            val segments = fullMatch.split(",").map { it.trim() }
            val fileName = segments[0]
            var width = 0
            var height = 0

            if (segments.size == 3) {
                width = segments[1].toIntOrNull() ?: 0
                height = segments[2].toIntOrNull() ?: 0
            }

            // If we have valid width/height, store them so the loader can apply sizing
            if (width > 0 && height > 0) {
                HtmlImageLoader.imageSizeMap[fileName] = SizeInfo(width, height)
                """<p style="text-align:center;"><img src="$fileName" width="$width" height="$height" /></p>"""
            } else {
                """<p style="text-align:center;"><img src="$fileName" /></p>"""
            }
        }
    }

    // Extracts the src attribute from an <img ...> tag
    private fun extractImgSrc(imgTag: String): String? {
        val srcMatcher = Pattern.compile("src\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(imgTag)
        return if (srcMatcher.find()) srcMatcher.group(1) else null
    }

    private fun extractWidth(imgTag: String): Int {
        val wMatcher = widthPattern.matcher(imgTag)
        return if (wMatcher.find()) wMatcher.group(1)?.toIntOrNull() ?: 0 else 0
    }

    private fun extractHeight(imgTag: String): Int {
        val hMatcher = heightPattern.matcher(imgTag)
        return if (hMatcher.find()) hMatcher.group(1)?.toIntOrNull() ?: 0 else 0
    }

    // Extracts the src attribute from a <video> or <audio> tag
    private fun extractSrcAttribute(tag: String): String? {
        val srcMatcher = Pattern.compile("src\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(tag)
        return if (srcMatcher.find()) srcMatcher.group(1) else null
    }

    /**
     * Generic helper to apply a callback-based replacement for pattern matches.
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
