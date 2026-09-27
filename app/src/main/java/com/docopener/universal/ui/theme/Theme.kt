package com.docopener.universal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ProfessionalDarkColorScheme = darkColorScheme(
    primary = AccentPrimaryLight,
    onPrimary = DarkBackground,
    primaryContainer = AccentPrimary,
    onPrimaryContainer = TextPrimary,
    secondary = AccentCyan,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorderLight
)

@Composable
fun UniversalDocTheme(
    content: @Composable () -> Unit
) {
    // Professional minimal OLED / Charcoal Dark Mode by default
    MaterialTheme(
        colorScheme = ProfessionalDarkColorScheme,
        content = content
    )
}
