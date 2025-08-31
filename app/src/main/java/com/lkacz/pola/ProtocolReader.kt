// Filename: ProtocolReader.kt
package com.lkacz.pola

import android.content.Context
import android.net.Uri

class ProtocolReader {
    fun readFromAssets(
        context: Context,
        fileName: String,
    ): String {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Error reading asset file: ${e.message}"
        }
    }

    /**
     * Attempts to read file content from a content URI. If the URI starts with
     * "file:///android_asset/", it reads from assets instead of regular storage.
     */
    fun readFileContent(
        context: Context,
        uri: Uri,
    ): String {
        return try {
            val uriString = uri.toString()
            if (uriString.startsWith("file:///android_asset/")) {
                val assetName = uriString.removePrefix("file:///android_asset/")
                readFromAssets(context, assetName)
            } else {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                } ?: "Error: Unable to open file"
            }
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }
}
