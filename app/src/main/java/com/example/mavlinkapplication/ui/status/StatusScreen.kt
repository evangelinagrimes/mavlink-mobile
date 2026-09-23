package com.example.mavlinkapplication.ui.status

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mavlinkapplication.domain.AttitudeData
import com.example.mavlinkapplication.domain.BatteryData
import com.example.mavlinkapplication.domain.GpsData
import com.example.mavlinkapplication.domain.GpsFixType
import com.example.mavlinkapplication.domain.LinkQuality
import com.example.mavlinkapplication.domain.MessageSeverity
import com.example.mavlinkapplication.domain.VehicleParam
import com.example.mavlinkapplication.domain.VfrHudData
import com.example.mavlinkapplication.theme.AppText
import com.example.mavlinkapplication.theme.StatusColors
import com.example.mavlinkapplication.ui.params.ParamEditState
import kotlin.math.hypot

private enum class StatusPanel { NONE, VEHICLE_INFO, VEHICLE_MESSAGES }
private const val HUD_MESSAGE_RECENCY_MS = 10_000L
private const val MPS_TO_MPH = 2.23694f
private val SIDE_PANEL_WIDTH = 340.dp
private val CARD_COLUMN_WIDTH = 168.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    viewModel: StatusViewModel = hiltViewModel(),
    onNavigateToParams: () -> Unit,
) {
    val linkQuality by viewModel.linkQuality.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val unreadMessageCount by viewModel.unreadMessageCount.collectAsStateWithLifecycle()
    val modeChangeState by viewModel.modeChangeState.collectAsStateWithLifecycle()
    val disarmState by viewModel.disarmState.collectAsStateWithLifecycle()
    val attitude by viewModel.attitude.collectAsStateWithLifecycle()
    val vfrHud by viewModel.vfrHud.collectAsStateWithLifecycle()
    val gps by viewModel.gps.collectAsStateWithLifecycle()
    val nowMs by viewModel.nowMs.collectAsStateWithLifecycle()
    val throttleCapParam by viewModel.throttleCapParam.collectAsStateWithLifecycle()
    val throttleCapEditState by viewModel.throttleCapEditState.collectAsStateWithLifecycle()
    val battery by viewModel.battery.collectAsStateWithLifecycle()
    var activePanel by remember { mutableStateOf(StatusPanel.NONE) }

    Scaffold(
        topBar = {
            AppTopBar(
                linkQuality = linkQuality,
                modeChangeState = modeChangeState,
                disarmState = disarmState,
                nowMs = nowMs,
                onModeSelected = viewModel::setMode,
                onParamsClick = onNavigateToParams,
                onForceDisarm = viewModel::forceDisarm,
                onMessagesClick = {
                    activePanel = StatusPanel.VEHICLE_MESSAGES
                    viewModel.markMessagesRead()
                },
                onSettingsClick = { activePanel = StatusPanel.VEHICLE_INFO },
                unreadMessageCount = unreadMessageCount,
                battery = battery,
            )
        },
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            MainStatusContent(
                linkQuality = linkQuality,
                attitude = attitude,
                vfrHud = vfrHud,
                gps = gps,
                battery = battery,
                recentWarning = recentWarningFor(messages, nowMs),
                latestMessage = messages.firstOrNull(),
                throttleCapParam = throttleCapParam,
                throttleCapEditState = throttleCapEditState,
                onSetThrottleCap = viewModel::setThrottleCap,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )

            when (activePanel) {
                StatusPanel.VEHICLE_INFO -> VehicleInfoPanel(
                    linkQuality = linkQuality,
                    battery = battery,
                    onClose = { activePanel = StatusPanel.NONE },
                    modifier = Modifier.width(SIDE_PANEL_WIDTH).fillMaxHeight(),
                )
                StatusPanel.VEHICLE_MESSAGES -> VehicleMessagesPanel(
                    messages = messages,
                    onClose = { activePanel = StatusPanel.NONE },
                    modifier = Modifier.width(SIDE_PANEL_WIDTH).fillMaxHeight(),
                )
                StatusPanel.NONE -> Unit
            }
        }
    }
}

/** Most recent WARNING/CRITICAL message if it's still recent, else null — never a stale one. */
private fun recentWarningFor(messages: List<VehicleMessage>, nowMs: Long): VehicleMessage? {
    val newest = messages.firstOrNull() ?: return null
    if (newest.severity == MessageSeverity.INFO) return null
    if (nowMs - newest.timestampMs > HUD_MESSAGE_RECENCY_MS) return null
    return newest
}

@Composable
private fun MainStatusContent(
    linkQuality: LinkQuality,
    attitude: AttitudeData?,
    vfrHud: VfrHudData?,
    gps: GpsData?,
    battery: BatteryData?,
    recentWarning: VehicleMessage?,
    latestMessage: VehicleMessage?,
    throttleCapParam: VehicleParam?,
    throttleCapEditState: ParamEditState,
    onSetThrottleCap: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Hud(
                linkQuality = linkQuality,
                attitude = attitude,
                speedMph = vfrHud?.groundSpeedMps?.let { it * MPS_TO_MPH },
                recentWarning = recentWarning,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            ReadoutColumn(
                linkQuality = linkQuality,
                gps = gps,
                battery = battery,
                modifier = Modifier.width(CARD_COLUMN_WIDTH).fillMaxHeight(),
            )
        }

        ThrottleCapRow(
            param = throttleCapParam,
            editState = throttleCapEditState,
            onSetValue = onSetThrottleCap,
        )

        MessageTicker(latestMessage)
    }
}

/**
 * Imitates Mission Planner's HUD: an artificial horizon (sky/ground split, tilts with
 * roll, shifts with pitch) with a fixed boresight reticle. ARMED/DISARMED is a small
 * badge at the top rather than a full row, so the horizon can fill most of the screen.
 * Big warning text only appears when something is actually wrong — a nominal link
 * shows nothing but the horizon and the crosshair.
 */
@Composable
private fun Hud(
    linkQuality: LinkQuality,
    attitude: AttitudeData?,
    speedMph: Float?,
    recentWarning: VehicleMessage?,
    modifier: Modifier = Modifier,
) {
    val isArmed = (linkQuality as? LinkQuality.EverConnected)?.isArmed ?: false

    Box(modifier = modifier) {
        if (attitude != null) {
            ArtificialHorizon(
                modifier = Modifier.fillMaxSize(),
                rollDeg = attitude.rollDeg,
                pitchDeg = attitude.pitchDeg,
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
        }

        ArmedBadge(isArmed = isArmed, modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
        SpeedTape(speedMph, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp))

        val (warningText, isWarning) = bannerTextFor(linkQuality, recentWarning)
        if (isWarning) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = warningText,
                    style = AppText.Hero,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (attitude == null) {
            Text(
                text = "NO ATTITUDE DATA",
                style = AppText.Label,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            )
        }

        HudCrosshair(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ArmedBadge(isArmed: Boolean, modifier: Modifier = Modifier) {
    val color = if (isArmed) MaterialTheme.colorScheme.error else StatusColors.Ok
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = if (isArmed) "ARMED" else "DISARMED",
            style = AppText.Label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** A recent real warning takes priority over the generic link-quality text. */
private fun bannerTextFor(linkQuality: LinkQuality, recentWarning: VehicleMessage?): Pair<String, Boolean> {
    if (recentWarning != null) return recentWarning.text.uppercase() to true
    return when (linkQuality) {
        LinkQuality.NeverConnected -> "WAITING FOR VEHICLE" to true
        is LinkQuality.EverConnected.Fresh -> "" to false
        is LinkQuality.EverConnected.Degraded -> "LINK DEGRADED — CHECK RADIO" to true
        is LinkQuality.EverConnected.Lost -> "LINK LOST" to true
    }
}

/** Fixed boresight reticle — represents where the rover is pointed, not tilted with the horizon. */
@Composable
private fun HudCrosshair(modifier: Modifier = Modifier) {
    val tickColor = Color(0xFFFFC107)
    Canvas(modifier = modifier.size(width = 64.dp, height = 16.dp)) {
        val midY = size.height / 2f
        val gap = 10f
        val wingLen = (size.width - gap) / 2f
        val strokeW = 3f

        drawLine(tickColor, Offset(0f, midY), Offset(wingLen, midY), strokeWidth = strokeW)
        drawLine(tickColor, Offset(size.width - wingLen, midY), Offset(size.width, midY), strokeWidth = strokeW)
        drawCircle(tickColor, radius = 4f, center = Offset(size.width / 2f, midY))
        drawLine(
            tickColor,
            Offset(size.width / 2f, midY),
            Offset(size.width / 2f, midY + 8f),
            strokeWidth = strokeW,
        )
    }
}

@Composable
private fun ArtificialHorizon(modifier: Modifier = Modifier, rollDeg: Float, pitchDeg: Float) {
    val skyColor = Color(0xFF3A5A78) // desaturated so white HUD text stays readable over it
    val groundColor = Color(0xFF5A4A38)
    val horizonLineColor = Color.White.copy(alpha = 0.7f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val diag = hypot(w, h)
        val pitchPxPerDeg = h / 90f
        val pitchOffset = pitchDeg * pitchPxPerDeg

        rotate(degrees = -rollDeg, pivot = Offset(w / 2f, h / 2f)) {
            translate(top = pitchOffset) {
                drawRect(
                    color = groundColor,
                    topLeft = Offset(-diag, h / 2f),
                    size = Size(diag * 2, diag * 2),
                )
                drawRect(
                    color = skyColor,
                    topLeft = Offset(-diag, h / 2f - diag * 2),
                    size = Size(diag * 2, diag * 2),
                )
                drawLine(
                    color = horizonLineColor,
                    start = Offset(-diag, h / 2f),
                    end = Offset(diag * 2, h / 2f),
                    strokeWidth = 3f,
                )
            }
        }
    }
}

/** speedMph == null means no VFR_HUD received yet — shown as an explicit NO DATA tape, not 0. */
@Composable
private fun SpeedTape(speedMph: Float?, modifier: Modifier = Modifier) {
    val max = 10f
    val labels = listOf(10, 8, 6, 4, 2, 0)
    val trackColor = Color.White.copy(alpha = 0.4f)
    val fillColor = Color(0xFFFFC107)

    Row(modifier = modifier.height(220.dp).width(56.dp)) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackX = size.width * 0.3f
                drawLine(trackColor, Offset(trackX, 0f), Offset(trackX, size.height), strokeWidth = 4f)
                if (speedMph != null) {
                    val fraction = speedMph.coerceIn(0f, max) / max
                    val y = size.height - size.height * fraction
                    drawCircle(fillColor, radius = 7f, center = Offset(trackX, y))
                }
                labels.forEach { mph ->
                    val y = size.height - size.height * (mph / max)
                    drawLine(trackColor, Offset(trackX - 6f, y), Offset(trackX + 6f, y), strokeWidth = 3f)
                }
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                labels.forEach { mph ->
                    Text(
                        text = if (mph == 0) "mph" else mph.toString(),
                        style = AppText.Label,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadoutColumn(
    linkQuality: LinkQuality,
    gps: GpsData?,
    battery: BatteryData?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val (linkText, linkColor) = when (linkQuality) {
            LinkQuality.NeverConnected -> "—" to MaterialTheme.colorScheme.onSurfaceVariant
            is LinkQuality.EverConnected.Fresh -> "LIVE" to StatusColors.Ok
            is LinkQuality.EverConnected.Degraded -> "DEGRADED" to StatusColors.Caution
            is LinkQuality.EverConnected.Lost -> "LOST" to MaterialTheme.colorScheme.error
        }
        ReadoutCard(label = "LINK", value = linkText, valueColor = linkColor)

        val gpsText = gps?.let { "${it.fixType.toDisplayString()}\n${it.satellitesVisible} SATS" } ?: "—"
        val gpsColor = when (gps?.fixType) {
            GpsFixType.FIX_3D, GpsFixType.DGPS, GpsFixType.RTK_FLOAT, GpsFixType.RTK_FIXED, GpsFixType.STATIC, GpsFixType.PPP ->
                StatusColors.Ok
            GpsFixType.FIX_2D -> StatusColors.Caution
            else -> MaterialTheme.colorScheme.error
        }
        ReadoutCard(label = "GPS", value = gpsText, valueColor = if (gps == null) MaterialTheme.colorScheme.onSurfaceVariant else gpsColor)

        // EKF_STATUS_REPORT isn't streaming from this SITL — honest unknown, not fabricated.
        ReadoutCard(label = "EKF", value = "—", valueColor = MaterialTheme.colorScheme.onSurfaceVariant)

        val batteryText = battery?.let { "%.1fV\n%d%%".format(it.voltageMv / 1000f, it.remainingPercent) } ?: "—"
        val batteryColor = when {
            battery == null -> MaterialTheme.colorScheme.onSurfaceVariant
            battery.remainingPercent in 0..15 -> MaterialTheme.colorScheme.error
            battery.remainingPercent in 16..40 -> StatusColors.Caution
            else -> StatusColors.Ok
        }
        ReadoutCard(label = "BATTERY", value = batteryText, valueColor = batteryColor)
    }
}

@Composable
private fun ReadoutCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        Text(label, style = AppText.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = AppText.Readout, color = valueColor)
    }
}

private fun GpsFixType.toDisplayString(): String = when (this) {
    GpsFixType.NO_GPS -> "NO GPS"
    GpsFixType.NO_FIX -> "NO FIX"
    GpsFixType.FIX_2D -> "2D FIX"
    GpsFixType.FIX_3D -> "3D FIX"
    GpsFixType.DGPS -> "DGPS"
    GpsFixType.RTK_FLOAT -> "RTK FLOAT"
    GpsFixType.RTK_FIXED -> "RTK FIXED"
    GpsFixType.STATIC -> "STATIC"
    GpsFixType.PPP -> "PPP"
    GpsFixType.UNKNOWN -> "—"
}

private const val THROTTLE_CAP_PARAM_ID = "MOT_THR_MAX"

@Composable
private fun ThrottleCapRow(
    param: VehicleParam?,
    editState: ParamEditState,
    onSetValue: (Float) -> Unit,
) {
    var localValue by remember(param?.value) { mutableStateOf(param?.value ?: 100f) }
    val isPending = editState is ParamEditState.Pending && editState.id == THROTTLE_CAP_PARAM_ID
    val isFailed = editState is ParamEditState.Failed && editState.id == THROTTLE_CAP_PARAM_ID

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "THROTTLE CAP",
            style = AppText.Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        if (param == null) {
            Text("NO DATA", style = AppText.Body, color = MaterialTheme.colorScheme.error)
        } else {
            Slider(
                value = localValue,
                onValueChange = { localValue = it },
                onValueChangeFinished = { onSetValue(localValue) },
                valueRange = 0f..100f,
                enabled = !isPending,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.CenterEnd) {
                when {
                    isPending -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    isFailed -> Icon(
                        Icons.Filled.WarningAmber,
                        contentDescription = "Throttle cap not confirmed",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    else -> Text("${localValue.toInt()}%", style = AppText.Body, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** One-line ticker showing the latest message — the log itself lives behind the messages icon. */
@Composable
private fun MessageTicker(latest: VehicleMessage?) {
    val color = when (latest?.severity) {
        MessageSeverity.CRITICAL -> MaterialTheme.colorScheme.error
        MessageSeverity.WARNING -> StatusColors.Caution
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = latest?.let { "${it.timestamp}  ${it.text}" } ?: "No messages yet",
            style = AppText.Label,
            color = color,
            maxLines = 1,
        )
    }
}
