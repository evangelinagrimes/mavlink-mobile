package com.example.mavlinkapplication.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RoverModeTest {

    @Test fun `raw 0 maps to Manual`() {
        assertEquals(RoverMode.Manual, RoverMode.fromRaw(0))
    }

    @Test fun `raw 10 maps to Auto`() {
        assertEquals(RoverMode.Auto, RoverMode.fromRaw(10))
    }

    @Test fun `raw 11 maps to Rtl`() {
        assertEquals(RoverMode.Rtl, RoverMode.fromRaw(11))
    }

    @Test fun `raw 12 maps to SmartRtl`() {
        assertEquals(RoverMode.SmartRtl, RoverMode.fromRaw(12))
    }

    @Test fun `raw 15 maps to Guided`() {
        assertEquals(RoverMode.Guided, RoverMode.fromRaw(15))
    }

    @Test fun `unrecognized raw returns UnknownMode carrying the value`() {
        val mode = assertIs<RoverMode.UnknownMode>(RoverMode.fromRaw(999))
        assertEquals(999, mode.rawValue)
    }

    @Test fun `raw 2 is not a defined mode`() {
        assertIs<RoverMode.UnknownMode>(RoverMode.fromRaw(2))
    }

    @Test fun `negative raw returns UnknownMode`() {
        assertIs<RoverMode.UnknownMode>(RoverMode.fromRaw(-1))
    }

    @Test fun `allKnown is non-empty`() {
        assertTrue(RoverMode.allKnown.isNotEmpty())
    }

    @Test fun `allKnown contains Manual`() {
        assertNotNull(RoverMode.allKnown.find { it == RoverMode.Manual })
    }

    @Test fun `allKnown does not contain UnknownMode instances`() {
        assertTrue(RoverMode.allKnown.none { it is RoverMode.UnknownMode })
    }
}
