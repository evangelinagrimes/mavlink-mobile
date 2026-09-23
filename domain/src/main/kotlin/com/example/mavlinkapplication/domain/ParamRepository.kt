package com.example.mavlinkapplication.domain

interface ParamRepository {
    suspend fun getAll(): List<VehicleParam>
    suspend fun get(id: String): VehicleParam?
    suspend fun set(id: String, value: Float): Result<Unit>
}
