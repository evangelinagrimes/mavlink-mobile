package com.example.mavlinkapplication.domain

interface ModeRepository {
    /**
     * Request a mode change by raw ArduPilot custom_mode number.
     * The MAVSDK implementation will need to use MAV_CMD_DO_SET_MODE or equivalent —
     * MAVSDK's high-level Action.setFlightMode() is PX4-oriented and won't map correctly
     * to ArduPilot Rover modes. See Phase 2 implementation notes.
     */
    suspend fun setMode(rawCustomMode: Int): Result<Unit>
}
