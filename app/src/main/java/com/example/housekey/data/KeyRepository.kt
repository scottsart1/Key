package com.example.housekey.data

import com.example.housekey.hce.EmulationStore
import com.example.housekey.hce.HceManager
import com.example.housekey.hce.NdefFactory
import com.example.housekey.util.Hex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for stored keys and which one is being emulated.
 * Bridges the Room database, the emulation preferences and the HCE routing APIs.
 */
class KeyRepository(
    private val dao: KeyDao,
    private val store: EmulationStore,
    private val hce: HceManager,
) {

    val keys: Flow<List<KeyEntity>> = dao.observeAll()

    private val _activeKeyId = MutableStateFlow(store.activeKeyId)
    val activeKeyId: StateFlow<Long> = _activeKeyId.asStateFlow()

    suspend fun getById(id: Long): KeyEntity? = dao.getById(id)

    /** Inserts a new key or updates an existing one; returns its id. */
    suspend fun upsert(key: KeyEntity): Long {
        val id = if (key.id == 0L) {
            dao.insert(key.copy(createdAt = System.currentTimeMillis()))
        } else {
            dao.update(key)
            key.id
        }
        // Keep emulation in sync if the active key was edited.
        if (id == _activeKeyId.value) {
            dao.getById(id)?.let { setActive(it) }
        }
        return id
    }

    suspend fun delete(key: KeyEntity) {
        if (key.id == _activeKeyId.value) clearActive()
        dao.delete(key)
    }

    /** Makes [key] the emulated credential, updating routing and stored state. */
    fun setActive(key: KeyEntity) {
        when (key.type) {
            KeyType.NDEF_TEXT -> {
                hce.unregisterRawAids()
                store.setNdef(key.id, NdefFactory.ndefFileForText(key.content))
            }

            KeyType.NDEF_URI -> {
                hce.unregisterRawAids()
                store.setNdef(key.id, NdefFactory.ndefFileForUri(key.content))
            }

            KeyType.RAW_APDU -> {
                val aid = Hex.decodeOrNull(key.aid) ?: return
                val select = Hex.decodeOrNull(key.selectResponse) ?: byteArrayOf(0x90.toByte(), 0x00)
                val fallback = Hex.decodeOrNull(key.fallbackSw) ?: byteArrayOf(0x6D, 0x00)
                val pairs = ApduPairs.parse(key.apduPairs)
                hce.unregisterRawAids()
                hce.registerRawAid(aid)
                store.setRaw(key.id, aid, select, pairs, fallback)
            }
        }
        _activeKeyId.value = key.id
    }

    fun clearActive() {
        hce.unregisterRawAids()
        store.clear()
        _activeKeyId.value = -1L
    }

    // Capability/state passthrough for the UI.
    fun isNfcSupported(): Boolean = hce.isNfcSupported()
    fun isNfcEnabled(): Boolean = hce.isNfcEnabled()
    fun isHceSupported(): Boolean = hce.isHceSupported()
    fun isDefaultForNdef(): Boolean = hce.isDefaultForNdef()
}
