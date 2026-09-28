package com.example.foodmemory.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = FoodForest,
    onPrimary = Color.White,
    secondary = FoodSage,
    onSecondary = FoodInk,
    tertiary = FoodPeach,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = FoodSurfaceVariant,
    outlineVariant = FoodOutlineVariant,
    onBackground = FoodInk,
    onSurface = FoodInk
)

@Composable
fun FoodMemoryTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
