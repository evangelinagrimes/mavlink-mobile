package com.example.mavlinkapplication.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary          = Color(0xFFFF6D00),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFBF360C),
    secondary        = Color(0xFF546E7A),
    error            = Color(0xFFCF6679),
    background       = Color(0xFF121212),
    surface          = Color(0xFF1E1E1E),
    onBackground     = Color(0xFFE0E0E0),
    onSurface        = Color(0xFFE0E0E0),
)

private val LightColors = lightColorScheme(
    primary          = Color(0xFFBF360C),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFFF8A65),
    secondary        = Color(0xFF455A64),
    error            = Color(0xFFB00020),
    background       = Color(0xFFFAFAFA),
    surface          = Color.White,
)

@Composable
fun ArduTargetTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
