package com.example.data.database.dao

import androidx.room.*
import com.example.data.database.entity.PlaylistEntity
import com.example.data.database.entity.PlaylistSongCrossRef
import com.example.data.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    fun getPlaylistById(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistByIdDirect(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongCrossRefs(crossRefs: List<PlaylistSongCrossRef>)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_song_cross_ref r ON s.id = r.songId
        WHERE r.playlistId = :playlistId AND s.isAvailable = 1
        ORDER BY r.orderIndex ASC, r.addedAt ASC
    """)
    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    fun getPlaylistSongCount(playlistId: Long): Flow<Int>

    @Query("UPDATE playlists SET songCount = (SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlistId = :playlistId) WHERE id = :playlistId")
    suspend fun updatePlaylistCount(playlistId: Long)
}
