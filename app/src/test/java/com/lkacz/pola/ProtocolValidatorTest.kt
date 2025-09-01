package com.lkacz.pola

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolValidatorTest {
    private val validator = ProtocolValidator()

    @Test
    fun detects_unrecognized_command() {
        val results = validator.validate(listOf("FOO;bar"))
        assertTrue(results.first().error.contains("Unrecognized"))
    }

    @Test
    fun detects_duplicate_label() {
        val lines = listOf("LABEL;A", "INSTRUCTION;H;B;OK", "LABEL;A")
        val results = validator.validate(lines)
        val dupErrors = results.filter { it.error.contains("duplicated") }
        assertEquals(2, dupErrors.size)
    }

    @Test
    fun randomize_on_without_off_added_at_eof() {
        val res = validator.validate(listOf("RANDOMIZE_ON"))
        assertTrue(res.last().error.contains("not closed"))
    }

    @Test
    fun timer_validation() {
        val res = validator.validate(listOf("TIMER;H;B;10;GO"))
        assertTrue(res.first().error.isEmpty())
    }

    @Test
    fun alignment_invalid_detected() {
        val res = validator.validate(listOf("BODY_ALIGNMENT;DIAGONAL"))
        assertTrue(res.first().error.contains("invalid alignment"))
    }

    @Test
    fun size_and_color_validation() {
        val res = validator.validate(listOf(
            "BODY_SIZE;-5",
            "HEADER_COLOR;blue",
            "RESPONSE_TEXT_COLOR;#FFAABB",
        ))
        assertTrue(res[0].error.contains("positive number"))
        assertTrue(res[1].error.contains("hex color"))
        assertTrue(res[2].error.isEmpty())
    }

    @Test
    fun inputfield_randomized_warnings() {
        val res = validator.validate(listOf("INPUTFIELD[RANDOMIZED];H;B;onlyOne"))
        assertTrue(res.first().warning.contains("fewer than 2"))
    }

    @Test
    fun timer_large_value_warns() {
        val res = validator.validate(listOf("TIMER;H;B;5000;GO"))
        assertTrue(res.first().warning.contains("over 3600"))
    }

    @Test
    fun study_id_duplicate_and_missing_value() {
        val res = validator.validate(listOf(
            "STUDY_ID;", // missing value
            "STUDY_ID;ABC123" // duplicate
        ))
        assertTrue(res[0].error.contains("missing"))
        assertTrue(res[1].error.contains("Duplicate"))
    }

    @Test
    fun randomize_off_without_on_errors() {
        val res = validator.validate(listOf("RANDOMIZE_OFF"))
        assertTrue(res[0].error.contains("without matching"))
    }

    @Test
    fun stray_semicolon_detected() {
        val res = validator.validate(listOf("LABEL;A;"))
        assertTrue(res[0].error.contains("stray semicolon"))
    }

    @Test
    fun eight_digit_color_and_large_size_warning() {
        val res = validator.validate(listOf(
            "BODY_COLOR;#FF112233",
            "BODY_SIZE;250" // should warn as unusually large
        ))
        assertTrue("8-digit color should be accepted", res[0].error.isEmpty())
        assertTrue(res[1].warning.contains("unusually large"))
    }
}
