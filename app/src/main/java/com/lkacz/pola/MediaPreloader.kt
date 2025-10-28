package com.lkacz.pola

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

/**
 * Preloads media assets for the next slide in the background to minimize
 * transition delays. Extracts media references from protocol commands and
 * warms the cache before the fragment is displayed.
 */
object MediaPreloader {
    private const val TAG = "MediaPreloader"
    private val audioPattern = Pattern.compile("<([^>]+\\.(?:mp3|wav))(?:,[^>]+)?>", Pattern.CASE_INSENSITIVE)
    private val imagePattern = Pattern.compile("<([^>]+\\.(?:jpg|png))(?:,[^>]+)?>", Pattern.CASE_INSENSITIVE)
    private val videoPattern = Pattern.compile("<([^>]+\\.mp4)(?:,[^>]+)?>", Pattern.CASE_INSENSITIVE)
    private val htmlPattern = Pattern.compile("<([^>]+\\.html)>", Pattern.CASE_INSENSITIVE)

    /**
     * Preload media for the next fragment based on the protocol command.
     * Should be called on a background scope when a new fragment is loaded.
     */
    fun preloadNextFragment(
        context: Context,
        scope: CoroutineScope,
        resourcesFolderUri: Uri?,
        commandLine: String?,
    ) {
        if (commandLine.isNullOrBlank() || resourcesFolderUri == null) return

        scope.launch(Dispatchers.IO) {
            try {
                val parts = ParsingUtils.customSplitSemicolons(commandLine).map { it.trim('"') }
                val directive = parts.firstOrNull()?.uppercase() ?: return@launch

                when (directive) {
                    "INSTRUCTION" -> {
                        preloadTextMedia(context, resourcesFolderUri, parts.getOrNull(1))  // header
                        preloadTextMedia(context, resourcesFolderUri, parts.getOrNull(2))  // body
                        preloadTextMedia(context, resourcesFolderUri, parts.getOrNull(3))  // button
                    }
                    "TIMER" -> {
                        preloadTextMedia(context, resourcesFolderUri, parts.getOrNull(1))  // header
                        preloadTextMedia(context, resourcesFolderUri, parts.getOrNull(2))  // body
                        preloadTextMedia(context, resourcesFolderUri, parts.getOrNull(4))  // button
                    }
                    "SCALE", "SCALE[RANDOMIZED]" -> {
                        preloadTextMedia(context, resourcesFolderUri, parts.getOrNull(1))  // header
                        preloadTextMedia(context, resourcesFolderUri, parts.getOrNull(2))  // body
                        preloadTextMedia(context, resourcesFolderUri, parts.getOrNull(3))  // item
                        parts.drop(4).forEach { preloadTextMedia(context, resourcesFolderUri, it) }  // responses
                    }
                    "INPUTFIELD", "INPUTFIELD[RANDOMIZED]" -> {
                        preloadTextMedia(context, resourcesFolderUri, parts.getOrNull(1))  // header
                        preloadTextMedia(context, resourcesFolderUri, parts.getOrNull(2))  // body
                        preloadTextMedia(context, resourcesFolderUri, parts.lastOrNull())  // button
                    }
                    "HTML" -> {
                        val htmlFile = parts.getOrNull(1)
                        if (!htmlFile.isNullOrBlank()) {
                            ResourceFileCache.getFile(context, resourcesFolderUri, htmlFile)
                        }
                        preloadTextMedia(context, resourcesFolderUri, parts.getOrNull(2))  // button
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to preload media for next fragment", e)
            }
        }
    }

    private fun preloadTextMedia(
        context: Context,
        resourcesFolderUri: Uri,
        text: String?,
    ) {
        if (text.isNullOrBlank()) return

        // Preload audio files - just warm the file cache, MediaPlayer will handle actual loading
        audioPattern.matcher(text).apply {
            while (find()) {
                val fileName = group(1)?.split(",")?.firstOrNull()?.trim()
                if (!fileName.isNullOrBlank()) {
                    ResourceFileCache.getFile(context, resourcesFolderUri, fileName)
                }
            }
        }

        // Preload images - just warm the file cache, HtmlImageLoader will decode on-demand
        imagePattern.matcher(text).apply {
            while (find()) {
                val fileName = group(1)?.split(",")?.firstOrNull()?.trim()
                if (!fileName.isNullOrBlank()) {
                    ResourceFileCache.getFile(context, resourcesFolderUri, fileName)
                }
            }
        }

        // Preload videos - just warm the file cache
        videoPattern.matcher(text).apply {
            while (find()) {
                val fileName = group(1)?.split(",")?.firstOrNull()?.trim()
                if (!fileName.isNullOrBlank()) {
                    ResourceFileCache.getFile(context, resourcesFolderUri, fileName)
                }
            }
        }

        // Preload HTML files
        htmlPattern.matcher(text).apply {
            while (find()) {
                val fileName = group(1)?.trim()
                if (!fileName.isNullOrBlank()) {
                    ResourceFileCache.getFile(context, resourcesFolderUri, fileName)
                }
            }
        }
    }
}
