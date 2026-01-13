package com.snowsnooks.tpradio.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TPRadioTheme(
    themeIndex: Int = 0,
    previousThemeIndex: Int = 0,
    transitionProgress: Float = 1f, // 0f = show previous, 1f = show current
    content: @Composable () -> Unit
) {
    val safeCurrentIndex = themeIndex.coerceIn(0, colorSchemes.size - 1)
    val safePrevIndex = previousThemeIndex.coerceIn(0, colorSchemes.size - 1)

    val prevScheme = colorSchemes[safePrevIndex]
    val currentScheme = colorSchemes[safeCurrentIndex]

    // Interpolate colors based on transitionProgress
    val primary = lerp(prevScheme.primary, currentScheme.primary, transitionProgress)
    val onPrimary = lerp(prevScheme.onPrimary, currentScheme.onPrimary, transitionProgress)
    val background = lerp(prevScheme.background, currentScheme.background, transitionProgress)
    val onBackground = lerp(prevScheme.onBackground, currentScheme.onBackground, transitionProgress)
    val surface = lerp(prevScheme.surface, currentScheme.surface, transitionProgress)
    val onSurface = lerp(prevScheme.onSurface, currentScheme.onSurface, transitionProgress)
    val surfaceVariant = lerp(prevScheme.surfaceVariant, currentScheme.surfaceVariant, transitionProgress)

    val interpolatedScheme = currentScheme.copy(
        primary = primary,
        onPrimary = onPrimary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant
    )

    MaterialTheme(
        colorScheme = interpolatedScheme,
        typography = CustomTypography,
        content = content
    )
}

private fun lerp(start: androidx.compose.ui.graphics.Color, end: androidx.compose.ui.graphics.Color, fraction: Float): androidx.compose.ui.graphics.Color {
    return androidx.compose.ui.graphics.Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}