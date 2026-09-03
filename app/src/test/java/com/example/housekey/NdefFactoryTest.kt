package com.example.housekey

import com.example.housekey.hce.NdefFactory
import com.example.housekey.util.Hex
import org.junit.Assert.assertEquals
import org.junit.Test

class NdefFactoryTest {

    @Test
    fun textRecord_matchesSpec() {
        // "Hi": header D1, type len 01, payload len 05, 'T'(54), status 02, "en", "Hi".
        // NDEF file prefixes the 2-byte length (0009).
        assertEquals(
            "0009D101055402656E4869",
            Hex.encode(NdefFactory.ndefFileForText("Hi")),
        )
    }

    @Test
    fun uriRecord_abbreviatesKnownPrefix() {
        // "https://" -> prefix code 04, rest "example.com".
        assertEquals(
            "0010D1010C55046578616D706C652E636F6D",
            Hex.encode(NdefFactory.ndefFileForUri("https://example.com")),
        )
    }

    @Test
    fun capabilityContainer_is15BytesAndWellFormed() {
        val cc = NdefFactory.CC_FILE
        assertEquals(15, cc.size)
        assertEquals("000F", Hex.encode(cc.copyOfRange(0, 2))) // CCLEN
        assertEquals("E104", Hex.encode(cc.copyOfRange(9, 11))) // NDEF file id
        assertEquals(0x00.toByte(), cc[13]) // read granted
        assertEquals(0xFF.toByte(), cc[14]) // write denied
    }
}
