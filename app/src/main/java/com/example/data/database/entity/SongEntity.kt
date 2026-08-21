package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["mediaStoreId"], unique = true),
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["genre"]),
        Index(value = ["isFavorite"]),
        Index(value = ["playCount"]),
        Index(value = ["dateAdded"]),
        Index(value = ["dateLastPlayed"])
    ]
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaStoreId: Long,
    val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val duration: Long = 0L, // in milliseconds
    val mimeType: String? = null,
    val fileSize: Long = 0L,
    val dateAdded: Long = 0L,
    val albumArtUri: String? = null,
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val storageType: String = "INTERNAL", // "INTERNAL" or "SD_CARD"
    val folderPath: String = "",
    val dateLastPlayed: Long? = null,
    val isAvailable: Boolean = true
)
