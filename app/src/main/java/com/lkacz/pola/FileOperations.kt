package com.lkacz.pola

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

class FileOperations(
    private val mainFolder: File,
    initialFile: File,
) {
    @Volatile
    private var targetFile: File = initialFile
    
    @Volatile
    private var writer: BufferedWriter? = null
    
    private val lock = Any()

    fun createFileAndFolder() {
        synchronized(lock) {
            try {
                if (!mainFolder.exists()) {
                    mainFolder.mkdirs()
                }
                val current = targetFile
                val isFileNewlyCreated = if (!current.exists()) current.createNewFile() else false
                
                // Close existing writer if any
                writer?.close()
                
                // Open new writer
                writer = BufferedWriter(OutputStreamWriter(FileOutputStream(current, true), Charsets.UTF_8))
                
                if (isFileNewlyCreated) {
                    writeHeaderInternal()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @Synchronized
    fun updateTargetFile(newFile: File) {
        synchronized(lock) {
            try {
                // Flush and close old writer
                writer?.flush()
                writer?.close()
                
                targetFile = newFile
                
                // Check if new file needs header
                val needsHeader = !newFile.exists()
                if (needsHeader) {
                    newFile.parentFile?.mkdirs()
                    newFile.createNewFile()
                }
                
                // Open new writer
                writer = BufferedWriter(OutputStreamWriter(FileOutputStream(newFile, true), Charsets.UTF_8))
                
                if (needsHeader) {
                    writeHeaderInternal()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    
    private fun writeHeaderInternal() {
        try {
            writer?.write("DATE\tTIME\tHEADER\tBODY\tITEM\tITEM RESPONSE (number)\tITEM RESPONSE (text)\tWAITING TIME\tOTHER\n")
            writer?.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun writeToCSV(logMessage: String) {
        synchronized(lock) {
            try {
                // Ensure file and writer exist
                if (!targetFile.exists()) {
                    targetFile.parentFile?.mkdirs()
                    targetFile.createNewFile()
                    writer?.close()
                    writer = BufferedWriter(OutputStreamWriter(FileOutputStream(targetFile, true), Charsets.UTF_8))
                    writeHeaderInternal()
                }
                
                // Write to buffer (does NOT immediately flush to disk)
                writer?.write(logMessage)
                // Note: We intentionally don't flush here to avoid blocking
                // Buffer will auto-flush when full or when file is closed
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    
    fun flush() {
        synchronized(lock) {
            try {
                writer?.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    
    fun close() {
        synchronized(lock) {
            try {
                writer?.flush()
                writer?.close()
                writer = null
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
