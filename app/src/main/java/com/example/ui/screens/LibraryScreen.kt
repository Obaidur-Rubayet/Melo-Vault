package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.dao.AlbumSummary
import com.example.data.database.dao.ArtistSummary
import com.example.data.database.dao.GenreSummary
import com.example.data.database.entity.CustomFolderEntity
import com.example.data.database.entity.PlaylistEntity
import com.example.data.repository.MusicRepository
import com.example.playback.PlayerManager
import com.example.ui.components.AlbumArtImage
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.generatePlaceholderGradient
import kotlinx.coroutines.launch

enum class LibraryTab(val title: String) {
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    GENRES("Genres"),
    PLAYLISTS("Playlists"),
    FOLDERS("Folders")
}

@Composable
fun LibraryScreen(
    repository: MusicRepository,
    playerManager: PlayerManager,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToGenre: (String) -> Unit,
    onNavigateToPlaylist: (Long, String) -> Unit,
    onNavigateToFolder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.ALBUMS) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe Library data
    val albums by repository.getAllAlbums().collectAsStateWithLifecycle(initialValue = emptyList())
    val artists by repository.getAllArtists().collectAsStateWithLifecycle(initialValue = emptyList())
    val genres by repository.getAllGenres().collectAsStateWithLifecycle(initialValue = emptyList())
    val playlists by repository.getAllPlaylists().collectAsStateWithLifecycle(initialValue = emptyList())
    val customFolders by repository.getCustomFolders().collectAsStateWithLifecycle(initialValue = emptyList())

    // SAF folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // ignore
            }
            val displayName = uri.lastPathSegment?.substringAfterLast(':') ?: "Music Folder"
            coroutineScope.launch {
                repository.addCustomFolder(uri.toString(), displayName)
                repository.directScanAndSync()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 100.dp)
    ) {
        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }
        ) {
            LibraryTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.title,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                LibraryTab.ALBUMS -> AlbumsTabContent(
                    albums = albums,
                    onAlbumClick = onNavigateToAlbum
                )
                LibraryTab.ARTISTS -> ArtistsTabContent(
                    artists = artists,
                    onArtistClick = onNavigateToArtist
                )
                LibraryTab.GENRES -> GenresTabContent(
                    genres = genres,
                    onGenreClick = onNavigateToGenre
                )
                LibraryTab.PLAYLISTS -> PlaylistsTabContent(
                    playlists = playlists,
                    onCreatePlaylist = { showCreatePlaylistDialog = true },
                    onPlaylistClick = onNavigateToPlaylist,
                    onDeletePlaylist = { playlistId ->
                        coroutineScope.launch { repository.deletePlaylist(playlistId) }
                    }
                )
                LibraryTab.FOLDERS -> FoldersTabContent(
                    customFolders = customFolders,
                    onAddFolderClick = { folderPickerLauncher.launch(null) },
                    onFolderClick = onNavigateToFolder,
                    onDeleteFolder = { folderId ->
                        coroutineScope.launch { repository.deleteCustomFolder(folderId) }
                    }
                )
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name, description, isDynamic, dynamicType, dynamicCriteria ->
                coroutineScope.launch {
                    if (isDynamic && dynamicType != null) {
                        repository.createDynamicPlaylist(name, description, dynamicType, dynamicCriteria)
                    } else {
                        repository.createPlaylist(name, description)
                    }
                }
            }
        )
    }
}

@Composable
private fun AlbumsTabContent(
    albums: List<AlbumSummary>,
    onAlbumClick: (String) -> Unit
) {
    if (albums.isEmpty()) {
        EmptyStateView(
            title = "No Albums Found",
            message = "Audio files on your device will automatically be grouped into albums."
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(albums, key = { it.album + it.artist }) { album ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlbumClick(album.album) }
                        .testTag("album_card_${album.album}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        AlbumArtImage(
                            artUri = album.albumArtUri,
                            title = album.album,
                            artist = album.artist,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = album.album,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = album.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${album.songCount} songs",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistsTabContent(
    artists: List<ArtistSummary>,
    onArtistClick: (String) -> Unit
) {
    if (artists.isEmpty()) {
        EmptyStateView(
            title = "No Artists Found",
            message = "Artists will appear here once audio files are discovered."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(artists, key = { it.artist }) { artist ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onArtistClick(artist.artist) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artist.artist,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${artist.songCount} songs • ${artist.albumCount} albums",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenresTabContent(
    genres: List<GenreSummary>,
    onGenreClick: (String) -> Unit
) {
    if (genres.isEmpty()) {
        EmptyStateView(
            title = "No Genres Found",
            message = "Genres extracted from local ID3 tags will appear here."
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(genres, key = { it.genre }) { genre ->
                val gradient = remember(genre.genre) {
                    generatePlaceholderGradient(genre.genre)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clickable { onGenreClick(genre.genre) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(gradient)
                            .padding(14.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Text(
                                text = genre.genre,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "${genre.songCount} songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTabContent(
    playlists: List<PlaylistEntity>,
    onCreatePlaylist: () -> Unit,
    onPlaylistClick: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Create Playlist Action Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${playlists.size} Playlists",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = onCreatePlaylist,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Playlist")
            }
        }

        if (playlists.isEmpty()) {
            EmptyStateView(
                title = "No Custom Playlists",
                message = "Create custom offline playlists to organize your favorite music.",
                actionButtonText = "Create Playlist",
                onActionClick = onCreatePlaylist
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaylistClick(playlist.id, playlist.name) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (playlist.isDynamic) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (playlist.isDynamic) Icons.Default.Bolt else Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = if (playlist.isDynamic) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (playlist.isDynamic) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "SMART",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (playlist.isDynamic) {
                                        if (playlist.dynamicCriteria != null) {
                                            "Auto • ${playlist.dynamicCriteria}"
                                        } else {
                                            "Auto-updating • ${playlist.description.ifEmpty { "Smart Playlist" }}"
                                        }
                                    } else {
                                        "${playlist.songCount} songs${if (playlist.description.isNotBlank()) " • " + playlist.description else ""}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete playlist",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Playlist") },
                            text = { Text("Are you sure you want to delete '${playlist.name}'? Your music files will not be deleted.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        onDeletePlaylist(playlist.id)
                                        showDeleteDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoldersTabContent(
    customFolders: List<CustomFolderEntity>,
    onAddFolderClick: () -> Unit,
    onFolderClick: (String) -> Unit,
    onDeleteFolder: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Music Locations",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = onAddFolderClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Folder")
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Built-in Storage Items
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFolderClick("Internal Storage") },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Internal Storage",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Auto-scanned via MediaStore",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFolderClick("SD Card") },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SdCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "External SD Card Storage",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Auto-scanned via MediaStore",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (customFolders.isNotEmpty()) {
                item {
                    Text(
                        text = "Manually Added Folders",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                items(customFolders, key = { it.id }) { folder ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFolderClick(folder.displayName) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.displayName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = folder.uriString,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onDeleteFolder(folder.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove folder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
