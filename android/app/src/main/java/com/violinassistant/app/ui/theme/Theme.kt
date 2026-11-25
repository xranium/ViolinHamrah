package com.violinassistant.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

val LocalAppTheme = staticCompositionLocalOf { AppThemes.Midnight }

object AppTheme {
    val colors: AppThemePalette
        @Composable
        get() = LocalAppTheme.current
}

@Composable
fun ViolinHamrahTheme(
    theme: AppThemePalette = AppThemes.Midnight,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalAppTheme provides theme
    ) {
        MaterialTheme(
            colorScheme = theme.toColorScheme(),
            typography = AppTypography,
            content = content
        )
    }
}
