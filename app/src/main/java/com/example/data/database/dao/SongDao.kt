package com.example.data.database.dao

import androidx.room.*
import com.example.data.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

data class AlbumSummary(
    val album: String,
    val artist: String,
    val albumArtUri: String?,
    val songCount: Int,
    val totalDuration: Long,
    val year: Int?
)

data class ArtistSummary(
    val artist: String,
    val songCount: Int,
    val albumCount: Int,
    val albumArtUri: String?
)

data class GenreSummary(
    val genre: String,
    val songCount: Int,
    val albumArtUri: String?
)

data class StorageStats(
    val totalSongs: Int,
    val totalAlbums: Int,
    val totalArtists: Int,
    val totalDuration: Long,
    val internalSongs: Int,
    val sdCardSongs: Int,
    val totalFileSize: Long
)

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE isAvailable = 1")
    suspend fun getAllSongsDirect(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE isAvailable = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getAllSongsSortedByTitleAsc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isAvailable = 1 ORDER BY title COLLATE NOCASE DESC")
    fun getAllSongsSortedByTitleDesc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isAvailable = 1 ORDER BY artist COLLATE NOCASE ASC, title COLLATE NOCASE ASC")
    fun getAllSongsSortedByArtist(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isAvailable = 1 ORDER BY album COLLATE NOCASE ASC, trackNumber ASC, title COLLATE NOCASE ASC")
    fun getAllSongsSortedByAlbum(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isAvailable = 1 ORDER BY dateAdded DESC")
    fun getAllSongsSortedByDateAdded(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isAvailable = 1 ORDER BY playCount DESC, title COLLATE NOCASE ASC")
    fun getAllSongsSortedByPlayCount(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isAvailable = 1 ORDER BY duration DESC")
    fun getAllSongsSortedByDuration(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    fun getSongByIdFlow(id: Long): Flow<SongEntity?>

    @Query("SELECT * FROM songs WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    suspend fun getSongByMediaStoreId(mediaStoreId: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE isFavorite = 1 AND isAvailable = 1 ORDER BY dateAdded DESC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE dateLastPlayed IS NOT NULL AND isAvailable = 1 ORDER BY dateLastPlayed DESC LIMIT :limit")
    fun getRecentlyPlayedSongs(limit: Int = 30): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isAvailable = 1 ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentlyAddedSongs(limit: Int = 30): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE playCount > 0 AND isAvailable = 1 ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayedSongs(limit: Int = 30): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE album = :album AND isAvailable = 1 ORDER BY trackNumber ASC, title COLLATE NOCASE ASC")
    fun getSongsByAlbum(album: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artist = :artist AND isAvailable = 1 ORDER BY album COLLATE NOCASE ASC, trackNumber ASC, title COLLATE NOCASE ASC")
    fun getSongsByArtist(artist: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE genre LIKE '%' || :genre || '%' AND isAvailable = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getSongsByGenre(genre: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE year >= :minYear AND year <= :maxYear AND isAvailable = 1 ORDER BY year DESC, title COLLATE NOCASE ASC")
    fun getSongsByYearRange(minYear: Int, maxYear: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isAvailable = 1 ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentlyAddedDynamic(limit: Int = 100): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE playCount > 0 AND isAvailable = 1 ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayedDynamic(limit: Int = 100): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE folderPath = :folderPath AND isAvailable = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getSongsByFolder(folderPath: String): Flow<List<SongEntity>>

    @Query("""
        SELECT * FROM songs 
        WHERE isAvailable = 1 AND (
            title LIKE '%' || :query || '%' OR 
            artist LIKE '%' || :query || '%' OR 
            album LIKE '%' || :query || '%' OR 
            genre LIKE '%' || :query || '%'
        )
        ORDER BY title COLLATE NOCASE ASC
    """)
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("""
        SELECT album, artist, albumArtUri, COUNT(id) as songCount, SUM(duration) as totalDuration, MAX(year) as year 
        FROM songs 
        WHERE isAvailable = 1 AND album != '' 
        GROUP BY album, artist 
        ORDER BY album COLLATE NOCASE ASC
    """)
    fun getAllAlbums(): Flow<List<AlbumSummary>>

    @Query("""
        SELECT artist, COUNT(id) as songCount, COUNT(DISTINCT album) as albumCount, MAX(albumArtUri) as albumArtUri 
        FROM songs 
        WHERE isAvailable = 1 AND artist != '' 
        GROUP BY artist 
        ORDER BY artist COLLATE NOCASE ASC
    """)
    fun getAllArtists(): Flow<List<ArtistSummary>>

    @Query("""
        SELECT genre, COUNT(id) as songCount, MAX(albumArtUri) as albumArtUri 
        FROM songs 
        WHERE isAvailable = 1 AND genre IS NOT NULL AND genre != '' 
        GROUP BY genre 
        ORDER BY songCount DESC, genre COLLATE NOCASE ASC
    """)
    fun getAllGenres(): Flow<List<GenreSummary>>

    @Query("SELECT COUNT(*) FROM songs WHERE isAvailable = 1")
    fun getSongCount(): Flow<Int>

    @Query("""
        SELECT 
            COUNT(*) as totalSongs,
            COUNT(DISTINCT album) as totalAlbums,
            COUNT(DISTINCT artist) as totalArtists,
            COALESCE(SUM(duration), 0) as totalDuration,
            COALESCE(SUM(CASE WHEN storageType = 'INTERNAL' THEN 1 ELSE 0 END), 0) as internalSongs,
            COALESCE(SUM(CASE WHEN storageType = 'SD_CARD' THEN 1 ELSE 0 END), 0) as sdCardSongs,
            COALESCE(SUM(fileSize), 0) as totalFileSize
        FROM songs 
        WHERE isAvailable = 1
    """)
    fun getStorageStats(): Flow<StorageStats>

    @Query("SELECT mediaStoreId FROM songs")
    suspend fun getAllMediaStoreIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun updateFavorite(songId: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1, dateLastPlayed = :timestamp WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE songs SET isAvailable = :isAvailable WHERE mediaStoreId = :mediaStoreId")
    suspend fun updateAvailability(mediaStoreId: Long, isAvailable: Boolean)

    @Query("DELETE FROM songs WHERE mediaStoreId NOT IN (:activeMediaStoreIds)")
    suspend fun deleteSongsNotIn(activeMediaStoreIds: List<Long>)

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSongById(songId: Long)
}
