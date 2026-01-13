@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.snowsnooks.tpradio.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.darkColorScheme

val Scheme5 = darkColorScheme(
    primary = Color(0xFF17358F),   // highlight
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFF17358F),
    onBackground = Color(0xFF05D7D7),
    surface = Color(0xFF17358F),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF05D7D7)
)

val Scheme4 = darkColorScheme(
    primary = Color(0xFFFF5A00),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFF5A00),
    onBackground = Color(0xFFAFAFAF),
    surface = Color(0xFFFF5A00),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFAFAFAF)
)

val Scheme3 = darkColorScheme(
    primary = Color(0xFFFF9EB1),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFF9EB1),
    onBackground = Color(0xFFFF1B18),
    surface = Color(0xFFFF9EB1),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFF1B18)
)

val Scheme2 = darkColorScheme(
    primary = Color(0xFF1B1E1D),
    onPrimary = Color(0xFFEFEFEF),
    background = Color(0xFF1B1E1D),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1B1E1D),
    onSurface = Color(0xffe1db38),
    surfaceVariant = Color(0xFFFFFFFF)
)

val Scheme1 = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFEFEFEF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF333638),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xff259ff2),
    surfaceVariant = Color(0xFF333638)
)

val colorSchemes: List<ColorScheme> = listOf(Scheme1, Scheme2, Scheme3, Scheme4, Scheme5)