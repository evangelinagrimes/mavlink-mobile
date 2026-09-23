package com.example.mavlinkapplication.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryUnknown
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mavlinkapplication.domain.BatteryData
import com.example.mavlinkapplication.domain.LinkQuality
import com.example.mavlinkapplication.domain.RoverMode
import com.example.mavlinkapplication.theme.AppText
import com.example.mavlinkapplication.theme.StatusColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TOUCH_TARGET = 48.dp

/**
 * Persistent chrome: [mode selector, disarm] on the left, a live link-status pill in
 * the center, [params, messages, vehicle info, battery] on the right. Every tappable
 * element here has a real 48dp touch target — this is a field app, not a phone app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    linkQuality: LinkQuality,
    modeChangeState: ModeChangeState,
    disarmState: DisarmState,
    nowMs: Long,
    onModeSelected: (Int) -> Unit,
    onParamsClick: () -> Unit,
    onForceDisarm: () -> Unit,
    onMessagesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    unreadMessageCount: Int,
    battery: BatteryData?,
) {
    val isArmed = (linkQuality as? LinkQuality.EverConnected)?.isArmed ?: false
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModeSelectorButton(linkQuality, modeChangeState, onModeSelected)
                    Spacer(Modifier.width(8.dp))
                    DisarmButton(isArmed = isArmed, disarmState = disarmState, onConfirm = onForceDisarm)
                }

                LinkStatusPill(linkQuality, nowMs)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onParamsClick, modifier = Modifier.size(TOUCH_TARGET)) {
                        Icon(
                            Icons.Filled.List,
                            contentDescription = "Parameters",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        )
                    }
                    MessageIconWithBadge(count = unreadMessageCount, onClick = onMessagesClick)
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(TOUCH_TARGET)) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Vehicle info",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    BatteryIndicator(battery)
                    Spacer(Modifier.width(8.dp))
                }
            }
        },
    )
}

/** LIVE / DEGRADED / LOST / WAITING, color-coded, with the live heartbeat age. */
@Composable
private fun LinkStatusPill(linkQuality: LinkQuality, nowMs: Long) {
    val (label, color, ageMs) = when (linkQuality) {
        LinkQuality.NeverConnected -> Triple("WAITING", MaterialTheme.colorScheme.onSurfaceVariant, null)
        is LinkQuality.EverConnected.Fresh -> Triple("LIVE", StatusColors.Ok, nowMs - linkQuality.lastHeartbeatMs)
        is LinkQuality.EverConnected.Degraded -> Triple("DEGRADED", StatusColors.Caution, nowMs - linkQuality.lastHeartbeatMs)
        is LinkQuality.EverConnected.Lost -> Triple("LOST", MaterialTheme.colorScheme.error, nowMs - linkQuality.lastHeartbeatMs)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = AppText.Label, color = color)
        if (ageMs != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = "%.1fs".format(ageMs / 1000f),
                style = AppText.Label,
                color = color.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun ModeSelectorButton(
    linkQuality: LinkQuality,
    modeChangeState: ModeChangeState,
    onModeSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentMode = (linkQuality as? LinkQuality.EverConnected)?.mode
    val isPending = modeChangeState is ModeChangeState.Pending
    val isFailed = modeChangeState is ModeChangeState.Failed

    Box {
        ChromeButton(
            onClick = { if (!isPending) expanded = true },
            contentColor = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        ) {
            if (isPending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
            } else {
                Text(
                    text = (currentMode?.displayName ?: "MODE").uppercase(),
                    style = AppText.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isFailed) {
                    Icon(
                        Icons.Filled.WarningAmber,
                        contentDescription = "Mode change not confirmed",
                        modifier = Modifier.size(16.dp),
                    )
                }
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RoverMode.allKnownWithRaw.forEach { (raw, mode) ->
                DropdownMenuItem(
                    text = { Text(mode.displayName, style = AppText.Body) },
                    onClick = {
                        onModeSelected(raw)
                        expanded = false
                    },
                )
            }
        }
    }
}

private const val DISARM_HOLD_MS = 1_000L

/**
 * Force-disarm, confirmed by holding for ~1s (a fill animation shows progress) so a
 * bump or glove brush can't trigger it. Greyed out and non-interactive while already
 * disarmed — this app never arms, only disarms.
 */
@Composable
private fun DisarmButton(isArmed: Boolean, disarmState: DisarmState, onConfirm: () -> Unit) {
    val isPending = disarmState is DisarmState.Pending
    val isFailed = disarmState is DisarmState.Failed
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    val baseColor = MaterialTheme.colorScheme.error
    val enabled = isArmed && !isPending

    Box(
        modifier = Modifier
            .height(TOUCH_TARGET)
            .clip(RoundedCornerShape(8.dp))
            .background(baseColor.copy(alpha = if (enabled) 1f else 0.3f))
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                holdJob = scope.launch {
                                    val start = System.currentTimeMillis()
                                    while (true) {
                                        val elapsed = System.currentTimeMillis() - start
                                        holdProgress = (elapsed / DISARM_HOLD_MS.toFloat()).coerceIn(0f, 1f)
                                        if (holdProgress >= 1f) {
                                            onConfirm()
                                            break
                                        }
                                        delay(16)
                                    }
                                }
                                tryAwaitRelease()
                                holdJob?.cancel()
                                holdProgress = 0f
                            },
                        )
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(holdProgress)
                .background(Color.White.copy(alpha = 0.35f)),
        )
        when {
            isPending -> CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
            else -> Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp),
            ) {
                Text(text = "DISARM", style = AppText.Body, color = Color.White)
                if (isFailed) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.WarningAmber,
                        contentDescription = "Disarm not confirmed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChromeButton(
    onClick: () -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
        ),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(TOUCH_TARGET),
        content = content,
    )
}

/** Real SYS_STATUS data — null (never received) is visually distinct from a low/critical reading. */
@Composable
private fun BatteryIndicator(battery: BatteryData?) {
    val percent = battery?.remainingPercent?.takeIf { it in 0..100 }
    val (icon, tint) = when {
        percent == null -> Icons.Filled.BatteryUnknown to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        percent <= 15 -> Icons.Filled.BatteryAlert to MaterialTheme.colorScheme.error
        percent <= 40 -> Icons.Filled.Battery2Bar to MaterialTheme.colorScheme.error
        percent <= 70 -> Icons.Filled.Battery4Bar to StatusColors.Caution
        percent <= 90 -> Icons.Filled.Battery6Bar to StatusColors.Ok
        else -> Icons.Filled.BatteryFull to StatusColors.Ok
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = "Battery", tint = tint)
        if (percent != null) {
            Spacer(Modifier.width(2.dp))
            Text(text = "$percent%", style = AppText.Label, color = tint)
        }
    }
}

@Composable
private fun MessageIconWithBadge(count: Int, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick, modifier = Modifier.size(TOUCH_TARGET)) {
            Icon(
                Icons.Filled.ChatBubble,
                contentDescription = "Vehicle messages",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .size(17.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (count > 9) "9+" else count.toString(),
                    style = AppText.Label.copy(fontSize = 9.sp, lineHeight = 9.sp),
                    color = Color.White,
                )
            }
        }
    }
}
