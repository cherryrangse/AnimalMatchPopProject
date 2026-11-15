package com.pixelvector.animalmatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AnimalMatchColorScheme = lightColorScheme(
    primary = BunnyPink,
    secondary = PenguinBlue,
    tertiary = CatYellow,
    background = PastelBackground,
    surface = TopHudSurface,
    onPrimary = TopHudSurface,
    onBackground = DeepText,
    onSurface = DeepText
)

@Composable
fun AnimalMatchPopTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AnimalMatchColorScheme,
        content = content
    )
}
