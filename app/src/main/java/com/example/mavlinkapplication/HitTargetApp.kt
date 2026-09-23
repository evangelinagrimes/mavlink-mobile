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
            // runConnectionLoop never returns: it connects, then reconnects on its own
            // if the heartbeat goes stale (link drop, or SITL not up yet at launch).
            // tcpout://127.0.0.1:5760 — adb reverse maps emulator:5760 → host SITL TCP 5760
            connectionManager.runConnectionLoop("tcpout://127.0.0.1:5760")
        }
    }
}
