package com.lkacz.pola

import org.junit.Assert.assertEquals
import org.junit.Test

class LoggerUtilsTest {

    @Test
    fun sanitizeStudyIdForFile_replacesUnsafeCharacters() {
        val result = sanitizeStudyIdForFile(" Study 123/ABC#Test ")
        assertEquals("Study_123_ABC_Test", result)
    }

    @Test
    fun sanitizeStudyIdForFile_preservesSafeCharacters() {
        val result = sanitizeStudyIdForFile("Participant-01_A")
        assertEquals("Participant-01_A", result)
    }

    @Test
    fun formatCsvRow_sanitizesCellsAndJoinsWithTabs() {
        val output = formatCsvRow(listOf("Hello\nWorld", "Test\tValue", null))
        assertEquals("Hello World\tTest Value\t\n", output)
    }
}
