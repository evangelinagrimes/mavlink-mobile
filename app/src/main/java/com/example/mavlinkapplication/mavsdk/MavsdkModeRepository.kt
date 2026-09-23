package com.example.mavlinkapplication.mavsdk

import com.example.mavlinkapplication.domain.ModeRepository
import io.mavsdk.mavlink_direct.MavlinkDirect
import kotlinx.coroutines.rx2.await
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements [ModeRepository] by sending MAV_CMD_DO_SET_MODE via MavlinkDirect.
 *
 * [CONFIRMED] Command format from ArduPilot docs (MAV_CMD_DO_SET_MODE = 176):
 *   param1 = 1.0  (MAV_MODE_FLAG_CUSTOM_MODE_ENABLED)
 *   param2 = rawCustomMode (ArduPilot Rover mode number, e.g. 10=AUTO, 11=RTL)
 *
 * [CONFIRMED via jar inspection]
 *   MavlinkDirect.MavlinkMessage(messageName, systemId, componentId, targetSystemId, targetComponentId, fieldsJson)
 *   MavlinkDirect.sendMessage(MavlinkMessage): Completable
 */
@Singleton
class MavsdkModeRepository @Inject constructor(
    private val connectionManager: MavsdkConnectionManager,
) : ModeRepository {

    override suspend fun setMode(rawCustomMode: Int): Result<Unit> {
        val sys = connectionManager.system
            ?: return Result.failure(IllegalStateException("Not connected"))

        return try {
            val fields = JSONObject().apply {
                put("command", 176)           // MAV_CMD_DO_SET_MODE
                put("param1", 1.0)            // MAV_MODE_FLAG_CUSTOM_MODE_ENABLED
                put("param2", rawCustomMode.toDouble())
                put("param3", 0.0)
                put("param4", 0.0)
                put("param5", 0.0)
                put("param6", 0.0)
                put("param7", 0.0)
                put("confirmation", 0)
            }.toString()

            val msg = MavlinkDirect.MavlinkMessage(
                /* messageName      = */ "COMMAND_LONG",
                /* systemId         = */ 255,
                /* componentId      = */ 0,
                /* targetSystemId   = */ 1,
                /* targetComponentId= */ 0,
                /* fieldsJson       = */ fields,
            )

            sys.mavlinkDirect.sendMessage(msg).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
