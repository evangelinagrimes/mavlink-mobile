package com.example.mavlinkapplication

import android.app.Application
import com.example.mavlinkapplication.mavsdk.MavsdkConnectionManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HitTargetApp : Application() {

    @Inject lateinit var connectionManager: MavsdkConnectionManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            // MavsdkServer.run() blocks until SITL connects; runs on IO.
            // Change the address string to match your test environment:
            //   emulator → "udpout://10.0.2.2:14550"  (SITL on host port 14550)
            //   physical → "udpout://<host-ip>:14550"
            //   SITL-in  → "udpin://0.0.0.0:14540"    (SITL must reach this device)
            // tcp://127.0.0.1:5760 — adb reverse maps emulator:5760 → host SITL TCP 5760
            connectionManager.connect("tcpout://127.0.0.1:5760")
        }
    }
}
