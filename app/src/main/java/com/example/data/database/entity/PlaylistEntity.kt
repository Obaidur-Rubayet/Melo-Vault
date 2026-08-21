package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0,
    val coverArtUri: String? = null,
    val isDynamic: Boolean = false,
    val dynamicType: String? = null, // e.g. "RECENTLY_ADDED", "MOST_PLAYED", "GENRE", "ARTIST", "DECADE", "FAVORITES"
    val dynamicCriteria: String? = null // e.g. "Rock", "Queen", "2020s"
)
