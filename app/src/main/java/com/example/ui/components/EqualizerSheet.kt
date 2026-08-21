package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playback.EqualizerPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    isEnabled: Boolean,
    presets: List<EqualizerPreset>,
    selectedPreset: String,
    bands: List<Int>,
    bassBoost: Int,
    virtualizer: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (String) -> Unit,
    onBandChange: (bandIndex: Int, gainDb: Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val bandFrequencies = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Equalizer & Effects",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets Horizontal Chips
            Text(
                text = "Presets",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = selectedPreset.equals(preset.name, ignoreCase = true),
                        onClick = { onSelectPreset(preset.name) },
                        label = { Text(preset.name) },
                        enabled = isEnabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5-Band Sliders
            Text(
                text = "Frequency Bands",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                bands.forEachIndexed { index, gain ->
                    val freqLabel = bandFrequencies.getOrElse(index) { "Band $index" }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (gain > 0) "+$gain dB" else "$gain dB",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = gain.toFloat(),
                            onValueChange = { onBandChange(index, it.toInt()) },
                            valueRange = -10f..10f,
                            steps = 19,
                            enabled = isEnabled,
                            modifier = Modifier
                                .height(120.dp)
                                .width(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = freqLabel,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Audio Enhancements (Bass Boost & Virtualizer)
            Text(
                text = "Audio Enhancements",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Bass Boost
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Bass Boost", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${(bassBoost / 10)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = bassBoost.toFloat(),
                    onValueChange = { onBassBoostChange(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Virtualizer (3D Sound)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "3D Surround / Virtualizer", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${(virtualizer / 10)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = virtualizer.toFloat(),
                    onValueChange = { onVirtualizerChange(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
