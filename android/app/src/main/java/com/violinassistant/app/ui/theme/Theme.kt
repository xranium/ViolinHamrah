package com.violinassistant.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextPrimary,
    primaryContainer = BgCardHover,
    surface = BgCard,
    onSurface = TextPrimary,
    background = BgPrimary,
    onBackground = TextPrimary,
    error = ErrorRed,
    onError = TextPrimary,
    outline = BorderColor
)

@Composable
fun ViolinHamrahTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            content = content
        )
    }
}
