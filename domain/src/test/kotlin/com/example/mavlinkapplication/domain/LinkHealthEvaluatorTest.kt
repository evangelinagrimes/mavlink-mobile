package com.example.mavlinkapplication.domain

import com.example.mavlinkapplication.domain.LinkHealthEvaluator.FRESH_THRESHOLD_MS
import com.example.mavlinkapplication.domain.LinkHealthEvaluator.LOST_THRESHOLD_MS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LinkHealthEvaluatorTest {

    private val now = 100_000L

    /** Helper: create a HeartbeatData that arrived [ageMs] milliseconds before [now]. */
    private fun hb(
        ageMs: Long = 0,
        isArmed: Boolean = false,
        rawCustomMode: Int = 0,
    ) = HeartbeatData(
        timestampMs  = now - ageMs,
        isArmed      = isArmed,
        rawCustomMode = rawCustomMode,
    )

    // ── NeverConnected ──────────────────────────────────────────────────────────

    @Test fun `no heartbeat ever returns NeverConnected`() {
        assertEquals(LinkQuality.NeverConnected, LinkHealthEvaluator.evaluate(null, now))
    }

    // ── Fresh ───────────────────────────────────────────────────────────────────

    @Test fun `age 0 is Fresh`() {
        assertIs<LinkQuality.EverConnected.Fresh>(LinkHealthEvaluator.evaluate(hb(0), now))
    }

    @Test fun `age one ms inside fresh threshold is Fresh`() {
        assertIs<LinkQuality.EverConnected.Fresh>(
            LinkHealthEvaluator.evaluate(hb(FRESH_THRESHOLD_MS - 1), now)
        )
    }

    // ── Degraded ────────────────────────────────────────────────────────────────

    @Test fun `age at exactly fresh threshold boundary is Degraded`() {
        assertIs<LinkQuality.EverConnected.Degraded>(
            LinkHealthEvaluator.evaluate(hb(FRESH_THRESHOLD_MS), now)
        )
    }

    @Test fun `age midway between thresholds is Degraded`() {
        assertIs<LinkQuality.EverConnected.Degraded>(
            LinkHealthEvaluator.evaluate(hb(3_000), now)
        )
    }

    @Test fun `age one ms inside lost threshold is Degraded`() {
        assertIs<LinkQuality.EverConnected.Degraded>(
            LinkHealthEvaluator.evaluate(hb(LOST_THRESHOLD_MS - 1), now)
        )
    }

    // ── Lost ────────────────────────────────────────────────────────────────────

    @Test fun `age at exactly lost threshold boundary is Lost`() {
        assertIs<LinkQuality.EverConnected.Lost>(
            LinkHealthEvaluator.evaluate(hb(LOST_THRESHOLD_MS), now)
        )
    }

    @Test fun `age well beyond lost threshold is Lost`() {
        assertIs<LinkQuality.EverConnected.Lost>(
            LinkHealthEvaluator.evaluate(hb(30_000), now)
        )
    }

    // ── armed/mode carry-through ────────────────────────────────────────────────

    @Test fun `isArmed preserved through Degraded`() {
        val result = LinkHealthEvaluator.evaluate(hb(3_000, isArmed = true), now)
        assertIs<LinkQuality.EverConnected.Degraded>(result)
        assertTrue(result.isArmed)
    }

    @Test fun `isArmed preserved through Lost`() {
        val result = LinkHealthEvaluator.evaluate(hb(10_000, isArmed = true), now)
        assertIs<LinkQuality.EverConnected.Lost>(result)
        assertTrue(result.isArmed)
    }

    @Test fun `mode preserved through Degraded`() {
        val result = LinkHealthEvaluator.evaluate(hb(3_000, rawCustomMode = 10), now)
        assertIs<LinkQuality.EverConnected.Degraded>(result)
        assertEquals(RoverMode.Auto, result.mode)
    }

    @Test fun `mode preserved through Lost`() {
        val result = LinkHealthEvaluator.evaluate(hb(10_000, rawCustomMode = 11), now)
        assertIs<LinkQuality.EverConnected.Lost>(result)
        assertEquals(RoverMode.Rtl, result.mode)
    }

    // ── unknown mode ────────────────────────────────────────────────────────────

    @Test fun `unrecognized custom mode returns UnknownMode carrying the raw value`() {
        val result = LinkHealthEvaluator.evaluate(hb(0, rawCustomMode = 999), now)
        assertIs<LinkQuality.EverConnected.Fresh>(result)
        val mode = assertIs<RoverMode.UnknownMode>(result.mode)
        assertEquals(999, mode.rawValue)
    }
}
