package com.example.mavlinkapplication.mavsdk

import com.example.mavlinkapplication.domain.ParamRepository
import com.example.mavlinkapplication.domain.VehicleParam
import com.example.mavlinkapplication.domain.VehicleParam.ParamType.REAL32
import com.example.mavlinkapplication.domain.VehicleParam.ParamType.INT32
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub ParamRepository with a small set of representative Rover params.
 * Replace with MavsdkParamRepository once MAVSDK 3.0.0 Param API is verified.
 */
@Singleton
class StubParamRepository @Inject constructor() : ParamRepository {

    private val store = mutableListOf(
        VehicleParam("CRUISE_SPEED",    2.0f,   REAL32),
        VehicleParam("WP_RADIUS",       2.0f,   REAL32),
        VehicleParam("TURN_MAX_G",      2.0f,   REAL32),
        VehicleParam("ACRO_TURN_RATE",  180.0f, REAL32),
        VehicleParam("ATC_STR_RAT_MAX", 6.28f,  REAL32),
        VehicleParam("SPEED_MIN",       1.0f,   REAL32),
        VehicleParam("ARMING_CHECK",    1.0f,   INT32),
    )

    override suspend fun getAll(): List<VehicleParam> = store.toList()

    override suspend fun get(id: String): VehicleParam? = store.find { it.id == id }

    override suspend fun set(id: String, value: Float): Result<Unit> {
        val i = store.indexOfFirst { it.id == id }
        if (i >= 0) store[i] = store[i].copy(value = value)
        return Result.success(Unit)
    }
}
