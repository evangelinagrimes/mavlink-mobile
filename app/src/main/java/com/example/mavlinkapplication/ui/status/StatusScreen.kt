package com.example.mavlinkapplication.ui.status

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mavlinkapplication.domain.LinkQuality
import com.example.mavlinkapplication.domain.RoverMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    viewModel: StatusViewModel = hiltViewModel(),
    onNavigateToParams: () -> Unit,
) {
    val linkQuality by viewModel.linkQuality.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ModeSelector(linkQuality, viewModel::setMode) },
                actions = {
                    TextButton(onClick = onNavigateToParams) {
                        Text("PARAMS", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = { /* TODO Phase 2b: disconnect — out of scope for now */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("DISCONNECT")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LinkQualityPanel(
                linkQuality = linkQuality,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f),
            )
            ArmedPanel(
                linkQuality = linkQuality,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f),
            )
        }
    }
}

@Composable
private fun ModeSelector(linkQuality: LinkQuality, onModeSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentMode = (linkQuality as? LinkQuality.EverConnected)?.mode

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = (currentMode?.displayName ?: "—") + "  ▾",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RoverMode.allKnownWithRaw.forEach { (raw, mode) ->
                DropdownMenuItem(
                    text = { Text(mode.displayName) },
                    onClick = {
                        onModeSelected(raw)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun LinkQualityPanel(linkQuality: LinkQuality, modifier: Modifier = Modifier) {
    val (bgColor, label, sub) = when (linkQuality) {
        LinkQuality.NeverConnected ->
            Triple(Color(0xFF263238), "NOT CONNECTED", "Waiting for vehicle…")
        is LinkQuality.EverConnected.Fresh ->
            Triple(Color(0xFF1B5E20), "LIVE", "Link is current")
        is LinkQuality.EverConnected.Degraded ->
            Triple(Color(0xFFBF360C), "DEGRADED", "Heartbeat late — check radio")
        is LinkQuality.EverConnected.Lost ->
            Triple(Color(0xFF7F0000), "LINK LOST", "Last known state shown")
    }

    Box(
        modifier = modifier.background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun ArmedPanel(linkQuality: LinkQuality, modifier: Modifier = Modifier) {
    val isArmed = (linkQuality as? LinkQuality.EverConnected)?.isArmed ?: false
    val dotColor = if (isArmed) Color(0xFFD32F2F) else Color(0xFF388E3C)
    val label    = if (isArmed) "ARMED" else "DISARMED"

    Row(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.headlineLarge)
    }
}
