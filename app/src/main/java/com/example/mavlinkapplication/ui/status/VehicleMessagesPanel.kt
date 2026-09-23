package com.example.mavlinkapplication.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mavlinkapplication.domain.MessageSeverity
import com.example.mavlinkapplication.theme.AppText
import com.example.mavlinkapplication.theme.StatusColors

@Composable
fun VehicleMessagesPanel(
    messages: List<VehicleMessage>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 20.dp, start = 20.dp, end = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "MESSAGES", style = AppText.Hero, fontWeight = FontWeight.Bold)
            TextButton(onClick = onClose) { Text("CLOSE", style = AppText.Body) }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        if (messages.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "No messages yet",
                    style = AppText.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(messages) { message ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Text(
                        text = message.timestamp,
                        style = AppText.Label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(
                        text = message.text,
                        style = AppText.Body,
                        color = message.severityColor(),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun VehicleMessage.severityColor() = when (severity) {
    MessageSeverity.CRITICAL -> MaterialTheme.colorScheme.error
    MessageSeverity.WARNING -> StatusColors.Caution
    MessageSeverity.INFO -> MaterialTheme.colorScheme.onSurface
}
