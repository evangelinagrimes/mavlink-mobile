package com.example.mavlinkapplication.domain

data class VehicleParam(
    val id: String,
    val value: Float,
    val type: ParamType,
) {
    enum class ParamType {
        UINT8, INT8, UINT16, INT16, UINT32, INT32, REAL32, REAL64
    }
}
