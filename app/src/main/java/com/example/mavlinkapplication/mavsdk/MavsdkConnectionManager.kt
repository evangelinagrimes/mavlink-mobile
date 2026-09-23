package com.example.mavlinkapplication.mavsdk

import com.example.mavlinkapplication.domain.LinkHealthEvaluator
import io.mavsdk.System
import io.mavsdk.mavsdkserver.MavsdkServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

private const val RECONNECT_BACKOFF_MS = 2_000L

/**
 * Owns the MavsdkServer lifecycle and the System (plugin entry point).
 *
 * [CONFIRMED] API from MAVSDK-Java 3.17.4 jar inspection:
 *   io.mavsdk.mavsdkserver.MavsdkServer() — no Context parameter
 *   server.run(systemAddress: String): Int → port
 *   System("127.0.0.1", port)
 */
@Singleton
class MavsdkConnectionManager @Inject constructor() {
    private var mavsdkServer: MavsdkServer? = null

    @Volatile var system: System? = null
        private set

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    /**
     * Connects and then supervises the link forever: if no autopilot heartbeat arrives
     * for longer than [LinkHealthEvaluator.LOST_THRESHOLD_MS] (whether we never connected,
     * SITL wasn't up yet, or a live link dropped), tears down and reconnects. Call once
     * from application startup; never returns.
     */
    suspend fun runConnectionLoop(systemAddress: String) {
        while (true) {
            try {
                connectOnce(systemAddress)
                waitUntilStale()
            } catch (_: Exception) {
                // Fall through to disconnect + retry below.
            }
            disconnect()
            delay(RECONNECT_BACKOFF_MS)
        }
    }

    private suspend fun connectOnce(systemAddress: String) {
        withContext(Dispatchers.IO) {
            val server = MavsdkServer()
            val port = server.run(systemAddress)
            mavsdkServer = server
            system = System("127.0.0.1", port)
            _connected.value = true
        }
    }

    /** Suspends until the autopilot's heartbeat has gone stale. */
    private suspend fun waitUntilStale() {
        val sys = system ?: return
        withContext(Dispatchers.IO) {
            val heartbeatFlow = sys.mavlinkDirect.getMessage("HEARTBEAT")
                .toObservable().asFlow()
                .filter { it.componentId == AUTOPILOT_COMPONENT_ID }

            while (true) {
                val gotOne = withTimeoutOrNull(LinkHealthEvaluator.LOST_THRESHOLD_MS) {
                    heartbeatFlow.first()
                }
                if (gotOne == null) return@withContext // stale — trigger reconnect
            }
        }
    }

    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            _connected.value = false
            system?.dispose()
            system = null
            mavsdkServer?.stop()
            mavsdkServer?.attach()
            mavsdkServer?.destroy()
            mavsdkServer = null
        }
    }
}
