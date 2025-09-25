package com.lkacz.pola

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MediaTagHandlerTest {
    @Before
    fun clearMap() {
        HtmlImageLoader.imageSizeMap.clear()
    }

    @Test
    fun video_and_audio_replaced() {
        val html = "<p>Test <video src=\"vid.mp4\"></video> and <audio src=\"sound.mp3\"></audio></p>"
        val handler = MediaTagHandler()
        val out = handler.beforeFromHtml(html)
        assertTrue(out.contains("[Video: vid.mp4]"))
        assertTrue(out.contains("[Audio: sound.mp3]"))
    }

    @Test
    fun image_dimensions_captured() {
        val html = "<img src=\"img.png\" width=\"120\" height=\"80\"/>"
        val handler = MediaTagHandler()
        handler.beforeFromHtml(html)
        val size = HtmlImageLoader.imageSizeMap["img.png"]
        assertEquals(120, size?.width)
        assertEquals(80, size?.height)
    }

    @Test
    fun image_tag_unmodified() {
        val html = "<p>Before <img src=\"img.png\" width=\"10\" height=\"20\"/> After</p>"
        val handler = MediaTagHandler()
        val out = handler.beforeFromHtml(html)
        assertTrue(out.contains("<img src=\"img.png\" width=\"10\" height=\"20\"/>"))
    }
}
