package com.example.mavlinkapplication.domain

import kotlinx.coroutines.flow.Flow

interface TelemetryRepository {
    /** Cold flow that emits each heartbeat as it arrives from the vehicle. */
    val heartbeats: Flow<HeartbeatData>

    /** From SYS_STATUS. */
    val battery: Flow<BatteryData>

    /** From GPS_RAW_INT. */
    val gps: Flow<GpsData>

    /** From ATTITUDE. */
    val attitude: Flow<AttitudeData>

    /** From VFR_HUD (ground speed, heading, throttle). */
    val vfrHud: Flow<VfrHudData>

    /** From STATUSTEXT — the autopilot's own log/warning stream. */
    val statusTexts: Flow<StatusTextEvent>
}
