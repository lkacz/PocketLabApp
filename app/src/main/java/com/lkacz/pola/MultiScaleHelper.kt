package com.lkacz.pola

import kotlin.random.Random

/**
 * Handles parsing lines starting with MULTISCALE or RANDOMIZED_MULTISCALE,
 * expanding them into individual SCALE instructions.
 */
object MultiScaleHelper {

    /**
     * Expands a single MULTISCALE or RANDOMIZED_MULTISCALE line into multiple SCALE instructions.
     *
     * @param line The raw line from the protocol (e.g., MULTISCALE;HEADER;INTRO;[item1;item2;item3];Possible;Responses).
     * @return A list of SCALE instructions, optionally randomized if RANDOMIZED_MULTISCALE.
     */
    fun expandMultiScaleLine(line: String): List<String> {
        val linesToAdd = mutableListOf<String>()
        val isRandom = line.trim().startsWith("RANDOMIZED_MULTISCALE")

        val bracketStart = line.indexOf('[')
        val bracketEnd = line.indexOf(']')
        if (bracketStart < 0 || bracketEnd < 0 || bracketEnd <= bracketStart) return listOf(line)

        val preBracket = line.substring(0, bracketStart).split(';')
        val multiItems = line.substring(bracketStart + 1, bracketEnd).split(';')
        val postBracket = line.substring(bracketEnd + 1).split(';').drop(1)  // Drop leading empty string after bracket

        val header = preBracket.getOrNull(1).orEmpty()
        val introduction = preBracket.getOrNull(2).orEmpty()
        val responses = postBracket.joinToString(";")

        for (item in multiItems) {
            val trimmedItem = item.trim()
            if (trimmedItem.isNotEmpty()) {
                // Create a SCALE instruction
                val newLine = "SCALE;$header;$introduction;$trimmedItem;$responses"
                linesToAdd.add(newLine)
            }
        }

        if (isRandom) {
            // Randomize the expanded SCALE instructions
            linesToAdd.shuffle(Random)
        }

        return linesToAdd
    }
}
