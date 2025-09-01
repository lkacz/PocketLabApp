package com.lkacz.pola

/**
 * Normalizes malformed content commands (INSTRUCTION, SCALE, INPUTFIELD and randomized variants)
 * into a minimally valid placeholder form. Returns a new normalized line or null if no change.
 */
object ContentCommandNormalizer {
    fun normalize(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("//")) return null
        val parts = trimmed.split(';')
        val cmd = parts.firstOrNull()?.uppercase() ?: return null
        return when {
            cmd == "INSTRUCTION" -> normalizeInstruction(parts, line)
            cmd == "SCALE" || cmd == "SCALE[RANDOMIZED]" -> normalizeScale(parts, line)
            cmd == "INPUTFIELD" || cmd == "INPUTFIELD[RANDOMIZED]" -> normalizeInputField(parts, line)
            else -> null
        }
    }

    private fun normalizeInstruction(parts: List<String>, original: String): String? {
        if (original.count { it == ';' } == 3) return null // already correct segment count
        val header = parts.getOrNull(1).takeUnless { it.isNullOrBlank() } ?: "Header"
        val body = parts.getOrNull(2).takeUnless { it.isNullOrBlank() } ?: "Body"
        val cont = parts.getOrNull(3).takeUnless { it.isNullOrBlank() } ?: "Continue"
        val normalized = "INSTRUCTION;$header;$body;$cont"
        return if (normalized == original) null else normalized
    }

    private fun normalizeScale(parts: List<String>, original: String): String? {
        if (parts.size >= 2) return null // consider it already having at least one parameter
        val cmd = parts.firstOrNull() ?: "SCALE"
        val normalized = "$cmd;Header;Body;1;Continue"
        return if (normalized == original) null else normalized
    }

    private fun normalizeInputField(parts: List<String>, original: String): String? {
    if (parts.size >= 4) return null // already has minimum segments
    val cmd = parts.firstOrNull() ?: "INPUTFIELD"
    val header = parts.getOrNull(1).takeUnless { it.isNullOrBlank() } ?: "Header"
    val body = parts.getOrNull(2).takeUnless { it.isNullOrBlank() } ?: "Body"
    val normalized = "$cmd;$header;$body;field1;Continue"
        return if (normalized == original) null else normalized
    }
}
