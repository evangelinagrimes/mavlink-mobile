package com.example.mavlinkapplication.mavsdk

import com.example.mavlinkapplication.domain.ArmRepository
import com.example.mavlinkapplication.domain.CommandException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MAV_CMD_COMPONENT_ARM_DISARM = 400. param1 = 0 (disarm). param2 = 21196 is the
 * documented "force" magic value in the MAVLink common.xml spec for this command —
 * bypasses the autopilot's normal disarm checks. NOT independently jar-verified
 * (it's a protocol constant, not an SDK API), but confirmed to actually disarm SITL
 * in this session's testing.
 */
private const val FORCE_MAGIC = 21196.0

@Singleton
class MavsdkArmRepository @Inject constructor(
    private val connectionManager: MavsdkConnectionManager,
) : ArmRepository {

    override suspend fun forceDisarm(): Result<Unit> {
        val sys = connectionManager.system
            ?: return Result.failure(CommandException.NotConnected)

        return sys.sendCommandLong(
            command = 400, // MAV_CMD_COMPONENT_ARM_DISARM
            param1 = 0.0,  // disarm
            param2 = FORCE_MAGIC,
        )
    }
}
