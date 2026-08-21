package com.example.data.model

data class Album(
    val title: String,
    val artist: String,
    val albumArtUri: String?,
    val songCount: Int,
    val totalDuration: Long,
    val year: Int?
)

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val albumArtUri: String?
)

data class Genre(
    val name: String,
    val songCount: Int,
    val albumArtUri: String?
)

data class Playlist(
    val id: Long,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0,
    val coverArtUri: String? = null,
    val isDynamic: Boolean = false,
    val dynamicType: String? = null,
    val dynamicCriteria: String? = null
)

fun com.example.data.database.entity.PlaylistEntity.toDomainModel(): Playlist = Playlist(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt,
    songCount = songCount,
    coverArtUri = coverArtUri,
    isDynamic = isDynamic,
    dynamicType = dynamicType,
    dynamicCriteria = dynamicCriteria
)

enum class SongSortOption(val displayName: String) {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DATE_ADDED("Recently Added"),
    MOST_PLAYED("Most Played"),
    DURATION("Duration")
}
