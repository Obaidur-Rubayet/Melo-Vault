package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun MeloVaultLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 40.dp,
    showText: Boolean = true,
    tagline: String? = "Your Device • Your Way"
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon matching outside launcher icon
        Box(
            modifier = Modifier
                .size(iconSize)
                .shadow(10.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF242830), Color(0xFF14161A)),
                        start = Offset(0f, 0f),
                        end = Offset(100f, 100f)
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))
                    ),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.melovault_launcher_icon_1787319411877),
                contentDescription = "MeloVault Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        }

        if (showText) {
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "MeloVault",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (tagline != null) {
                    Text(
                        text = tagline,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp
                        ),
                        color = primaryColor.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

