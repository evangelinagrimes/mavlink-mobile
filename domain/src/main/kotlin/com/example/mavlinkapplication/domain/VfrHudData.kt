package com.example.mavlinkapplication.domain

/**
 * From VFR_HUD. [CONFIRMED live against SITL 2026-09-23]: groundspeed is m/s,
 * heading is degrees (0-360), throttle is percent.
 */
data class VfrHudData(
    val groundSpeedMps: Float,
    val headingDeg: Int,
    val throttlePercent: Int,
)
