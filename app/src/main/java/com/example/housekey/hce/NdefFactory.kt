package com.example.housekey.hce

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Builds the byte structures needed to emulate an NFC Forum Type 4 Tag:
 *   - NDEF records (Text and URI),
 *   - the NDEF file (a 2-byte length prefix followed by the NDEF message),
 *   - the Capability Container (CC) file that describes the NDEF file.
 *
 * These are consumed by [KeyHostApduService] when responding to a reader.
 */
object NdefFactory {

    /** Standard NDEF Tag Application AID selected by readers. */
    val NDEF_AID = byteArrayOf(0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01)

    /** Conventional file identifiers used inside the Type 4 application. */
    val CC_FILE_ID = byteArrayOf(0xE1.toByte(), 0x03)
    val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04)

    /**
     * Capability Container file (15 bytes):
     * CCLEN=000F, mapping v2.0, MLe=00F6, MLc=00FF, then the NDEF File Control
     * TLV: file id E104, max size 8000, read granted (00), write denied (FF).
     */
    val CC_FILE = byteArrayOf(
        0x00, 0x0F,             // CCLEN
        0x20,                   // mapping version 2.0
        0x00, 0xF6.toByte(),    // MLe (max R-APDU data)
        0x00, 0xFF.toByte(),    // MLc (max C-APDU data)
        0x04, 0x06,             // NDEF File Control TLV: T=04, L=06
        0xE1.toByte(), 0x04,    // NDEF file id
        0x80.toByte(), 0x00,    // max NDEF file size (32768)
        0x00,                   // read access granted
        0xFF.toByte(),          // write access denied (read-only)
    )

    /** URI prefix abbreviations from the NDEF URI RTD, indexed by code. */
    private val URI_PREFIXES = arrayOf(
        "", "http://www.", "https://www.", "http://", "https://", "tel:",
        "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://",
        "sftp://", "smb://", "nfs://", "ftp://", "dav://", "news:",
        "telnet://", "imap:", "rtsp://", "urn:", "pop:", "sip:", "sips:",
        "tftp:", "btspp://", "btl2cap://", "btgoep://", "tcpobex://",
        "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:",
        "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:",
    )

    /** Builds a well-known Text record payload for [text] with an "en" locale. */
    fun textRecord(text: String): ByteArray {
        val lang = "en".toByteArray(StandardCharsets.US_ASCII)
        val content = text.toByteArray(StandardCharsets.UTF_8)
        val payload = ByteArray(1 + lang.size + content.size)
        payload[0] = lang.size.toByte() // status byte: UTF-8, lang length in low bits
        System.arraycopy(lang, 0, payload, 1, lang.size)
        System.arraycopy(content, 0, payload, 1 + lang.size, content.size)
        return record(TYPE_TEXT, payload)
    }

    /** Builds a well-known URI record, abbreviating a known prefix when possible. */
    fun uriRecord(uri: String): ByteArray {
        var prefixCode = 0
        var rest = uri
        // Longest matching prefix wins (index 0 is the empty prefix, skip it).
        for (i in 1 until URI_PREFIXES.size) {
            val p = URI_PREFIXES[i]
            if (uri.startsWith(p) && p.length > URI_PREFIXES[prefixCode].length) {
                prefixCode = i
                rest = uri.substring(p.length)
            }
        }
        val restBytes = rest.toByteArray(StandardCharsets.UTF_8)
        val payload = ByteArray(1 + restBytes.size)
        payload[0] = prefixCode.toByte()
        System.arraycopy(restBytes, 0, payload, 1, restBytes.size)
        return record(TYPE_URI, payload)
    }

    /**
     * Wraps a single NDEF record as a complete NDEF file: a 2-byte big-endian
     * length (NLEN) followed by the record bytes. A single record has both the
     * Message Begin and Message End flags set.
     */
    fun ndefFileFromRecord(record: ByteArray): ByteArray {
        val len = record.size
        val out = ByteArray(2 + len)
        out[0] = ((len ushr 8) and 0xFF).toByte()
        out[1] = (len and 0xFF).toByte()
        System.arraycopy(record, 0, out, 2, len)
        return out
    }

    fun ndefFileForText(text: String): ByteArray = ndefFileFromRecord(textRecord(text))

    fun ndefFileForUri(uri: String): ByteArray = ndefFileFromRecord(uriRecord(uri))

    private const val TNF_WELL_KNOWN = 0x01
    private val TYPE_TEXT = byteArrayOf('T'.code.toByte())
    private val TYPE_URI = byteArrayOf('U'.code.toByte())

    /**
     * Assembles an NDEF record that is both the first and last record of its
     * message. Uses the compact short-record form (1-byte length) when the
     * payload fits in 255 bytes, and the long form (4-byte length) otherwise, so
     * long text/URIs are still encoded correctly.
     */
    private fun record(type: ByteArray, payload: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val shortRecord = payload.size <= 0xFF
        // Flags: MB(0x80) | ME(0x40) | [SR(0x10) when short] | TNF. Chunk/IL cleared.
        var flags = 0x80 or 0x40 or TNF_WELL_KNOWN
        if (shortRecord) flags = flags or 0x10
        out.write(flags)
        out.write(type.size)
        if (shortRecord) {
            out.write(payload.size and 0xFF)
        } else {
            out.write((payload.size ushr 24) and 0xFF)
            out.write((payload.size ushr 16) and 0xFF)
            out.write((payload.size ushr 8) and 0xFF)
            out.write(payload.size and 0xFF)
        }
        out.write(type)
        out.write(payload)
        return out.toByteArray()
    }
}
