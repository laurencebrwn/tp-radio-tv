package com.example.tpradio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TPRadioTheme(
    intervalMillis: Long = 1 * 10 * 1000,
    syncToTrack: Boolean = false,
    trackThemeIndex: Int? = null,
    onSchemeChanged: (Int) -> Unit = {},
    content: @Composable () -> Unit
) {
    val schemes = listOf(Scheme1, Scheme2, Scheme3, Scheme4, Scheme5)
    var autoIndex by remember { mutableStateOf(0) }

    // Use track index if syncing, otherwise use auto rotation
    val currentIndex = if (syncToTrack && trackThemeIndex != null) {
        trackThemeIndex.coerceIn(0, schemes.size - 1)
    } else {
        autoIndex
    }

    // Only auto-rotate if not syncing to track
    LaunchedEffect(syncToTrack) {
        if (!syncToTrack) {
            while (true) {
                delay(intervalMillis)
                autoIndex = (autoIndex + 1) % schemes.size
                onSchemeChanged(autoIndex)
            }
        }
    }

    // Notify when track-synced theme changes
    LaunchedEffect(currentIndex) {
        onSchemeChanged(currentIndex)
    }

    MaterialTheme(
        colorScheme = schemes[currentIndex],
        typography = CustomTypography,
        content = content
    )
}
