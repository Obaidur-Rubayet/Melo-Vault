package com.example.data.mediastore

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log

data class CleanedMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String?,
    val genre: String?,
    val year: Int?,
    val trackNumber: Int
)

object MetadataHelper {
    private const val TAG = "MetadataHelper"

    private val AUDIO_EXTENSIONS = listOf(
        ".mp3", ".m4a", ".flac", ".wav", ".aac", ".ogg", ".opus",
        ".amr", ".3gp", ".wma", ".alac", ".aiff", ".dsd", ".mka"
    )

    private val JUNK_PATTERNS = listOf(
        Regex("""\[(128|192|256|320)\s*kbps\]""", RegexOption.IGNORE_CASE),
        Regex("""\((128|192|256|320)\s*kbps\)""", RegexOption.IGNORE_CASE),
        Regex("""\[(official\s*(video|audio|lyrics|music\s*video|hd|hq|4k)?)\]""", RegexOption.IGNORE_CASE),
        Regex("""\((official\s*(video|audio|lyrics|music\s*video|hd|hq|4k)?)\)""", RegexOption.IGNORE_CASE),
        Regex("""\[lyrics?\]""", RegexOption.IGNORE_CASE),
        Regex("""\(lyrics?\)""", RegexOption.IGNORE_CASE),
        Regex("""\[full\s*(song|audio|video)\]""", RegexOption.IGNORE_CASE),
        Regex("""\(full\s*(song|audio|video)\)""", RegexOption.IGNORE_CASE),
        Regex("""\b(320kbps|128kbps|256kbps|48khz|flac|lossless|hq|hd)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(pagalworld|mr-jatt|djmaza|naasongs|djpunjab|songsmp3|jiosaavn|gaana|wynk)\b.*""", RegexOption.IGNORE_CASE),
        Regex("""_128k|_320k|_64k""", RegexOption.IGNORE_CASE)
    )

    /**
     * Cleans raw filenames or titles with underscores, pluses, extensions, and common web junk.
     */
    fun cleanText(raw: String): String {
        if (raw.isBlank()) return "Unknown"
        var text = raw.trim()

        // Strip file extensions
        for (ext in AUDIO_EXTENSIONS) {
            if (text.endsWith(ext, ignoreCase = true)) {
                text = text.substring(0, text.length - ext.length).trim()
            }
        }

        // Replace URL encodings
        text = text.replace("%20", " ")

        // Replace underscores and plus signs with spaces
        text = text.replace('_', ' ').replace('+', ' ')

        // Strip junk tag patterns
        for (pattern in JUNK_PATTERNS) {
            text = text.replace(pattern, " ")
        }

        // Clean track number prefixes like "01. ", "01 - ", "01 ", "1. "
        text = text.replace(Regex("""^\d{1,3}\s*[\.\-_]\s*"""), "")

        // Collapse multiple spaces
        text = text.replace(Regex("""\s+"""), " ").trim()

        return text.ifEmpty { raw.trim() }
    }

    /**
     * Inspects title and artist, splitting "Artist - Title" if artist is missing or unknown.
     */
    fun resolveTitleAndArtist(rawTitle: String, rawArtist: String?): Pair<String, String> {
        val cleanedTitle = cleanText(rawTitle)
        val isArtistUnknown = rawArtist.isNullOrBlank() ||
                rawArtist.equals("<unknown>", ignoreCase = true) ||
                rawArtist.equals("Unknown Artist", ignoreCase = true) ||
                rawArtist.equals("Unknown", ignoreCase = true)

        if (isArtistUnknown) {
            // Check if title has "Artist - Title" or "Title - Artist" format
            if (cleanedTitle.contains(" - ")) {
                val parts = cleanedTitle.split(" - ", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    return Pair(parts[1].trim(), parts[0].trim()) // Title, Artist
                }
            } else if (cleanedTitle.contains(" – ")) { // en-dash
                val parts = cleanedTitle.split(" – ", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    return Pair(parts[1].trim(), parts[0].trim())
                }
            }
            return Pair(cleanedTitle, "Unknown Artist")
        }

        val cleanedArtist = cleanText(rawArtist ?: "Unknown Artist")
        return Pair(cleanedTitle, cleanedArtist)
    }

    /**
     * Extracts rich metadata using MediaMetadataRetriever directly from the file descriptor or URI.
     */
    fun extractRichMetadata(
        context: Context,
        uri: Uri,
        fallbackTitle: String,
        fallbackArtist: String,
        fallbackAlbum: String
    ): CleanedMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)

            val tagTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val tagArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val tagAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val tagAlbumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            val tagGenre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            val tagDate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
            val tagTrack = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)

            val (finalTitle, finalArtist) = if (!tagTitle.isNullOrBlank() && !tagArtist.isNullOrBlank()) {
                Pair(cleanText(tagTitle), cleanText(tagArtist))
            } else if (!tagTitle.isNullOrBlank()) {
                resolveTitleAndArtist(tagTitle, fallbackArtist)
            } else {
                resolveTitleAndArtist(fallbackTitle, tagArtist ?: fallbackArtist)
            }

            val finalAlbum = if (!tagAlbum.isNullOrBlank() && !tagAlbum.equals("<unknown>", ignoreCase = true)) {
                cleanText(tagAlbum)
            } else {
                cleanText(fallbackAlbum)
            }

            val finalGenre = if (!tagGenre.isNullOrBlank() && !tagGenre.equals("<unknown>", ignoreCase = true)) {
                cleanText(tagGenre)
            } else null

            val parsedYear = tagDate?.filter { it.isDigit() }?.take(4)?.toIntOrNull()

            val parsedTrack = tagTrack?.substringBefore('/')?.filter { it.isDigit() }?.toIntOrNull() ?: 0

            CleanedMetadata(
                title = finalTitle.ifEmpty { "Unknown Title" },
                artist = finalArtist.ifEmpty { "Unknown Artist" },
                album = finalAlbum.ifEmpty { "Unknown Album" },
                albumArtist = tagAlbumArtist?.let { cleanText(it) } ?: finalArtist,
                genre = finalGenre,
                year = parsedYear,
                trackNumber = parsedTrack
            )
        } catch (e: Exception) {
            val (resolvedTitle, resolvedArtist) = resolveTitleAndArtist(fallbackTitle, fallbackArtist)
            CleanedMetadata(
                title = resolvedTitle,
                artist = resolvedArtist,
                album = cleanText(fallbackAlbum),
                albumArtist = resolvedArtist,
                genre = null,
                year = null,
                trackNumber = 0
            )
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
