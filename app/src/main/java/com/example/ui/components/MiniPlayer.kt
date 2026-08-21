package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    progressMs: Long,
    durationMs: Long,
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = song != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        if (song == null) return@AnimatedVisibility

        val progressFraction = if (durationMs > 0) {
            (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .testTag("mini_player"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1F2226),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column {
                // Linear Progress bar on top edge of mini player
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.08f),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArtImage(
                        artUri = song.albumArtUri,
                        title = song.displayTitle,
                        artist = song.displayArtist,
                        size = 46.dp,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                    ) {
                        Text(
                            text = song.displayTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = song.displayArtist,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF9BA1A6),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                    }

                    // Favorite Button
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("mini_player_favorite")
                    ) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (song.isFavorite) "Liked" else "Like",
                            tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else Color(0xFF9BA1A6),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play/Pause Button
                    FilledIconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("mini_player_play_pause"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Skip Next Button
                    IconButton(
                        onClick = onSkipNextClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("mini_player_skip_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next song",
                            tint = Color(0xFFE3E2E6),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
