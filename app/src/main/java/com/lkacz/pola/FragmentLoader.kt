// Filename: FragmentLoader.kt
package com.lkacz.pola

import androidx.fragment.app.Fragment
import java.io.BufferedReader

/**
 * Revised FragmentLoader that supports existing commands plus dynamic commands like
 * TRANSITIONS;off/slide, TIMER_SOUND;..., HEADER_SIZE;..., BODY_SIZE;..., etc.
 *
 * Each time one of these directives is encountered during protocol execution,
 * it updates the corresponding settings (SharedPreferences, in-memory managers),
 * then proceeds to the next line. No fragment is returned for these lines.
 *
 * LABEL and GOTO remain exceptions that allow jumping around in the protocol.
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
        rawLines.forEach { line ->
            lines.add(line.trim())
        }

        // Build label map for GOTO jumps
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
     * Loads the next valid Fragment (skipping or processing special commands).
     * Returns EndFragment if we hit the end of the list.
     */
    fun loadNextFragment(): Fragment {
        while (true) {
            currentIndex++
            if (currentIndex >= lines.size) {
                return EndFragment()
            }
            val line = lines[currentIndex]
            if (line.isBlank()) continue

            val parts = line.split(";").map { it.trim('"') }
            val directive = parts.firstOrNull()?.uppercase() ?: ""

            when (directive) {
                "LABEL" -> {
                    // Just skip label lines in normal flow
                    continue
                }

                "GOTO" -> {
                    jumpToLabel(parts.getOrNull(1).orEmpty())
                    continue
                }

                // === Dynamically updated commands below ===
                "TRANSITIONS" -> {
                    // e.g.  TRANSITIONS;off   or   TRANSITIONS;slide
                    val mode = parts.getOrNull(1)?.lowercase() ?: "off"
                    TransitionManager.setTransitionMode(getContext(), mode)
                    continue
                }

                "TIMER_SOUND" -> {
                    // e.g.  TIMER_SOUND;mytimersound.mp3
                    val filename = parts.getOrNull(1)?.trim().orEmpty()
                    getContext().getSharedPreferences("ProtocolPrefs",
                        android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putString("CUSTOM_TIMER_SOUND", filename)
                        .apply()
                    continue
                }

                "HEADER_SIZE", "BODY_SIZE", "BUTTON_SIZE", "ITEM_SIZE", "RESPONSE_SIZE" -> {
                    // e.g.  HEADER_SIZE;48
                    val sizeValue = parts.getOrNull(1)?.toFloatOrNull()
                    if (sizeValue != null) {
                        when (directive) {
                            "HEADER_SIZE"   -> FontSizeManager.setHeaderSize(getContext(), sizeValue)
                            "BODY_SIZE"     -> FontSizeManager.setBodySize(getContext(), sizeValue)
                            "BUTTON_SIZE"   -> FontSizeManager.setButtonSize(getContext(), sizeValue)
                            "ITEM_SIZE"     -> FontSizeManager.setItemSize(getContext(), sizeValue)
                            "RESPONSE_SIZE" -> FontSizeManager.setResponseSize(getContext(), sizeValue)
                        }
                    }
                    continue
                }

                // === Actual fragment-producing commands below ===
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

                else -> {
                    // If we encounter an unknown or unhandled directive, just skip it
                    continue
                }
            }
        }
    }

    /**
     * Jumps to a label and then continues protocol execution.
     */
    fun jumpToLabelAndLoad(label: String): Fragment {
        jumpToLabel(label)
        return loadNextFragment()
    }

    private fun jumpToLabel(label: String) {
        val targetIndex = labelMap[label] ?: -1
        if (targetIndex == -1) {
            // Label not found, jump to end
            currentIndex = lines.size
            return
        }
        currentIndex = targetIndex
    }

    // ---------- Private fragment creation helpers ---------------

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
                val lbl = resp.substring(bracketStart + 1, bracketEnd).trim()
                branchResponses.add(displayText to lbl)
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

    private fun createCustomHtmlFragment(parts: List<String>): Fragment {
        val fileName = parts.getOrNull(1).orEmpty()
        return CustomHtmlFragment.newInstance(fileName)
    }

    private fun getContext() = logger.let {
        // For convenience, we just get the context from logger (which is a singleton).
        // Or retrieve from your Activity if you prefer an alternative approach.
        it.javaClass
            .getDeclaredField("context")
            .apply { isAccessible = true }
            .get(it) as android.content.Context
    }
}
