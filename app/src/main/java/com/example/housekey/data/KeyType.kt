package com.example.housekey.data

/** The kind of credential a stored key represents and how it is emulated. */
enum class KeyType {
    /** Emulated as an NDEF Type 4 tag exposing a plain-text record. */
    NDEF_TEXT,

    /** Emulated as an NDEF Type 4 tag exposing a URI/URL record. */
    NDEF_URI,

    /** Emulated as a raw ISO-DEP responder for a custom AID. */
    RAW_APDU,
}
