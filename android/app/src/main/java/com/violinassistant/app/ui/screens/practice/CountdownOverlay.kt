package com.violinassistant.app.ui.screens.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinassistant.app.audio.CountdownPhase
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.ui.theme.AppTheme
import kotlin.math.roundToInt

/**
 * Tempo-synced 3 -> 2 -> 1 -> برو! preparation countdown overlay shown over the score
 * before the microphone opens for pitch recognition.
 */
@Composable
fun CountdownOverlay(
    phase: CountdownPhase,
    beatPulseCount: Long,
    tempoBpm: Float,
    modifier: Modifier = Modifier
) {
    val visible = phase == CountdownPhase.COUNT_3 ||
        phase == CountdownPhase.COUNT_2 ||
        phase == CountdownPhase.COUNT_1 ||
        phase == CountdownPhase.GO

    val colors = AppTheme.colors
    val scale = remember { Animatable(1f) }
    LaunchedEffect(phase, beatPulseCount) {
        if (visible) {
            scale.snapTo(1.24f)
            scale.animateTo(1f, animationSpec = tween(durationMillis = 220))
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(220)),
        modifier = modifier
    ) {
        val isGo = phase == CountdownPhase.GO
        val displayLabel = when (phase) {
            CountdownPhase.COUNT_3 -> "3"
            CountdownPhase.COUNT_2 -> "2"
            CountdownPhase.COUNT_1 -> "1"
            CountdownPhase.GO -> Fa.COUNTDOWN_GO
            else -> ""
        }
        val accentColor = if (isGo) colors.accent else colors.primary

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .widthIn(min = 136.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.bgDark.copy(alpha = 0.90f))
                .border(1.5.dp, accentColor.copy(alpha = 0.75f), RoundedCornerShape(24.dp))
                .padding(horizontal = 28.dp, vertical = 18.dp)
        ) {
            Text(
                text = displayLabel,
                fontSize = if (isGo) 40.sp else 54.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = if (isGo) "♩ ${tempoBpm.roundToInt()}" else Fa.COUNTDOWN_READY,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary
            )
        }
    }
}
