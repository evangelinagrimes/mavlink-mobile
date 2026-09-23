package com.example.mavlinkapplication.domain

/**
 * From ATTITUDE. [CONFIRMED live against SITL 2026-09-23]: roll/pitch/yaw arrive in
 * radians; converted to degrees at the repository boundary so nothing downstream
 * has to remember which unit it's holding.
 */
data class AttitudeData(
    val rollDeg: Float,
    val pitchDeg: Float,
    val yawDeg: Float,
)
