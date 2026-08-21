package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Song
import com.example.data.model.toDomainModel
import com.example.data.repository.MusicRepository
import com.example.playback.PlayerManager
import com.example.ui.components.AlbumArtImage
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SongListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    title: String,
    subtitle: String,
    songs: List<Song>,
    artUri: String? = null,
    playerManager: PlayerManager,
    onBackClick: () -> Unit,
    onEditMetadata: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onDeleteSongFromPlaylist: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentSong by playerManager.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerManager.isPlaying.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
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
                .padding(bottom = 90.dp)
        ) {
            // Header with artwork and quick actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtImage(
                    artUri = artUri ?: songs.firstOrNull()?.albumArtUri,
                    title = title,
                    artist = subtitle,
                    size = 90.dp,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${songs.size} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Play All & Shuffle Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            playerManager.playSongList(songs, 0)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = songs.isNotEmpty()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play All")
                }

                OutlinedButton(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            val shuffled = songs.shuffled()
                            playerManager.playSongList(shuffled, 0)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = songs.isNotEmpty()
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                modifier = Modifier.padding(top = 8.dp)
            )

            if (songs.isEmpty()) {
                EmptyStateView(
                    title = "No Songs",
                    message = "No audio tracks found in this category."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                        SongListItem(
                            song = song,
                            isPlaying = isPlaying,
                            isCurrentSong = currentSong?.id == song.id,
                            onClick = {
                                playerManager.playSongList(songs, index)
                            },
                            onFavoriteToggle = {
                                playerManager.toggleFavorite(song)
                            },
                            onPlayNext = { playerManager.playNext(song) },
                            onAddToQueue = { playerManager.addToQueue(song) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            onEditMetadata = { onEditMetadata(song) }
                        )
                    }
                }
            }
        }
    }
}
