package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    val remMinutes = minutes % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, remMinutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", remMinutes, seconds)
    }
}

@Composable
fun SongListItem(
    song: Song,
    isPlaying: Boolean,
    isCurrentSong: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: (Boolean) -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditMetadata: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("song_item_${song.id}"),
        color = if (isCurrentSong) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            Color.Transparent
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art Thumbnail
            Box(contentAlignment = Alignment.Center) {
                AlbumArtImage(
                    artUri = song.albumArtUri,
                    title = song.displayTitle,
                    artist = song.displayArtist,
                    size = 52.dp,
                    shape = RoundedCornerShape(10.dp)
                )

                if (isCurrentSong) {
                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Playing" else "Paused",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title, Artist, Album details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = song.displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isCurrentSong) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${song.displayArtist} • ${song.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = " • ${formatDuration(song.duration)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Favorite Button
            IconButton(
                onClick = { onFavoriteToggle(!song.isFavorite) },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("favorite_button_${song.id}")
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (song.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // 3-Dots Dropdown Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("song_menu_${song.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Song options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Next") },
                        leadingIcon = { Icon(Icons.Default.PlaylistPlay, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onPlayNext()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Queue") },
                        leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddToQueue()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Metadata") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEditMetadata()
                        }
                    )
                }
            }
        }
    }
}
