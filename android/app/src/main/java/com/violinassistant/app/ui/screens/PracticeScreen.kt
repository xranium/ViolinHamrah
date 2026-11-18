package com.violinassistant.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.violinassistant.app.data.ScoreEntity
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.model.NoteStatus
import com.violinassistant.app.practice.EngineStatus
import com.violinassistant.app.practice.PracticeEngineState
import com.violinassistant.app.ui.notation.ScoreNotationView
import com.violinassistant.app.ui.theme.*
import com.violinassistant.app.util.Formatters

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
        Box(modifier = Modifier.fillMaxSize().background(BgPrimary).padding(32.dp), contentAlignment = Alignment.Center) {
            Text("لطفاً ابتدا یک قطعه را از کتابخانه انتخاب کنید.", color = TextSecondary)
        }
        return
    }

    val isFinished = engineState.status == EngineStatus.FINISHED && engineState.overallScore != null
    val currentNote = engineState.currentExpectedNote
    val detected = engineState.detectedPitch

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Bar: Back to Library, Title and Composer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToLibrary) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "بازگشت", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(text = score.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = score.composer, fontSize = 11.sp, color = TextSecondary)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgCard)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = "کتاب نت و تمرین", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
            }
        }

        if (!micError.isNullOrBlank()) {
            Text(text = micError, color = ErrorRed, fontSize = 11.sp)
        }

        if (isFinished) {
            PracticeResultsView(
                score = engineState.overallScore ?: 0,
                onRetry = onResetPractice,
                onBackToLibrary = onBackToLibrary
            )
        } else {
            // Sheet Music Book View (White paper background with black notes like a real book)
            ScoreNotationView(
                xmlContent = score.xmlContent,
                currentNoteIndex = engineState.currentIndex,
                currentStatus = engineState.noteStatuses[engineState.currentIndex] ?: NoteStatus.PENDING,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Live Practice Status Dock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Expected Note
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "نت مورد انتظار: ", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = currentNote?.pitchName ?: "—",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    // Detected Pitch
                    val matchColor = when {
                        detected == null -> TextMuted
                        detected.midi == currentNote?.midi -> AccentGreen
                        else -> ErrorRed
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "نت دریافتی: ", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = detected?.pitchName ?: "—",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = matchColor
                        )
                        if (detected != null) {
                            Text(
                                text = " (${Formatters.toPersianDigits(detected.centsDeviation)} سِنت)",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Progress Bar
                LinearProgressIndicator(
                    progress = if (engineState.totalNotes > 0) engineState.currentIndex.toFloat() / engineState.totalNotes else 0f,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = PrimaryBlue,
                    trackColor = BorderColor
                )

                // Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = handleToggle,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isPracticing) ErrorRed else AccentGreen),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(if (isPracticing) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPracticing) Fa.STOP_PRACTICE else Fa.START_PRACTICE, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onResetPractice,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                }
            }
        }
    }
}
