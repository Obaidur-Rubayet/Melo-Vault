package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Song

@Composable
fun MetadataEditDialog(
    song: Song,
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String, album: String, albumArtist: String?, genre: String?, year: Int?, trackNumber: Int) -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var albumArtist by remember { mutableStateOf(song.albumArtist ?: "") }
    var album by remember { mutableStateOf(song.album) }
    var genre by remember { mutableStateOf(song.genre ?: "") }
    var yearStr by remember { mutableStateOf(song.year?.toString() ?: "") }
    var trackNumberStr by remember { mutableStateOf(song.trackNumber.toString()) }

    val commonGenres = listOf("Rock", "Pop", "Hip Hop", "Jazz", "Electronic", "Classical", "Metal", "R&B", "Indie", "Acoustic")

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                "Edit Track Metadata",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Update ID3 tags and local metadata for this song.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = albumArtist,
                    onValueChange = { albumArtist = it },
                    label = { Text("Album Artist (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Genre") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick genre chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonGenres.forEach { suggestion ->
                        FilterChip(
                            selected = genre.equals(suggestion, ignoreCase = true),
                            onClick = { genre = suggestion },
                            label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { yearStr = it },
                        label = { Text("Year") },
                        placeholder = { Text("2024") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = trackNumberStr,
                        onValueChange = { trackNumberStr = it },
                        label = { Text("Track #") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val year = yearStr.toIntOrNull()
                    val track = trackNumberStr.toIntOrNull() ?: 0
                    onSave(
                        title.trim().ifEmpty { "Unknown Title" },
                        artist.trim().ifEmpty { "Unknown Artist" },
                        album.trim().ifEmpty { "Unknown Album" },
                        albumArtist.trim().ifEmpty { null },
                        genre.trim().ifEmpty { null },
                        year,
                        track
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
