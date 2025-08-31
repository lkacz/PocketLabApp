// Filename: ParsingUtils.kt
package com.lkacz.pola

/**
 * Utility object that provides custom splitting logic to handle semicolons
 * only when outside of bracketed sections. This ensures bracketed content
 * like [Item1;Item2;Item3] remains intact as a single part.
 */
object ParsingUtils {
    /**
     * Splits [line] by semicolons that are NOT inside square brackets.
     * For example, the line:
     *   SCALE;Header;Intro;[Item1;Item2];Resp1;Resp2
     * will produce:
     *   ["SCALE", "Header", "Intro", "[Item1;Item2]", "Resp1", "Resp2"]
     */
    fun customSplitSemicolons(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var insideBracket = false

        for (char in line) {
            when (char) {
                '[' -> {
                    insideBracket = true
                    sb.append(char)
                }
                ']' -> {
                    insideBracket = false
                    sb.append(char)
                }
                ';' -> {
                    if (insideBracket) {
                        // Semicolon is inside brackets, do not split
                        sb.append(char)
                    } else {
                        // Outside bracket, treat as delimiter
                        result.add(sb.toString())
                        sb.setLength(0)
                    }
                }
                else -> {
                    sb.append(char)
                }
            }
        }
        // Add whatever remains (if any)
        if (sb.isNotEmpty()) {
            result.add(sb.toString())
        }
        return result
    }
}
