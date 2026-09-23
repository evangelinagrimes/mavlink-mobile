package com.example.mavlinkapplication.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mavlinkapplication.domain.ArmRepository
import com.example.mavlinkapplication.domain.AttitudeData
import com.example.mavlinkapplication.domain.BatteryData
import com.example.mavlinkapplication.domain.CommandException
import com.example.mavlinkapplication.domain.GpsData
import com.example.mavlinkapplication.domain.HeartbeatData
import com.example.mavlinkapplication.domain.LinkHealthEvaluator
import com.example.mavlinkapplication.domain.LinkQuality
import com.example.mavlinkapplication.domain.MessageSeverity
import com.example.mavlinkapplication.domain.ModeRepository
import com.example.mavlinkapplication.domain.ParamRepository
import com.example.mavlinkapplication.domain.RoverMode
import com.example.mavlinkapplication.domain.TelemetryRepository
import com.example.mavlinkapplication.domain.VehicleParam
import com.example.mavlinkapplication.domain.VfrHudData
import com.example.mavlinkapplication.domain.severityBucket
import com.example.mavlinkapplication.ui.params.ParamEditState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val MAX_MESSAGES = 50
private const val COMMAND_CONFIRM_TIMEOUT_MS = 3_000L
private const val THROTTLE_CAP_PARAM_ID = "MOT_THR_MAX"

/**
 * Mode-change confirmation is driven by real heartbeat data: HEARTBEAT carries
 * custom_mode, so "confirmed" means the next heartbeat reports the mode we asked for.
 */
sealed class ModeChangeState {
    data object Idle : ModeChangeState()
    data class Pending(val requestedRawMode: Int) : ModeChangeState()
    data class Failed(val requestedRawMode: Int, val reason: String) : ModeChangeState()
}

/** Same pattern as mode: HEARTBEAT's armed flag confirms the disarm actually took. */
sealed class DisarmState {
    data object Idle : DisarmState()
    data object Pending : DisarmState()
    data class Failed(val reason: String) : DisarmState()
}

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val telemetryRepository: TelemetryRepository,
    private val modeRepository: ModeRepository,
    private val armRepository: ArmRepository,
    private val paramRepository: ParamRepository,
) : ViewModel() {

    private val _linkQuality = MutableStateFlow<LinkQuality>(LinkQuality.NeverConnected)
    val linkQuality: StateFlow<LinkQuality> = _linkQuality.asStateFlow()

    // null = no data received yet on this stream. Never defaulted to a fake reading.
    val battery: StateFlow<BatteryData?> = telemetryRepository.battery
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val gps: StateFlow<GpsData?> = telemetryRepository.gps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val attitude: StateFlow<AttitudeData?> = telemetryRepository.attitude
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val vfrHud: StateFlow<VfrHudData?> = telemetryRepository.vfrHud
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Event log: real state transitions (link quality, mode/disarm requests) and the
    // autopilot's own STATUSTEXT stream — never fabricated sample text.
    private val _messages = MutableStateFlow<List<VehicleMessage>>(emptyList())
    val messages: StateFlow<List<VehicleMessage>> = _messages.asStateFlow()

    private val _unreadMessageCount = MutableStateFlow(0)
    val unreadMessageCount: StateFlow<Int> = _unreadMessageCount.asStateFlow()

    // Ticks alongside the link-quality poll so the UI can expire a stale HUD warning
    // even when no new message has arrived to trigger recomposition on its own.
    private val _nowMs = MutableStateFlow(System.currentTimeMillis())
    val nowMs: StateFlow<Long> = _nowMs.asStateFlow()

    private val _modeChangeState = MutableStateFlow<ModeChangeState>(ModeChangeState.Idle)
    val modeChangeState: StateFlow<ModeChangeState> = _modeChangeState.asStateFlow()
    private var modeConfirmJob: Job? = null

    private val _disarmState = MutableStateFlow<DisarmState>(DisarmState.Idle)
    val disarmState: StateFlow<DisarmState> = _disarmState.asStateFlow()
    private var disarmJob: Job? = null

    // Throttle cap (MOT_THR_MAX). Holds the full VehicleParam (not just the value) so we
    // know its real MAVLink type before ever sending a set — see ParamRepository.set().
    private val _throttleCapParam = MutableStateFlow<VehicleParam?>(null)
    val throttleCapParam: StateFlow<VehicleParam?> = _throttleCapParam.asStateFlow()
    private val _throttleCapEditState = MutableStateFlow<ParamEditState>(ParamEditState.Idle)
    val throttleCapEditState: StateFlow<ParamEditState> = _throttleCapEditState.asStateFlow()

    @Volatile private var lastHeartbeat: HeartbeatData? = null
    private var lastLoggedStateKind: Int? = null // NeverConnected/Fresh/Degraded/Lost, to log only on transition

    init {
        viewModelScope.launch {
            telemetryRepository.heartbeats.collect { hb ->
                lastHeartbeat = hb
                updateLinkQuality()
            }
        }
        // Tick every 500ms so a silent link visibly degrades instead of freezing on the
        // last-good reading. See LinkHealthEvaluator for threshold values.
        viewModelScope.launch {
            while (true) {
                delay(500)
                updateLinkQuality()
                _nowMs.value = System.currentTimeMillis()
            }
        }
        // The autopilot's own messages (PreArm failures, EKF notices, ...), not just ours.
        viewModelScope.launch {
            telemetryRepository.statusTexts.collect { event ->
                appendMessage(event.text, event.severityBucket())
            }
        }
        loadThrottleCap()
    }

    fun setMode(rawCustomMode: Int) {
        val requestedMode = RoverMode.fromRaw(rawCustomMode)
        appendMessage("Mode change requested: ${requestedMode.displayName.uppercase()}")
        _modeChangeState.value = ModeChangeState.Pending(rawCustomMode)

        modeConfirmJob?.cancel()
        modeConfirmJob = viewModelScope.launch {
            val sendResult = modeRepository.setMode(rawCustomMode)
            if (sendResult.isFailure) {
                val reason = describeCommandFailure(sendResult.exceptionOrNull())
                _modeChangeState.value = ModeChangeState.Failed(rawCustomMode, reason)
                appendMessage(
                    "Mode change to ${requestedMode.displayName.uppercase()} failed: $reason",
                    MessageSeverity.WARNING,
                )
                return@launch
            }

            // Acked by the vehicle — now confirm the transition actually happened.
            val confirmed = withTimeoutOrNull(COMMAND_CONFIRM_TIMEOUT_MS) {
                _linkQuality.filter { state -> (state as? LinkQuality.EverConnected)?.mode == requestedMode }.first()
            }
            if (confirmed != null) {
                _modeChangeState.value = ModeChangeState.Idle
                appendMessage("Mode confirmed: ${requestedMode.displayName.uppercase()}")
            } else {
                _modeChangeState.value = ModeChangeState.Failed(rawCustomMode, "acked but not confirmed")
                appendMessage(
                    "Mode change to ${requestedMode.displayName.uppercase()} acked but not confirmed by heartbeat",
                    MessageSeverity.WARNING,
                )
            }
        }
    }

    /** Force-disarm — bypasses the autopilot's normal disarm checks. */
    fun forceDisarm() {
        appendMessage("Force disarm requested", MessageSeverity.WARNING)
        _disarmState.value = DisarmState.Pending

        disarmJob?.cancel()
        disarmJob = viewModelScope.launch {
            val sendResult = armRepository.forceDisarm()
            if (sendResult.isFailure) {
                val reason = describeCommandFailure(sendResult.exceptionOrNull())
                _disarmState.value = DisarmState.Failed(reason)
                appendMessage("Force disarm failed: $reason", MessageSeverity.CRITICAL)
                return@launch
            }

            val confirmed = withTimeoutOrNull(COMMAND_CONFIRM_TIMEOUT_MS) {
                _linkQuality.filter { state -> (state as? LinkQuality.EverConnected)?.isArmed == false }.first()
            }
            if (confirmed != null) {
                _disarmState.value = DisarmState.Idle
                appendMessage("Disarm confirmed")
            } else {
                _disarmState.value = DisarmState.Failed("acked but not confirmed")
                appendMessage("Force disarm acked but not confirmed by heartbeat", MessageSeverity.WARNING)
            }
        }
    }

    private fun loadThrottleCap() {
        viewModelScope.launch {
            _throttleCapParam.value = paramRepository.get(THROTTLE_CAP_PARAM_ID)
        }
    }

    fun setThrottleCap(percent: Float) {
        val knownType = _throttleCapParam.value?.type ?: return // haven't learned the real type yet
        _throttleCapEditState.value = ParamEditState.Pending(THROTTLE_CAP_PARAM_ID)
        viewModelScope.launch {
            val result = paramRepository.set(THROTTLE_CAP_PARAM_ID, percent, knownType)
            if (result.isSuccess) {
                _throttleCapEditState.value = ParamEditState.Idle
                _throttleCapParam.value = paramRepository.get(THROTTLE_CAP_PARAM_ID)
                appendMessage("Throttle cap set to ${percent.toInt()}%")
            } else {
                _throttleCapEditState.value = ParamEditState.Failed(THROTTLE_CAP_PARAM_ID)
                appendMessage(
                    "Throttle cap set failed: ${describeCommandFailure(result.exceptionOrNull())}",
                    MessageSeverity.WARNING,
                )
            }
        }
    }

    fun markMessagesRead() {
        _unreadMessageCount.value = 0
    }

    private fun describeCommandFailure(e: Throwable?): String = when (e) {
        is CommandException.Rejected -> "rejected by vehicle (MAV_RESULT=${e.mavResult})"
        is CommandException.NoAck -> "no response from vehicle"
        is CommandException.NotConnected -> "not connected"
        else -> e?.message ?: "unknown error"
    }

    private fun updateLinkQuality() {
        val newState = LinkHealthEvaluator.evaluate(lastHeartbeat, System.currentTimeMillis())
        _linkQuality.value = newState
        logStateTransitionIfNeeded(newState)
    }

    private fun logStateTransitionIfNeeded(state: LinkQuality) {
        val kind = when (state) {
            is LinkQuality.NeverConnected -> 0
            is LinkQuality.EverConnected.Fresh -> 1
            is LinkQuality.EverConnected.Degraded -> 2
            is LinkQuality.EverConnected.Lost -> 3
        }
        if (kind == lastLoggedStateKind) return
        val previousKind = lastLoggedStateKind
        lastLoggedStateKind = kind

        // Don't log the very first evaluation (startup noise) unless it's an actual arrival.
        if (previousKind == null && kind != 1) return

        val text = when (kind) {
            1 -> if (previousKind == null) "Heartbeat received — link established" else "Link recovered — heartbeat is current"
            2 -> "Heartbeat late — link degraded"
            3 -> "No heartbeat — link lost"
            else -> return
        }
        appendMessage(text, if (kind == 2 || kind == 3) MessageSeverity.WARNING else MessageSeverity.INFO)
    }

    private fun appendMessage(text: String, severity: MessageSeverity = MessageSeverity.INFO) {
        val nowMs = System.currentTimeMillis()
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(nowMs))
        _messages.value = (listOf(VehicleMessage(timestamp, nowMs, text, severity)) + _messages.value).take(MAX_MESSAGES)
        _unreadMessageCount.value += 1
    }
}
