package com.violinassistant.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.ui.theme.AppTheme

enum class Screen(val title: String, val icon: ImageVector) {
    HOME(Fa.NAV_HOME, Icons.Rounded.Home),
    LIBRARY(Fa.NAV_LIBRARY, Icons.Rounded.LibraryMusic),
    TUNER(Fa.NAV_TUNER, Icons.Rounded.Tune),
    HISTORY(Fa.NAV_HISTORY, Icons.Rounded.BarChart)
}

@Composable
fun BottomNavBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    val colors = AppTheme.colors

    Column {
        HorizontalDivider(color = colors.borderColor.copy(alpha = 0.6f), thickness = 1.dp)
        NavigationBar(
            containerColor = colors.bgCard,
            tonalElevation = 0.dp
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
                    label = {
                        Text(
                            text = screen.title,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                        unselectedIconColor = colors.textSecondary,
                        unselectedTextColor = colors.textSecondary,
                        indicatorColor = colors.primary.copy(alpha = 0.14f)
                    )
                )
            }
        }
    }
}
