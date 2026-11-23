package com.violinassistant.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.ui.theme.AppTheme

enum class Screen(val title: String, val icon: ImageVector) {
    HOME(Fa.NAV_HOME, Icons.Default.Home),
    LIBRARY(Fa.NAV_LIBRARY, Icons.Default.LibraryMusic),
    HISTORY(Fa.NAV_HISTORY, Icons.Default.BarChart)
}

@Composable
fun BottomNavBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    val colors = AppTheme.colors

    NavigationBar(
        containerColor = colors.bgCard,
        tonalElevation = 4.dp
    ) {
        Screen.values().forEach { screen ->
            val isSelected = currentScreen == screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(screen) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title
                    )
                },
                label = { Text(screen.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    unselectedIconColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary,
                    indicatorColor = colors.primary.copy(alpha = 0.15f)
                )
            )
        }
    }
}
