package com.example.mavlinkapplication.domain

/**
 * Pure, stateless evaluator for link health.
 *
 * Call [evaluate] on every received heartbeat AND on a periodic tick (500ms is a good interval)
 * so a silent link visibly degrades instead of freezing on the last-good reading.
 */
object LinkHealthEvaluator {

    /** Heartbeats younger than this are considered live. ArduPilot sends at 1 Hz. */
    const val FRESH_THRESHOLD_MS: Long = 1_500L

    /** Heartbeats older than this mean the link is lost. */
    const val LOST_THRESHOLD_MS: Long = 5_000L

    fun evaluate(lastHeartbeat: HeartbeatData?, nowMs: Long): LinkQuality {
        lastHeartbeat ?: return LinkQuality.NeverConnected

        val ageMs = nowMs - lastHeartbeat.timestampMs
        val mode  = RoverMode.fromRaw(lastHeartbeat.rawCustomMode)

        return when {
            ageMs < FRESH_THRESHOLD_MS -> LinkQuality.EverConnected.Fresh(
                isArmed          = lastHeartbeat.isArmed,
                mode             = mode,
                lastHeartbeatMs  = lastHeartbeat.timestampMs,
            )
            ageMs < LOST_THRESHOLD_MS -> LinkQuality.EverConnected.Degraded(
                isArmed          = lastHeartbeat.isArmed,
                mode             = mode,
                lastHeartbeatMs  = lastHeartbeat.timestampMs,
            )
            else -> LinkQuality.EverConnected.Lost(
                isArmed          = lastHeartbeat.isArmed,
                mode             = mode,
                lastHeartbeatMs  = lastHeartbeat.timestampMs,
            )
        }
    }
}
