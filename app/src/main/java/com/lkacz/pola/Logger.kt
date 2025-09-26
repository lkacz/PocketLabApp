package com.lkacz.pola

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class Logger private constructor(private val context: Context) {
    private val excelOperations = ExcelOperations()
    private val sharedPref: SharedPreferences = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingJobs = Collections.synchronizedList(mutableListOf<Job>())
    private val renameLock = Any()

    private val timeStamp: String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date())

    private var activeParticipantId: String? =
        sharedPref.getString(Prefs.KEY_PARTICIPANT_ID, null)?.takeIf { it.isNotBlank() }?.let { sanitizeStudyIdForFile(it) }
    private var activeStudyId: String? =
        sharedPref.getString("STUDY_ID", null)?.takeIf { it.isNotBlank() }?.let { sanitizeStudyIdForFile(it) }

    private var currentFileName: String = buildFileName(activeParticipantId, activeStudyId)

    private val storageRoot: File =
        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(context.filesDir, "documents").also { it.mkdirs() }
    private val mainFolder: File = File(storageRoot, "PoLA_Data")

    @Volatile
    private var currentFile: File = File(mainFolder, currentFileName)
    private val fileOperations: FileOperations = FileOperations(mainFolder, currentFile)

    @Volatile
    private var isBackupCreated = false

    init {
        fileOperations.createFileAndFolder()
    }

    fun logInstructionFragment(
        header: String,
        body: String,
    ) {
        logToCSV(arrayOf(header, body, null, null, null, null, null, null))
    }

    fun logTimerFragment(
        header: String,
        body: String,
        timeInSeconds: Int,
        other: String? = null,
    ) {
        logToCSV(arrayOf(header, body, null, null, null, timeInSeconds.toString(), null, other))
    }

    fun logScaleFragment(
        header: String,
        body: String,
        item: String,
        responseNumber: Int,
        responseText: String,
    ) {
        logToCSV(arrayOf(header, body, item, responseNumber.toString(), responseText, null, null, null))
    }

    fun logInputFieldFragment(
        header: String,
        body: String,
        item: String,
        response: String,
        isNumeric: Boolean,
    ) {
        val responseNumber = if (isNumeric) response else null
        val responseText = if (!isNumeric) response else null
        logToCSV(arrayOf(header, body, item, responseNumber, responseText, null, null, null))
    }

    fun logOther(message: String) {
        logToCSV(arrayOf(null, null, null, null, null, null, message, null))
    }

    fun updateParticipantId(newParticipantIdRaw: String?) {
        val sanitized = newParticipantIdRaw?.takeIf { it.isNotBlank() }?.let { sanitizeStudyIdForFile(it) }
        applyIdentifiers(sanitized, activeStudyId)
    }

    fun updateStudyId(newStudyIdRaw: String?) {
        val sanitized = newStudyIdRaw?.takeIf { it.isNotBlank() }?.let { sanitizeStudyIdForFile(it) }
        applyIdentifiers(activeParticipantId, sanitized)
    }

    fun backupLogFile() {
        scope.launch {
            awaitPendingWrites()
            if (!currentFile.exists()) return@launch
            try {
                val xlsxFile = File(mainFolder, currentFileName.replace(".csv", ".xlsx"))
                excelOperations.createXlsxBackup(
                    xlsxFile,
                    currentFile,
                    BufferedReader(StringReader(ProtocolManager.originalProtocol ?: "")),
                    BufferedReader(StringReader(ProtocolManager.finalProtocol ?: "")),
                )

                if (isBackupCreated) return@launch

                val parentFile = mainFolder.parentFile ?: return@launch
                val backupFolder = File(parentFile, backupFolderName)
                if (!backupFolder.exists()) {
                    backupFolder.mkdirs()
                }

                val baseName = currentFileName.removeSuffix(".csv")
                val backupCsv = File(backupFolder, "${baseName}_backup.csv")
                currentFile.copyTo(backupCsv, overwrite = true)

                val backupXlsx = File(backupFolder, "${baseName}_backup.xlsx")
                excelOperations.createXlsxBackup(
                    backupXlsx,
                    currentFile,
                    BufferedReader(StringReader(ProtocolManager.originalProtocol ?: "")),
                    BufferedReader(StringReader(ProtocolManager.finalProtocol ?: "")),
                )

                isBackupCreated = true
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun flushAndClose() {
        runBlocking {
            awaitPendingWrites()
        }
        scope.cancel()
    }

    private fun logToCSV(cells: Array<String?>) {
        val (date, time) = getCurrentDateTimeComponents()
        val rowCells = mutableListOf<String?>()
        rowCells.add(date)
        rowCells.add(time)
        rowCells.addAll(cells.asList())
        enqueueWrite(formatCsvRow(rowCells))
    }

    private fun enqueueWrite(row: String) {
        val job = scope.launch { fileOperations.writeToCSV(row) }
        pendingJobs.add(job)
        job.invokeOnCompletion { pendingJobs.remove(job) }
    }

    private fun getCurrentDateTimeComponents(): Pair<String, String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formatted = dateFormat.format(Date())
        val splitIndex = formatted.indexOf(' ')
        return if (splitIndex >= 0) {
            Pair(formatted.substring(0, splitIndex), formatted.substring(splitIndex + 1))
        } else {
            Pair(formatted, "00:00:00")
        }
    }

    private fun applyIdentifiers(participantId: String?, studyId: String?) {
        synchronized(renameLock) {
            if (participantId == activeParticipantId && studyId == activeStudyId) return
            runBlocking {
                awaitPendingWrites()
                withContext(Dispatchers.IO) {
                    renameLogFileLocked(participantId, studyId)
                }
            }
        }
    }

    private fun renameLogFileLocked(participantId: String?, studyId: String?) {
        val newFileName = buildFileName(participantId, studyId)
        if (newFileName == currentFileName) {
            activeParticipantId = participantId
            activeStudyId = studyId
            return
        }

        val newFile = File(mainFolder, newFileName)
        try {
            if (currentFile.exists() && currentFile != newFile) {
                currentFile.copyTo(newFile, overwrite = true)
                currentFile.delete()
            } else if (!newFile.exists()) {
                newFile.parentFile?.mkdirs()
            }

            fileOperations.updateTargetFile(newFile)
            currentFile = newFile
            currentFileName = newFileName
            activeParticipantId = participantId
            activeStudyId = studyId
            isBackupCreated = false
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
    }

    private fun buildFileName(participantId: String?, studyId: String?): String {
        val parts = mutableListOf<String>()
        if (!participantId.isNullOrBlank()) {
            parts += participantId
        }
        if (!studyId.isNullOrBlank()) {
            parts += studyId
        }
        parts += "output_${timeStamp}"
        return parts.joinToString("_") + ".csv"
    }

    private suspend fun awaitPendingWrites() {
        val snapshot: List<Job> = synchronized(pendingJobs) { pendingJobs.toList() }
        snapshot.forEach { it.join() }
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
            INSTANCE?.flushAndClose()
            INSTANCE = null
        }
    }
}

internal fun sanitizeStudyIdForFile(raw: String): String {
    return raw.trim().replace("[^A-Za-z0-9-_]".toRegex(), "_")
}

internal fun sanitizeCsvCell(cell: String?): String {
    return cell
        ?.replace("\r", " ")
        ?.replace("\n", " ")
        ?.replace('\t', ' ')
        ?.trim()
        ?: ""
}

internal fun formatCsvRow(cells: List<String?>): String {
    val sanitized = cells.map { sanitizeCsvCell(it) }
    return sanitized.joinToString("\t", postfix = "\n")
}
