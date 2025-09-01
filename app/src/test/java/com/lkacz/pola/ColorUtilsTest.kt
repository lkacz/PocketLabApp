package com.lkacz.pola

import org.junit.Assert.*
import org.junit.Test

class ColorUtilsTest {
    @Test
    fun expands_short_rgb() {
        assertEquals("#AABBCC", ColorUtils.normalizeColorValue("#ABC"))
    }

    @Test
    fun expands_short_argb() {
        assertEquals("#FFAABBCC", ColorUtils.normalizeColorValue("#FABC"))
    }

    @Test
    fun named_color_resolved() {
        assertEquals("#FF0000", ColorUtils.normalizeColorValue("red"))
    }

    @Test
    fun already_valid_returned_uppercase() {
        assertEquals("#112233", ColorUtils.normalizeColorValue("#112233"))
        assertEquals("#FF112233", ColorUtils.normalizeColorValue("#ff112233"))
    }

    @Test
    fun invalid_returns_null() {
        assertNull(ColorUtils.normalizeColorValue("notAColor"))
        assertNull(ColorUtils.normalizeColorValue("#12"))
    }
}
