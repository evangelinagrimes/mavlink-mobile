package com.example.mavlinkapplication.ui.status

import com.example.mavlinkapplication.domain.MessageSeverity

/**
 * Fields genuinely not derivable from data we have today — not fabricated numbers.
 * Speed, roll/pitch, EKF, GPS, and the vehicle message log are now real (StatusViewModel).
 *
 * Remaining Phase-2 TODO:
 *   linkMetrics             -> RADIO_STATUS (signal strength); packet loss needs our own
 *                              sequence-number gap tracking, MAVSDK doesn't expose it directly
 *   vehicleConfig.firmware  -> AUTOPILOT_VERSION
 *   vehicleConfig.roverId   -> heartbeat's system/component id (already in the raw MAVLink
 *                              message, just not surfaced through HeartbeatData yet)
 */
data class VehicleConfig(
    val roverName: String,
    val connectionType: String,
    val roverId: String,
    val baud: String,
    val firmware: String,
    val mac: String,
)

data class LinkMetrics(
    val signalStrengthDbm: Float?,
    val packetLossPercent: Float?,
)

data class VehicleMessage(
    val timestamp: String,
    val timestampMs: Long,
    val text: String,
    val severity: MessageSeverity = MessageSeverity.INFO,
)

object MockTelemetry {
    val vehicleConfig = VehicleConfig(
        roverName = "ArduPilot Rover",
        connectionType = "TCP", // true today — MavsdkConnectionManager always connects via tcpout://
        roverId = "—",
        baud = "N/A", // not applicable over TCP; meaningful once a serial link is supported
        firmware = "—",
        mac = "—",
    )

    val linkMetrics = LinkMetrics(
        signalStrengthDbm = null,
        packetLossPercent = null,
    )
}
