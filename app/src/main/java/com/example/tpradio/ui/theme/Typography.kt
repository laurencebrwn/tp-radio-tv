package com.example.tpradio.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.Typography
import com.example.tpradio.R

// Create a FontFamily from your .ttf file
val CustomFont = FontFamily(
    Font(R.font.neue_haas_unica_pro_medium, FontWeight.Normal),
    // Add more weights if you have them:
    // Font(R.font.my_custom_font_bold, FontWeight.Bold),
)

// Set all typography to use your custom font
val CustomTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = CustomFont),
    displayMedium = Typography().displayMedium.copy(fontFamily = CustomFont),
    displaySmall = Typography().displaySmall.copy(fontFamily = CustomFont),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = CustomFont),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = CustomFont),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = CustomFont),
    titleLarge = Typography().titleLarge.copy(fontFamily = CustomFont),
    titleMedium = Typography().titleMedium.copy(fontFamily = CustomFont),
    titleSmall = Typography().titleSmall.copy(fontFamily = CustomFont),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = CustomFont),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = CustomFont),
    bodySmall = Typography().bodySmall.copy(fontFamily = CustomFont),
    labelLarge = Typography().labelLarge.copy(fontFamily = CustomFont),
    labelMedium = Typography().labelMedium.copy(fontFamily = CustomFont),
    labelSmall = Typography().labelSmall.copy(fontFamily = CustomFont)
)