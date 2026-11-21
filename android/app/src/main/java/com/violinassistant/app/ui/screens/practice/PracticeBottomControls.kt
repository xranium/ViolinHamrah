package com.violinassistant.app.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.model.NoteEvent
import com.violinassistant.app.ui.theme.*

/**
 * Compact 56dp practice bottom control bar inspired by Trala.
 * Contains expected note badge, score size (سایز) zoom slider, restart, and Start/Pause/Resume.
 */
@Composable
fun PracticeBottomControls(
    expectedNote: NoteEvent?,
    isPracticing: Boolean,
    hasStarted: Boolean,
    scale: Float,
    onScaleChange: (Float) -> Unit,
    onTogglePractice: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(BgDark)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expected note indicator badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E293B))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            if (expectedNote == null) {
                Text("—", fontSize = 13.sp, color = TextSecondary)
            } else if (expectedNote.isRest) {
                Text("سکوت", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            } else {
                Text(
                    text = expectedNote.pitchName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                val fingering = getViolinFingering(expectedNote.midi)
                if (fingering.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = fingering, fontSize = 10.sp, color = TextSecondary)
                }
            }
        }

        // سایز (Score Scale) Slider
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "سایز",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(end = 2.dp)
            )
            Slider(
                value = scale,
                onValueChange = onScaleChange,
                valueRange = 70f..180f,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryBlue,
                    activeTrackColor = PrimaryBlue,
                    inactiveTrackColor = Color(0xFF334155)
                ),
                modifier = Modifier.weight(1f)
            )
        }

        // Controls: Restart + Start/Pause/Resume
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onRestart,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = Fa.RESTART,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            val buttonLabel = when {
                isPracticing -> Fa.PAUSE
                hasStarted -> Fa.RESUME
                else -> Fa.START
            }
            Button(
                onClick = onTogglePractice,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPracticing) ErrorRed else AccentGreen
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = if (isPracticing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = buttonLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun getViolinFingering(midi: Int): String {
    return when {
        midi < 55 -> ""
        midi in 55..61 -> "سیم G • انگشت ${when (midi) { 55 -> "۰"; 56, 57 -> "۱"; 58, 59 -> "۲"; else -> "۳" }}"
        midi in 62..68 -> "سیم D • انگشت ${when (midi) { 62 -> "۰"; 63, 64 -> "۱"; 65, 66 -> "۲"; else -> "۳" }}"
        midi in 69..75 -> "سیم A • انگشت ${when (midi) { 69 -> "۰"; 70, 71 -> "۱"; 72, 73 -> "۲"; else -> "۳" }}"
        midi in 76..84 -> "سیم E • انگشت ${when (midi) { 76 -> "۰"; 77, 78 -> "۱"; 79, 80 -> "۲"; 81, 82 -> "۳"; else -> "۴" }}"
        else -> "سیم E"
    }
}
