package com.lkacz.pola

import org.junit.Assert.assertEquals
import org.junit.Test

class ParsingUtilsTest {
    @Test
    fun customSplitSemicolons_basic() {
        val line = "SCALE;Header;Intro;[One;Two];Resp1;Resp2"
        val parts = ParsingUtils.customSplitSemicolons(line)
        assertEquals(listOf("SCALE","Header","Intro","[One;Two]","Resp1","Resp2"), parts)
    }

    @Test
    fun customSplitSemicolons_nestedBracketsIgnored() {
        val line = "INPUTFIELD;Title;Body;[A;B;C];Continue"
        val parts = ParsingUtils.customSplitSemicolons(line)
        assertEquals(listOf("INPUTFIELD","Title","Body","[A;B;C]","Continue"), parts)
    }
}