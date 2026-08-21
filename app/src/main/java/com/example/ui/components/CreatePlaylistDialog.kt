package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class PlaylistType {
    STANDARD,
    DYNAMIC
}

enum class DynamicRuleType(
    val title: String,
    val icon: ImageVector,
    val defaultName: String,
    val description: String
) {
    RECENTLY_ADDED("Recently Added", Icons.Default.Schedule, "Recently Added", "Auto-includes your latest downloaded & scanned music tracks"),
    MOST_PLAYED("Most Played", Icons.Default.LocalFireDepartment, "Heavy Rotation", "Auto-includes your top most-played offline tracks"),
    GENRE("Genre Rule", Icons.Default.MusicNote, "Rock Collection", "Auto-includes all songs matching a specific musical genre"),
    ARTIST("Artist Rule", Icons.Default.Person, "Artist Spotlight", "Auto-includes all songs by a specific artist"),
    FAVORITES("All Favorites", Icons.Default.Favorite, "Liked Songs", "Auto-includes every song you tap the heart on"),
    DECADE_80S("80s Classics", Icons.Default.History, "80s Rewind", "Songs released between 1980 and 1989"),
    DECADE_90S("90s Hits", Icons.Default.History, "90s Anthems", "Songs released between 1990 and 1999"),
    DECADE_2000S("2000s Era", Icons.Default.History, "2000s Throwback", "Songs released between 2000 and 2009"),
    DECADE_2010S("2010s Hits", Icons.Default.History, "2010s Mix", "Songs released between 2010 and 2019"),
    DECADE_2020S("2020s Current", Icons.Default.History, "Modern Waves", "Songs released in 2020 or newer")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, isDynamic: Boolean, dynamicType: String?, dynamicCriteria: String?) -> Unit
) {
    var selectedType by remember { mutableStateOf(PlaylistType.STANDARD) }
    var selectedRule by remember { mutableStateOf(DynamicRuleType.RECENTLY_ADDED) }
    var criteriaInput by remember { mutableStateOf("Rock") }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Update default name when rule changes in dynamic mode
    LaunchedEffect(selectedRule, selectedType, criteriaInput) {
        if (selectedType == PlaylistType.DYNAMIC) {
            when (selectedRule) {
                DynamicRuleType.GENRE -> {
                    name = if (criteriaInput.isNotBlank()) "$criteriaInput Vibes" else "Genre Mix"
                    description = "Dynamic playlist for all $criteriaInput songs"
                }
                DynamicRuleType.ARTIST -> {
                    name = if (criteriaInput.isNotBlank()) "Best of $criteriaInput" else "Artist Spotlight"
                    description = "Auto-updating collection for $criteriaInput"
                }
                else -> {
                    name = selectedRule.defaultName
                    description = selectedRule.description
                }
            }
        }
    }

    val genreOptions = listOf("Rock", "Pop", "Jazz", "Electronic", "Hip Hop", "Classical", "Metal", "R&B", "Indie", "Acoustic", "Soundtrack")

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (selectedType == PlaylistType.DYNAMIC) Icons.Default.Bolt else Icons.Default.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = if (selectedType == PlaylistType.DYNAMIC) "New Smart Playlist" else "New Playlist",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type selector tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedType.ordinal,
                    containerColor = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedType == PlaylistType.STANDARD,
                        onClick = {
                            selectedType = PlaylistType.STANDARD
                            if (name.isEmpty() || name == selectedRule.defaultName) name = ""
                        },
                        text = { Text("Standard") },
                        icon = { Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedType == PlaylistType.DYNAMIC,
                        onClick = {
                            selectedType = PlaylistType.DYNAMIC
                            name = selectedRule.defaultName
                            description = selectedRule.description
                        },
                        text = { Text("⚡ Smart Dynamic") },
                        icon = { Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                if (selectedType == PlaylistType.DYNAMIC) {
                    // Smart Playlist explanation banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Smart playlists automatically populate and refresh in real-time as your music library changes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = "Select Auto-Population Rule:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    // Rule Selector Chips Grid / Row
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                DynamicRuleType.RECENTLY_ADDED,
                                DynamicRuleType.MOST_PLAYED,
                                DynamicRuleType.GENRE,
                                DynamicRuleType.ARTIST,
                                DynamicRuleType.FAVORITES
                            ).forEach { rule ->
                                FilterChip(
                                    selected = selectedRule == rule,
                                    onClick = {
                                        selectedRule = rule
                                        if (rule == DynamicRuleType.GENRE && criteriaInput.isBlank()) criteriaInput = "Rock"
                                        if (rule == DynamicRuleType.ARTIST && criteriaInput.isBlank()) criteriaInput = "Queen"
                                    },
                                    label = { Text(rule.title) },
                                    leadingIcon = {
                                        Icon(rule.icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // Era Decades Chips Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                DynamicRuleType.DECADE_80S,
                                DynamicRuleType.DECADE_90S,
                                DynamicRuleType.DECADE_2000S,
                                DynamicRuleType.DECADE_2010S,
                                DynamicRuleType.DECADE_2020S
                            ).forEach { rule ->
                                FilterChip(
                                    selected = selectedRule == rule,
                                    onClick = { selectedRule = rule },
                                    label = { Text(rule.title) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // Rule-specific criteria input
                    if (selectedRule == DynamicRuleType.GENRE) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = criteriaInput,
                                onValueChange = { criteriaInput = it },
                                label = { Text("Genre Name") },
                                placeholder = { Text("e.g. Rock, Metal, Electronic") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                genreOptions.forEach { g ->
                                    AssistChip(
                                        onClick = { criteriaInput = g },
                                        label = { Text(g, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    } else if (selectedRule == DynamicRuleType.ARTIST) {
                        OutlinedTextField(
                            value = criteriaInput,
                            onValueChange = { criteriaInput = it },
                            label = { Text("Artist Name") },
                            placeholder = { Text("e.g. The Beatles, Daft Punk") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Playlist Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name *") },
                    placeholder = { Text(if (selectedType == PlaylistType.DYNAMIC) "Smart Playlist Name" else "e.g. Chill Beats, Workout Mix") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Playlist Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = false,
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val isDynamic = selectedType == PlaylistType.DYNAMIC
                        val dynamicType = if (isDynamic) selectedRule.name else null
                        val dynamicCriteria = if (isDynamic && (selectedRule == DynamicRuleType.GENRE || selectedRule == DynamicRuleType.ARTIST)) {
                            criteriaInput.trim().ifEmpty { null }
                        } else null

                        onConfirm(
                            name.trim(),
                            description.trim(),
                            isDynamic,
                            dynamicType,
                            dynamicCriteria
                        )
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (selectedType == PlaylistType.DYNAMIC) "Create Smart Playlist" else "Create Playlist")
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
