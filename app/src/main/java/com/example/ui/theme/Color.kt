package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Dark Palette Foundation (Immersive UI Theme)
val BackgroundDark = Color(0xFF0D0D0F)
val SurfaceDark = Color(0xFF1A1C1E)
val SurfaceVariantDark = Color(0xFF2D3135)
val CardBackgroundDark = Color(0xFF1A1C1E)
val CardBorderDark = Color(0x14FFFFFF)
val SurfaceElevatedDark = Color(0xFF1F2226)

// Text Colors (Dark)
val TextPrimaryDark = Color(0xFFE3E2E6)
val TextSecondaryDark = Color(0xFF9BA1A6)
val TextTertiaryDark = Color(0xFF6C737A)

// Light Palette Foundation
val BackgroundLight = Color(0xFFF4F6F8)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE8ECEF)
val CardBackgroundLight = Color(0xFFFFFFFF)
val CardBorderLight = Color(0xFFE0E4E8)

// Text Colors (Light)
val TextPrimaryLight = Color(0xFF1A1C1E)
val TextSecondaryLight = Color(0xFF5C636A)
val TextTertiaryLight = Color(0xFF8C939A)

// MeloVault Accent Color Options (Immersive UI default: Mint Emerald)
enum class AccentPalette(
    val id: String,
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
) {
    EMERALD_GROOVE(
        "emerald",
        "Emerald Glow",
        Color(0xFF81C784),
        Color(0xFF4DB6AC),
        Color(0xFFA5D6A7)
    ),
    CYAN_ELECTRIC(
        "cyan",
        "Electric Cyan",
        Color(0xFF00E5FF),
        Color(0xFF00B0FF),
        Color(0xFF4DB6AC)
    ),
    VIOLET_NEON(
        "violet",
        "Neon Violet",
        Color(0xFFB388FF),
        Color(0xFF7C4DFF),
        Color(0xFF81C784)
    ),
    ROSE_PULSE(
        "rose",
        "Vivid Rose",
        Color(0xFFFF4081),
        Color(0xFFFF5252),
        Color(0xFF4DB6AC)
    ),
    SUNSET_BEAT(
        "amber",
        "Sunset Beat",
        Color(0xFFFFB74D),
        Color(0xFFFF7043),
        Color(0xFF81C784)
    )
}
