package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.MeloVaultDatabase
import com.example.data.database.dao.AlbumSummary
import com.example.data.database.dao.ArtistSummary
import com.example.data.database.dao.GenreSummary
import com.example.data.database.dao.StorageStats
import com.example.data.database.entity.CustomFolderEntity
import com.example.data.database.entity.PlaybackHistoryEntity
import com.example.data.database.entity.PlaylistEntity
import com.example.data.database.entity.PlaylistSongCrossRef
import com.example.data.database.entity.SongEntity
import com.example.data.mediastore.MediaStoreScanner
import com.example.data.mediastore.ScanProgress
import com.example.data.model.SongSortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val database: MeloVaultDatabase = MeloVaultDatabase.getDatabase(context)
) {
    private val songDao = database.songDao()
    private val playlistDao = database.playlistDao()
    private val customFolderDao = database.customFolderDao()
    private val playbackHistoryDao = database.playbackHistoryDao()
    private val mediaStoreScanner = MediaStoreScanner(context)

    companion object {
        private const val TAG = "MusicRepository"
    }

    // Songs
    fun getSongs(sortOption: SongSortOption): Flow<List<SongEntity>> {
        return when (sortOption) {
            SongSortOption.TITLE_ASC -> songDao.getAllSongsSortedByTitleAsc()
            SongSortOption.TITLE_DESC -> songDao.getAllSongsSortedByTitleDesc()
            SongSortOption.ARTIST -> songDao.getAllSongsSortedByArtist()
            SongSortOption.ALBUM -> songDao.getAllSongsSortedByAlbum()
            SongSortOption.DATE_ADDED -> songDao.getAllSongsSortedByDateAdded()
            SongSortOption.MOST_PLAYED -> songDao.getAllSongsSortedByPlayCount()
            SongSortOption.DURATION -> songDao.getAllSongsSortedByDuration()
        }
    }

    fun getSongById(id: Long): Flow<SongEntity?> = songDao.getSongByIdFlow(id)

    suspend fun getSongByIdDirect(id: Long): SongEntity? = withContext(Dispatchers.IO) {
        songDao.getSongById(id)
    }

    fun getFavoriteSongs(): Flow<List<SongEntity>> = songDao.getFavoriteSongs()

    fun getRecentlyPlayedSongs(limit: Int = 30): Flow<List<SongEntity>> =
        songDao.getRecentlyPlayedSongs(limit)

    fun getRecentlyAddedSongs(limit: Int = 30): Flow<List<SongEntity>> =
        songDao.getRecentlyAddedSongs(limit)

    fun getMostPlayedSongs(limit: Int = 30): Flow<List<SongEntity>> =
        songDao.getMostPlayedSongs(limit)

    fun getSongsByAlbum(album: String): Flow<List<SongEntity>> =
        songDao.getSongsByAlbum(album)

    fun getSongsByArtist(artist: String): Flow<List<SongEntity>> =
        songDao.getSongsByArtist(artist)

    fun getSongsByGenre(genre: String): Flow<List<SongEntity>> =
        songDao.getSongsByGenre(genre)

    fun getSongsByFolder(folderPath: String): Flow<List<SongEntity>> =
        songDao.getSongsByFolder(folderPath)

    fun searchSongs(query: String): Flow<List<SongEntity>> =
        songDao.searchSongs(query)

    // Summaries
    fun getAllAlbums(): Flow<List<AlbumSummary>> = songDao.getAllAlbums()

    fun getAllArtists(): Flow<List<ArtistSummary>> = songDao.getAllArtists()

    fun getAllGenres(): Flow<List<GenreSummary>> = songDao.getAllGenres()

    fun getSongCount(): Flow<Int> = songDao.getSongCount()

    fun getStorageStats(): Flow<StorageStats> = songDao.getStorageStats()

    // Playlists
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    fun getPlaylistById(id: Long): Flow<PlaylistEntity?> = playlistDao.getPlaylistById(id)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>> =
        playlistDao.getPlaylistById(playlistId).flatMapLatest { playlist ->
            if (playlist == null) {
                flowOf(emptyList())
            } else if (playlist.isDynamic) {
                when (playlist.dynamicType) {
                    "RECENTLY_ADDED" -> songDao.getRecentlyAddedDynamic(100)
                    "MOST_PLAYED" -> songDao.getMostPlayedDynamic(100)
                    "GENRE" -> songDao.getSongsByGenre(playlist.dynamicCriteria ?: "")
                    "ARTIST" -> songDao.getSongsByArtist(playlist.dynamicCriteria ?: "")
                    "FAVORITES" -> songDao.getFavoriteSongs()
                    "DECADE_80S" -> songDao.getSongsByYearRange(1980, 1989)
                    "DECADE_90S" -> songDao.getSongsByYearRange(1990, 1999)
                    "DECADE_2000S" -> songDao.getSongsByYearRange(2000, 2009)
                    "DECADE_2010S" -> songDao.getSongsByYearRange(2010, 2019)
                    "DECADE_2020S" -> songDao.getSongsByYearRange(2020, 2099)
                    else -> songDao.getRecentlyAddedDynamic(100)
                }
            } else {
                playlistDao.getSongsForPlaylist(playlistId)
            }
        }

    suspend fun createPlaylist(name: String, description: String = ""): Long =
        withContext(Dispatchers.IO) {
            val playlist = PlaylistEntity(name = name, description = description, isDynamic = false)
            playlistDao.insertPlaylist(playlist)
        }

    suspend fun createDynamicPlaylist(
        name: String,
        description: String = "",
        dynamicType: String,
        dynamicCriteria: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val playlist = PlaylistEntity(
            name = name,
            description = description,
            isDynamic = true,
            dynamicType = dynamicType,
            dynamicCriteria = dynamicCriteria
        )
        playlistDao.insertPlaylist(playlist)
    }

    suspend fun renamePlaylist(id: Long, newName: String) = withContext(Dispatchers.IO) {
        val existing = playlistDao.getPlaylistByIdDirect(id)
        if (existing != null) {
            playlistDao.updatePlaylist(existing.copy(name = newName))
        }
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistById(id)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylistSongCrossRef(
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId)
        )
        playlistDao.updatePlaylistCount(playlistId)
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) =
        withContext(Dispatchers.IO) {
            val crossRefs = songIds.mapIndexed { index, songId ->
                PlaylistSongCrossRef(playlistId = playlistId, songId = songId, orderIndex = index)
            }
            playlistDao.insertPlaylistSongCrossRefs(crossRefs)
            playlistDao.updatePlaylistCount(playlistId)
        }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) =
        withContext(Dispatchers.IO) {
            playlistDao.removeSongFromPlaylist(playlistId, songId)
            playlistDao.updatePlaylistCount(playlistId)
        }

    // Folders
    fun getCustomFolders(): Flow<List<CustomFolderEntity>> = customFolderDao.getAllFolders()

    suspend fun addCustomFolder(uriString: String, displayName: String) =
        withContext(Dispatchers.IO) {
            customFolderDao.insertFolder(
                CustomFolderEntity(uriString = uriString, displayName = displayName)
            )
        }

    suspend fun deleteCustomFolder(id: Long) = withContext(Dispatchers.IO) {
        customFolderDao.deleteFolderById(id)
    }

    // Song actions
    suspend fun toggleFavorite(songId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        songDao.updateFavorite(songId, isFavorite)
    }

    suspend fun recordSongPlay(songId: Long, durationPlayed: Long = 0L) =
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            songDao.incrementPlayCount(songId, timestamp)
            playbackHistoryDao.insertHistory(
                PlaybackHistoryEntity(
                    songId = songId,
                    playedAt = timestamp,
                    durationPlayed = durationPlayed
                )
            )
        }

    suspend fun updateSongMetadata(
        songId: Long,
        title: String,
        artist: String,
        album: String,
        albumArtist: String? = null,
        genre: String?,
        year: Int?,
        trackNumber: Int
    ) = withContext(Dispatchers.IO) {
        val existing = songDao.getSongById(songId)
        if (existing != null) {
            songDao.updateSong(
                existing.copy(
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = albumArtist,
                    genre = genre,
                    year = year,
                    trackNumber = trackNumber
                )
            )
        }
    }

    // Scanning & Synchronization
    fun scanAndSyncMusicProgress(): Flow<ScanProgress> = flow {
        emit(ScanProgress(0, 0, 0, 0, 0, "Starting discovery...", false))

        val scannedSongs = mutableListOf<SongEntity>()
        mediaStoreScanner.scanMusicFiles().collect { progress ->
            emit(progress)
            if (progress.isComplete) {
                scannedSongs.addAll(mediaStoreScanner.fetchDirectSongsList())
            }
        }

        // Sync with DB
        withContext(Dispatchers.IO) {
            val existingIds = songDao.getAllMediaStoreIds().toSet()
            val newMediaStoreIds = scannedSongs.map { it.mediaStoreId }.toSet()

            // Insert new songs, preserve existing favorite and playCount status
            for (scanned in scannedSongs) {
                val existing = songDao.getSongByMediaStoreId(scanned.mediaStoreId)
                if (existing == null) {
                    songDao.insertSong(scanned)
                } else {
                    // Update metadata if changed while preserving user state
                    songDao.updateSong(
                        scanned.copy(
                            id = existing.id,
                            isFavorite = existing.isFavorite,
                            playCount = existing.playCount,
                            dateLastPlayed = existing.dateLastPlayed
                        )
                    )
                }
            }

            // Mark missing as unavailable (do not delete to preserve user playlists & history)
            for (oldId in existingIds) {
                if (!newMediaStoreIds.contains(oldId)) {
                    songDao.updateAvailability(oldId, isAvailable = false)
                }
            }
        }

        emit(
            ScanProgress(
                songsFound = scannedSongs.size,
                isComplete = true
            )
        )
    }.flowOn(Dispatchers.IO)

    suspend fun directScanAndSync(): Int = withContext(Dispatchers.IO) {
        try {
            val scannedSongs = mediaStoreScanner.fetchDirectSongsList()
            for (scanned in scannedSongs) {
                val existing = songDao.getSongByMediaStoreId(scanned.mediaStoreId)
                if (existing == null) {
                    songDao.insertSong(scanned)
                } else {
                    songDao.updateSong(
                        scanned.copy(
                            id = existing.id,
                            isFavorite = existing.isFavorite,
                            playCount = existing.playCount,
                            dateLastPlayed = existing.dateLastPlayed,
                            isAvailable = true
                        )
                    )
                }
            }
            scannedSongs.size
        } catch (e: Exception) {
            Log.e(TAG, "Error in directScanAndSync", e)
            0
        }
    }

    suspend fun cleanAllSongsMetadata(): Int = withContext(Dispatchers.IO) {
        try {
            val allSongs = songDao.getAllSongsDirect()
            var cleanedCount = 0
            for (song in allSongs) {
                val needsCleaning = song.title.contains('_') ||
                        song.title.contains('+') ||
                        song.title.endsWith(".mp3", ignoreCase = true) ||
                        song.title.endsWith(".m4a", ignoreCase = true) ||
                        song.artist.contains('_') ||
                        song.artist.equals("Unknown Artist", ignoreCase = true) ||
                        song.artist.equals("<unknown>", ignoreCase = true)

                if (needsCleaning) {
                    val meta = com.example.data.mediastore.MetadataHelper.extractRichMetadata(
                        context = context,
                        uri = android.net.Uri.parse(song.contentUri),
                        fallbackTitle = song.title,
                        fallbackArtist = song.artist,
                        fallbackAlbum = song.album
                    )

                    songDao.updateSong(
                        song.copy(
                            title = meta.title,
                            artist = meta.artist,
                            album = meta.album,
                            albumArtist = meta.albumArtist ?: meta.artist,
                            genre = meta.genre ?: song.genre,
                            year = meta.year ?: song.year,
                            trackNumber = if (song.trackNumber > 0) song.trackNumber else meta.trackNumber
                        )
                    )
                    cleanedCount++
                }
            }
            cleanedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning songs metadata", e)
            0
        }
    }
}
