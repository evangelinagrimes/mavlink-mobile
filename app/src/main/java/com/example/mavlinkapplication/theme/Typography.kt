package com.example.mavlinkapplication.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Exactly four text sizes, everywhere. Every Material3 Typography slot maps to one of
 * these four — nothing in the app should reach for a fifth size. Access these directly
 * (AppText.Label / .Body / .Readout / .Hero) rather than MaterialTheme.typography.*
 * where the semantic Material3 slot name doesn't matter.
 */
object AppText {
    /** Uppercase captions, field labels, button text. */
    val Label = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)

    /** Body copy, dialog content. */
    val Body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)

    /** Numeric/status readouts (GPS, EKF, battery, params). Tabular figures so digits don't jitter. */
    val Readout = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")

    /** ARMED/DISARMED, panel headers, the one or two things per screen that should dominate. */
    val Hero = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
}

val AppTypography = Typography(
    displayLarge = AppText.Hero,
    displayMedium = AppText.Hero,
    displaySmall = AppText.Hero,
    headlineLarge = AppText.Hero,
    headlineMedium = AppText.Hero,
    headlineSmall = AppText.Hero,
    titleLarge = AppText.Hero,
    titleMedium = AppText.Readout,
    titleSmall = AppText.Readout,
    bodyLarge = AppText.Body,
    bodyMedium = AppText.Body,
    bodySmall = AppText.Body,
    labelLarge = AppText.Label,
    labelMedium = AppText.Label,
    labelSmall = AppText.Label,
)
