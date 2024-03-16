package com.lkacz.pola

import androidx.fragment.app.Fragment
import java.io.BufferedReader

class FragmentLoader(private val bufferedReader: BufferedReader, private val logger: Logger) {
    fun loadNextFragment(): Fragment {
        return try {
            generateSequence { bufferedReader.readLine() }
                .map(String::trim)
                .firstOrNull { it.startsWith("INSTRUCTION") || it.startsWith("TIMER") || it.startsWith("TAP_INSTRUCTION") ||
                        it.startsWith("SCALE") || it.startsWith("INPUTFIELD") }
                ?.let { instructionLine ->
                    val parts = instructionLine.split(";").map { it.trim('"') }
                    when (parts.firstOrNull()) {
                        "INSTRUCTION" -> InstructionFragment.newInstance(parts.getOrNull(1), parts.getOrNull(2), parts.getOrNull(3))
                        "TIMER" -> TimerFragment.newInstance(parts.getOrNull(1), parts.getOrNull(2), parts.getOrNull(3), parts.getOrNull(4)?.toIntOrNull())
                        "TAP_INSTRUCTION" -> TapInstructionFragment.newInstance(parts.getOrNull(1), parts.getOrNull(2), parts.getOrNull(3))
                        "SCALE" -> ScaleFragment.newInstance(parts.getOrNull(1), parts.getOrNull(2), parts.getOrNull(3), parts.drop(4))
                        "INPUTFIELD" -> InputFieldFragment.newInstance(parts.getOrNull(1), parts.getOrNull(2), parts.getOrNull(3), parts.drop(4))
                        else -> EndFragment()
                    }
                } ?: EndFragment()
        } catch (e: Exception) {
            e.printStackTrace()
            EndFragment()
        }
    }
}