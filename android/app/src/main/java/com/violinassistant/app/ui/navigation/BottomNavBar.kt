package com.violinassistant.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.ui.theme.BgCard
import com.violinassistant.app.ui.theme.BorderColor
import com.violinassistant.app.ui.theme.PrimaryBlue
import com.violinassistant.app.ui.theme.TextSecondary

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
    NavigationBar(
        containerColor = BgCard,
        tonalElevation = NavigationBarDefaults.Elevation
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
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = BorderColor
                )
            )
        }
    }
}
