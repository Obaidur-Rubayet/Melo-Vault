package com.example.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.data.database.entity.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

data class ScanProgress(
    val songsFound: Int = 0,
    val albumsFound: Int = 0,
    val artistsFound: Int = 0,
    val internalSongs: Int = 0,
    val sdCardSongs: Int = 0,
    val currentSong: String = "",
    val isComplete: Boolean = false
)

class MediaStoreScanner(private val context: Context) {

    companion object {
        private const val TAG = "MediaStoreScanner"
        private val SUPPORTED_MIME_TYPES = setOf(
            "audio/mpeg", "audio/mp3", "audio/mp4", "audio/m4a",
            "audio/aac", "audio/flac", "audio/x-flac", "audio/wav",
            "audio/x-wav", "audio/ogg", "audio/opus", "audio/amr",
            "audio/3gpp", "audio/x-matroska"
        )
    }

    fun scanMusicFiles(): Flow<ScanProgress> = flow {
        emit(ScanProgress(0, 0, 0, 0, 0, "Initializing scan...", false))

        val songsList = mutableListOf<SongEntity>()
        val albumsSet = mutableSetOf<String>()
        val artistsSet = mutableSetOf<String>()
        var internalCount = 0
        var sdCardCount = 0

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.RELATIVE_PATH
            } else {
                MediaStore.Audio.Media.DATA
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                MediaStore.Audio.Media.GENRE
            } else {
                MediaStore.Audio.Media._ID // fallback
            }
        )

        // Select files that are music or duration > 5000ms
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("5000") // At least 5 seconds
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

                val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else {
                    cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                }

                val genreColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else {
                    -1
                }

                var count = 0
                while (cursor.moveToNext()) {
                    val mediaStoreId = cursor.getLong(idColumn)
                    val rawTitle = cursor.getString(titleColumn) ?: "Unknown Title"
                    val rawArtist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val rawAlbum = cursor.getString(albumColumn) ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdColumn)
                    val duration = cursor.getLong(durationColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)
                    val fileSize = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val trackRaw = cursor.getInt(trackColumn)

                    // Clean names
                    val title = if (rawTitle.isBlank() || rawTitle == "<unknown>") "Unknown Title" else rawTitle
                    val artist = if (rawArtist.isBlank() || rawArtist == "<unknown>") "Unknown Artist" else rawArtist
                    val album = if (rawAlbum.isBlank() || rawAlbum == "<unknown>") "Unknown Album" else rawAlbum

                    var rawGenre: String? = null
                    if (genreColumn != -1) {
                        try {
                            rawGenre = cursor.getString(genreColumn)
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    val genre = if (rawGenre.isNullOrBlank() || rawGenre == "<unknown>") null else rawGenre

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaStoreId
                    ).toString()

                    val albumArtUri = if (albumId > 0) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()
                    } else null

                    val rawPath = if (pathColumn != -1) cursor.getString(pathColumn) ?: "" else ""
                    val storageType = determineStorageType(rawPath)
                    if (storageType == "SD_CARD") {
                        sdCardCount++
                    } else {
                        internalCount++
                    }

                    // Extract clean and rich metadata
                    val cleanedMeta = MetadataHelper.extractRichMetadata(
                        context = context,
                        uri = Uri.parse(contentUri),
                        fallbackTitle = rawTitle,
                        fallbackArtist = rawArtist,
                        fallbackAlbum = rawAlbum
                    )

                    val trackNumber = if (trackRaw >= 1000) trackRaw % 1000 else if (trackRaw > 0) trackRaw else cleanedMeta.trackNumber
                    val discNumber = if (trackRaw >= 1000) trackRaw / 1000 else 1

                    val songEntity = SongEntity(
                        mediaStoreId = mediaStoreId,
                        contentUri = contentUri,
                        title = cleanedMeta.title,
                        artist = cleanedMeta.artist,
                        album = cleanedMeta.album,
                        albumArtist = cleanedMeta.albumArtist ?: cleanedMeta.artist,
                        genre = cleanedMeta.genre ?: genre,
                        year = cleanedMeta.year,
                        trackNumber = trackNumber,
                        discNumber = discNumber,
                        duration = duration,
                        mimeType = mimeType,
                        fileSize = fileSize,
                        dateAdded = dateAdded,
                        albumArtUri = albumArtUri,
                        playCount = 0,
                        isFavorite = false,
                        storageType = storageType,
                        folderPath = extractFolderName(rawPath),
                        dateLastPlayed = null,
                        isAvailable = true
                    )

                    songsList.add(songEntity)
                    albumsSet.add(cleanedMeta.album)
                    artistsSet.add(cleanedMeta.artist)
                    count++

                    if (count % 20 == 0 || count <= 10) {
                        emit(
                            ScanProgress(
                                songsFound = count,
                                albumsFound = albumsSet.size,
                                artistsFound = artistsSet.size,
                                internalSongs = internalCount,
                                sdCardSongs = sdCardCount,
                                currentSong = title,
                                isComplete = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning MediaStore", e)
        }

        emit(
            ScanProgress(
                songsFound = songsList.size,
                albumsFound = albumsSet.size,
                artistsFound = artistsSet.size,
                internalSongs = internalCount,
                sdCardSongs = sdCardCount,
                currentSong = "",
                isComplete = true
            )
        )
    }.flowOn(Dispatchers.IO)

    fun fetchDirectSongsList(): List<SongEntity> {
        val songsList = mutableListOf<SongEntity>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.RELATIVE_PATH
            } else {
                MediaStore.Audio.Media.DATA
            }
        )

        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("5000")
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

                val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else {
                    cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                }

                while (cursor.moveToNext()) {
                    val mediaStoreId = cursor.getLong(idColumn)
                    val rawTitle = cursor.getString(titleColumn) ?: "Unknown Title"
                    val rawArtist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val rawAlbum = cursor.getString(albumColumn) ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdColumn)
                    val duration = cursor.getLong(durationColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)
                    val fileSize = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val trackRaw = cursor.getInt(trackColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaStoreId
                    ).toString()

                    val albumArtUri = if (albumId > 0) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()
                    } else null

                    val rawPath = if (pathColumn != -1) cursor.getString(pathColumn) ?: "" else ""
                    val storageType = determineStorageType(rawPath)

                    val cleanedMeta = MetadataHelper.extractRichMetadata(
                        context = context,
                        uri = Uri.parse(contentUri),
                        fallbackTitle = rawTitle,
                        fallbackArtist = rawArtist,
                        fallbackAlbum = rawAlbum
                    )

                    val trackNumber = if (trackRaw >= 1000) trackRaw % 1000 else if (trackRaw > 0) trackRaw else cleanedMeta.trackNumber
                    val discNumber = if (trackRaw >= 1000) trackRaw / 1000 else 1

                    songsList.add(
                        SongEntity(
                            mediaStoreId = mediaStoreId,
                            contentUri = contentUri,
                            title = cleanedMeta.title,
                            artist = cleanedMeta.artist,
                            album = cleanedMeta.album,
                            albumArtist = cleanedMeta.albumArtist ?: cleanedMeta.artist,
                            genre = cleanedMeta.genre,
                            year = cleanedMeta.year,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            duration = duration,
                            mimeType = mimeType,
                            fileSize = fileSize,
                            dateAdded = dateAdded,
                            albumArtUri = albumArtUri,
                            playCount = 0,
                            isFavorite = false,
                            storageType = storageType,
                            folderPath = extractFolderName(rawPath),
                            dateLastPlayed = null,
                            isAvailable = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching direct songs list", e)
        }
        return songsList
    }

    private fun determineStorageType(path: String): String {
        if (path.isEmpty()) return "INTERNAL"
        val lower = path.lowercase()
        return if (lower.contains("emulated") || lower.startsWith("music/") || lower.startsWith("download/")) {
            "INTERNAL"
        } else if (lower.contains("sdcard") || lower.contains("/storage/") && !lower.contains("emulated")) {
            "SD_CARD"
        } else {
            "INTERNAL"
        }
    }

    private fun extractFolderName(path: String): String {
        if (path.isEmpty()) return "Music"
        val normalized = path.replace("\\", "/").trimEnd('/')
        val lastSlash = normalized.lastIndexOf('/')
        return if (lastSlash != -1 && lastSlash < normalized.length - 1) {
            normalized.substring(lastSlash + 1)
        } else if (normalized.isNotEmpty()) {
            normalized
        } else {
            "Music"
        }
    }
}
