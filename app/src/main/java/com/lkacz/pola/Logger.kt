package com.lkacz.pola

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
    private var outputFolderUri: Uri? =
        sharedPref.getString(Prefs.KEY_OUTPUT_FOLDER_URI, null)?.let(Uri::parse)

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

    fun updateOutputFolderUri(newUri: Uri?) {
        outputFolderUri = newUri
    }

    suspend fun backupLogFile() {
        withContext(Dispatchers.IO) {
            awaitPendingWrites()
            // Flush any buffered writes before backing up
            fileOperations.flush()
            
            if (!currentFile.exists()) return@withContext
            try {
                val xlsxFile = File(mainFolder, currentFileName.removeSuffix(TSV_EXTENSION) + ".xlsx")
                excelOperations.createXlsxBackup(
                    xlsxFile,
                    currentFile,
                    BufferedReader(StringReader(ProtocolManager.originalProtocol ?: "")),
                    BufferedReader(StringReader(ProtocolManager.finalProtocol ?: "")),
                )

                val savedLocations = exportLogToSharedLocations(currentFile, xlsxFile)
                if (savedLocations.isNotEmpty()) {
                    showSaveToast(savedLocations)
                }

                if (isBackupCreated) return@withContext

                val parentFile = mainFolder.parentFile ?: return@withContext
                val backupFolder = File(parentFile, BACKUP_FOLDER_NAME)
                if (!backupFolder.exists()) {
                    backupFolder.mkdirs()
                }

                val baseName = currentFileName.removeSuffix(TSV_EXTENSION)
                val backupTsv = File(backupFolder, "${baseName}_backup" + TSV_EXTENSION)
                currentFile.copyTo(backupTsv, overwrite = true)

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
        fileOperations.close()
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

    private fun applyIdentifiers(
        participantId: String?,
        studyId: String?,
    ) {
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

    private fun renameLogFileLocked(
        participantId: String?,
        studyId: String?,
    ) {
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

    private fun buildFileName(
        participantId: String?,
        studyId: String?,
    ): String {
        val parts = mutableListOf<String>()
        if (!participantId.isNullOrBlank()) {
            parts += participantId
        }
        if (!studyId.isNullOrBlank()) {
            parts += studyId
        }
        parts += "output_$timeStamp"
        return parts.joinToString("_") + TSV_EXTENSION
    }

    private suspend fun awaitPendingWrites() {
        val snapshot: List<Job> = synchronized(pendingJobs) { pendingJobs.toList() }
        snapshot.forEach { it.join() }
    }

    private fun exportLogToSharedLocations(
        tsvFile: File,
        xlsxFile: File?,
    ): List<String> {
        val savedPaths = mutableListOf<String>()
        val baseName = currentFileName.removeSuffix(TSV_EXTENSION)
        val tsvName = baseName + TSV_EXTENSION
        val xlsxName = "$baseName.xlsx"

        val targetUri = outputFolderUri
        if (targetUri != null) {
            val folder = DocumentFile.fromTreeUri(context, targetUri)
            if (folder != null && folder.isDirectory) {
                val customTsvExported = exportFileToDocumentTree(folder, tsvName, TSV_MIME_TYPE, tsvFile)
                if (customTsvExported) {
                    val folderLabel =
                        folder.name
                            ?: targetUri.lastPathSegment
                            ?: context.getString(R.string.value_selected_folder)
                    savedPaths += "$folderLabel/$tsvName"
                }
                if (xlsxFile?.exists() == true) {
                    exportFileToDocumentTree(
                        folder,
                        xlsxName,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        xlsxFile,
                    )
                }
            }
        } else {
            val downloadsTsvExported = exportFileToDownloads(tsvName, TSV_MIME_TYPE, tsvFile)
            if (downloadsTsvExported) {
                savedPaths += "Downloads/PoLA_Data/$tsvName"
            }
            if (xlsxFile?.exists() == true) {
                exportFileToDownloads(
                    xlsxName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    xlsxFile,
                )
            }
        }

        return savedPaths.distinct()
    }

    private fun exportFileToDownloads(
        displayName: String,
        mimeType: String,
        sourceFile: File,
    ): Boolean {
        if (!sourceFile.exists()) return false
        return try {
            val resolver = context.contentResolver
            val relativePath = Environment.DIRECTORY_DOWNLOADS + "/PoLA_Data/"
            val selection =
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf(displayName, relativePath)
            resolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs)

            val values =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

            val uri: Uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { output ->
                sourceFile.inputStream().use { input -> input.copyTo(output) }
            } ?: return false

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            false
        }
    }

    private fun exportFileToDocumentTree(
        folder: DocumentFile,
        displayName: String,
        mimeType: String,
        sourceFile: File,
    ): Boolean {
        if (!sourceFile.exists()) return false
        return try {
            folder.findFile(displayName)?.let { existing -> existing.delete() }
            val target = folder.createFile(mimeType, displayName) ?: return false
            context.contentResolver.openOutputStream(target.uri)?.use { output ->
                sourceFile.inputStream().use { input -> input.copyTo(output) }
            } ?: return false
            true
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            false
        }
    }

    private fun showSaveToast(savedPaths: List<String>) {
        if (savedPaths.isEmpty()) return
        val message =
            if (savedPaths.size == 1) {
                context.getString(R.string.toast_export_saved_single, savedPaths.first())
            } else {
                val joined = savedPaths.joinToString(separator = "\n") { "• $it" }
                context.getString(R.string.toast_export_saved_multi, joined)
            }
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        @Volatile
        private var instance: Logger? = null
        private const val BACKUP_FOLDER_NAME = "PoLA_Backup"
        private const val TSV_EXTENSION = ".tsv"
        private const val TSV_MIME_TYPE = "text/tab-separated-values"

        fun getInstance(context: Context): Logger {
            return instance ?: synchronized(this) {
                instance ?: Logger(context).also { instance = it }
            }
        }

        fun resetInstance() {
            instance?.flushAndClose()
            instance = null
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
