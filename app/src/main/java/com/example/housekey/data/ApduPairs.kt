package com.example.housekey.data

import com.example.housekey.util.Hex

/** Parses and validates the multiline "COMMANDHEX=RESPONSEHEX" editor text. */
object ApduPairs {

    /** Parses valid pairs, silently skipping blank or malformed lines. */
    fun parse(text: String): List<Pair<ByteArray, ByteArray>> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val idx = line.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                val cmd = Hex.decodeOrNull(line.substring(0, idx)) ?: return@mapNotNull null
                val resp = Hex.decodeOrNull(line.substring(idx + 1)) ?: return@mapNotNull null
                cmd to resp
            }
            .toList()

    /** Returns the 0-based index of the first malformed line, or null if all valid. */
    fun firstInvalidLine(text: String): Int? {
        text.lines().forEachIndexed { i, raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEachIndexed
            val idx = line.indexOf('=')
            if (idx <= 0) return i
            val cmd = Hex.decodeOrNull(line.substring(0, idx))
            val resp = Hex.decodeOrNull(line.substring(idx + 1))
            if (cmd == null || resp == null) return i
        }
        return null
    }
}
