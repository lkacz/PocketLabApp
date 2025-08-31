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
            val inputStream =
                when {
                    uri != null && isAssetUri(uri.toString()) -> {
                        val assetFileName = extractAssetFileName(uri.toString())
                        context.assets.open(assetFileName)
                    }
                    uri != null -> {
                        context.contentResolver.openInputStream(uri)
                    }
                    else -> {
                        val mode = sharedPref.getString("CURRENT_MODE", "demo")
                        val fileName =
                            if (mode == "tutorial") "tutorial_protocol.txt" else "demo_protocol.txt"
                        context.assets.open(fileName)
                    }
                }
            inputStream?.bufferedReader().use { reader ->
                val lines = reader?.readLines()
                originalProtocol = lines?.joinToString("\n")
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

    /**
     * Converts multi-line recognized commands to single-line format
     * for recognized commands. This ensures lines like:
     *
     *   INPUTFIELD;
     *       MyHeader;
     *       Some text;
     *       [Field1;Field2];
     *       Continue
     *
     * become:
     *
     *   INPUTFIELD;MyHeader;Some text;[Field1;Field2];Continue
     */
    // Merging logic moved to pure ProtocolTransformer for testability.

    private fun performManipulations() { finalProtocol = ProtocolTransformer.transform(originalProtocol) }
    
    init {
        // Lightweight initialization logging when in debug builds (Timber planted in MainActivity)
        if ((context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            try {
                timber.log.Timber.d("ProtocolManager initialized")
            } catch (_: Throwable) {
                // ignore if Timber not yet planted
            }
        }
    }

    fun getManipulatedProtocol(): BufferedReader {
        performManipulations()
        return BufferedReader(StringReader(finalProtocol))
    }

    private fun isAssetUri(uriString: String): Boolean {
        return uriString.startsWith("file:///android_asset/")
    }

    private fun extractAssetFileName(uriString: String): String {
        return uriString.removePrefix("file:///android_asset/").trim()
    }

    companion object {
        var originalProtocol: String? = null
        var finalProtocol: String? = null
    }
}
