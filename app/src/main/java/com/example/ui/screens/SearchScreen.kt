package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Song
import com.example.data.model.toDomainModel
import com.example.data.repository.MusicRepository
import com.example.playback.PlayerManager
import com.example.ui.components.AlbumArtImage
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SongListItem

enum class SearchCategory(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    repository: MusicRepository,
    playerManager: PlayerManager,
    onBackClick: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onEditMetadata: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SearchCategory.ALL) }

    val rawSearchResults by repository.searchSongs(query).collectAsStateWithLifecycle(initialValue = emptyList())
    val albumsList by repository.getAllAlbums().collectAsStateWithLifecycle(initialValue = emptyList())
    val artistsList by repository.getAllArtists().collectAsStateWithLifecycle(initialValue = emptyList())

    val currentSong by playerManager.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerManager.isPlaying.collectAsStateWithLifecycle()

    val matchedSongs = remember(rawSearchResults) { rawSearchResults.map { it.toDomainModel() } }
    val matchedAlbums = remember(albumsList, query) {
        if (query.isBlank()) emptyList()
        else albumsList.filter { it.album.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
    }
    val matchedArtists = remember(artistsList, query) {
        if (query.isBlank()) emptyList()
        else artistsList.filter { it.artist.contains(query, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    placeholder = { Text("Search songs, artists, albums...") },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                    ),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("search_text_input")
                                )
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SearchCategory.values().forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category.label) }
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 90.dp)
        ) {
            if (query.isBlank()) {
                EmptyStateView(
                    title = "Search MeloVault",
                    message = "Find any song, artist, or album stored on your device.",
                    icon = Icons.Default.Search
                )
            } else if (matchedSongs.isEmpty() && matchedAlbums.isEmpty() && matchedArtists.isEmpty()) {
                EmptyStateView(
                    title = "No results found",
                    message = "No offline matches found for '$query'."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Artists Section
                    if ((selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.ARTISTS) && matchedArtists.isNotEmpty()) {
                        item {
                            Text(
                                text = "Artists",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        items(matchedArtists.size) { i ->
                            val artist = matchedArtists[i]
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToArtist(artist.artist) },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = artist.artist,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        }
                    }

                    // Albums Section
                    if ((selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.ALBUMS) && matchedAlbums.isNotEmpty()) {
                        item {
                            Text(
                                text = "Albums",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        items(matchedAlbums.size) { i ->
                            val album = matchedAlbums[i]
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToAlbum(album.album) },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AlbumArtImage(
                                        artUri = album.albumArtUri,
                                        title = album.album,
                                        artist = album.artist,
                                        size = 46.dp,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = album.album,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = album.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Songs Section
                    if ((selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.SONGS) && matchedSongs.isNotEmpty()) {
                        item {
                            Text(
                                text = "Songs",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        itemsIndexed(matchedSongs, key = { _, song -> song.id }) { index, song ->
                            SongListItem(
                                song = song,
                                isPlaying = isPlaying,
                                isCurrentSong = currentSong?.id == song.id,
                                onClick = {
                                    playerManager.playSongList(matchedSongs, index)
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
}
