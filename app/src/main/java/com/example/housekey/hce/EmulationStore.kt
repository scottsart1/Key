package com.example.housekey.hce

import android.content.Context
import com.example.housekey.util.Hex

/**
 * Persists which credential is active so that [KeyHostApduService] — which the OS
 * may start in a fresh process at tap time — can load it synchronously without
 * touching the Room database on the main thread.
 */
class EmulationStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** DB id of the active key, or -1 when none. Used by the UI to show state. */
    var activeKeyId: Long
        get() = prefs.getLong(KEY_ID, -1L)
        private set(value) = prefs.edit().putLong(KEY_ID, value).apply()

    fun setNdef(keyId: Long, ndefFile: ByteArray) {
        prefs.edit()
            .putString(KEY_TYPE, TYPE_NDEF)
            .putLong(KEY_ID, keyId)
            .putString(KEY_NDEF, Hex.encode(ndefFile))
            .apply()
    }

    fun setRaw(
        keyId: Long,
        aid: ByteArray,
        selectResponse: ByteArray,
        pairs: List<Pair<ByteArray, ByteArray>>,
        fallbackSw: ByteArray,
    ) {
        val serialized = pairs.joinToString(PAIR_SEP) {
            Hex.encode(it.first) + PAIR_KV + Hex.encode(it.second)
        }
        prefs.edit()
            .putString(KEY_TYPE, TYPE_RAW)
            .putLong(KEY_ID, keyId)
            .putString(KEY_AID, Hex.encode(aid))
            .putString(KEY_SELECT, Hex.encode(selectResponse))
            .putString(KEY_PAIRS, serialized)
            .putString(KEY_FALLBACK, Hex.encode(fallbackSw))
            .apply()
    }

    fun clear() {
        prefs.edit()
            .putString(KEY_TYPE, TYPE_NONE)
            .putLong(KEY_ID, -1L)
            .apply()
    }

    /** Reads the active configuration into a ready-to-serve [ActiveEmulation]. */
    fun load(): ActiveEmulation {
        return when (prefs.getString(KEY_TYPE, TYPE_NONE)) {
            TYPE_NDEF -> {
                val ndef = prefs.getString(KEY_NDEF, null)?.let { Hex.decodeOrNull(it) }
                    ?: return ActiveEmulation.None
                ActiveEmulation.Ndef(ndef)
            }

            TYPE_RAW -> {
                val aid = prefs.getString(KEY_AID, null)?.let { Hex.decodeOrNull(it) }
                    ?: return ActiveEmulation.None
                val select = prefs.getString(KEY_SELECT, null)?.let { Hex.decodeOrNull(it) }
                    ?: DEFAULT_SW_OK
                val fallback = prefs.getString(KEY_FALLBACK, null)?.let { Hex.decodeOrNull(it) }
                    ?: DEFAULT_SW_UNKNOWN
                val pairs = prefs.getString(KEY_PAIRS, "").orEmpty()
                    .split(PAIR_SEP)
                    .filter { it.contains(PAIR_KV) }
                    .mapNotNull { line ->
                        val (c, r) = line.split(PAIR_KV, limit = 2)
                        val cmd = Hex.decodeOrNull(c) ?: return@mapNotNull null
                        val resp = Hex.decodeOrNull(r) ?: return@mapNotNull null
                        cmd to resp
                    }
                ActiveEmulation.Raw(aid, select, pairs, fallback)
            }

            else -> ActiveEmulation.None
        }
    }

    private companion object {
        const val PREFS = "emulation_state"
        const val KEY_TYPE = "type"
        const val KEY_ID = "active_id"
        const val KEY_NDEF = "ndef"
        const val KEY_AID = "aid"
        const val KEY_SELECT = "select"
        const val KEY_PAIRS = "pairs"
        const val KEY_FALLBACK = "fallback"

        const val TYPE_NONE = "NONE"
        const val TYPE_NDEF = "NDEF"
        const val TYPE_RAW = "RAW"

        const val PAIR_SEP = "\n"
        const val PAIR_KV = "="

        val DEFAULT_SW_OK = byteArrayOf(0x90.toByte(), 0x00)
        val DEFAULT_SW_UNKNOWN = byteArrayOf(0x6D, 0x00)
    }
}
