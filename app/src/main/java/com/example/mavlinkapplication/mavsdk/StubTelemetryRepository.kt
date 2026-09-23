package com.example.mavlinkapplication.mavsdk

import com.example.mavlinkapplication.domain.HeartbeatData
import com.example.mavlinkapplication.domain.TelemetryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub TelemetryRepository emitting a synthetic heartbeat at 1 Hz.
 * Replace with MavsdkTelemetryRepository once MAVSDK 3.0.0 API is verified.
 */
@Singleton
class StubTelemetryRepository @Inject constructor() : TelemetryRepository {

    override val heartbeats: Flow<HeartbeatData> = flow {
        while (true) {
            emit(
                HeartbeatData(
                    timestampMs   = System.currentTimeMillis(),
                    isArmed       = false,
                    rawCustomMode = 0, // MANUAL
                )
            )
            delay(1_000)
        }
    }.flowOn(Dispatchers.IO)
}
