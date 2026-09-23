package com.example.mavlinkapplication.mavsdk

import com.example.mavlinkapplication.domain.ParamRepository
import com.example.mavlinkapplication.domain.VehicleParam
import com.example.mavlinkapplication.domain.VehicleParam.ParamType
import kotlinx.coroutines.rx2.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements [ParamRepository] using MAVSDK-Java 3.17.4 Param plugin.
 *
 * [CONFIRMED via jar inspection]:
 *   getAllParams() → Single<Param.AllParams>
 *   AllParams.getFloatParams() → List<Param.FloatParam> (Kotlin: .floatParams)
 *   AllParams.getIntParams()   → List<Param.IntParam>   (Kotlin: .intParams)
 *   FloatParam.getName() / .getValue() (Kotlin: .name / .value)
 *   getParamFloat(id)    → Single<Float>
 *   setParamFloat(id, v) → Completable
 *   getParamInt(id)      → Single<Integer>
 *   setParamInt(id, v)   → Completable
 */
@Singleton
class MavsdkParamRepository @Inject constructor(
    private val connectionManager: MavsdkConnectionManager,
) : ParamRepository {

    override suspend fun getAll(): List<VehicleParam> {
        val param = connectionManager.system?.param ?: return emptyList()
        return try {
            val all = param.getAllParams().await()
            val floats = all.floatParams.map { VehicleParam(it.name, it.value, ParamType.REAL32) }
            val ints   = all.intParams.map   { VehicleParam(it.name, it.value.toFloat(), ParamType.INT32) }
            (floats + ints).sortedBy { it.id }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun get(id: String): VehicleParam? {
        val param = connectionManager.system?.param ?: return null
        return try {
            VehicleParam(id, param.getParamFloat(id).await(), ParamType.REAL32)
        } catch (_: Exception) {
            try {
                VehicleParam(id, param.getParamInt(id).await().toFloat(), ParamType.INT32)
            } catch (_: Exception) {
                null
            }
        }
    }

    override suspend fun set(id: String, value: Float): Result<Unit> {
        val param = connectionManager.system?.param
            ?: return Result.failure(IllegalStateException("Not connected"))
        return try {
            param.setParamFloat(id, value).await()
            Result.success(Unit)
        } catch (_: Exception) {
            try {
                param.setParamInt(id, value.toInt()).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
