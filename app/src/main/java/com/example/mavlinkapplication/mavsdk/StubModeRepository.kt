package com.example.mavlinkapplication.mavsdk

import com.example.mavlinkapplication.domain.ModeRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub ModeRepository — no-op.
 * Replace with MavsdkModeRepository once the correct MAVLink command for
 * ArduPilot Rover mode changes is verified (see ModeRepository.kt comments).
 */
@Singleton
class StubModeRepository @Inject constructor() : ModeRepository {
    override suspend fun setMode(rawCustomMode: Int): Result<Unit> = Result.success(Unit)
}
