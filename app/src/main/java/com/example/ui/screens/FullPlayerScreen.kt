package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Song
import com.example.playback.PlayerManager
import com.example.playback.RepeatMode
import com.example.ui.components.AlbumArtImage
import com.example.ui.components.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    playerManager: PlayerManager,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onEditMetadata: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by playerManager.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by playerManager.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by playerManager.durationMs.collectAsStateWithLifecycle()
    val shuffleMode by playerManager.shuffleMode.collectAsStateWithLifecycle()
    val repeatMode by playerManager.repeatMode.collectAsStateWithLifecycle()
    val sleepTimerRemaining by playerManager.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderValue by remember { mutableStateOf(0f) }
    var menuExpanded by remember { mutableStateOf(false) }

    val song = currentSong ?: return

    val totalDuration = if (durationMs > 0) durationMs else song.duration
    val currentPosition = if (isDraggingSlider) (dragSliderValue * totalDuration).toLong() else currentPositionMs
    val sliderProgress = if (totalDuration > 0) {
        (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val albumArtScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.92f,
        label = "art_scale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "PLAYING FROM LIBRARY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = song.album,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_full_player_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize player",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add to Playlist") },
                                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onAddToPlaylist(song)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Metadata") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEditMetadata(song)
                                }
                            )
                        }
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // High-Res Album Artwork Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .scale(albumArtScale)
                    .shadow(24.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtImage(
                    artUri = song.albumArtUri,
                    title = song.title,
                    artist = song.artist,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(24.dp),
                    iconSize = 64.dp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title, Artist, & Favorite Heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = song.displayTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.displayArtist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onEditMetadata(song) },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("full_player_edit_metadata")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit song details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { playerManager.toggleFavorite(song) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("full_player_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Slider & Timestamps
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = sliderProgress,
                    onValueChange = {
                        isDraggingSlider = true
                        dragSliderValue = it
                    },
                    onValueChangeFinished = {
                        val seekToMs = (dragSliderValue * totalDuration).toLong()
                        playerManager.seekTo(seekToMs)
                        isDraggingSlider = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_progress_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(totalDuration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Playback Controls: Shuffle, Prev, Play/Pause, Next, Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(
                    onClick = { playerManager.toggleShuffle() },
                    modifier = Modifier.testTag("player_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous Button
                IconButton(
                    onClick = { playerManager.skipToPrevious() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous song",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause FAB
                FilledIconButton(
                    onClick = { playerManager.togglePlayPause() },
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(12.dp, CircleShape)
                        .testTag("player_play_pause_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Next Button
                IconButton(
                    onClick = { playerManager.skipToNext() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next song",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat Button
                IconButton(
                    onClick = { playerManager.toggleRepeat() },
                    modifier = Modifier.testTag("player_repeat_button")
                ) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat mode: ${repeatMode.name}",
                        tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Actions: Equalizer, Sleep Timer, Queue
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Equalizer Action
                    TextButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier.testTag("player_equalizer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Equalizer",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Sleep Timer Action
                    TextButton(
                        onClick = onOpenSleepTimer,
                        modifier = Modifier.testTag("player_sleep_timer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = if (sleepTimerRemaining != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (sleepTimerRemaining != null) "Timer On" else "Timer",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (sleepTimerRemaining != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Queue Action
                    TextButton(
                        onClick = onOpenQueue,
                        modifier = Modifier.testTag("player_queue_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Queue",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
