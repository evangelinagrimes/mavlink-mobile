package com.example.mavlinkapplication.mavsdk

import com.example.mavlinkapplication.domain.CommandException
import io.mavsdk.System as MavsdkSystem
import io.mavsdk.mavlink_direct.MavlinkDirect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/** MAV_COMP_ID_AUTOPILOT1 — [CONFIRMED live against SITL 2026-09-23]. */
internal const val AUTOPILOT_COMPONENT_ID = 1

private const val ACK_TIMEOUT_MS = 800L
private const val MAX_RETRIES = 3

/**
 * Sends a MAV_CMD as COMMAND_LONG and waits for a matching COMMAND_ACK, resending up
 * to [MAX_RETRIES] times if no ack arrives within [ACK_TIMEOUT_MS] — MAVLink's command
 * protocol expects the sender to retry on silence, since the link can drop a packet
 * without either side knowing.
 *
 * Known limitation: COMMAND_LONG has no per-request id, so if the same [command] is
 * sent twice in quick succession, an ack could be attributed to the wrong of the two
 * in-flight requests. The protocol doesn't give us a way to disambiguate.
 */
internal suspend fun MavsdkSystem.sendCommandLong(
    command: Int,
    param1: Double = 0.0,
    param2: Double = 0.0,
    param3: Double = 0.0,
    param4: Double = 0.0,
    param5: Double = 0.0,
    param6: Double = 0.0,
    param7: Double = 0.0,
): Result<Unit> {
    val ackFlow = mavlinkDirect.getMessage("COMMAND_ACK")
        .toObservable().asFlow()
        .filter { it.componentId == AUTOPILOT_COMPONENT_ID }
        .mapNotNull { msg ->
            val json = try {
                JSONObject(msg.fieldsJson ?: "")
            } catch (_: Exception) {
                null
            } ?: return@mapNotNull null
            if (json.optInt("command", -1) != command) return@mapNotNull null
            if (!json.has("result")) return@mapNotNull null
            json.optInt("result")
        }

    repeat(MAX_RETRIES) { attempt ->
        val fields = JSONObject().apply {
            put("command", command)
            put("param1", param1)
            put("param2", param2)
            put("param3", param3)
            put("param4", param4)
            put("param5", param5)
            put("param6", param6)
            put("param7", param7)
            put("confirmation", attempt)
        }.toString()
        val msg = MavlinkDirect.MavlinkMessage("COMMAND_LONG", 255, 0, 1, 0, fields)
        try {
            mavlinkDirect.sendMessage(msg).await()
        } catch (_: Exception) {
            // Send failed at the transport level — fall through and retry.
        }

        val result = withTimeoutOrNull(ACK_TIMEOUT_MS) { ackFlow.first() }
        if (result != null) {
            return if (result == 0) {
                Result.success(Unit) // MAV_RESULT_ACCEPTED
            } else {
                Result.failure(CommandException.Rejected(result))
            }
        }
    }
    return Result.failure(CommandException.NoAck)
}
