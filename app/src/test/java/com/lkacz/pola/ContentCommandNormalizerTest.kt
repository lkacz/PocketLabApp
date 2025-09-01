package com.lkacz.pola

import org.junit.Assert.*
import org.junit.Test

class ContentCommandNormalizerTest {
    @Test
    fun normalizes_malformed_instruction() {
        val original = "INSTRUCTION;OnlyHeader"
        val normalized = ContentCommandNormalizer.normalize(original)
        assertEquals("INSTRUCTION;OnlyHeader;Body;Continue".replace("OnlyHeader","OnlyHeader"), normalized)
    }

    @Test
    fun does_not_change_valid_instruction() {
        val original = "INSTRUCTION;H;B;Go"
        assertNull(ContentCommandNormalizer.normalize(original))
    }

    @Test
    fun scale_with_no_params_normalized() {
        val original = "SCALE"
        val normalized = ContentCommandNormalizer.normalize(original)
        assertEquals("SCALE;Header;Body;1;Continue", normalized)
    }

    @Test
    fun inputfield_with_insufficient_segments_normalized() {
        val original = "INPUTFIELD;H;B" // only 3 segments (needs >=4)
        val normalized = ContentCommandNormalizer.normalize(original)
        assertEquals("INPUTFIELD;Header;Body;field1;Continue".replace("Header","H").replace("Body","B"), normalized)
    }
}
