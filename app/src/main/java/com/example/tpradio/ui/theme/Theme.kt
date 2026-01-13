package com.example.tpradio.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TPRadioTheme(
    themeIndex: Int = 0,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorSchemes[themeIndex.coerceIn(0, colorSchemes.size - 1)],
        typography = CustomTypography,
        content = content
    )
}