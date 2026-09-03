package com.example.housekey

import com.example.housekey.data.ApduPairs
import com.example.housekey.util.Hex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApduPairsTest {

    @Test
    fun parse_readsValidPairsAndSkipsBlanks() {
        val text = "00A4040007A0000000031010=6300\n\n80CA9F7F00=DEADBEEF9000\n"
        val pairs = ApduPairs.parse(text)
        assertEquals(2, pairs.size)
        assertEquals("00A4040007A0000000031010", Hex.encode(pairs[0].first))
        assertEquals("6300", Hex.encode(pairs[0].second))
        assertEquals("DEADBEEF9000", Hex.encode(pairs[1].second))
    }

    @Test
    fun firstInvalidLine_flagsBadHex() {
        assertNull(ApduPairs.firstInvalidLine("00A4=9000\n\n80CA=DEAD"))
        assertEquals(1, ApduPairs.firstInvalidLine("00A4=9000\nZZ=9000"))
        assertEquals(0, ApduPairs.firstInvalidLine("nohexhere"))
    }
}
