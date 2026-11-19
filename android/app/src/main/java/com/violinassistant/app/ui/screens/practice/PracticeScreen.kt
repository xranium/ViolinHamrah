package com.violinassistant.app.ui.screens.practice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.violinassistant.app.data.ScoreEntity
import com.violinassistant.app.model.NoteStatus
import com.violinassistant.app.practice.EngineStatus
import com.violinassistant.app.practice.PracticeEngineState
import com.violinassistant.app.ui.notation.ScoreNotationView
import com.violinassistant.app.ui.screens.PracticeResultsView
import com.violinassistant.app.ui.theme.BgDark
import com.violinassistant.app.ui.theme.ErrorRed
import com.violinassistant.app.ui.theme.TextSecondary

/**
 * Minimal, sheet-music-dominant practice screen inspired by Trala (Requirements R1-R5).
 * Sheet notation occupies 85-90% of screen with warm-white paper canvas on dark background.
 */
@Composable
fun PracticeScreen(
    score: ScoreEntity?,
    engineState: PracticeEngineState,
    isPracticing: Boolean,
    micError: String?,
    onTogglePractice: () -> Unit,
    onResetPractice: () -> Unit,
    onBackToLibrary: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) onTogglePractice() }

    val handleToggle = {
        val hasMic = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMic) onTogglePractice()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    if (score == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(BgDark).padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("لطفاً ابتدا یک قطعه را از کتابخانه انتخاب کنید.", color = TextSecondary)
        }
        return
    }

    val isFinished = engineState.status == EngineStatus.FINISHED && engineState.overallScore != null
    val currentNote = engineState.currentExpectedNote
    val detected = engineState.detectedPitch
    val isWrongPitch = isPracticing && detected != null && currentNote != null &&
        !currentNote.isRest && detected.midi > 0 && detected.midi != currentNote.midi

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Compact 44dp Top Bar
        PracticeTopBar(
            title = score.title,
            onBack = onBackToLibrary
        )

        if (!micError.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ErrorRed.copy(alpha = 0.2f))
                    .padding(vertical = 4.dp, horizontal = 12.dp)
            ) {
                Text(text = micError, color = ErrorRed, fontSize = 12.sp)
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
                    modifier = Modifier.fillMaxSize()
                )

                // Floating Error Bubble
                WrongNoteBubble(
                    isWrong = isWrongPitch,
                    triggerKey = detected?.midi to detected?.centsDeviation,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }

            // Compact 52dp Bottom Controls
            PracticeBottomControls(
                expectedNote = currentNote,
                isPracticing = isPracticing,
                hasStarted = engineState.currentIndex > 0 || isPracticing,
                onTogglePractice = handleToggle,
                onRestart = onResetPractice
            )
        }
    }
}
