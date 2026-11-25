package com.violinassistant.app.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.ui.theme.AppTheme
import kotlin.math.roundToInt

/**
 * Compact 44dp practice top bar displaying back navigation, score title,
 * metronome tempo/mute chip, and theme palette action.
 */
@Composable
fun PracticeTopBar(
    title: String,
    onBack: () -> Unit,
    tempoBpm: Float = 80f,
    isMetronomePlaying: Boolean = false,
    isMetronomeMuted: Boolean = false,
    beatPulseCount: Long = 0L,
    onToggleMetronomeMute: () -> Unit = {},
    onOpenThemeDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(colors.bgDark)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val pulseOn = isMetronomePlaying && (beatPulseCount % 2L == 1L)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.bgCard)
                    .clickable(onClick = onToggleMetronomeMute)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (pulseOn) colors.accent else colors.textSecondary.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "♩ ${tempoBpm.roundToInt()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMetronomeMuted) colors.textSecondary else colors.textPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isMetronomeMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = Fa.METRONOME_LABEL,
                    tint = if (isMetronomeMuted) colors.textSecondary else colors.primary,
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = onOpenThemeDialog,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "پوسته",
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
