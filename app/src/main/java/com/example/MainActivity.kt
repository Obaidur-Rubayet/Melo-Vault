package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.entity.PlaylistEntity
import com.example.data.database.entity.SongEntity
import com.example.data.model.Song
import com.example.data.model.toDomainModel
import com.example.data.repository.MusicRepository
import com.example.playback.PlayerManager
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.MeloVaultTheme
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object Songs : Screen()
    object Library : Screen()
    object Settings : Screen()
    object Search : Screen()
    data class AlbumDetail(val albumName: String) : Screen()
    data class ArtistDetail(val artistName: String) : Screen()
    data class GenreDetail(val genreName: String) : Screen()
    data class PlaylistDetail(val playlistId: Long, val playlistName: String) : Screen()
    data class FolderDetail(val folderName: String) : Screen()
    object About : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MeloVaultApplication
        val repository = app.repository
        val playerManager = app.playerManager

        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()

            var themeMode by remember { mutableStateOf(ThemeMode.DARK) }
            var accentPalette by remember { mutableStateOf(AccentPalette.CYAN_ELECTRIC) }

            val hasAudioPermission = remember {
                val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            }

            var isFirstLaunchFlow by remember { mutableStateOf(!hasAudioPermission) }

            MeloVaultTheme(themeMode = themeMode, accentPalette = accentPalette) {
                if (isFirstLaunchFlow) {
                    FirstLaunchScreen(
                        repository = repository,
                        onCompleted = { isFirstLaunchFlow = false }
                    )
                } else {
                    MeloVaultApp(
                        repository = repository,
                        playerManager = playerManager,
                        currentThemeMode = themeMode,
                        currentAccent = accentPalette,
                        onThemeModeChange = { themeMode = it },
                        onAccentChange = { accentPalette = it }
                    )
                }
            }
        }
    }
}

@Composable
fun MeloVaultApp(
    repository: MusicRepository,
    playerManager: PlayerManager,
    currentThemeMode: ThemeMode,
    currentAccent: AccentPalette,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentPalette) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var screenBackstack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }

    fun navigateTo(screen: Screen) {
        screenBackstack = screenBackstack + screen
        currentScreen = screen
    }

    fun navigateBack() {
        if (screenBackstack.size > 1) {
            val updated = screenBackstack.dropLast(1)
            screenBackstack = updated
            currentScreen = updated.last()
        } else {
            currentScreen = Screen.Home
            screenBackstack = listOf(Screen.Home)
        }
    }

    BackHandler(enabled = true) {
        navigateBack()
    }

    // Playback State
    val currentSong by playerManager.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerManager.isPlaying.collectAsStateWithLifecycle()
    val progressMs by playerManager.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by playerManager.durationMs.collectAsStateWithLifecycle()
    val queue by playerManager.queue.collectAsStateWithLifecycle()
    val currentQueueIndex by playerManager.currentQueueIndex.collectAsStateWithLifecycle()
    val sleepTimerRemaining by playerManager.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()
    val isEqualizerEnabled by playerManager.isEqualizerEnabled.collectAsStateWithLifecycle()
    val equalizerBands by playerManager.equalizerBands.collectAsStateWithLifecycle()
    val bassBoost by playerManager.bassBoostLevel.collectAsStateWithLifecycle()
    val virtualizer by playerManager.virtualizerLevel.collectAsStateWithLifecycle()
    val selectedPreset by playerManager.selectedPreset.collectAsStateWithLifecycle()

    val allPlaylists by repository.getAllPlaylists().collectAsStateWithLifecycle(initialValue = emptyList())

    // Auto-clean any unformatted or underscore-laden metadata in background
    LaunchedEffect(Unit) {
        repository.cleanAllSongsMetadata()
    }

    // Dialog & Sheet States
    var showFullPlayer by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var songForAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var songForMetadataEdit by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (!showFullPlayer) {
                Column {
                    // Mini Player
                    MiniPlayer(
                        song = currentSong,
                        isPlaying = isPlaying,
                        progressMs = progressMs,
                        durationMs = durationMs,
                        onPlayPauseClick = { playerManager.togglePlayPause() },
                        onSkipNextClick = { playerManager.skipToNext() },
                        onFavoriteClick = { currentSong?.let { playerManager.toggleFavorite(it) } },
                        onClick = { showFullPlayer = true },
                        onLongClick = { currentSong?.let { songForMetadataEdit = it } }
                    )

                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = Color(0xFF141618),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp
                    ) {
                        val isHome = currentScreen is Screen.Home
                        val isSongs = currentScreen is Screen.Songs
                        val isLibrary = currentScreen is Screen.Library
                        val isSettings = currentScreen is Screen.Settings

                        NavigationBarItem(
                            selected = isHome,
                            onClick = {
                                currentScreen = Screen.Home
                                screenBackstack = listOf(Screen.Home)
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontWeight = if (isHome) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF0D0D0F),
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = Color(0xFF9BA1A6),
                                unselectedTextColor = Color(0xFF9BA1A6)
                            ),
                            modifier = Modifier.testTag("nav_home")
                        )
                        NavigationBarItem(
                            selected = isSongs,
                            onClick = {
                                currentScreen = Screen.Songs
                                screenBackstack = listOf(Screen.Songs)
                            },
                            icon = { Icon(Icons.Default.MusicNote, contentDescription = "Songs") },
                            label = { Text("Songs", fontWeight = if (isSongs) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF0D0D0F),
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = Color(0xFF9BA1A6),
                                unselectedTextColor = Color(0xFF9BA1A6)
                            ),
                            modifier = Modifier.testTag("nav_songs")
                        )
                        NavigationBarItem(
                            selected = isLibrary,
                            onClick = {
                                currentScreen = Screen.Library
                                screenBackstack = listOf(Screen.Library)
                            },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                            label = { Text("Library", fontWeight = if (isLibrary) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF0D0D0F),
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = Color(0xFF9BA1A6),
                                unselectedTextColor = Color(0xFF9BA1A6)
                            ),
                            modifier = Modifier.testTag("nav_library")
                        )
                        NavigationBarItem(
                            selected = isSettings,
                            onClick = {
                                currentScreen = Screen.Settings
                                screenBackstack = listOf(Screen.Settings)
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", fontWeight = if (isSettings) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF0D0D0F),
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = Color(0xFF9BA1A6),
                                unselectedTextColor = Color(0xFF9BA1A6)
                            ),
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val screen = currentScreen) {
                is Screen.Home -> HomeScreen(
                    repository = repository,
                    playerManager = playerManager,
                    onNavigateToSearch = { navigateTo(Screen.Search) },
                    onNavigateToSongs = { navigateTo(Screen.Songs) },
                    onNavigateToAlbum = { album -> navigateTo(Screen.AlbumDetail(album)) },
                    onNavigateToArtist = { artist -> navigateTo(Screen.ArtistDetail(artist)) },
                    onNavigateToPlaylist = { id, name -> navigateTo(Screen.PlaylistDetail(id, name)) },
                    onNavigateToSettings = { navigateTo(Screen.Settings) },
                    onEditMetadata = { song -> songForMetadataEdit = song },
                    onAddToPlaylist = { song -> songForAddToPlaylist = song }
                )
                is Screen.Songs -> SongsScreen(
                    repository = repository,
                    playerManager = playerManager,
                    onEditMetadata = { song -> songForMetadataEdit = song },
                    onAddToPlaylist = { song -> songForAddToPlaylist = song }
                )
                is Screen.Library -> LibraryScreen(
                    repository = repository,
                    playerManager = playerManager,
                    onNavigateToAlbum = { album -> navigateTo(Screen.AlbumDetail(album)) },
                    onNavigateToArtist = { artist -> navigateTo(Screen.ArtistDetail(artist)) },
                    onNavigateToGenre = { genre -> navigateTo(Screen.GenreDetail(genre)) },
                    onNavigateToPlaylist = { id, name -> navigateTo(Screen.PlaylistDetail(id, name)) },
                    onNavigateToFolder = { folder -> navigateTo(Screen.FolderDetail(folder)) }
                )
                is Screen.Settings -> SettingsScreen(
                    repository = repository,
                    playerManager = playerManager,
                    currentThemeMode = currentThemeMode,
                    currentAccent = currentAccent,
                    onThemeModeChange = onThemeModeChange,
                    onAccentChange = onAccentChange,
                    onBackClick = { navigateBack() },
                    onOpenEqualizer = { showEqualizerSheet = true },
                    onOpenAbout = { navigateTo(Screen.About) }
                )
                is Screen.About -> AboutScreen(
                    onBackClick = { navigateBack() }
                )
                is Screen.Search -> SearchScreen(
                    repository = repository,
                    playerManager = playerManager,
                    onBackClick = { navigateBack() },
                    onNavigateToAlbum = { album -> navigateTo(Screen.AlbumDetail(album)) },
                    onNavigateToArtist = { artist -> navigateTo(Screen.ArtistDetail(artist)) },
                    onEditMetadata = { song -> songForMetadataEdit = song },
                    onAddToPlaylist = { song -> songForAddToPlaylist = song }
                )
                is Screen.AlbumDetail -> {
                    val albumSongEntities by repository.getSongsByAlbum(screen.albumName)
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    val albumSongs = remember(albumSongEntities) { albumSongEntities.map { it.toDomainModel() } }
                    DetailScreen(
                        title = screen.albumName,
                        subtitle = albumSongs.firstOrNull()?.artist ?: "Album",
                        songs = albumSongs,
                        artUri = albumSongs.firstOrNull()?.albumArtUri,
                        playerManager = playerManager,
                        onBackClick = { navigateBack() },
                        onEditMetadata = { song -> songForMetadataEdit = song },
                        onAddToPlaylist = { song -> songForAddToPlaylist = song }
                    )
                }
                is Screen.ArtistDetail -> {
                    val artistSongEntities by repository.getSongsByArtist(screen.artistName)
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    val artistSongs = remember(artistSongEntities) { artistSongEntities.map { it.toDomainModel() } }
                    DetailScreen(
                        title = screen.artistName,
                        subtitle = "Artist",
                        songs = artistSongs,
                        artUri = artistSongs.firstOrNull()?.albumArtUri,
                        playerManager = playerManager,
                        onBackClick = { navigateBack() },
                        onEditMetadata = { song -> songForMetadataEdit = song },
                        onAddToPlaylist = { song -> songForAddToPlaylist = song }
                    )
                }
                is Screen.GenreDetail -> {
                    val genreSongEntities by repository.getSongsByGenre(screen.genreName)
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    val genreSongs = remember(genreSongEntities) { genreSongEntities.map { it.toDomainModel() } }
                    DetailScreen(
                        title = screen.genreName,
                        subtitle = "Genre",
                        songs = genreSongs,
                        playerManager = playerManager,
                        onBackClick = { navigateBack() },
                        onEditMetadata = { song -> songForMetadataEdit = song },
                        onAddToPlaylist = { song -> songForAddToPlaylist = song }
                    )
                }
                is Screen.PlaylistDetail -> {
                    val playlistSongEntities by repository.getSongsForPlaylist(screen.playlistId)
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    val playlistSongs = remember(playlistSongEntities) { playlistSongEntities.map { it.toDomainModel() } }
                    DetailScreen(
                        title = screen.playlistName,
                        subtitle = "Playlist",
                        songs = playlistSongs,
                        playerManager = playerManager,
                        onBackClick = { navigateBack() },
                        onEditMetadata = { song -> songForMetadataEdit = song },
                        onAddToPlaylist = { song -> songForAddToPlaylist = song },
                        onDeleteSongFromPlaylist = { songId ->
                            coroutineScope.launch {
                                repository.removeSongFromPlaylist(screen.playlistId, songId)
                            }
                        }
                    )
                }
                is Screen.FolderDetail -> {
                    val folderSongEntities by repository.getSongsByFolder(screen.folderName)
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    val folderSongs = remember(folderSongEntities) { folderSongEntities.map { it.toDomainModel() } }
                    DetailScreen(
                        title = screen.folderName,
                        subtitle = "Folder",
                        songs = folderSongs,
                        playerManager = playerManager,
                        onBackClick = { navigateBack() },
                        onEditMetadata = { song -> songForMetadataEdit = song },
                        onAddToPlaylist = { song -> songForAddToPlaylist = song }
                    )
                }
            }
        }
    }

    // Full Player Screen Overlay
    if (showFullPlayer && currentSong != null) {
        FullPlayerScreen(
            playerManager = playerManager,
            onDismiss = { showFullPlayer = false },
            onOpenQueue = { showQueueSheet = true },
            onOpenEqualizer = { showEqualizerSheet = true },
            onOpenSleepTimer = { showSleepTimerDialog = true },
            onEditMetadata = { song -> songForMetadataEdit = song },
            onAddToPlaylist = { song -> songForAddToPlaylist = song }
        )
    }

    // Global Dialogs & Sheets
    if (showEqualizerSheet) {
        EqualizerSheet(
            isEnabled = isEqualizerEnabled,
            presets = playerManager.presets,
            selectedPreset = selectedPreset,
            bands = equalizerBands,
            bassBoost = bassBoost,
            virtualizer = virtualizer,
            onToggleEnabled = { playerManager.toggleEqualizer(it) },
            onSelectPreset = { playerManager.setEqualizerPreset(it) },
            onBandChange = { band, gain -> playerManager.setBandGain(band, gain) },
            onBassBoostChange = { playerManager.setBassBoost(it) },
            onVirtualizerChange = { playerManager.setVirtualizer(it) },
            onDismiss = { showEqualizerSheet = false }
        )
    }

    if (showQueueSheet) {
        QueueSheet(
            queue = queue,
            currentIndex = currentQueueIndex,
            onSongClick = { index ->
                playerManager.playSongList(queue, index)
            },
            onRemoveSong = { index ->
                playerManager.removeFromQueue(index)
            },
            onClearQueue = {
                playerManager.clearQueue()
                showQueueSheet = false
            },
            onSaveAsPlaylist = {
                showCreatePlaylistDialog = true
            },
            onDismiss = { showQueueSheet = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            remainingSeconds = sleepTimerRemaining,
            onDismiss = { showSleepTimerDialog = false },
            onSetTimer = { minutes, afterSong ->
                playerManager.startSleepTimer(minutes, afterSong)
            },
            onCancelTimer = {
                playerManager.cancelSleepTimer()
            }
        )
    }

    songForAddToPlaylist?.let { song ->
        AddToPlaylistDialog(
            playlists = allPlaylists,
            onDismiss = { songForAddToPlaylist = null },
            onPlaylistSelected = { playlistId ->
                coroutineScope.launch {
                    repository.addSongToPlaylist(playlistId, song.id)
                }
                songForAddToPlaylist = null
            },
            onCreateNewPlaylistClick = {
                showCreatePlaylistDialog = true
            }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name, description, isDynamic, dynamicType, dynamicCriteria ->
                coroutineScope.launch {
                    val newPlaylistId = if (isDynamic && dynamicType != null) {
                        repository.createDynamicPlaylist(name, description, dynamicType, dynamicCriteria)
                    } else {
                        repository.createPlaylist(name, description)
                    }
                    songForAddToPlaylist?.let { song ->
                        if (!isDynamic) {
                            repository.addSongToPlaylist(newPlaylistId, song.id)
                        }
                        songForAddToPlaylist = null
                    }
                }
            }
        )
    }

    songForMetadataEdit?.let { song ->
        MetadataEditDialog(
            song = song,
            onDismiss = { songForMetadataEdit = null },
            onSave = { title, artist, album, albumArtist, genre, year, trackNumber ->
                coroutineScope.launch {
                    repository.updateSongMetadata(
                        songId = song.id,
                        title = title,
                        artist = artist,
                        album = album,
                        albumArtist = albumArtist,
                        genre = genre,
                        year = year,
                        trackNumber = trackNumber
                    )
                    playerManager.updateSongMetadata(
                        songId = song.id,
                        title = title,
                        artist = artist,
                        album = album,
                        albumArtist = albumArtist,
                        genre = genre,
                        year = year,
                        trackNumber = trackNumber
                    )
                }
                songForMetadataEdit = null
            }
        )
    }
}
