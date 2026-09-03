package com.example.housekey

import com.example.housekey.hce.ActiveEmulation
import com.example.housekey.hce.ApduProcessor
import com.example.housekey.hce.NdefFactory
import com.example.housekey.util.Hex
import org.junit.Assert.assertEquals
import org.junit.Test

class ApduProcessorTest {

    private fun bytes(hex: String): ByteArray = Hex.decodeOrNull(hex)!!
    private fun hex(bytes: ByteArray): String = Hex.encode(bytes)

    // --- NDEF Type 4 tag flow --------------------------------------------------

    private fun ndefProcessor(text: String = "Hi") =
        ApduProcessor(ActiveEmulation.Ndef(NdefFactory.ndefFileForText(text)))

    @Test
    fun ndef_selectApplication_ok() {
        val p = ndefProcessor()
        assertEquals("9000", hex(p.process(bytes("00A4040007D276000085010100"))))
    }

    @Test
    fun ndef_selectWrongApplication_notFound() {
        val p = ndefProcessor()
        assertEquals("6A82", hex(p.process(bytes("00A4040007A0000000031010"))))
    }

    @Test
    fun ndef_fullReadSequence() {
        val p = ndefProcessor("Hi")
        assertEquals("9000", hex(p.process(bytes("00A4040007D276000085010100"))))

        // Select and read the Capability Container.
        assertEquals("9000", hex(p.process(bytes("00A4000C02E103"))))
        val cc = p.process(bytes("00B000000F"))
        assertEquals("000F20", hex(cc).substring(0, 6))
        assertEquals("9000", hex(cc).takeLast(4))

        // Select the NDEF file and read its 2-byte length (NLEN).
        assertEquals("9000", hex(p.process(bytes("00A4000C02E104"))))
        assertEquals("00099000", hex(p.process(bytes("00B0000002"))))

        // Read the NDEF message body (9 bytes) starting after NLEN.
        assertEquals("D101055402656E48699000", hex(p.process(bytes("00B0000209"))))
    }

    @Test
    fun ndef_readBinaryWithoutSelectedFile_notAllowed() {
        val p = ndefProcessor()
        p.process(bytes("00A4040007D276000085010100"))
        assertEquals("6986", hex(p.process(bytes("00B000000F"))))
    }

    @Test
    fun ndef_unknownInstruction_notSupported() {
        val p = ndefProcessor()
        p.process(bytes("00A4040007D276000085010100"))
        assertEquals("6D00", hex(p.process(bytes("00FF000000"))))
    }

    @Test
    fun ndef_selectUnknownFile_notFound() {
        val p = ndefProcessor()
        p.process(bytes("00A4040007D276000085010100"))
        assertEquals("6A82", hex(p.process(bytes("00A4000C02EFFF"))))
    }

    @Test
    fun deactivate_resetsSelectedFile() {
        val p = ndefProcessor()
        p.process(bytes("00A4040007D276000085010100"))
        p.process(bytes("00A4000C02E104"))
        p.reset()
        assertEquals("6986", hex(p.process(bytes("00B0000002"))))
    }

    // --- Raw APDU responder ----------------------------------------------------

    private fun rawProcessor(): ApduProcessor {
        val active = ActiveEmulation.Raw(
            aid = bytes("A0000000031010"),
            selectResponse = bytes("9000"),
            pairs = listOf(
                bytes("80CA9F7F00") to bytes("DEADBEEF9000"),
            ),
            fallbackSw = bytes("6D00"),
        )
        return ApduProcessor(active)
    }

    @Test
    fun raw_selectMatchingAid_returnsSelectResponse() {
        val p = rawProcessor()
        assertEquals("9000", hex(p.process(bytes("00A4040007A000000003101000"))))
    }

    @Test
    fun raw_selectWrongAid_notFound() {
        val p = rawProcessor()
        assertEquals("6A82", hex(p.process(bytes("00A4040007D276000085010100"))))
    }

    @Test
    fun raw_matchingCommand_returnsPairResponse() {
        val p = rawProcessor()
        p.process(bytes("00A4040007A000000003101000"))
        assertEquals("DEADBEEF9000", hex(p.process(bytes("80CA9F7F00"))))
    }

    @Test
    fun raw_unmatchedCommand_returnsFallback() {
        val p = rawProcessor()
        p.process(bytes("00A4040007A000000003101000"))
        assertEquals("6D00", hex(p.process(bytes("00B0000000"))))
    }

    // --- Shared error handling -------------------------------------------------

    @Test
    fun badClass_notSupported() {
        val p = ndefProcessor()
        assertEquals("6E00", hex(p.process(bytes("80A4040007D276000085010100"))))
    }

    @Test
    fun none_active_returnsNotFound() {
        val p = ApduProcessor(ActiveEmulation.None)
        assertEquals("6A82", hex(p.process(bytes("00A4040007D276000085010100"))))
    }
}
