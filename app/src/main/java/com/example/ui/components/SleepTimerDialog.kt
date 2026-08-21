package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SleepTimerDialog(
    remainingSeconds: Int?,
    onDismiss: () -> Unit,
    onSetTimer: (minutes: Int, afterSong: Boolean) -> Unit,
    onCancelTimer: () -> Unit
) {
    val options = listOf(
        Pair("15 minutes", 15),
        Pair("30 minutes", 30),
        Pair("45 minutes", 45),
        Pair("60 minutes", 60),
        Pair("90 minutes", 90)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Sleep Timer")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (remainingSeconds != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val statusText = if (remainingSeconds == -1) {
                                "Stopping after current song"
                            } else {
                                "Stopping in ${remainingSeconds / 60}m ${remainingSeconds % 60}s"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = {
                                onCancelTimer()
                                onDismiss()
                            }) {
                                Text("Turn Off")
                            }
                        }
                    }
                }

                Text(
                    text = "Turn off music automatically:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                options.forEach { (label, minutes) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSetTimer(minutes, false)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }

                // End after current song option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSetTimer(0, true)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "End after current song",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
