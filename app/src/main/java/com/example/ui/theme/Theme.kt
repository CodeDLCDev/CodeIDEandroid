package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarculaColorScheme = darkColorScheme(
    primary = DarculaPrimary,
    onPrimary = DarculaBackground,
    background = DarculaBackground,
    onBackground = DarculaTextMain,
    surface = DarculaSurface,
    onSurface = DarculaTextMain,
    surfaceVariant = DarculaSurface,
    onSurfaceVariant = DarculaTextSecondary,
    error = DarculaError,
    onError = DarculaBackground
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightBackground,
    background = LightBackground,
    onBackground = LightTextMain,
    surface = LightSurface,
    onSurface = LightTextMain,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightTextSecondary,
    error = LightError,
    onError = LightBackground
)

@Composable
fun CodeIDETheme(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) DarculaColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
