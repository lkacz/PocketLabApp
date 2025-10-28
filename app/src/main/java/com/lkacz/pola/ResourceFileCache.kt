package com.lkacz.pola

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.util.concurrent.ConcurrentHashMap

/**
 * Caches the contents of the user-selected resources folder to avoid repeatedly
 * scanning large directories via [DocumentFile.findFile].
 */
object ResourceFileCache {
    private const val TAG = "ResourceFileCache"
    
    private data class CacheEntry(
        val filesByName: Map<String, DocumentFile>,
        val createdAtMs: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val buildLocks = ConcurrentHashMap<String, Any>()
    private const val CACHE_TTL_MS = 60_000L

    fun getFile(
        context: Context,
        folderUri: Uri?,
        fileName: String,
    ): DocumentFile? {
        if (folderUri == null || fileName.isBlank()) {
            return null
        }
        val key = folderUri.toString()
        val now = SystemClock.elapsedRealtime()
        
        // Check if we have a valid cached entry
        cache[key]?.let { entry ->
            if (now - entry.createdAtMs <= CACHE_TTL_MS) {
                return entry.filesByName[fileName.lowercase()]
            }
        }
        
        // Cache is missing or expired - need to rebuild
        // Use a per-folder lock to prevent multiple threads from building the same cache
        val lock = buildLocks.getOrPut(key) { Any() }
        synchronized(lock) {
            // Double-check: another thread might have built the cache while we waited
            cache[key]?.let { entry ->
                if (now - entry.createdAtMs <= CACHE_TTL_MS) {
                    return entry.filesByName[fileName.lowercase()]
                }
            }
            
            // Build the cache
            buildCache(context, folderUri)?.let { newEntry ->
                cache[key] = newEntry
                Log.d(TAG, "Cache rebuilt for $key with ${newEntry.filesByName.size} files")
                return newEntry.filesByName[fileName.lowercase()]
            }
        }
        
        return null
    }

    fun invalidate(folderUri: Uri) {
        val key = folderUri.toString()
        cache.remove(key)
        buildLocks.remove(key)
    }

    fun invalidateAll() {
        cache.clear()
        buildLocks.clear()
    }

    private fun buildCache(
        context: Context,
        folderUri: Uri,
    ): CacheEntry? {
        return try {
            val parentFolder = DocumentFile.fromTreeUri(context, folderUri) ?: return null
            val filesByName =
                parentFolder
                    .listFiles()
                    .mapNotNull { document ->
                        val name = document.name ?: return@mapNotNull null
                        name.lowercase() to document
                    }
                    .toMap()
            CacheEntry(filesByName, SystemClock.elapsedRealtime())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build resource file cache", e)
            null
        }
    }
}
