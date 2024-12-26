package com.lkacz.pola

import android.os.Environment
import android.content.Context
import android.content.SharedPreferences
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class Logger private constructor(private val context: Context) {
    private val fileOperations: FileOperations
    private var isBackupCreated = false
    private val excelOperations = ExcelOperations()
    private val sharedPref: SharedPreferences = context.getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)

    private val studyId: String? = sharedPref.getString("STUDY_ID", null)
    private val fileName: String
    private val mainFolder: File
    private val file: File

    init {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        fileName = if (studyId.isNullOrEmpty()) {
            "output_$timeStamp.csv"
        } else {
            "${studyId}_output_$timeStamp.csv"
        }

        val publicStorage = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        mainFolder = File(publicStorage, "PoLA_Data")
        file = File(mainFolder, fileName)
        fileOperations = FileOperations(mainFolder, file)
        fileOperations.createFileAndFolder()
    }

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun logToCSV(cells: Array<String?>) {
        val dateTime = getCurrentDateTime().split(" ")
        val date = dateTime[0]
        val time = dateTime[1]
        val sanitizedCells = cells.map { it ?: "" }
        val logMessage = "$date\t$time\t${sanitizedCells.joinToString("\t")}\n"
        fileOperations.writeToCSV(logMessage)
    }

    fun logInstructionFragment(header: String, body: String) {
        // Column layout changed from INTRODUCTION to BODY
        logToCSV(arrayOf(header, body, null, null, null, null, null, null))
    }

    fun logTimerFragment(header: String, body: String, timeInSeconds: Int, other: String? = null) {
        logToCSV(arrayOf(header, body, null, null, null, timeInSeconds.toString(), null, other))
    }

    /**
     * Renamed "intro" to "body" in the signature and CSV structure
     */
    fun logScaleFragment(header: String, body: String, item: String, responseNumber: Int, responseText: String) {
        logToCSV(arrayOf(header, body, item, responseNumber.toString(), responseText, null, null, null))
    }

    /**
     * "intro" param changed to "body" in CSV columns
     */
    fun logInputFieldFragment(header: String, body: String, item: String, response: String, isNumeric: Boolean) {
        val responseNumber = if (isNumeric) response else null
        val responseText = if (!isNumeric) response else null
        logToCSV(arrayOf(header, body, item, responseNumber, responseText, null, null, null))
    }

    fun logOther(message: String) {
        logToCSV(arrayOf(null, null, null, null, null, null, message, null))
    }

    fun backupLogFile() {
        if (!file.exists()) {
            return
        }
        try {
            val mainFolderXlsxFileName = fileName.replace(".csv", ".xlsx")
            val mainFolderXlsxFile = File(mainFolder, mainFolderXlsxFileName)
            excelOperations.createXlsxBackup(
                mainFolderXlsxFile,
                file,
                BufferedReader(StringReader(ProtocolManager.originalProtocol ?: "")),
                BufferedReader(StringReader(ProtocolManager.finalProtocol ?: ""))
            )

            if (isBackupCreated) return
            val parentFile = mainFolder.parentFile ?: return
            val backupFolder = File(parentFile, backupFolderName)
            if (!backupFolder.exists()) {
                backupFolder.mkdirs()
            }

            val backupFileNameCsv = fileName.replace(".csv", "_backup.csv")
            val backupFileCsv = File(backupFolder, backupFileNameCsv)
            file.copyTo(backupFileCsv, overwrite = true)

            val backupFileNameXlsx = fileName.replace(".csv", "_backup.xlsx")
            val backupFileXlsx = File(backupFolder, backupFileNameXlsx)
            excelOperations.createXlsxBackup(
                backupFileXlsx,
                file,
                BufferedReader(StringReader(ProtocolManager.originalProtocol ?: "")),
                BufferedReader(StringReader(ProtocolManager.finalProtocol ?: ""))
            )

            isBackupCreated = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: Logger? = null
        private const val backupFolderName = "PoLA_Backup"

        fun getInstance(context: Context): Logger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Logger(context).also { INSTANCE = it }
            }
        }

        fun resetInstance() {
            INSTANCE = null
        }
    }
}
