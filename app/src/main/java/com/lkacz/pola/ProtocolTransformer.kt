package com.lkacz.pola

import kotlin.random.Random

/**
 * Pure protocol transformation pipeline extracted from ProtocolManager to enable
 * deterministic unit testing without Android dependencies.
 * Steps:
 * 1. Merge multi-line recognized commands (INPUTFIELD; etc.)
 * 2. Apply RANDOMIZE_ON / RANDOMIZE_OFF shuffling blocks
 * 3. Expand SCALE / SCALE[RANDOMIZED] multi-item lines
 */
object ProtocolTransformer {
    fun transform(originalProtocol: String?, rnd: Random = Random): String {
        if (originalProtocol == null) return ""
        val rawLines = originalProtocol.lines()
        val singleLineCommands = mergeMultiLineCommands(rawLines)
        val newLines = mutableListOf<String>()
        var inRandom = false
        val randomBuf = mutableListOf<String>()
        for (line in singleLineCommands) {
            when (line.trim().uppercase()) {
                "RANDOMIZE_ON" -> inRandom = true
                "RANDOMIZE_OFF" -> {
                    inRandom = false
                    randomBuf.shuffle(rnd)
                    randomBuf.forEach { newLines.addAll(MultiScaleHelper.expandScaleLine(it, rnd)) }
                    randomBuf.clear()
                }
                else -> if (inRandom) randomBuf.add(line) else newLines.addAll(MultiScaleHelper.expandScaleLine(line, rnd))
            }
        }
        if (inRandom && randomBuf.isNotEmpty()) {
            randomBuf.shuffle(rnd)
            randomBuf.forEach { newLines.addAll(MultiScaleHelper.expandScaleLine(it, rnd)) }
            randomBuf.clear()
        }
        return newLines.joinToString("\n")
    }

    // ---- Internal helpers (copied & trimmed from original ProtocolManager) ----
    private fun mergeMultiLineCommands(rawLines: List<String>): List<String> {
        val recognized = setOf(
            // Only commands realistically spanning multiple lines
            "INPUTFIELD","INPUTFIELD[RANDOMIZED]","INSTRUCTION","HTML"
        )
        val merged = mutableListOf<String>()
        var buf = StringBuilder()
        var merging = false
        for (orig in rawLines) {
            val line = orig.trim()
            if (line.isEmpty()) {
                if (!merging) merged.add(line) else buf.append(" ")
                continue
            }
            val split = line.split(";").map { it.trim() }
            val first = split.firstOrNull()?.uppercase()
            if (!merging) {
                if (first != null && recognized.contains(first)) {
                    val hasTrailing = orig.trimEnd().endsWith(";") && line.count { it == ';' } == 1
                    if (line.endsWith(";") && split.size == 1) {
                        buf = StringBuilder(line.removeSuffix(";")); merging = true; continue
                    } else if (hasTrailing || lineEndsWithSemicolonButIncomplete(split)) {
                        buf = StringBuilder(line.removeSuffix(";")); merging = true; continue
                    } else {
                        merged.add(line)
                    }
                } else merged.add(line)
            } else {
                val endsWith = line.endsWith(";")
                val content = if (endsWith) line.removeSuffix(";") else line
                buf.append(";").append(content)
                if (!endsWith) { merged.add(buf.toString()); merging = false }
            }
        }
        if (merging && buf.isNotEmpty()) merged.add(buf.toString())
        return merged
    }
    private fun lineEndsWithSemicolonButIncomplete(split: List<String>) = split.size < 3
}
