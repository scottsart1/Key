package com.example.housekey.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import com.example.housekey.data.KeyType
import com.example.housekey.util.Hex
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/** The result of reading a physical tag, ready to display or import. */
data class TagReading(
    val uid: String,
    val techs: List<String>,
    /** Decoded NDEF text or URI, or null if the tag carries no NDEF message. */
    val ndefContent: String?,
    /** The importable key type when NDEF content was decoded, else null. */
    val importType: KeyType?,
)

/** Parses a discovered [Tag] into a [TagReading] without any blocking I/O. */
object NfcReader {

    fun read(tag: Tag): TagReading {
        val uid = Hex.encodeSpaced(tag.id ?: ByteArray(0))
        val techs = tag.techList.orEmpty().map { it.substringAfterLast('.') }

        val message: NdefMessage? = runCatching { Ndef.get(tag)?.cachedNdefMessage }.getOrNull()
        val record = message?.records?.firstOrNull()
        val (content, type) = record?.let { decode(it) } ?: (null to null)

        return TagReading(uid = uid, techs = techs, ndefContent = content, importType = type)
    }

    private fun decode(record: NdefRecord): Pair<String, KeyType>? {
        if (record.tnf != NdefRecord.TNF_WELL_KNOWN) return null
        return when {
            record.type.contentEquals(NdefRecord.RTD_TEXT) -> decodeText(record)?.let { it to KeyType.NDEF_TEXT }
            record.type.contentEquals(NdefRecord.RTD_URI) ->
                record.toUri()?.toString()?.let { it to KeyType.NDEF_URI }

            else -> null
        }
    }

    private fun decodeText(record: NdefRecord): String? {
        val payload = record.payload
        if (payload.isEmpty()) return null
        val status = payload[0].toInt()
        val isUtf16 = (status and 0x80) != 0
        val langLength = status and 0x3F
        if (payload.size < 1 + langLength) return null
        val charset: Charset = if (isUtf16) StandardCharsets.UTF_16 else StandardCharsets.UTF_8
        return String(
            payload,
            1 + langLength,
            payload.size - 1 - langLength,
            charset,
        )
    }
}
