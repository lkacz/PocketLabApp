package com.example.frags

import android.content.Context
import java.io.BufferedReader
import java.io.StringReader
import kotlin.random.Random

class ProtocolManager(private val context: Context) {

    // Method to read the original protocol.txt file into memory.
    fun readOriginalProtocol() {
        context.assets.open("protocol.txt").bufferedReader().use {
            originalProtocol = it.readText()
        }
    }

    // Method to randomize sections of the protocol.
    private fun performManipulations() {
        val lines = originalProtocol?.lines()
        val newLines = mutableListOf<String>()
        var randomize = false
        val randomSection = mutableListOf<String>()

        if (lines != null) {
            for (line in lines) {
                when {
                    line.trim() == "RANDOMIZEON" -> {
                        randomize = true
                    }
                    line.trim() == "RANDOMIZEOFF" -> {
                        randomize = false
                        randomSection.shuffle(Random)
                        newLines.addAll(randomSection)
                        randomSection.clear()
                    }
                    line.trim().startsWith("MULTIQUESTIONNAIRE") || line.trim().startsWith("RANDOMMULTIQUESTIONNAIRE") -> {
                        val isRandom = line.trim().startsWith("RANDOMMULTIQUESTIONNAIRE")
                        val bracketStart = line.indexOf('[')
                        val bracketEnd = line.indexOf(']')

                        // Extract sections of the line before, within, and after brackets
                        val preBracket = line.substring(0, bracketStart).split(';')
                        val multiItems = line.substring(bracketStart + 1, bracketEnd).split(';')
                        val postBracket = line.substring(bracketEnd + 1).split(';').drop(1)  // drop 1 to remove the leading empty string

                        val header = preBracket[1]
                        val introduction = preBracket[2]
                        val responses = postBracket.joinToString(";")

                        val questionnaireLines = mutableListOf<String>()

                        for (item in multiItems) {
                            val newLine = "QUESTIONNAIRE;$header;$introduction;$item;$responses"
                            questionnaireLines.add(newLine)
                        }

                        if (isRandom) {
                            questionnaireLines.shuffle(Random)
                        }

                        newLines.addAll(questionnaireLines)
                    }
                    else -> {
                        if (randomize) {
                            randomSection.add(line)
                        } else {
                            newLines.add(line)
                        }
                    }
                }
            }
        }

        finalProtocol = newLines.joinToString("\n")
    }

    // Method to provide the manipulated protocol as BufferedReader.
    fun getManipulatedProtocol(): BufferedReader {
        performManipulations()
        return BufferedReader(StringReader(finalProtocol))
    }

    companion object {
        var originalProtocol: String? = null
        var finalProtocol: String? = null
    }
}