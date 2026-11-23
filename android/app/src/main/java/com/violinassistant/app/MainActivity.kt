package com.violinassistant.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.violinassistant.app.data.ScoreEntity
import com.violinassistant.app.ui.navigation.BottomNavBar
import com.violinassistant.app.ui.navigation.Screen
import com.violinassistant.app.ui.screens.*
import com.violinassistant.app.ui.screens.practice.PracticeScreen
import com.violinassistant.app.ui.theme.AppThemes
import com.violinassistant.app.ui.theme.ThemeSelectionDialog
import com.violinassistant.app.ui.theme.ViolinHamrahTheme
import com.violinassistant.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE) }
            val savedThemeId = remember { prefs.getString("selected_theme_id", AppThemes.Midnight.id) }
            var currentTheme by remember { mutableStateOf(AppThemes.fromId(savedThemeId)) }
            var showThemeDialog by remember {
                mutableStateOf(!prefs.getBoolean("has_chosen_theme_v1", false))
            }

            ViolinHamrahTheme(theme = currentTheme) {
                var currentScreen by remember { mutableStateOf(Screen.HOME) }
                var viewingScore by remember { mutableStateOf<ScoreEntity?>(null) }

                BackHandler(enabled = viewingScore != null) {
                    viewModel.stopPractice()
                    viewingScore = null
                }

                val scores by viewModel.scores.collectAsState()
                val sessions by viewModel.sessions.collectAsState()
                val engineState by viewModel.engineState.collectAsState()
                val isPracticing by viewModel.isPracticing.collectAsState()
                val syncMessage by viewModel.syncMessage.collectAsState()
                val micError by viewModel.micError.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (viewingScore == null) {
                            BottomNavBar(
                                currentScreen = currentScreen,
                                onScreenSelected = { screen ->
                                    currentScreen = screen
                                    if (screen != Screen.LIBRARY) {
                                        viewingScore = null
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            Screen.HOME -> HomeScreen(
                                scores = scores,
                                sessions = sessions,
                                onStartPractice = {
                                    val target = viewingScore ?: scores.firstOrNull()
                                    if (target != null) {
                                        viewModel.selectScore(target)
                                        viewingScore = target
                                        currentScreen = Screen.LIBRARY
                                    }
                                },
                                onSelectScore = { score ->
                                    viewModel.selectScore(score)
                                    viewingScore = score
                                    currentScreen = Screen.LIBRARY
                                },
                                onOpenThemeDialog = { showThemeDialog = true }
                            )

                            Screen.LIBRARY -> {
                                if (viewingScore == null) {
                                    LibraryScreen(
                                        scores = scores,
                                        syncMessage = syncMessage,
                                        onSelectScore = { score ->
                                            viewModel.selectScore(score)
                                            viewingScore = score
                                        },
                                        onImportFile = { stream, name ->
                                            viewModel.importCustomScore(stream, name)
                                        },
                                        onSyncGitHub = {
                                            viewModel.syncWithGitHub()
                                        },
                                        onOpenThemeDialog = { showThemeDialog = true }
                                    )
                                } else {
                                    PracticeScreen(
                                        score = viewingScore,
                                        engineState = engineState,
                                        isPracticing = isPracticing,
                                        micError = micError,
                                        onTogglePractice = { viewModel.togglePractice() },
                                        onResetPractice = { viewModel.resetPractice() },
                                        onBackToLibrary = {
                                            viewModel.stopPractice()
                                            viewingScore = null
                                        },
                                        onOpenThemeDialog = { showThemeDialog = true }
                                    )
                                }
                            }

                            Screen.HISTORY -> HistoryScreen(
                                sessions = sessions,
                                onOpenThemeDialog = { showThemeDialog = true }
                            )
                        }
                    }
                }

                if (showThemeDialog) {
                    ThemeSelectionDialog(
                        currentTheme = currentTheme,
                        onSelectTheme = { selected ->
                            currentTheme = selected
                            prefs.edit().putString("selected_theme_id", selected.id).apply()
                        },
                        onDismiss = {
                            showThemeDialog = false
                            prefs.edit().putBoolean("has_chosen_theme_v1", true).apply()
                        }
                    )
                }
            }
        }
    }
}
