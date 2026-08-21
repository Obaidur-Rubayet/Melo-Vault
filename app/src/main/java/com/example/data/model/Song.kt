package com.example.data.model

import com.example.data.database.entity.SongEntity

data class Song(
    val id: Long,
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
    val duration: Long = 0L,
    val mimeType: String? = null,
    val fileSize: Long = 0L,
    val dateAdded: Long = 0L,
    val albumArtUri: String? = null,
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val storageType: String = "INTERNAL",
    val folderPath: String = "",
    val dateLastPlayed: Long? = null,
    val isAvailable: Boolean = true
) {
    val displayTitle: String
        get() = if (title.contains('_') || title.contains('+') || title.endsWith(".mp3", ignoreCase = true)) {
            title.replace('_', ' ').replace('+', ' ')
                .replace(Regex("""\.(mp3|m4a|flac|wav|aac|ogg|opus)$""", RegexOption.IGNORE_CASE), "")
                .trim()
        } else {
            title
        }

    val displayArtist: String
        get() = if (artist.contains('_') || artist.contains('+')) {
            artist.replace('_', ' ').replace('+', ' ').trim()
        } else {
            artist
        }
}

fun SongEntity.toDomainModel(): Song {
    return Song(
        id = id,
        mediaStoreId = mediaStoreId,
        contentUri = contentUri,
        title = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        genre = genre,
        year = year,
        trackNumber = trackNumber,
        discNumber = discNumber,
        duration = duration,
        mimeType = mimeType,
        fileSize = fileSize,
        dateAdded = dateAdded,
        albumArtUri = albumArtUri,
        playCount = playCount,
        isFavorite = isFavorite,
        storageType = storageType,
        folderPath = folderPath,
        dateLastPlayed = dateLastPlayed,
        isAvailable = isAvailable
    )
}

fun Song.toEntity(): SongEntity {
    return SongEntity(
        id = id,
        mediaStoreId = mediaStoreId,
        contentUri = contentUri,
        title = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        genre = genre,
        year = year,
        trackNumber = trackNumber,
        discNumber = discNumber,
        duration = duration,
        mimeType = mimeType,
        fileSize = fileSize,
        dateAdded = dateAdded,
        albumArtUri = albumArtUri,
        playCount = playCount,
        isFavorite = isFavorite,
        storageType = storageType,
        folderPath = folderPath,
        dateLastPlayed = dateLastPlayed,
        isAvailable = isAvailable
    )
}
