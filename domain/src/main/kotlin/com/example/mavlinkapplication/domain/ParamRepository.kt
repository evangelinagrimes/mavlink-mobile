package com.example.mavlinkapplication.domain

interface ParamRepository {
    suspend fun getAll(): List<VehicleParam>
    suspend fun get(id: String): VehicleParam?

    /**
     * [type] must match the param's real MAVLink type (from the [VehicleParam] being
     * edited) — the implementation sends a type-specific PARAM_SET, it doesn't guess.
     */
    suspend fun set(id: String, value: Float, type: VehicleParam.ParamType): Result<Unit>
}
