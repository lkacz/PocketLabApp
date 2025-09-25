package com.lkacz.pola

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MultiScaleHelperTest {
    @Test
    fun noExpansionWhenSingleItem() {
        val line = "SCALE;Hdr;Intro;[OnlyOne];Resp1;Resp2"
        val result = MultiScaleHelper.expandScaleLine(line, rnd = Random(0))
        assertEquals(1, result.size)
        assertEquals(line, result.first())
    }

    @Test
    fun expandsMultipleItemsInOrderForNormalScale() {
        val line = "SCALE;Hdr;Intro;[A;B;C];Resp"
        val result = MultiScaleHelper.expandScaleLine(line, rnd = Random(0))
        assertEquals(
            listOf(
                "SCALE;Hdr;Intro;A;Resp",
                "SCALE;Hdr;Intro;B;Resp",
                "SCALE;Hdr;Intro;C;Resp",
            ),
            result,
        )
    }

    @Test
    fun randomizedScaleShufflesButKeepsAllItems() {
        val line = "SCALE[RANDOMIZED];Hdr;Intro;[A;B;C;D];Resp"
        val resultSeed0 = MultiScaleHelper.expandScaleLine(line, rnd = Random(0))
        val resultSeed1 = MultiScaleHelper.expandScaleLine(line, rnd = Random(1))
        assertEquals(4, resultSeed0.size)
        assertEquals(4, resultSeed1.size)
        val items0 = resultSeed0.map { it.split(';')[3] }.toSet()
        val items1 = resultSeed1.map { it.split(';')[3] }.toSet()
        assertEquals(setOf("A", "B", "C", "D"), items0)
        assertEquals(setOf("A", "B", "C", "D"), items1)
        // High likelihood orders differ (non-deterministic assertion avoided)
        assertTrue(resultSeed0 != resultSeed1)
    }
}
