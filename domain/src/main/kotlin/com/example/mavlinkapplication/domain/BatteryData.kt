package com.example.mavlinkapplication.domain

/**
 * From SYS_STATUS. [CONFIRMED live against SITL 2026-09-23]: voltage_battery is
 * millivolts, battery_remaining is percent (-1 if the autopilot doesn't know it).
 */
data class BatteryData(
    val voltageMv: Int,
    val remainingPercent: Int,
)
