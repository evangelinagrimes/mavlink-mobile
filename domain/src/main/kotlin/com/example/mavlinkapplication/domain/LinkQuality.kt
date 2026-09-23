package com.example.mavlinkapplication.domain

sealed class LinkQuality {

    /** No heartbeat has ever been received. */
    data object NeverConnected : LinkQuality()

    /**
     * At least one heartbeat has arrived. Sub-states reflect freshness of the most recent one.
     * Armed/mode are always populated so the UI can display last-known state even when degraded.
     */
    sealed class EverConnected : LinkQuality() {
        abstract val isArmed: Boolean
        abstract val mode: RoverMode
        abstract val lastHeartbeatMs: Long

        /** Last heartbeat arrived within [LinkHealthEvaluator.FRESH_THRESHOLD_MS]. Link is live. */
        data class Fresh(
            override val isArmed: Boolean,
            override val mode: RoverMode,
            override val lastHeartbeatMs: Long,
        ) : EverConnected()

        /**
         * Heartbeat is late — between the fresh and lost thresholds.
         * Show a warning but keep showing last-known armed/mode.
         */
        data class Degraded(
            override val isArmed: Boolean,
            override val mode: RoverMode,
            override val lastHeartbeatMs: Long,
        ) : EverConnected()

        /**
         * No heartbeat for longer than [LinkHealthEvaluator.LOST_THRESHOLD_MS].
         * Mark the link lost but still show last-known armed/mode — never blank.
         */
        data class Lost(
            override val isArmed: Boolean,
            override val mode: RoverMode,
            override val lastHeartbeatMs: Long,
        ) : EverConnected()
    }
}
