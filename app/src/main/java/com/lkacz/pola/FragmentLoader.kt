package com.lkacz.pola

import androidx.fragment.app.Fragment
import java.io.BufferedReader

/**
 * Revised FragmentLoader that supports:
 *  - BRANCH_SCALE
 *  - LABEL
 *  - GOTO
 *
 * We parse all lines upfront, store labels in a map, and maintain a current index.
 * When we see GOTO;XYZ, we jump to LABEL;XYZ. "LABEL;XYZ" lines themselves are not fragments.
 */
class FragmentLoader(
    private val bufferedReader: BufferedReader,
    private val logger: Logger
) {
    private val lines = mutableListOf<String>()
    private val labelMap = mutableMapOf<String, Int>()
    private var currentIndex = -1

    init {
        // Read all lines, trim them, store in list
        val rawLines = bufferedReader.readLines()
        rawLines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            lines.add(trimmed)
        }
        // Build the label map
        lines.forEachIndexed { index, line ->
            if (line.startsWith("LABEL;")) {
                val labelName = line.split(";").getOrNull(1)?.trim().orEmpty()
                if (labelName.isNotEmpty()) {
                    labelMap[labelName] = index
                }
            }
        }
    }

    /**
     * Returns the next Fragment based on the next recognized instruction.
     * If we see GOTO, we jump to a label. If we see LABEL, we skip it and proceed.
     * If we see BRANCH_SCALE, we create a BranchScaleFragment.
     * Otherwise, handle existing instructions as usual.
     */
    fun loadNextFragment(): Fragment {
        while (true) {
            currentIndex++
            if (currentIndex >= lines.size) {
                // No more instructions; end
                return EndFragment()
            }
            val instructionLine = lines[currentIndex]
            if (instructionLine.isBlank()) {
                continue
            }

            // Parse directive
            val parts = instructionLine.split(";").map { it.trim('"') }
            val directive = parts.firstOrNull() ?: ""
            when {
                directive == "LABEL" -> {
                    // Skip label lines
                    continue
                }
                directive == "GOTO" -> {
                    val labelName = parts.getOrNull(1).orEmpty()
                    jumpToLabel(labelName)
                    continue
                }
                directive == "BRANCH_SCALE" -> {
                    return createBranchScaleFragment(parts)
                }
                directive == "INSTRUCTION" -> {
                    return createInstructionFragment(parts)
                }
                directive == "TIMER" -> {
                    return createTimerFragment(parts)
                }
                directive == "TAP_INSTRUCTION" -> {
                    return createTapInstructionFragment(parts)
                }
                directive == "SCALE" -> {
                    return createScaleFragment(parts)
                }
                directive == "INPUTFIELD" -> {
                    return createInputFieldFragment(parts)
                }
                // Unrecognized or other lines
                else -> {
                    // Could be just a comment or a random line
                    if (directive.isBlank()) {
                        continue
                    }
                    // Possibly the user typed something else, skip
                    continue
                }
            }
        }
    }

    /**
     * External entry point if we need to forcibly jump from a fragment, e.g. from BranchScaleFragment.
     */
    fun jumpToLabelAndLoad(label: String): Fragment {
        jumpToLabel(label)
        return loadNextFragment()
    }

    /**
     * Moves currentIndex to the line after the matching label, or beyond the end if not found.
     */
    private fun jumpToLabel(label: String) {
        val targetIndex = labelMap[label] ?: -1
        if (targetIndex == -1) {
            // Label not found, skip to end
            currentIndex = lines.size
            return
        }
        // Move to label line
        currentIndex = targetIndex
    }

    /**
     * Creates a BranchScaleFragment from a BRANCH_SCALE line:
     * "BRANCH_SCALE;Header;Body;Item;Resp1[Label1];Resp2;Resp3[Label3]..."
     */
    private fun createBranchScaleFragment(parts: List<String>): Fragment {
        // The first 4 positions: 0=BRANCH_SCALE, 1=Header, 2=Body, 3=Item
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val item = parts.getOrNull(3)

        // The rest are responses, each may have a bracket
        val rawResponses = parts.drop(4)
        val branchResponses = mutableListOf<Pair<String, String?>>()

        rawResponses.forEach { resp ->
            // e.g. "Very negative[Part1]" or "Somewhat negative"
            val bracketStart = resp.indexOf('[')
            val bracketEnd = resp.indexOf(']')
            if (bracketStart in 0 until bracketEnd) {
                val displayText = resp.substring(0, bracketStart).trim()
                val label = resp.substring(bracketStart + 1, bracketEnd).trim()
                branchResponses.add(displayText to label)
            } else {
                // No bracket
                branchResponses.add(resp to null)
            }
        }

        return BranchScaleFragment.newInstance(header, body, item, branchResponses)
    }

    private fun createInstructionFragment(parts: List<String>): Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val buttonText = parts.getOrNull(3)
        return InstructionFragment.newInstance(header, body, buttonText)
    }

    private fun createTimerFragment(parts: List<String>): Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val buttonText = parts.getOrNull(3)
        val timeSeconds = parts.getOrNull(4)?.toIntOrNull() ?: 0
        return TimerFragment.newInstance(header, body, buttonText, timeSeconds)
    }

    private fun createTapInstructionFragment(parts: List<String>): Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val buttonText = parts.getOrNull(3)
        return TapInstructionFragment.newInstance(header, body, buttonText)
    }

    private fun createScaleFragment(parts: List<String>): Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val item = parts.getOrNull(3)
        val responses = parts.drop(4)
        return ScaleFragment.newInstance(header, body, item, responses)
    }

    private fun createInputFieldFragment(parts: List<String>): Fragment {
        val heading = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val buttonName = parts.getOrNull(3)
        val fields = parts.drop(4)
        return InputFieldFragment.newInstance(heading, body, buttonName, fields)
    }
}
