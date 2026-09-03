package com.example.housekey.hce

/**
 * Pure APDU state machine shared by [KeyHostApduService]. Kept free of Android
 * dependencies so its exact byte behaviour can be unit-tested on the JVM.
 *
 * Handles two emulation modes selected by the current [ActiveEmulation]:
 *  - NDEF Type 4 Tag: SELECT application / SELECT file (CC, NDEF) / READ BINARY.
 *    These are ISO 7816 commands (class byte 0x00) and anything else is rejected.
 *  - Raw APDU: a custom-AID responder driven by user-defined command/response
 *    pairs. Proprietary class bytes (e.g. 0x80) are common here, so the class byte
 *    is not restricted in this mode.
 */
class ApduProcessor(private var active: ActiveEmulation) {

    private enum class SelectedFile { NONE, CC, NDEF }

    private var selectedFile = SelectedFile.NONE

    /** Swaps in a new active configuration (called at the start of each tap). */
    fun setActive(newActive: ActiveEmulation) {
        active = newActive
    }

    /** Clears transient selection state when the field is lost. */
    fun reset() {
        selectedFile = SelectedFile.NONE
    }

    fun process(cmd: ByteArray): ByteArray {
        if (cmd.size < 4) return SW_WRONG_LENGTH
        return when (val a = active) {
            is ActiveEmulation.Ndef -> handleNdefMode(cmd)
            is ActiveEmulation.Raw -> handleRawMode(cmd, a)
            ActiveEmulation.None -> SW_FILE_NOT_FOUND
        }
    }

    // --- NDEF Type 4 tag mode --------------------------------------------------

    private fun handleNdefMode(cmd: ByteArray): ByteArray {
        if (isSelectByName(cmd)) {
            selectedFile = SelectedFile.NONE
            val aid = extractAid(cmd) ?: return SW_WRONG_LENGTH
            return if (aid.contentEquals(NdefFactory.NDEF_AID)) SW_OK else SW_FILE_NOT_FOUND
        }
        if (cmd[0].toInt() and 0xFF != 0x00) return SW_CLA_NOT_SUPPORTED
        return when (cmd[1].toInt() and 0xFF) {
            INS_SELECT -> handleSelectFile(cmd)
            INS_READ_BINARY -> handleReadBinary(cmd)
            else -> SW_INS_NOT_SUPPORTED
        }
    }

    private fun handleSelectFile(cmd: ByteArray): ByteArray {
        val p1 = cmd[2].toInt() and 0xFF
        if (p1 != 0x00 || cmd.size < 7) return SW_WRONG_P1P2
        val lc = cmd[4].toInt() and 0xFF
        if (lc != 2 || cmd.size < 5 + lc) return SW_WRONG_LENGTH
        val fileId = cmd.copyOfRange(5, 7)
        return when {
            fileId.contentEquals(NdefFactory.CC_FILE_ID) -> {
                selectedFile = SelectedFile.CC
                SW_OK
            }

            fileId.contentEquals(NdefFactory.NDEF_FILE_ID) -> {
                selectedFile = SelectedFile.NDEF
                SW_OK
            }

            else -> SW_FILE_NOT_FOUND
        }
    }

    private fun handleReadBinary(cmd: ByteArray): ByteArray {
        val ndef = active as? ActiveEmulation.Ndef ?: return SW_COMMAND_NOT_ALLOWED
        val file = when (selectedFile) {
            SelectedFile.CC -> NdefFactory.CC_FILE
            SelectedFile.NDEF -> ndef.ndefFile
            SelectedFile.NONE -> return SW_COMMAND_NOT_ALLOWED
        }
        if (cmd.size < 5) return SW_WRONG_LENGTH
        val offset = ((cmd[2].toInt() and 0xFF) shl 8) or (cmd[3].toInt() and 0xFF)
        if (offset > file.size) return SW_WRONG_P1P2
        var le = cmd[4].toInt() and 0xFF
        if (le == 0) le = 256
        val length = minOf(le, file.size - offset)
        return file.copyOfRange(offset, offset + length) + SW_OK
    }

    // --- Raw APDU mode ---------------------------------------------------------

    private fun handleRawMode(cmd: ByteArray, a: ActiveEmulation.Raw): ByteArray {
        if (isSelectByName(cmd)) {
            selectedFile = SelectedFile.NONE
            val aid = extractAid(cmd) ?: return SW_WRONG_LENGTH
            return if (aid.contentEquals(a.aid)) a.selectResponse else SW_FILE_NOT_FOUND
        }
        for ((command, response) in a.pairs) {
            if (cmd.contentEquals(command)) return response
        }
        return a.fallbackSw
    }

    // --- Helpers ---------------------------------------------------------------

    private fun extractAid(cmd: ByteArray): ByteArray? {
        if (cmd.size < 5) return null
        val lc = cmd[4].toInt() and 0xFF
        if (lc < 5 || lc > 16 || cmd.size < 5 + lc) return null
        return cmd.copyOfRange(5, 5 + lc)
    }

    companion object {
        const val INS_SELECT = 0xA4
        const val INS_READ_BINARY = 0xB0

        val SW_OK = byteArrayOf(0x90.toByte(), 0x00)
        val SW_FILE_NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())
        val SW_WRONG_P1P2 = byteArrayOf(0x6B, 0x00)
        val SW_WRONG_LENGTH = byteArrayOf(0x67, 0x00)
        val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D, 0x00)
        val SW_CLA_NOT_SUPPORTED = byteArrayOf(0x6E, 0x00)
        val SW_COMMAND_NOT_ALLOWED = byteArrayOf(0x69, 0x86.toByte())

        /**
         * True when [cmd] is an ISO 7816 SELECT-by-name (application select):
         * class 0x00, instruction 0xA4, P1 0x04.
         */
        fun isSelectByName(cmd: ByteArray): Boolean =
            cmd.size >= 5 &&
                (cmd[0].toInt() and 0xFF) == 0x00 &&
                (cmd[1].toInt() and 0xFF) == INS_SELECT &&
                (cmd[2].toInt() and 0xFF) == 0x04
    }
}
