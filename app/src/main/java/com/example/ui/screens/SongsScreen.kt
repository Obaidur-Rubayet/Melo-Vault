package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Song
import com.example.data.model.SongSortOption
import com.example.data.model.toDomainModel
import com.example.data.repository.MusicRepository
import com.example.playback.PlayerManager
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SongListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    repository: MusicRepository,
    playerManager: PlayerManager,
    onEditMetadata: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSort by remember { mutableStateOf(SongSortOption.TITLE_ASC) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val songEntities by repository.getSongs(selectedSort).collectAsStateWithLifecycle(initialValue = emptyList())
    val currentSong by playerManager.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerManager.isPlaying.collectAsStateWithLifecycle()

    val songs = remember(songEntities, searchQuery) {
        val domainList = songEntities.map { it.toDomainModel() }
        if (searchQuery.isBlank()) {
            domainList
        } else {
            val q = searchQuery.trim().lowercase()
            domainList.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 100.dp)
    ) {
        // Top Search Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter songs by title, artist...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Action Bar: Count, Sort, Play All & Shuffle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${songs.size} songs",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Sort Dropdown Button
                Box {
                    TextButton(
                        onClick = { sortMenuExpanded = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("sort_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedSort.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        SongSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayName,
                                        fontWeight = if (option == selectedSort) FontWeight.Bold else FontWeight.Normal,
                                        color = if (option == selectedSort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = if (option == selectedSort) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                } else null,
                                onClick = {
                                    selectedSort = option
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Quick Play & Shuffle Action Buttons
            Row {
                IconButton(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            val shuffled = songs.shuffled()
                            playerManager.playSongList(shuffled, 0)
                        }
                    },
                    modifier = Modifier.testTag("shuffle_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle all",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                FilledIconButton(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            playerManager.playSongList(songs, 0)
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("play_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play all",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            modifier = Modifier.padding(top = 4.dp)
        )

        // Song List
        if (songs.isEmpty()) {
            EmptyStateView(
                title = if (searchQuery.isNotEmpty()) "No Matching Songs" else "No Songs Found",
                message = if (searchQuery.isNotEmpty()) "Try a different search query." else "Scan your device to discover audio files."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                itemsIndexed(
                    items = songs,
                    key = { _, song -> song.id }
                ) { index, song ->
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
