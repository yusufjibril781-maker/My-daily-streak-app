package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CosmicWolfDarkColorScheme = darkColorScheme(
    primary = WolfAmber,
    secondary = WolfIceBlue,
    tertiary = WolfForestGreen,
    background = DeepSlateBg,
    surface = CharcoalCard,
    onBackground = TextLight,
    onSurface = TextLight,
    onPrimary = DeepSlateBg,
    onSecondary = DeepSlateBg,
    outline = SlateBorder
)

private val CosmicWolfLightColorScheme = lightColorScheme(
    primary = SolarAmber,
    secondary = TechBlue,
    tertiary = NaturalGreen,
    background = LightIceBg,
    surface = LightCard,
    onBackground = TextDark,
    onSurface = TextDark,
    onPrimary = LightCard,
    onSecondary = LightCard,
    outline = LightBorder
)

@Composable
fun WolfStreakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        CosmicWolfDarkColorScheme
    } else {
        CosmicWolfLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
