// Filename: FragmentLoader.kt
package com.lkacz.pola

import androidx.fragment.app.Fragment
import java.io.BufferedReader

/**
 * Revised FragmentLoader that supports existing commands plus CUSTOM_HTML for running custom HTML/JS code.
 */
class FragmentLoader(
    private val bufferedReader: BufferedReader,
    private val logger: Logger
) {
    private val lines = mutableListOf<String>()
    private val labelMap = mutableMapOf<String, Int>()
    private var currentIndex = -1

    init {
        val rawLines = bufferedReader.readLines()
        rawLines.forEachIndexed { _, line ->
            lines.add(line.trim())
        }
        lines.forEachIndexed { index, line ->
            if (line.startsWith("LABEL;")) {
                val labelName = line.split(";").getOrNull(1)?.trim().orEmpty()
                if (labelName.isNotEmpty()) {
                    labelMap[labelName] = index
                }
            }
        }
    }

    fun loadNextFragment(): Fragment {
        while (true) {
            currentIndex++
            if (currentIndex >= lines.size) {
                return EndFragment()
            }
            val instructionLine = lines[currentIndex]
            if (instructionLine.isBlank()) continue

            val parts = instructionLine.split(";").map { it.trim('"') }
            val directive = parts.firstOrNull() ?: ""
            when (directive) {
                "LABEL" -> continue
                "GOTO" -> {
                    jumpToLabel(parts.getOrNull(1).orEmpty())
                    continue
                }
                "BRANCH_SCALE" -> {
                    return createBranchScaleFragment(parts)
                }
                "INSTRUCTION" -> {
                    return createInstructionFragment(parts)
                }
                "TIMER" -> {
                    return createTimerFragment(parts)
                }
                "TAP_INSTRUCTION" -> {
                    return createTapInstructionFragment(parts)
                }
                "SCALE" -> {
                    return createScaleFragment(parts)
                }
                "INPUTFIELD" -> {
                    return createInputFieldFragment(parts)
                }
                "CUSTOM_HTML" -> {
                    return createCustomHtmlFragment(parts)
                }
                else -> continue
            }
        }
    }

    fun jumpToLabelAndLoad(label: String): Fragment {
        jumpToLabel(label)
        return loadNextFragment()
    }

    private fun jumpToLabel(label: String) {
        val targetIndex = labelMap[label] ?: -1
        if (targetIndex == -1) {
            currentIndex = lines.size
            return
        }
        currentIndex = targetIndex
    }

    private fun createBranchScaleFragment(parts: List<String>): Fragment {
        val header = parts.getOrNull(1)
        val body = parts.getOrNull(2)
        val item = parts.getOrNull(3)
        val rawResponses = parts.drop(4)
        val branchResponses = mutableListOf<Pair<String, String?>>()
        rawResponses.forEach { resp ->
            val bracketStart = resp.indexOf('[')
            val bracketEnd = resp.indexOf(']')
            if (bracketStart in 0 until bracketEnd) {
                val displayText = resp.substring(0, bracketStart).trim()
                val label = resp.substring(bracketStart + 1, bracketEnd).trim()
                branchResponses.add(displayText to label)
            } else {
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

    /**
     * Handles CUSTOM_HTML;mycode.html
     */
    private fun createCustomHtmlFragment(parts: List<String>): Fragment {
        val fileName = parts.getOrNull(1).orEmpty()
        return CustomHtmlFragment.newInstance(fileName)
    }
}
