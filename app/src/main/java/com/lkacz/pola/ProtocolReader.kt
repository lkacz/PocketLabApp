package com.lkacz.pola

import android.content.Context
import android.net.Uri

class ProtocolReader {

    fun readFromAssets(context: Context, fileName: String): String {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Error reading asset file: ${e.message}"
        }
    }

    fun readFileContent(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: "Error: Unable to open file"
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }
}
