package com.example.frags

import androidx.fragment.app.Fragment
import java.io.BufferedReader

class FragmentLoader(private val bufferedReader: BufferedReader, private val logger: Logger) {
    fun loadNextFragment(): Fragment {
        var instructionLine: String? = null

        // Read lines until a relevant keyword is found, END is encountered, or EOF is reached.
        while (bufferedReader.ready()) {
            val tempLine = bufferedReader.readLine().trim()
            if (tempLine.startsWith("END")) {
                return EndFragment()
            }
            if (tempLine.startsWith("LOG")) {
                logger.logOther(tempLine.split(";").getOrNull(1)?.trim('"') ?: "No message provided.")
                continue  // Skip the rest of the loop and read the next line.
            }
            if (tempLine.startsWith("INSTRUCTION") || tempLine.startsWith("FIXEDINSTRUCTION") ||
                tempLine.startsWith("TAPINSTRUCTION") || tempLine.startsWith("QUESTIONNAIRE") ||
                tempLine.startsWith("SETTINGS")) {
                instructionLine = tempLine
                break
            }
        }

        // If instructionLine is still null, it means we reached the EOF without finding a relevant keyword.
        if (instructionLine == null) {
            return EndFragment()
        }

        val parts = instructionLine.split(";").map { it.trim('"') }

        return when (parts.getOrNull(0)) {
            "INSTRUCTION" -> InstructionFragment.newInstance(
                parts.getOrNull(1),
                parts.getOrNull(2),
                parts.getOrNull(3)
            )
            "FIXEDINSTRUCTION" -> FixedInstructionFragment.newInstance(
                parts.getOrNull(1),
                parts.getOrNull(2),
                parts.getOrNull(3),
                parts.getOrNull(4)?.toIntOrNull()
            )
            "TAPINSTRUCTION" -> TapInstructionFragment.newInstance(
                parts.getOrNull(1),
                parts.getOrNull(2),
                parts.getOrNull(3)
            )
            "QUESTIONNAIRE" -> QuestionnaireFragment.newInstance(
                parts.getOrNull(1),
                parts.getOrNull(2),
                parts.getOrNull(3),
                parts.subList(4, parts.size)
            )
            "SETTINGS" -> SettingsFragment.newInstance(
                parts.getOrNull(1),
                parts.getOrNull(2),
                parts.getOrNull(3),
                parts.subList(4, parts.size)
            )
            else -> EndFragment()
        }
    }
}