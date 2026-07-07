package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable


private val DarkColorScheme = darkColorScheme(
    primary = PrimaryContainerColor, // reversed colors for dark mode to keep it bold but readable
    onPrimary = OnPrimaryContainerColor,
    primaryContainer = PrimaryColor,
    onPrimaryContainer = OnPrimaryColor,
    secondary = AccentPurple,
    onSecondary = OnAccentPurple,
    tertiary = AccentPink,
    onTertiary = OnAccentPink,
    background = OnBackgroundColor, // Dark grey background for dark theme
    surface = OnBackgroundColor,
    onBackground = BackgroundColor,
    onSurface = BackgroundColor,
    surfaceVariant = SurfaceVariantColor.copy(alpha = 0.2f),
    onSurfaceVariant = SurfaceVariantColor
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = OnPrimaryColor,
    primaryContainer = PrimaryContainerColor,
    onPrimaryContainer = OnPrimaryContainerColor,
    secondary = AccentPurple,
    onSecondary = OnAccentPurple,
    tertiary = AccentPink,
    onTertiary = OnAccentPink,
    background = BackgroundColor,
    surface = BackgroundColor,
    onBackground = OnBackgroundColor,
    onSurface = OnBackgroundColor,
    surfaceVariant = SurfaceVariantColor,
    onSurfaceVariant = OnSurfaceVariantColor,
    outline = OnSurfaceVariantColor.copy(alpha = 0.5f)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamicColor false to preserve the customized signature brand colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

