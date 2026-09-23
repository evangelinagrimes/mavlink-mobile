package com.example.mavlinkapplication.mavsdk

import com.example.mavlinkapplication.domain.CommandException
import com.example.mavlinkapplication.domain.ModeRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements [ModeRepository] by sending MAV_CMD_DO_SET_MODE via COMMAND_LONG, waiting
 * for COMMAND_ACK (with retry — see [sendCommandLong]).
 *
 * [CONFIRMED] Command format from ArduPilot docs (MAV_CMD_DO_SET_MODE = 176):
 *   param1 = 1.0  (MAV_MODE_FLAG_CUSTOM_MODE_ENABLED)
 *   param2 = rawCustomMode (ArduPilot Rover mode number, e.g. 10=AUTO, 11=RTL)
 *
 * A successful [Result] here means the vehicle acknowledged the request — it does not
 * by itself mean the mode has actually changed yet. The caller (StatusViewModel) still
 * confirms the actual transition against the next heartbeat's custom_mode.
 */
@Singleton
class MavsdkModeRepository @Inject constructor(
    private val connectionManager: MavsdkConnectionManager,
) : ModeRepository {

    override suspend fun setMode(rawCustomMode: Int): Result<Unit> {
        val sys = connectionManager.system
            ?: return Result.failure(CommandException.NotConnected)

        return sys.sendCommandLong(
            command = 176, // MAV_CMD_DO_SET_MODE
            param1 = 1.0,
            param2 = rawCustomMode.toDouble(),
        )
    }
}
