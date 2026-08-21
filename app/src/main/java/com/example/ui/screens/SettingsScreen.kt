package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.dao.StorageStats
import com.example.data.mediastore.ScanProgress
import com.example.data.repository.MusicRepository
import com.example.playback.PlayerManager
import com.example.ui.components.MeloVaultLogo
import com.example.ui.components.formatDuration
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format("%.2f GB", mb / 1024.0)
    } else {
        String.format("%.1f MB", mb)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: MusicRepository,
    playerManager: PlayerManager,
    currentThemeMode: ThemeMode,
    currentAccent: AccentPalette,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentPalette) -> Unit,
    onBackClick: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val versionName = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    val storageStats by repository.getStorageStats().collectAsStateWithLifecycle(initialValue = StorageStats(0, 0, 0, 0L, 0, 0, 0L))
    val isGaplessEnabled by playerManager.isGaplessEnabled.collectAsStateWithLifecycle()
    val crossfadeDuration by playerManager.crossfadeDurationSeconds.collectAsStateWithLifecycle()

    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(ScanProgress()) }
    var scanCompleteMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Library") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage & Library Sync Section
            SettingsSectionTitle("Library & Storage")

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF1A1C1E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rescan Device Music",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Deep scans internal storage & SD card for new songs and ID3 tag updates",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9BA1A6)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                if (!isScanning) {
                                    isScanning = true
                                    scanCompleteMessage = null
                                    coroutineScope.launch {
                                        repository.scanAndSyncMusicProgress().collectLatest { progress ->
                                            scanProgress = progress
                                            if (progress.isComplete) {
                                                isScanning = false
                                                scanCompleteMessage = "Scan complete! Found ${progress.songsFound} songs."
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isScanning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("rescan_music_button")
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rescan")
                            }
                        }
                    }

                    if (isScanning) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.08f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (scanProgress.currentSong.isNotEmpty()) "Scanning: ${scanProgress.currentSong}" else "Discovering music files...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }

                    if (scanCompleteMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = scanCompleteMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Storage Overview Breakdown
                    Text(
                        text = "Storage Overview",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = Color(0xFF9BA1A6)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StorageInfoTile("Total Tracks", "${storageStats.totalSongs}", Modifier.weight(1f))
                        StorageInfoTile("Storage Used", formatBytes(storageStats.totalFileSize), Modifier.weight(1f))
                        StorageInfoTile("Total Time", formatDuration(storageStats.totalDuration), Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Internal: ${storageStats.internalSongs} songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF81C784)
                        )
                        Text(
                            text = "SD Card: ${storageStats.sdCardSongs} songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4DB6AC)
                        )
                    }
                }
            }

            // Audio & Playback Section
            SettingsSectionTitle("Audio & Playback")

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF1A1C1E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Equalizer Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenEqualizer() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF2D3135),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Equalizer & Sound Effects",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                                Text(
                                    text = "5-band EQ, Bass Boost, and 3D Virtualizer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9BA1A6)
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF9BA1A6))
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                    // Gapless Playback Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF2D3135),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.FastForward,
                                        contentDescription = null,
                                        tint = if (isGaplessEnabled) MaterialTheme.colorScheme.primary else Color(0xFF9BA1A6),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Gapless Playback",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Seamless track transitions with zero silent gaps between songs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9BA1A6)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isGaplessEnabled,
                            onCheckedChange = { playerManager.setGaplessEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                    // Crossfade Duration Setting
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF2D3135),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = if (crossfadeDuration > 0) MaterialTheme.colorScheme.primary else Color(0xFF9BA1A6),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Audio Crossfade",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Smoothly blend track endings into track beginnings",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF9BA1A6)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (crossfadeDuration > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFF2D3135)
                            ) {
                                Text(
                                    text = if (crossfadeDuration == 0) "Off" else "${crossfadeDuration}s",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (crossfadeDuration > 0) MaterialTheme.colorScheme.primary else Color(0xFF9BA1A6),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick duration preset chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0 to "Off", 2 to "2s", 4 to "4s", 6 to "6s", 8 to "8s", 10 to "10s", 12 to "12s").forEach { (secs, label) ->
                                FilterChip(
                                    selected = crossfadeDuration == secs,
                                    onClick = { playerManager.setCrossfadeDuration(secs) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = crossfadeDuration.toFloat(),
                            onValueChange = { playerManager.setCrossfadeDuration(it.toInt()) },
                            valueRange = 0f..12f,
                            steps = 11,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color(0xFF2D3135)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Appearance & Theming Section
            SettingsSectionTitle("Appearance")

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF1A1C1E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.values().forEach { mode ->
                            FilterChip(
                                selected = currentThemeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Accent Color Palette",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AccentPalette.values().forEach { palette ->
                            val isSelected = currentAccent == palette
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color(0xFF2D3135),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                                modifier = Modifier.clickable { onAccentChange(palette) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(palette.primary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = palette.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Offline & Privacy Guarantee Banner
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF1A1C1E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF81C784).copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "100% Offline & Private",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF81C784)
                        )
                        Text(
                            text = "MeloVault operates entirely on your device. No cloud sync, no tracking, and no internet required for playback.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9BA1A6)
                        )
                    }
                }
            }

            // About MeloVault
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF1A1C1E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAbout() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        MeloVaultLogo(
                            iconSize = 40.dp,
                            showText = true,
                            tagline = "Your Music. Your Device. Your Way."
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Version $versionName • Tap for Developer & Legal Info",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9BA1A6)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open About",
                        tint = Color(0xFF9BA1A6)
                    )
                }
            }

            // Developer Credit Footer
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MeloVault v$versionName",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF9BA1A6)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Made by Obaidur Rahman",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9BA1A6).copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun StorageInfoTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
