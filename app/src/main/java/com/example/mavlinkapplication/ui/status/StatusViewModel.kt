package com.example.mavlinkapplication.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mavlinkapplication.domain.LinkHealthEvaluator
import com.example.mavlinkapplication.domain.LinkQuality
import com.example.mavlinkapplication.domain.ModeRepository
import com.example.mavlinkapplication.domain.TelemetryRepository
import com.example.mavlinkapplication.domain.HeartbeatData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val telemetryRepository: TelemetryRepository,
    private val modeRepository: ModeRepository,
) : ViewModel() {

    private val _linkQuality = MutableStateFlow<LinkQuality>(LinkQuality.NeverConnected)
    val linkQuality: StateFlow<LinkQuality> = _linkQuality.asStateFlow()

    @Volatile private var lastHeartbeat: HeartbeatData? = null

    init {
        // Update state immediately on every arriving heartbeat.
        viewModelScope.launch {
            telemetryRepository.heartbeats.collect { hb ->
                lastHeartbeat = hb
                _linkQuality.value = LinkHealthEvaluator.evaluate(hb, System.currentTimeMillis())
            }
        }
        // Also tick every 500ms so a silent link visibly degrades instead of
        // freezing on the last-good reading. See LinkHealthEvaluator for threshold values.
        viewModelScope.launch {
            while (true) {
                delay(500)
                _linkQuality.value = LinkHealthEvaluator.evaluate(
                    lastHeartbeat,
                    System.currentTimeMillis(),
                )
            }
        }
    }

    fun setMode(rawCustomMode: Int) {
        viewModelScope.launch {
            modeRepository.setMode(rawCustomMode)
        }
    }
}
