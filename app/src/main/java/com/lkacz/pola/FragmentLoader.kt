// Filename: FragmentLoader.kt
package com.lkacz.pola

import android.content.Context
import androidx.fragment.app.Fragment
import java.io.BufferedReader
import kotlin.random.Random

class FragmentLoader(
    bufferedReader: BufferedReader,
    private val logger: Logger
) {
    private val lines = mutableListOf<String>()
    private val labelMap = mutableMapOf<String, Int>()
    private var currentIndex = -1

    init {
        // 1) Read raw lines from the file:
        val rawLines = bufferedReader.readLines()

        // 2) Expand any line that includes multiple bracketed items [Item1;Item2;Item3]
        val expanded = mutableListOf<String>()
        for (originalLine in rawLines) {
            val trimmed = originalLine.trim()
            if (trimmed.isNotEmpty()) {
                expanded.addAll(expandMultiItemLine(trimmed))
            }
        }

        // 3) Store them
        lines.addAll(expanded)

        // 4) Build label map for GOTO jumps
        lines.forEachIndexed { index, line ->
            if (line.startsWith("LABEL;")) {
                val label = line.split(";").getOrNull(1)?.trim().orEmpty()
                if (label.isNotEmpty()) labelMap[label] = index
            }
        }

        // Optional: Log how many lines after expansion
        logger.logOther("FragmentLoader init: ${lines.size} lines after expanding multi-item lines.")
    }

    fun loadNextFragment(): Fragment {
        while (true) {
            currentIndex++
            if (currentIndex >= lines.size) {
                // No more lines => end
                return EndFragment()
            }
            val line = lines[currentIndex].trim()
            if (line.isBlank()) continue

            // Break into parts
            val parts = line.split(";").map { it.trim() }
            val directiveRaw = parts.firstOrNull()?.uppercase() ?: ""

            // Detect "SCALE[RANDOMIZED]" vs "SCALE"
            val isScaleRandom = directiveRaw.startsWith("SCALE[") &&
                    directiveRaw.contains("RANDOMIZED", ignoreCase = true)
            val directive = if (isScaleRandom) "SCALE_RANDOMIZED" else directiveRaw

            when (directive) {
                "LABEL" -> continue
                "GOTO" -> {
                    jumpToLabel(parts.getOrNull(1).orEmpty())
                    continue
                }
                "TRANSITIONS" -> {
                    val mode = parts.getOrNull(1)?.lowercase() ?: "off"
                    getContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
                        .edit().putString("TRANSITION_MODE", mode).apply()
                    continue
                }
                // ... handle other directives like TIMER_SOUND, HEADER_COLOR, etc. ...

                "SCALE", "SCALE_RANDOMIZED" -> {
                    return createScaleFragment(line, directive == "SCALE_RANDOMIZED")
                }

                "END" -> return EndFragment()

                else -> {
                    // Possibly unknown or an instruction, timer, etc.
                    // Call your existing methods or skip if unrecognized
                    continue
                }
            }
        }
    }

    fun jumpToLabelAndLoad(label: String): Fragment {
        jumpToLabel(label)
        return loadNextFragment()
    }

    private fun jumpToLabel(label: String) {
        val idx = labelMap[label] ?: -1
        if (idx < 0) {
            currentIndex = lines.size
        } else {
            currentIndex = idx - 1
        }
    }

    /**
     * expandMultiItemLine detects lines that match `SCALE...;[Item1;Item2;Item3];...`
     * and splits them into multiple single-item scale lines.
     */
    private fun expandMultiItemLine(originalLine: String): List<String> {
        val parts = originalLine.split(";").map { it.trim() }
        if (parts.size < 4) return listOf(originalLine) // Not enough segments to hold bracket items

        val first = parts[0].uppercase()
        if (!first.startsWith("SCALE")) return listOf(originalLine)

        // Check for bracket in the 4th part
        val itemsPart = parts[3]
        val bracketS = itemsPart.indexOf("[")
        val bracketE = itemsPart.indexOf("]")
        if (bracketS < 0 || bracketE <= bracketS) return listOf(originalLine)

        // Extract inside: e.g. [Pierwszy item;Drugi item;Trzeci item]
        val inside = itemsPart.substring(bracketS + 1, bracketE).trim()
        // e.g. "Pierwszy item;Drugi item;Trzeci item"
        val itemList = inside.split(";").map { it.trim() }.filter { it.isNotBlank() }

        // If <= 1 item, no expansion needed
        if (itemList.size <= 1) return listOf(originalLine)

        // We'll build multiple lines. Everything else except that bracket remains the same.
        val prefix = parts.take(3).joinToString(";")  // e.g. "SCALE;Title;Body"
        val responses = parts.drop(4).joinToString(";") // e.g. "Response 1;Response 2;Response 3"

        // For each item, create a new line
        val expandedLines = mutableListOf<String>()
        itemList.forEach { singleItem ->
            val bracketed = "[$singleItem]"
            val line =
                if (responses.isBlank()) "$prefix;$bracketed"
                else "$prefix;$bracketed;$responses"
            logger.logOther("Expanding multi-scale => $line") // For debugging
            expandedLines.add(line)
        }
        return expandedLines
    }

    /**
     * Now that each line has only one bracketed item, create a normal scale fragment.
     */
    private fun createScaleFragment(line: String, isRandom: Boolean): Fragment {
        val parts = line.split(";").map { it.trim() }
        val header = parts.getOrNull(1)
        val body   = parts.getOrNull(2)
        val itemPart = parts.getOrNull(3).orEmpty()
        val responses = parts.drop(4)

        // Single bracket item or none
        val bracketS = itemPart.indexOf("[")
        val bracketE = itemPart.indexOf("]")
        val singleItem = if (bracketS >= 0 && bracketE > bracketS) {
            itemPart.substring(bracketS + 1, bracketE).trim()
        } else itemPart

        // Check if random => typically meaningless with only 1 item
        // but if you want random item selection among multiple lines,
        // you can do that at the expansions stage

        // Check responses for optional branching
        val branchResp = mutableListOf<Pair<String,String?>>()
        responses.forEach { r ->
            val bs = r.indexOf('[')
            val be = r.indexOf(']')
            if (bs >= 0 && be > bs) {
                val disp = r.substring(0, bs).trim()
                val lbl  = r.substring(bs + 1, be).trim()
                branchResp.add(disp to lbl)
            } else {
                branchResp.add(r to null)
            }
        }

        // If any response has a label => branching scale
        return if (branchResp.any { it.second != null }) {
            ScaleFragment.newBranchInstance(header, body, singleItem, branchResp)
        } else {
            val respDisplay = branchResp.map { it.first }
            ScaleFragment.newInstance(header, body, singleItem, respDisplay)
        }
    }

    private fun getContext(): Context {
        // Reflection approach to retrieve the context from logger
        return logger.javaClass.getDeclaredField("context")
            .apply { isAccessible = true }
            .get(logger) as Context
    }

    private fun safeParseColor(str: String): Int {
        return try {
            android.graphics.Color.parseColor(str)
        } catch (_: Exception) {
            android.graphics.Color.BLACK
        }
    }
}
