package com.example.mavlinkapplication.mavsdk

import com.example.mavlinkapplication.domain.HeartbeatData
import com.example.mavlinkapplication.domain.TelemetryRepository
import io.mavsdk.mavlink_direct.MavlinkDirect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CONFIRMED via jar inspection, MAVSDK-Java 3.17.4]
 *   MavlinkDirect.getMessage("HEARTBEAT") → Flowable<MavlinkDirect.MavlinkMessage>
 *   MavlinkMessage.fieldsJson            → JSON string (getFieldsJson())
 *
 * Bridges Flowable<T> to Flow<T> via .toObservable().asFlow() because
 * kotlinx-coroutines-rx2 only has asFlow() for ObservableSource, not Publisher.
 *
 * flatMapLatest on connected: emits emptyFlow while disconnected, switches to
 * the real heartbeat stream as soon as connect() completes.
 */
@Singleton
class MavsdkTelemetryRepository @Inject constructor(
    private val connectionManager: MavsdkConnectionManager,
) : TelemetryRepository {

    override val heartbeats: Flow<HeartbeatData> =
        connectionManager.connected.flatMapLatest { isConnected ->
            if (!isConnected) return@flatMapLatest emptyFlow()
            val sys = connectionManager.system ?: return@flatMapLatest emptyFlow()
            sys.mavlinkDirect
                .getMessage("HEARTBEAT")
                .toObservable()
                .asFlow()
                .map { msg -> parseHeartbeat(msg) }
        }

    private fun parseHeartbeat(msg: MavlinkDirect.MavlinkMessage): HeartbeatData {
        val json = try {
            JSONObject(msg.fieldsJson ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        val customMode = json.optInt("custom_mode", 0)
        val baseMode   = json.optInt("base_mode", 0)
        val isArmed    = (baseMode and 0x80) != 0   // MAV_MODE_FLAG_SAFETY_ARMED
        return HeartbeatData(
            timestampMs   = System.currentTimeMillis(),
            isArmed       = isArmed,
            rawCustomMode = customMode,
        )
    }
}
