package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.dao.AlbumSummary
import com.example.data.database.dao.ArtistSummary
import com.example.data.database.entity.SongEntity
import com.example.data.model.Song
import com.example.data.model.toDomainModel
import com.example.data.repository.MusicRepository
import com.example.playback.PlayerManager
import com.example.ui.components.*

@Composable
fun HomeScreen(
    repository: MusicRepository,
    playerManager: PlayerManager,
    onNavigateToSearch: () -> Unit,
    onNavigateToSongs: () -> Unit,
    onNavigateToAlbum: (albumName: String) -> Unit,
    onNavigateToArtist: (artistName: String) -> Unit,
    onNavigateToPlaylist: (playlistId: Long, name: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onEditMetadata: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val recentlyPlayedEntities by repository.getRecentlyPlayedSongs(15).collectAsStateWithLifecycle(initialValue = emptyList())
    val recentlyAddedEntities by repository.getRecentlyAddedSongs(10).collectAsStateWithLifecycle(initialValue = emptyList())
    val mostPlayedEntities by repository.getMostPlayedSongs(10).collectAsStateWithLifecycle(initialValue = emptyList())
    val favoriteEntities by repository.getFavoriteSongs().collectAsStateWithLifecycle(initialValue = emptyList())
    val albumsList by repository.getAllAlbums().collectAsStateWithLifecycle(initialValue = emptyList())
    val artistsList by repository.getAllArtists().collectAsStateWithLifecycle(initialValue = emptyList())
    val songCount by repository.getSongCount().collectAsStateWithLifecycle(initialValue = 0)
    val storageStats by repository.getStorageStats().collectAsStateWithLifecycle(initialValue = com.example.data.database.dao.StorageStats(0, 0, 0, 0L, 0, 0, 0L))

    val currentSong by playerManager.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by playerManager.currentPositionMs.collectAsStateWithLifecycle()

    val recentlyPlayed = remember(recentlyPlayedEntities) { recentlyPlayedEntities.map { it.toDomainModel() } }
    val recentlyAdded = remember(recentlyAddedEntities) { recentlyAddedEntities.map { it.toDomainModel() } }
    val mostPlayed = remember(mostPlayedEntities) { mostPlayedEntities.map { it.toDomainModel() } }
    val favorites = remember(favoriteEntities) { favoriteEntities.map { it.toDomainModel() } }

    val featuredSong = currentSong ?: recentlyPlayed.firstOrNull() ?: recentlyAdded.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MeloVaultLogo(
                iconSize = 38.dp,
                showText = true,
                tagline = "Your Device • Your Way"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1A1C1E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.testTag("home_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search offline music",
                            tint = Color(0xFF9BA1A6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1A1C1E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF9BA1A6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (songCount == 0) {
            EmptyStateView(
                title = "No Music Discovered Yet",
                message = "Scan your internal storage or SD card to automatically find all your music.",
                actionButtonText = "Scan Now",
                onActionClick = onNavigateToSettings,
                modifier = Modifier.padding(top = 40.dp)
            )
        } else {
            // Immersive Hero Banner Card
            if (featuredSong != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    // Ambient gradient glow behind hero card
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(2.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF81C784).copy(alpha = 0.22f),
                                        Color(0xFF4DB6AC).copy(alpha = 0.22f)
                                    )
                                )
                            )
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .clickable {
                                if (currentSong?.id == featuredSong.id) {
                                    playerManager.togglePlayPause()
                                } else {
                                    val list = if (recentlyPlayed.isNotEmpty()) recentlyPlayed else listOf(featuredSong)
                                    val idx = list.indexOfFirst { it.id == featuredSong.id }.coerceAtLeast(0)
                                    playerManager.playSongList(list, idx)
                                }
                            }
                            .testTag("immersive_hero_card"),
                        color = Color(0xFF1A1C1E),
                        shape = RoundedCornerShape(28.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .shadow(16.dp, RoundedCornerShape(18.dp))
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF2D3135)),
                                contentAlignment = Alignment.Center
                            ) {
                                AlbumArtImage(
                                    artUri = featuredSong.albumArtUri,
                                    title = featuredSong.title,
                                    artist = featuredSong.artist,
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(18.dp),
                                    iconSize = 36.dp
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF81C784).copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = if (currentSong?.id == featuredSong.id && isPlaying) "NOW PLAYING" else "RECENTLY PLAYED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = Color(0xFF81C784),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = featuredSong.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${featuredSong.artist} • ${featuredSong.album}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9BA1A6),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isPlaying && currentSong?.id == featuredSong.id) Color(0xFF81C784)
                                                else Color(0xFF81C784).copy(alpha = 0.5f)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentSong?.id == featuredSong.id) "${formatDuration(currentPositionMs)} / ${formatDuration(featuredSong.duration)}" else formatDuration(featuredSong.duration),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF9BA1A6)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Storage Overview 2-Column Grid (Internal & SD Card)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Internal Storage Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp)),
                    color = Color(0xFF1A1C1E),
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Internal",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF81C784)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${storageStats.internalSongs.coerceAtLeast(songCount)}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "LOCAL TRACKS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFF9BA1A6)
                        )
                    }
                }

                // SD Card / External Storage Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp)),
                    color = Color(0xFF1A1C1E),
                    shape = RoundedCornerShape(22.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SD Card",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF4DB6AC)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${storageStats.sdCardSongs}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "EXTERNAL FILES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFF9BA1A6)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Discovery Queues (Liked Songs & Recently Added)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "DISCOVERY QUEUES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color(0xFF9BA1A6),
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                )

                // Liked Songs Item
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            if (favorites.isNotEmpty()) {
                                playerManager.playSongList(favorites, 0)
                            }
                        }
                        .testTag("liked_songs_hero_card"),
                    color = Color(0xFF1A1C1E).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF2D3135),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Liked Songs",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                            Text(
                                text = "${favorites.size} Local Favorites",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF9BA1A6)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Liked",
                                    tint = Color(0xFFE3E2E6),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Recently Added Item
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            if (recentlyAdded.isNotEmpty()) {
                                playerManager.playSongList(recentlyAdded, 0)
                            }
                        },
                    color = Color(0xFF1A1C1E).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF2D3135),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFF4DB6AC),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recently Added",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                            Text(
                                text = "${recentlyAdded.size} Newly Indexed Songs",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF9BA1A6)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Recent",
                                    tint = Color(0xFFE3E2E6),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recently Played Carousel
            if (recentlyPlayed.isNotEmpty()) {
                SectionHeader(
                    title = "Recently Played",
                    actionText = "See All",
                    onActionClick = onNavigateToSongs
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    recentlyPlayed.take(8).forEachIndexed { index, song ->
                        RecentlyPlayedCard(
                            song = song,
                            isCurrentSong = currentSong?.id == song.id,
                            isPlaying = isPlaying,
                            onClick = {
                                playerManager.playSongList(recentlyPlayed, index)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Albums Showcase
            if (albumsList.isNotEmpty()) {
                SectionHeader(
                    title = "Albums",
                    actionText = "More",
                    onActionClick = onNavigateToSongs
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    albumsList.take(6).forEach { album ->
                        AlbumGridCard(
                            album = album,
                            onClick = { onNavigateToAlbum(album.album) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Most Played Songs
            if (mostPlayed.isNotEmpty()) {
                SectionHeader(
                    title = "Most Played",
                    actionText = "Play All",
                    onActionClick = {
                        playerManager.playSongList(mostPlayed, 0)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    mostPlayed.take(5).forEachIndexed { index, song ->
                        SongListItem(
                            song = song,
                            isPlaying = isPlaying,
                            isCurrentSong = currentSong?.id == song.id,
                            onClick = {
                                playerManager.playSongList(mostPlayed, index)
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
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Recently Added
            if (recentlyAdded.isNotEmpty()) {
                SectionHeader(
                    title = "Recently Added",
                    actionText = "View All",
                    onActionClick = onNavigateToSongs
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    recentlyAdded.take(5).forEachIndexed { index, song ->
                        SongListItem(
                            song = song,
                            isPlaying = isPlaying,
                            isCurrentSong = currentSong?.id == song.id,
                            onClick = {
                                playerManager.playSongList(recentlyAdded, index)
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

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = (-0.3).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionText != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RecentlyPlayedCard(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1A1C1E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box {
                AlbumArtImage(
                    artUri = song.albumArtUri,
                    title = song.title,
                    artist = song.artist,
                    size = 116.dp,
                    shape = RoundedCornerShape(14.dp)
                )

                if (isCurrentSong) {
                    Surface(
                        modifier = Modifier
                            .size(116.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color(0xFF9BA1A6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AlbumGridCard(
    album: AlbumSummary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1A1C1E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            AlbumArtImage(
                artUri = album.albumArtUri,
                title = album.album,
                artist = album.artist,
                size = 116.dp,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = album.album,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${album.songCount} tracks",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color(0xFF9BA1A6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
