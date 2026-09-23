package com.example.mavlinkapplication.domain

/** A single heartbeat received from the vehicle. [timestampMs] is local wall-clock millis at receipt. */
data class HeartbeatData(
    val timestampMs: Long,
    val isArmed: Boolean,
    val rawCustomMode: Int,
)
