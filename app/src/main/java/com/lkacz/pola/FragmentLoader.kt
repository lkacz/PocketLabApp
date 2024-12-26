package com.lkacz.pola

import java.io.BufferedReader

/**
 * A sealed class hierarchy represents each possible type of instruction
 * previously handled by distinct Fragments in the XML-based approach.
 */
sealed class Instruction {
    data class InstructionContent(
        val header: String?,
        val body: String?,
        val nextButtonText: String?
    ) : Instruction()

    data class TimerContent(
        val header: String?,
        val body: String?,
        val nextButtonText: String?,
        val timeInSeconds: Int?
    ) : Instruction()

    data class TapInstructionContent(
        val header: String?,
        val body: String?,
        val nextButtonText: String?
    ) : Instruction()

    data class ScaleContent(
        val header: String?,
        val intro: String?,
        val item: String?,
        val responses: List<String>
    ) : Instruction()

    data class InputFieldContent(
        val heading: String?,
        val text: String?,
        val buttonName: String?,
        val inputFields: List<String>
    ) : Instruction()

    object End : Instruction()
}

/**
 * Loads the next instruction from the buffer instead of returning a Fragment.
 */
class InstructionLoader(
    private val bufferedReader: BufferedReader,
    private val logger: Logger
) {
    /**
     * Reads lines until it finds an instruction that starts with one of the
     * recognized tokens: INSTRUCTION, TIMER, TAP_INSTRUCTION, SCALE, or INPUTFIELD.
     * Returns a corresponding [Instruction] object.
     */
    fun loadNextInstruction(): Instruction {
        return try {
            generateSequence { bufferedReader.readLine() }
                .map(String::trim)
                .firstOrNull {
                    it.startsWith("INSTRUCTION") ||
                            it.startsWith("TIMER") ||
                            it.startsWith("TAP_INSTRUCTION") ||
                            it.startsWith("SCALE") ||
                            it.startsWith("INPUTFIELD")
                }
                ?.let { line ->
                    parseInstructionLine(line)
                }
                ?: Instruction.End  // If no more lines or instructions found
        } catch (e: Exception) {
            e.printStackTrace()
            Instruction.End
        }
    }

    /**
     * Parses a single protocol line and returns the appropriate Instruction subtype.
     */
    private fun parseInstructionLine(line: String): Instruction {
        val parts = line.split(";").map { it.trim('"') }
        return when (parts.firstOrNull()) {
            "INSTRUCTION" -> Instruction.InstructionContent(
                header = parts.getOrNull(1),
                body = parts.getOrNull(2),
                nextButtonText = parts.getOrNull(3)
            )

            "TIMER" -> Instruction.TimerContent(
                header = parts.getOrNull(1),
                body = parts.getOrNull(2),
                nextButtonText = parts.getOrNull(3),
                timeInSeconds = parts.getOrNull(4)?.toIntOrNull()
            )

            "TAP_INSTRUCTION" -> Instruction.TapInstructionContent(
                header = parts.getOrNull(1),
                body = parts.getOrNull(2),
                nextButtonText = parts.getOrNull(3)
            )

            "SCALE" -> {
                // parts[4..] represent potential scale responses
                val responses = parts.drop(4)
                Instruction.ScaleContent(
                    header = parts.getOrNull(1),
                    intro = parts.getOrNull(2),
                    item = parts.getOrNull(3),
                    responses = responses
                )
            }

            "INPUTFIELD" -> {
                val fields = parts.drop(4)
                Instruction.InputFieldContent(
                    heading = parts.getOrNull(1),
                    text = parts.getOrNull(2),
                    buttonName = parts.getOrNull(3),
                    inputFields = fields
                )
            }

            else -> Instruction.End
        }
    }
}
