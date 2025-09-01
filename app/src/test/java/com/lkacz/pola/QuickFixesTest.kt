package com.lkacz.pola

import org.junit.Assert.*
import org.junit.Test

class QuickFixesTest {
    @Test
    fun removes_trailing_semicolons() {
        val lines = listOf("LABEL;X;", "INSTRUCTION;H;B;C")
        val res = QuickFixes.removeStraySemicolons(lines)
        assertEquals(1, res.changedCount)
        assertEquals("LABEL;X", res.lines.first())
    }

    @Test
    fun removes_duplicate_study_ids() {
        val lines = listOf("STUDY_ID;A", "INSTRUCTION;H;B;C", "STUDY_ID;B")
        val res = QuickFixes.removeDuplicateStudyIds(lines)
        assertEquals(1, res.changedCount)
        assertEquals(2, res.lines.size)
    }

    @Test
    fun removes_duplicate_labels() {
        val lines = listOf("LABEL;A", "LABEL;A", "LABEL;B")
        val res = QuickFixes.removeDuplicateLabels(lines)
        assertEquals(1, res.changedCount)
        assertEquals(listOf("LABEL;A", "LABEL;B"), res.lines)
    }

    @Test
    fun inserts_missing_goto_labels() {
        val lines = listOf("GOTO;DEST", "INSTRUCTION;H;B;C")
        val res = QuickFixes.insertMissingGotoLabels(lines)
        assertEquals(1, res.changedCount)
        assertTrue(res.lines.contains("LABEL;DEST"))
        val idxGoto = res.lines.indexOfFirst { it.startsWith("GOTO;DEST") }
        val idxLabel = res.lines.indexOfFirst { it.startsWith("LABEL;DEST") }
        assertEquals(idxGoto + 1, idxLabel)
    }

    @Test
    fun normalizes_timer_lines() {
        val lines = listOf("TIMER;H;B;-5;Go", "TIMER;H;B;30;Go")
        val res = QuickFixes.normalizeTimerLines(lines)
        assertEquals(1, res.changedCount)
        assertTrue(res.lines.first().startsWith("TIMER;H;B;60;"))
    }

    @Test
    fun normalizes_timer_edge_cases() {
        val lines = listOf(
            "TIMER",                    // no segments
            "TIMER;H",                  // 1 segment after command
            "TIMER;H;B",                // 2 segments
            "TIMER;H;B;XYZ;",           // non-numeric time and empty continue
            "TIMER;H;B;9999999;CONT"    // huge time (kept but validator may warn)
        )
        val res = QuickFixes.normalizeTimerLines(lines)
    // At least first three must be changed (they were incomplete)
    assertTrue(res.changedCount >= 3)
        res.lines.forEach { l ->
            assertTrue(l.startsWith("TIMER;"))
            assertEquals(5, l.count { it == ';' } + 1)
        }
    }

    @Test
    fun normalizes_colors() {
        val lines = listOf("BODY_COLOR;#abc", "HEADER_COLOR;#112233")
        val res = QuickFixes.normalizeColors(lines)
        assertEquals(1, res.changedCount)
        assertEquals("BODY_COLOR;#AABBCC", res.lines.first())
    }

    @Test
    fun normalizes_content_commands() {
        val lines = listOf("INSTRUCTION;H", "SCALE", "INPUTFIELD;H;B")
        val res = QuickFixes.normalizeContentCommands(lines)
        assertEquals(3, res.changedCount)
        assertTrue(res.lines[0].startsWith("INSTRUCTION;H;"))
        assertTrue(res.lines[1].startsWith("SCALE;Header;Body;1;"))
        assertTrue(res.lines[2].startsWith("INPUTFIELD;H;B;field1;"))
    }

    @Test
    fun applies_safe_auto_fixes_aggregate() {
        val lines = listOf(
            "LABEL;A;",           // stray semicolon
            "BODY_COLOR;#abc",    // short color
            "TIMER;H;B;-5;Go",    // bad timer value
            "INSTRUCTION;OnlyHeader", // malformed instruction
            "LABEL;A",            // duplicate label
            "GOTO;MISSING"        // missing target label
        )
        val agg = QuickFixes.applySafeAutoFixes(lines)
        assertTrue(agg.totalChanges > 0)
        // Ensure each category accounted for (some maybe zero if input not triggering all)
        assertTrue(agg.breakdown.containsKey("straySemicolons"))
        assertTrue(agg.breakdown.containsKey("colors"))
        assertTrue(agg.breakdown.containsKey("timers"))
        assertTrue(agg.breakdown.containsKey("content"))
        assertTrue(agg.breakdown.containsKey("duplicateLabels"))
        assertTrue(agg.breakdown.containsKey("missingLabels"))
        // After fixes, ensure duplicate label removed
        val labelCountA = agg.lines.count { it.startsWith("LABEL;A") }
        assertEquals(1, labelCountA)
        // Missing label inserted after GOTO
        val idxGoto = agg.lines.indexOfFirst { it.startsWith("GOTO;MISSING") }
        val idxInserted = agg.lines.indexOfFirst { it.startsWith("LABEL;MISSING") }
        assertEquals(idxGoto + 1, idxInserted)
    }
}
