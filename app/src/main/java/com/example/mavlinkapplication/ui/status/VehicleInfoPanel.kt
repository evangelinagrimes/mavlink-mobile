package com.example.mavlinkapplication.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mavlinkapplication.domain.BatteryData
import com.example.mavlinkapplication.domain.LinkQuality
import com.example.mavlinkapplication.theme.AppText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VehicleInfoPanel(
    linkQuality: LinkQuality,
    battery: BatteryData?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = MockTelemetry.vehicleConfig
    val metrics = MockTelemetry.linkMetrics
    val lastHeartbeatMs = (linkQuality as? LinkQuality.EverConnected)?.lastHeartbeatMs

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = config.roverName, style = AppText.Hero, fontWeight = FontWeight.Bold)
            TextButton(onClick = onClose) { Text("CLOSE", style = AppText.Body) }
        }

        Text(
            text = "${config.connectionType} · ${config.roverId}",
            style = AppText.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))

        Text(
            text = "VEHICLE CONFIG",
            style = AppText.Label,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        ConfigRow("BAUD", config.baud)
        ConfigRow("FIRMWARE", config.firmware)
        ConfigRow("MAC", config.mac)

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MetricColumn(
                label = "BATTERY",
                value = battery?.let { "%.1fV · %d%%".format(it.voltageMv / 1000f, it.remainingPercent) } ?: "—",
            )
            MetricColumn(
                label = "SIGNAL",
                value = metrics.signalStrengthDbm?.let { "$it dBm" } ?: "—",
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MetricColumn(
                label = "LAST HEARTBEAT",
                value = formatHeartbeatAge(lastHeartbeatMs),
            )
            MetricColumn(
                label = "PACKET LOSS",
                value = metrics.packetLossPercent?.let { "$it%" } ?: "—",
            )
        }
    }
}

@Composable
private fun ConfigRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = AppText.Body,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = value,
            style = AppText.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, style = AppText.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = AppText.Readout, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatHeartbeatAge(lastHeartbeatMs: Long?): String {
    if (lastHeartbeatMs == null) return "—"
    val ageMs = System.currentTimeMillis() - lastHeartbeatMs
    val ageSec = ageMs / 1000
    return when {
        ageSec < 1 -> "just now"
        ageSec == 1L -> "1 second ago"
        ageSec < 60 -> "$ageSec seconds ago"
        else -> SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(lastHeartbeatMs))
    }
}
