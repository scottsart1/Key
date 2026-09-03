package com.example.housekey

import com.example.housekey.util.Hex
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HexTest {

    @Test
    fun encode_isUppercaseNoSeparators() {
        assertEquals("00A4040007", Hex.encode(byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x07)))
    }

    @Test
    fun decode_roundTrips() {
        val bytes = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        assertArrayEquals(bytes, Hex.decodeOrNull(Hex.encode(bytes)))
    }

    @Test
    fun decode_ignoresWhitespaceAnd0xPrefix() {
        assertArrayEquals(byteArrayOf(0x90.toByte(), 0x00), Hex.decodeOrNull("0x90 00"))
    }

    @Test
    fun decode_rejectsOddLengthAndNonHex() {
        assertNull(Hex.decodeOrNull("ABC"))
        assertNull(Hex.decodeOrNull("ZZ"))
        assertNull(Hex.decodeOrNull(""))
    }

    @Test
    fun isValid_matchesDecode() {
        assertTrue(Hex.isValid("9000"))
        assertFalse(Hex.isValid("900"))
    }

    @Test
    fun encodeSpaced_insertsSpaces() {
        assertEquals("90 00 A4", Hex.encodeSpaced(byteArrayOf(0x90.toByte(), 0x00, 0xA4.toByte())))
    }
}
