package com.example.mavlinkapplication.mavsdk

import io.mavsdk.System
import io.mavsdk.mavsdkserver.MavsdkServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the MavsdkServer lifecycle and the System (plugin entry point).
 *
 * Call [connect] once from the app. [connected] emits true when the System is ready so
 * repositories can flatMapLatest on it instead of calling [system] at construction time.
 *
 * [CONFIRMED] API from MAVSDK-Java 3.17.4 jar inspection:
 *   io.mavsdk.mavsdkserver.MavsdkServer() — no Context parameter
 *   server.run(systemAddress: String): Int → port
 *   System("127.0.0.1", port)
 *
 * Emulator address: "udpout://10.0.2.2:14550"
 *   10.0.2.2 = host loopback from Android emulator.
 *   SITL must be running and accepting on host UDP 14550.
 * Physical device: "udpout://<device-ip>:14550", SITL --out to device IP.
 * SITL inbound: "udpin://0.0.0.0:14540" — mavsdk_server listens; SITL must reach device port.
 */
@Singleton
class MavsdkConnectionManager @Inject constructor() {
    private var mavsdkServer: MavsdkServer? = null

    @Volatile var system: System? = null
        private set

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    suspend fun connect(systemAddress: String = "tcpout://127.0.0.1:5760") {
        withContext(Dispatchers.IO) {
            val server = MavsdkServer()
            val port = server.run(systemAddress)
            mavsdkServer = server
            system = System("127.0.0.1", port)
            _connected.value = true
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
