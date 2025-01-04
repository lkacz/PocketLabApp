// Filename: ProtocolManager.kt
package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.StringReader
import kotlin.random.Random

class ProtocolManager(private val context: Context) {

    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)

    fun readOriginalProtocol(uri: Uri? = null) {
        try {
            val inputStream = when {
                // 1) Check if a custom Uri was provided and if it points to an asset
                uri != null && isAssetUri(uri.toString()) -> {
                    // e.g. "file:///android_asset/demo_protocol.txt"
                    val assetFileName = extractAssetFileName(uri.toString())
                    context.assets.open(assetFileName)
                }
                // 2) If a custom Uri is provided and not an asset, open normally
                uri != null -> {
                    context.contentResolver.openInputStream(uri)
                }
                else -> {
                    // 3) If no Uri, fall back to mode-based default
                    val mode = sharedPref.getString("CURRENT_MODE", "demo")
                    val fileName = if (mode == "tutorial") "tutorial_protocol.txt" else "demo_protocol.txt"
                    context.assets.open(fileName)
                }
            }

            inputStream?.bufferedReader().use { reader ->
                val lines = reader?.readLines()
                originalProtocol = lines?.joinToString("\n")
                // Check if there's a STUDY_ID line
                lines?.firstOrNull { it.startsWith("STUDY_ID;") }?.let {
                    val studyId = it.split(";").getOrNull(1)
                    sharedPref.edit().putString("STUDY_ID", studyId).apply()
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun performManipulations() {
        val rawLines = originalProtocol?.lines() ?: emptyList()
        val newLines = mutableListOf<String>()
        var randomize = false
        val randomSection = mutableListOf<String>()

        for (line in rawLines) {
            val trimmed = line.trim()
            when {
                trimmed == "RANDOMIZE_ON" -> {
                    randomize = true
                }
                trimmed == "RANDOMIZE_OFF" -> {
                    randomize = false
                    randomSection.shuffle(Random)
                    randomSection.forEach { randomLine ->
                        newLines.addAll(MultiScaleHelper.expandScaleLine(randomLine))
                    }
                    randomSection.clear()
                }
                else -> {
                    if (randomize) {
                        randomSection.add(line)
                    } else {
                        val expanded = MultiScaleHelper.expandScaleLine(line)
                        newLines.addAll(expanded)
                    }
                }
            }
        }

        // If randomize was still on at the end
        if (randomize) {
            randomSection.shuffle(Random)
            randomSection.forEach { randomLine ->
                newLines.addAll(MultiScaleHelper.expandScaleLine(randomLine))
            }
            randomSection.clear()
        }

        finalProtocol = newLines.joinToString("\n")
    }

    fun getManipulatedProtocol(): BufferedReader {
        performManipulations()
        return BufferedReader(StringReader(finalProtocol))
    }

    /**
     * Check if the string starts with "file:///android_asset/".
     */
    private fun isAssetUri(uriString: String): Boolean {
        return uriString.startsWith("file:///android_asset/")
    }

    /**
     * Extract just the asset file name from e.g. "file:///android_asset/demo_protocol.txt".
     */
    private fun extractAssetFileName(uriString: String): String {
        return uriString.removePrefix("file:///android_asset/").trim()
    }

    companion object {
        var originalProtocol: String? = null
        var finalProtocol: String? = null
    }
}
