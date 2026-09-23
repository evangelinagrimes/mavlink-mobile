package com.example.mavlinkapplication.domain

/**
 * KNOWN LIMITATION: [value] is Float even for UINT32/INT32 params. Float only
 * represents integers exactly up to 2^24 (16,777,216) — an int param above that would
 * display and set incorrectly. No ArduPilot Rover param observed so far comes close to
 * that range, but this wasn't fixed here because it needs a wider domain type
 * (Int/Long/Double) across every caller, not a contained change.
 */
data class VehicleParam(
    val id: String,
    val value: Float,
    val type: ParamType,
) {
    enum class ParamType {
        UINT8, INT8, UINT16, INT16, UINT32, INT32, REAL32, REAL64
    }
}
