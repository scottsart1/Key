package com.example.housekey.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.housekey.HouseKeyApp
import com.example.housekey.data.KeyEntity
import com.example.housekey.data.KeyType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** List content plus which key is currently emulated. */
data class KeysUiState(
    val keys: List<KeyEntity> = emptyList(),
    val activeKeyId: Long = -1L,
)

/** Device NFC/HCE capability snapshot, refreshed when the app resumes. */
data class Capabilities(
    val nfcSupported: Boolean = true,
    val nfcEnabled: Boolean = true,
    val hceSupported: Boolean = true,
    val defaultForNdef: Boolean = true,
)

/** Editable form state for adding or editing a key. */
data class KeyDraft(
    val id: Long = 0L,
    val name: String = "",
    val type: KeyType = KeyType.NDEF_TEXT,
    val content: String = "",
    val aid: String = "",
    val selectResponse: String = "9000",
    val apduPairs: String = "",
    val fallbackSw: String = "6D00",
    val note: String = "",
) {
    fun toEntity(): KeyEntity = KeyEntity(
        id = id,
        name = name.trim(),
        type = type,
        content = content,
        aid = aid.filterNot { it.isWhitespace() }.uppercase(),
        selectResponse = selectResponse.filterNot { it.isWhitespace() }.uppercase(),
        apduPairs = apduPairs,
        fallbackSw = fallbackSw.filterNot { it.isWhitespace() }.uppercase(),
        note = note,
    )

    companion object {
        fun from(entity: KeyEntity) = KeyDraft(
            id = entity.id,
            name = entity.name,
            type = entity.type,
            content = entity.content,
            aid = entity.aid,
            selectResponse = entity.selectResponse,
            apduPairs = entity.apduPairs,
            fallbackSw = entity.fallbackSw,
            note = entity.note,
        )
    }
}

class KeyViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as HouseKeyApp).repository

    val uiState: StateFlow<KeysUiState> =
        combine(repository.keys, repository.activeKeyId) { keys, active ->
            KeysUiState(keys, active)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KeysUiState())

    private val _capabilities = MutableStateFlow(Capabilities())
    val capabilities: StateFlow<Capabilities> = _capabilities.asStateFlow()

    /** A draft pending because it was imported from a tag, consumed by the editor. */
    private val _pendingImport = MutableStateFlow<KeyDraft?>(null)
    val pendingImport: StateFlow<KeyDraft?> = _pendingImport.asStateFlow()

    fun refreshCapabilities() {
        _capabilities.value = Capabilities(
            nfcSupported = repository.isNfcSupported(),
            nfcEnabled = repository.isNfcEnabled(),
            hceSupported = repository.isHceSupported(),
            defaultForNdef = repository.isDefaultForNdef(),
        )
    }

    fun setActive(key: KeyEntity) = repository.setActive(key)

    fun stopEmulating() = repository.clearActive()

    fun save(draft: KeyDraft) {
        viewModelScope.launch { repository.upsert(draft.toEntity()) }
    }

    fun delete(key: KeyEntity) {
        viewModelScope.launch { repository.delete(key) }
    }

    suspend fun loadDraft(id: Long): KeyDraft? =
        repository.getById(id)?.let { KeyDraft.from(it) }

    fun stageImport(draft: KeyDraft) {
        _pendingImport.value = draft
    }

    fun consumePendingImport(): KeyDraft? {
        val value = _pendingImport.value
        _pendingImport.value = null
        return value
    }
}
