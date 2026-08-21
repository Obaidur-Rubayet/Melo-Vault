package com.example.data.database.dao

import androidx.room.*
import com.example.data.database.entity.PlaybackHistoryEntity
import com.example.data.database.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistoryEntity): Long

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playback_history h ON s.id = h.songId
        WHERE s.isAvailable = 1
        ORDER BY h.playedAt DESC
        LIMIT :limit
    """)
    fun getRecentPlaybackSongs(limit: Int = 50): Flow<List<SongEntity>>

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}
