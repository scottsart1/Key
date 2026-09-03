package com.example.housekey.util

/**
 * Small helpers for converting between hex strings and byte arrays. APDUs and
 * card payloads are handled as hex throughout the app so they are easy to store
 * and display.
 */
object Hex {

    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    /** Encodes bytes as an uppercase hex string with no separators. */
    fun encode(bytes: ByteArray): String {
        val out = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            out.append(HEX_CHARS[v ushr 4])
            out.append(HEX_CHARS[v and 0x0F])
        }
        return out.toString()
    }

    /** Encodes bytes as uppercase hex with a space between each byte. */
    fun encodeSpaced(bytes: ByteArray): String =
        bytes.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

    /**
     * Decodes a hex string to bytes. Whitespace and a leading "0x" are ignored.
     * Returns null when the input is not valid hexadecimal or has an odd length.
     */
    fun decodeOrNull(input: String): ByteArray? {
        val cleaned = input.trim()
            .removePrefix("0x")
            .removePrefix("0X")
            .filterNot { it.isWhitespace() }
        if (cleaned.isEmpty() || cleaned.length % 2 != 0) return null
        val out = ByteArray(cleaned.length / 2)
        var i = 0
        while (i < cleaned.length) {
            val hi = Character.digit(cleaned[i], 16)
            val lo = Character.digit(cleaned[i + 1], 16)
            if (hi < 0 || lo < 0) return null
            out[i / 2] = ((hi shl 4) or lo).toByte()
            i += 2
        }
        return out
    }

    /** True when [input] is valid, even-length hexadecimal. */
    fun isValid(input: String): Boolean = decodeOrNull(input) != null

    /** Concatenates byte arrays into one. */
    fun concat(vararg arrays: ByteArray): ByteArray {
        val size = arrays.sumOf { it.size }
        val out = ByteArray(size)
        var pos = 0
        for (a in arrays) {
            System.arraycopy(a, 0, out, pos, a.size)
            pos += a.size
        }
        return out
    }
}
