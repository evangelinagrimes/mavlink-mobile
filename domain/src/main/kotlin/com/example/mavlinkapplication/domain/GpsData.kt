package com.example.mavlinkapplication.domain

/**
 * From GPS_RAW_INT. [CONFIRMED live against SITL 2026-09-23]: fix_type/satellites_visible
 * are plain integers, eph is HDOP * 100 with 65535 meaning "unknown" per the MAVLink spec.
 */
data class GpsData(
    val fixType: GpsFixType,
    val satellitesVisible: Int,
    val hdop: Float?,
)

enum class GpsFixType {
    NO_GPS, NO_FIX, FIX_2D, FIX_3D, DGPS, RTK_FLOAT, RTK_FIXED, STATIC, PPP, UNKNOWN;

    companion object {
        fun fromRaw(raw: Int): GpsFixType = when (raw) {
            0 -> NO_GPS
            1 -> NO_FIX
            2 -> FIX_2D
            3 -> FIX_3D
            4 -> DGPS
            5 -> RTK_FLOAT
            6 -> RTK_FIXED
            7 -> STATIC
            8 -> PPP
            else -> UNKNOWN
        }
    }
}
