package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import kotlin.math.abs

@Composable
fun AlbumArtImage(
    artUri: String?,
    title: String,
    artist: String = "",
    modifier: Modifier = Modifier,
    size: Dp? = null,
    shape: Shape = RoundedCornerShape(8.dp),
    iconSize: Dp = 24.dp
) {
    val boxModifier = modifier
        .then(if (size != null) Modifier.size(size) else Modifier)
        .clip(shape)

    val gradientBrush = remember(title, artist) {
        generatePlaceholderGradient(title + artist)
    }

    if (!artUri.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = artUri,
            contentDescription = "$title Album Artwork",
            contentScale = ContentScale.Crop,
            modifier = boxModifier,
            loading = {
                FallbackArtBox(gradientBrush, iconSize)
            },
            error = {
                FallbackArtBox(gradientBrush, iconSize)
            }
        )
    } else {
        Box(
            modifier = boxModifier.background(gradientBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun FallbackArtBox(brush: Brush, iconSize: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(iconSize)
        )
    }
}

fun generatePlaceholderGradient(seed: String): Brush {
    val colorPairs = listOf(
        Pair(Color(0xFF1E3C72), Color(0xFF2A5298)),
        Pair(Color(0xFF8A2387), Color(0xFFE94057)),
        Pair(Color(0xFF0F2027), Color(0xFF203A43)),
        Pair(Color(0xFF2C3E50), Color(0xFF3498DB)),
        Pair(Color(0xFF4A00E0), Color(0xFF8E2DE2)),
        Pair(Color(0xFF11998E), Color(0xFF38EF7D)),
        Pair(Color(0xFFFC466B), Color(0xFF3F5EFB)),
        Pair(Color(0xFF3A1C71), Color(0xFFD76D77)),
        Pair(Color(0xFF134E5E), Color(0xFF71B280)),
        Pair(Color(0xFF000428), Color(0xFF004E92))
    )
    val hash = abs(seed.hashCode())
    val pair = colorPairs[hash % colorPairs.size]
    return Brush.linearGradient(listOf(pair.first, pair.second))
}
