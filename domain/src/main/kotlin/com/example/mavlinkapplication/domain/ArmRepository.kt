package com.example.mavlinkapplication.domain

interface ArmRepository {
    /**
     * MAV_CMD_COMPONENT_ARM_DISARM with the force-disarm magic value — bypasses the
     * autopilot's normal disarm checks. This is the "quickly disable the rover" control,
     * not a graceful disarm.
     */
    suspend fun forceDisarm(): Result<Unit>
}
