package com.example.housekey.hce

/**
 * The credential currently selected for emulation, in ready-to-serve form.
 * Deliberately free of Android dependencies so [ApduProcessor] stays unit-testable.
 */
sealed interface ActiveEmulation {
    /** Nothing is being emulated. */
    data object None : ActiveEmulation

    /** A Type 4 NDEF tag exposing [ndefFile] (2-byte length prefix + message). */
    class Ndef(val ndefFile: ByteArray) : ActiveEmulation

    /**
     * A raw ISO-DEP responder for a custom [aid]. After selection it replies with
     * [selectResponse]; later commands are matched against [pairs] (exact match on
     * the command bytes), falling back to [fallbackSw] when nothing matches.
     */
    class Raw(
        val aid: ByteArray,
        val selectResponse: ByteArray,
        val pairs: List<Pair<ByteArray, ByteArray>>,
        val fallbackSw: ByteArray,
    ) : ActiveEmulation
}
