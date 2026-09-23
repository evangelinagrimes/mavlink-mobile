package com.example.mavlinkapplication.mavsdk

import com.example.mavlinkapplication.domain.AttitudeData
import com.example.mavlinkapplication.domain.BatteryData
import com.example.mavlinkapplication.domain.GpsData
import com.example.mavlinkapplication.domain.GpsFixType
import com.example.mavlinkapplication.domain.HeartbeatData
import com.example.mavlinkapplication.domain.StatusTextEvent
import com.example.mavlinkapplication.domain.TelemetryRepository
import com.example.mavlinkapplication.domain.VfrHudData
import io.mavsdk.mavlink_direct.MavlinkDirect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.rx2.asFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CONFIRMED via jar inspection, MAVSDK-Java 3.17.4]
 *   MavlinkDirect.getMessage(name) → Flowable<MavlinkDirect.MavlinkMessage>
 *   MavlinkMessage.fieldsJson      → JSON string (getFieldsJson())
 *
 * Every stream here:
 *  - only accepts messages from componentId == AUTOPILOT_COMPONENT_ID. Other MAVLink
 *    components on the same link (companion computer, gimbal, a second GCS) also send
 *    some of these message types, and unfiltered would silently corrupt what we show.
 *  - drops (not defaults) a message that's missing the fields it needs, so a malformed
 *    packet is invisible rather than rendered as a fake zero/false value.
 *
 * Field names below are [CONFIRMED live against ArduRover SITL 2026-09-23] via a
 * temporary logging probe, not assumed from the MAVLink spec.
 */
@Singleton
class MavsdkTelemetryRepository @Inject constructor(
    private val connectionManager: MavsdkConnectionManager,
) : TelemetryRepository {

    private fun <T> subscribe(messageName: String, parse: (JSONObject) -> T?): Flow<T> =
        connectionManager.connected.flatMapLatest { isConnected ->
            if (!isConnected) return@flatMapLatest emptyFlow()
            val sys = connectionManager.system ?: return@flatMapLatest emptyFlow()
            sys.mavlinkDirect
                .getMessage(messageName)
                .toObservable()
                .asFlow()
                .filter { it.componentId == AUTOPILOT_COMPONENT_ID }
                .mapNotNull { msg ->
                    val json = try {
                        JSONObject(msg.fieldsJson ?: "")
                    } catch (_: Exception) {
                        null
                    } ?: return@mapNotNull null
                    parse(json)
                }
        }

    override val heartbeats: Flow<HeartbeatData> = subscribe("HEARTBEAT") { json ->
        if (!json.has("custom_mode") || !json.has("base_mode")) return@subscribe null
        val baseMode = json.optInt("base_mode")
        HeartbeatData(
            timestampMs = System.currentTimeMillis(),
            isArmed = (baseMode and 0x80) != 0, // MAV_MODE_FLAG_SAFETY_ARMED
            rawCustomMode = json.optInt("custom_mode"),
        )
    }

    override val battery: Flow<BatteryData> = subscribe("SYS_STATUS") { json ->
        if (!json.has("voltage_battery")) return@subscribe null
        BatteryData(
            voltageMv = json.optInt("voltage_battery"),
            remainingPercent = json.optInt("battery_remaining", -1),
        )
    }

    override val gps: Flow<GpsData> = subscribe("GPS_RAW_INT") { json ->
        if (!json.has("fix_type")) return@subscribe null
        val eph = json.optInt("eph", 65535)
        GpsData(
            fixType = GpsFixType.fromRaw(json.optInt("fix_type")),
            satellitesVisible = json.optInt("satellites_visible"),
            hdop = if (eph == 65535) null else eph / 100f,
        )
    }

    override val attitude: Flow<AttitudeData> = subscribe("ATTITUDE") { json ->
        if (!json.has("roll") || !json.has("pitch")) return@subscribe null
        AttitudeData(
            rollDeg = Math.toDegrees(json.optDouble("roll", 0.0)).toFloat(),
            pitchDeg = Math.toDegrees(json.optDouble("pitch", 0.0)).toFloat(),
            yawDeg = Math.toDegrees(json.optDouble("yaw", 0.0)).toFloat(),
        )
    }

    override val vfrHud: Flow<VfrHudData> = subscribe("VFR_HUD") { json ->
        if (!json.has("groundspeed")) return@subscribe null
        VfrHudData(
            groundSpeedMps = json.optDouble("groundspeed", 0.0).toFloat(),
            headingDeg = json.optInt("heading"),
            throttlePercent = json.optInt("throttle"),
        )
    }

    override val statusTexts: Flow<StatusTextEvent> = subscribe("STATUSTEXT") { json ->
        if (!json.has("text")) return@subscribe null
        StatusTextEvent(
            severity = json.optInt("severity", 6), // default to INFO if absent
            text = json.optString("text"),
        )
    }
}
