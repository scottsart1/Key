package com.example.housekey.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A stored credential the user can emulate.
 *
 * For NDEF keys, [content] holds the text or URI. For raw APDU keys, [aid],
 * [selectResponse], [apduPairs] and [fallbackSw] describe the responder and
 * [content] is unused.
 */
@Entity(tableName = "keys")
data class KeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: KeyType,
    val content: String = "",
    val aid: String = "",
    val selectResponse: String = "9000",
    /** Newline-separated "COMMANDHEX=RESPONSEHEX" pairs. */
    val apduPairs: String = "",
    val fallbackSw: String = "6D00",
    /** Free-form note, e.g. the UID captured when importing from a tag. */
    val note: String = "",
    val createdAt: Long = 0L,
)
