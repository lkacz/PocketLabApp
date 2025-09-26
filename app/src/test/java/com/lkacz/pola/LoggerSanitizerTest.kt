package com.lkacz.pola

import org.junit.Assert.assertEquals
import org.junit.Test

class LoggerSanitizerTest {
    @Test
    fun `sanitizeStudyIdForFile removes forbidden characters`() {
        val raw = " Study ID #42! "
        val sanitized = sanitizeStudyIdForFile(raw)
        assertEquals("Study_ID__42_", sanitized)
    }

    @Test
    fun `formatCsvRow normalizes whitespace`() {
        val row = formatCsvRow(listOf("Hello\tWorld", "Line\nBreak", null))
        assertEquals("Hello World\tLine Break\t\n", row)
    }
}
