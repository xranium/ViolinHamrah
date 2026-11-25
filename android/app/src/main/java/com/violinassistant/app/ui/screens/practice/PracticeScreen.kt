package com.violinassistant.app.ui.screens.practice

import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.violinassistant.app.audio.CountdownPhase
import com.violinassistant.app.audio.MetronomeState
import com.violinassistant.app.data.ScoreEntity
import com.violinassistant.app.model.NoteStatus
import com.violinassistant.app.practice.EngineStatus
import com.violinassistant.app.practice.PracticeEngineState
import com.violinassistant.app.ui.notation.ScoreNotationView
import com.violinassistant.app.ui.screens.PracticeResultsView
import com.violinassistant.app.ui.theme.AppTheme
import kotlin.math.abs

/**
 * Minimal, sheet-music-dominant practice screen inspired by Trala (Requirements R1-R5).
 * Sheet notation occupies 85-90% of screen with warm-white paper canvas on dark background.
 */
@Composable
fun PracticeScreen(
    score: ScoreEntity?,
    engineState: PracticeEngineState,
    isPracticing: Boolean,
    metronomeState: MetronomeState = MetronomeState(),
    micError: String?,
    onTogglePractice: () -> Unit,
    onResetPractice: () -> Unit,
    onToggleMetronomeMute: () -> Unit = {},
    onStartAfterPermission: () -> Unit = {},
    onBackToLibrary: () -> Unit,
    onOpenThemeDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val prefs = remember { context.getSharedPreferences("sheet_prefs", Context.MODE_PRIVATE) }
    var scale by remember { mutableStateOf(prefs.getFloat("sheet_scale", 100f)) }
    var measuresPerLine by remember { mutableStateOf(prefs.getInt("sheet_measures_per_line", 2)) }
    var pendingAutoStart by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (pendingAutoStart) {
                pendingAutoStart = false
                onStartAfterPermission()
            } else {
                onTogglePractice()
            }
        } else {
            pendingAutoStart = false
        }
    }

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    val runWithMic = { autoStartAfterGrant: Boolean, action: () -> Unit ->
        if (hasMicPermission()) {
            pendingAutoStart = false
            action()
        } else {
            pendingAutoStart = autoStartAfterGrant
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(score?.id) {
        if (score != null && !isPracticing && !hasMicPermission()) {
            pendingAutoStart = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val handleToggle = { runWithMic(false, onTogglePractice) }
    val handleRestart = { runWithMic(true, onResetPractice) }

    if (score == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(colors.bgDark).padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("لطفاً ابتدا یک قطعه را از کتابخانه انتخاب کنید.", color = colors.textSecondary)
        }
        return
    }

    val isFinished = engineState.status == EngineStatus.FINISHED && engineState.overallScore != null
    val currentNote = engineState.currentExpectedNote
    val detected = engineState.detectedPitch
    val isListeningActive = isPracticing && (
        metronomeState.countdownPhase == CountdownPhase.GO ||
            metronomeState.countdownPhase == CountdownPhase.LISTENING
        )
    val isWrongPitch = isListeningActive && detected != null && currentNote != null &&
        detected.midi > 0 &&
        engineState.status != EngineStatus.MEASURING_DURATION &&
        engineState.noteStatuses[engineState.currentIndex] == NoteStatus.WRONG_PITCH &&
        (currentNote.isRest || detected.midi != currentNote.midi)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgDark)
    ) {
        // Compact 44dp Top Bar with metronome chip
        PracticeTopBar(
            title = score.title,
            onBack = onBackToLibrary,
            tempoBpm = metronomeState.tempoBpm,
            isMetronomePlaying = metronomeState.isPlaying,
            isMetronomeMuted = metronomeState.isMuted,
            beatPulseCount = metronomeState.beatPulseCount,
            onToggleMetronomeMute = onToggleMetronomeMute,
            onOpenThemeDialog = onOpenThemeDialog
        )

        if (!micError.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.error.copy(alpha = 0.2f))
                    .padding(vertical = 4.dp, horizontal = 12.dp)
            ) {
                Text(text = micError, color = colors.error, fontSize = 12.sp)
            }
        }

        if (isFinished) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                PracticeResultsView(
                    score = engineState.overallScore ?: 0,
                    onRetry = onResetPractice,
                    onBackToLibrary = onBackToLibrary
                )
            }
        } else {
            // Dominant Sheet Music View (85-90% usable screen area)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ScoreNotationView(
                    xmlContent = score.xmlContent,
                    currentNoteIndex = engineState.currentIndex,
                    currentStatus = engineState.noteStatuses[engineState.currentIndex] ?: NoteStatus.PENDING,
                    noteStatuses = engineState.noteStatuses,
                    scale = scale,
                    measuresPerLine = measuresPerLine,
                    modifier = Modifier.fillMaxSize()
                )

                // Floating Error Bubble colored from green (close) to red (far)
                val wrongBubbleColor = remember(isWrongPitch, currentNote?.id, detected?.midi, detected?.centsDeviation, colors) {
                    if (!isWrongPitch || currentNote == null || currentNote.isRest || detected == null) {
                        colors.error
                    } else {
                        val semitoneDiff = abs((detected.midi - currentNote.midi) + (detected.centsDeviation / 100f))
                        val closeness = (1f - ((semitoneDiff - 0.5f) / 5.5f)).coerceIn(0f, 1f)
                        val midAmber = Color(0xFFD97706)
                        val nearGreen = Color(0xFF10B981)
                        if (closeness >= 0.5f) {
                            lerp(midAmber, nearGreen, (closeness - 0.5f) * 2f)
                        } else {
                            lerp(colors.error, midAmber, closeness * 2f)
                        }
                    }
                }
                WrongNoteBubble(
                    isWrong = isWrongPitch,
                    bubbleColor = wrongBubbleColor,
                    triggerKey = Triple(detected?.midi, detected?.centsDeviation, engineState.totalWrongAttempts),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )

                // Tempo-synced 3 -> 2 -> 1 -> Go Preparation Countdown Overlay
                CountdownOverlay(
                    phase = metronomeState.countdownPhase,
                    beatPulseCount = metronomeState.beatPulseCount,
                    tempoBpm = metronomeState.tempoBpm,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Single Compact Bottom Bar (Expected note + سایز slider + Controls)
            PracticeBottomControls(
                expectedNote = currentNote,
                isPracticing = isPracticing,
                hasStarted = engineState.currentIndex > 0 || isPracticing,
                scale = scale,
                onScaleChange = { newScale ->
                    scale = newScale
                    prefs.edit().putFloat("sheet_scale", newScale).apply()
                },
                onTogglePractice = handleToggle,
                onRestart = handleRestart
            )
        }
    }
}
