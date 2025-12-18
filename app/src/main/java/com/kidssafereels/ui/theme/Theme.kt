package com.kidssafereels.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fun, kid-friendly colors
val KidsPurple = Color(0xFF9C27B0)
val KidsPink = Color(0xFFE91E63)
val KidsBlue = Color(0xFF2196F3)
val KidsGreen = Color(0xFF4CAF50)
val KidsOrange = Color(0xFFFF9800)
val KidsYellow = Color(0xFFFFEB3B)
val KidsRed = Color(0xFFF44336)
val KidsTeal = Color(0xFF009688)

private val DarkColorScheme = darkColorScheme(
    primary = KidsPurple,
    secondary = KidsPink,
    tertiary = KidsBlue,
    background = Color.Black,
    surface = Color(0xFF121212),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = KidsPurple,
    secondary = KidsPink,
    tertiary = KidsBlue,
    background = Color.White,
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun KidsSafeReelsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

