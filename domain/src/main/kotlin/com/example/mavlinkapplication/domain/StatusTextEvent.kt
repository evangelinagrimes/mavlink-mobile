package com.example.mavlinkapplication.domain

/**
 * From STATUSTEXT — the autopilot's own log/warning stream (PreArm failures, EKF
 * notices, etc). Delivery over this connection is confirmed: MAVSDK's own internal
 * logger already surfaces these (seen this session as "ArduPilot Ready" etc.), we
 * just weren't parsing them ourselves yet. severity is MAV_SEVERITY (0=EMERGENCY..7=DEBUG).
 */
data class StatusTextEvent(
    val severity: Int,
    val text: String,
)

enum class MessageSeverity { CRITICAL, WARNING, INFO } // coarse bucket for display

fun StatusTextEvent.severityBucket(): MessageSeverity = when {
    severity <= 3 -> MessageSeverity.CRITICAL // EMERGENCY, ALERT, CRITICAL, ERROR
    severity <= 5 -> MessageSeverity.WARNING  // WARNING, NOTICE
    else -> MessageSeverity.INFO              // INFO, DEBUG
}
