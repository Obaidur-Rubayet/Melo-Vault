package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    DARK, LIGHT, SYSTEM
}

val LocalAccentPalette = staticCompositionLocalOf { AccentPalette.EMERALD_GROOVE }

fun getMeloVaultDarkColorScheme(accent: AccentPalette): ColorScheme {
    return darkColorScheme(
        primary = accent.primary,
        onPrimary = Color(0xFF0D0D0F),
        primaryContainer = accent.primary.copy(alpha = 0.15f),
        onPrimaryContainer = accent.primary,
        secondary = accent.secondary,
        onSecondary = Color(0xFF0D0D0F),
        secondaryContainer = accent.secondary.copy(alpha = 0.12f),
        onSecondaryContainer = accent.secondary,
        tertiary = accent.tertiary,
        onTertiary = Color.White,
        background = BackgroundDark,
        onBackground = TextPrimaryDark,
        surface = SurfaceDark,
        onSurface = TextPrimaryDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = TextSecondaryDark,
        outline = CardBorderDark
    )
}

fun getMeloVaultLightColorScheme(accent: AccentPalette): ColorScheme {
    return lightColorScheme(
        primary = accent.primary,
        onPrimary = Color.White,
        primaryContainer = accent.primary.copy(alpha = 0.15f),
        onPrimaryContainer = Color(0xFF1B5E20),
        secondary = accent.secondary,
        onSecondary = Color.White,
        secondaryContainer = accent.secondary.copy(alpha = 0.12f),
        onSecondaryContainer = Color(0xFF004D40),
        tertiary = accent.tertiary,
        onTertiary = Color.Black,
        background = BackgroundLight,
        onBackground = TextPrimaryLight,
        surface = SurfaceLight,
        onSurface = TextPrimaryLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = TextSecondaryLight,
        outline = CardBorderLight
    )
}

@Composable
fun MeloVaultTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentPalette: AccentPalette = AccentPalette.EMERALD_GROOVE,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) {
        getMeloVaultDarkColorScheme(accentPalette)
    } else {
        getMeloVaultLightColorScheme(accentPalette)
    }

    CompositionLocalProvider(
        LocalAccentPalette provides accentPalette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
