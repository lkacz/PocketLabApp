package com.lkacz.pola

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ProtocolTransformerTest {
    @Test
    fun mergesMultiLineInputField() {
        val protocol = """
            INPUTFIELD;
            Header Text;
            Body line;
            [A;B;C];
            Continue
        """.trimIndent()
        val transformed = ProtocolTransformer.transform(protocol, rnd = Random(0))
        val lines = transformed.lines().filter { it.isNotBlank() }
        assertEquals(1, lines.size)
        assertTrue(lines.first().startsWith("INPUTFIELD;Header Text;Body line;[A;B;C];Continue"))
    }

    @Test
    fun expandsScaleLineInOrder() {
        val protocol = "SCALE;Hdr;Intro;[One;Two;Three];Resp1;Resp2"
        val transformed = ProtocolTransformer.transform(protocol, rnd = Random(0))
        val lines = transformed.lines()
        assertEquals(3, lines.size)
        assertEquals("SCALE;Hdr;Intro;One;Resp1;Resp2", lines[0])
        assertEquals("SCALE;Hdr;Intro;Two;Resp1;Resp2", lines[1])
        assertEquals("SCALE;Hdr;Intro;Three;Resp1;Resp2", lines[2])
    }

    @Test
    fun expandsRandomizedScaleDeterministicallyWithSeed() {
        val protocol = "SCALE[RANDOMIZED];Hdr;Intro;[A;B;C];Resp"
        val transformedSeed0 = ProtocolTransformer.transform(protocol, rnd = Random(0))
        val transformedSeed1 = ProtocolTransformer.transform(protocol, rnd = Random(1))
        val lines0 = transformedSeed0.lines(); val lines1 = transformedSeed1.lines()
        assertEquals(3, lines0.size); assertEquals(3, lines1.size)
        val items0 = lines0.map { it.split(";")[3] }.toSet(); val items1 = lines1.map { it.split(";")[3] }.toSet()
        assertEquals(setOf("A","B","C"), items0); assertEquals(setOf("A","B","C"), items1)
    }

    @Test
    fun randomizeBlockShufflesDeterministically() {
        val protocol = """
            RANDOMIZE_ON
            LABEL;A
            LABEL;B
            LABEL;C
            RANDOMIZE_OFF
        """.trimIndent()
        val transformed = ProtocolTransformer.transform(protocol, rnd = Random(42))
    val lines = transformed.lines().filter { it.isNotBlank() }
    assertEquals(3, lines.size)
    assertEquals(setOf("LABEL;A","LABEL;B","LABEL;C"), lines.toSet())
    }
}
