package com.example.mavlinkapplication.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary            = Color(0xFF3B82F6), // blue accent — only things you can tap use this
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFBFDBFE),
    secondary          = Color(0xFF64748B),
    error              = Color(0xFFEF4444), // red — warnings and the destructive (disarm) action
    onError            = Color.White,
    background         = Color(0xFF0B1220), // navy
    onBackground       = Color(0xFFE2E8F0),
    surface            = Color(0xFF121B2E),
    onSurface          = Color(0xFFE2E8F0),
    surfaceVariant     = Color(0xFF1A2540), // elevated surface (cards, panels)
    onSurfaceVariant   = Color(0xFF94A3B8),
    outline            = Color(0xFF2A3B57),
)

private val LightColors = lightColorScheme(
    primary            = Color(0xFF2563EB),
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A5F),
    secondary          = Color(0xFF475569),
    error              = Color(0xFFDC2626),
    onError            = Color.White,
    background         = Color(0xFFF8FAFC),
    onBackground       = Color(0xFF0F172A),
    surface            = Color.White,
    onSurface          = Color(0xFF0F172A),
    surfaceVariant     = Color(0xFFF1F5F9),
    onSurfaceVariant   = Color(0xFF475569),
    outline            = Color(0xFFCBD5E1),
)

/**
 * Aviation-convention status colors: green=OK, amber=caution. Red uses the theme's
 * existing `error` slot rather than a duplicate token, since it's the same meaning
 * (warning/destructive). Never reuse [primary] for status — primary means "tappable".
 */
object StatusColors {
    val Ok: Color = Color(0xFF22C55E)
    val Caution: Color = Color(0xFFF59E0B)
}

@Composable
fun ArduTargetTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
