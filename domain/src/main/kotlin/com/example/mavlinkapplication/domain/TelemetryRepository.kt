package com.example.mavlinkapplication.domain

import kotlinx.coroutines.flow.Flow

interface TelemetryRepository {
    /** Cold flow that emits each heartbeat as it arrives from the vehicle. */
    val heartbeats: Flow<HeartbeatData>
}
