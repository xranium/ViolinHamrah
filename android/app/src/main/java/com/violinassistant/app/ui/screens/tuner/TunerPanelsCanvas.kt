package com.violinassistant.app.ui.screens.tuner

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.violinassistant.app.tuner.SoundcorsetFrequencyPool
import com.violinassistant.app.tuner.TunerUiState
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Soundcorset's Note Display Card (with "Listening..." animation and green score fill)
 * and scrolling Volume Envelope strip.
 */
@Composable
fun TunerNoteCard(
    state: TunerUiState,
    isPortrait: Boolean,
    modifier: Modifier = Modifier
) {
    var dotPhase by remember { mutableStateOf(1) }
    LaunchedEffect(state.hasActiveNote) {
        if (!state.hasActiveNote) {
            while (true) {
                delay(250L)
                dotPhase = (dotPhase % 3) + 1
            }
        }
    }

    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = min(w, h)

        if (!state.hasActiveNote) {
            drawRect(color = SoundcorsetPalette.TunerListeningBg)
            val label = "Listening" + ".".repeat(dotPhase)
            textPaint.color = SoundcorsetPalette.ListeningWhiteInPitchText.toArgb()
            textPaint.textSize = (w / 6f).coerceIn(24f, 64f)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                w * 0.5f,
                h * 0.5f + textPaint.textSize * 0.35f,
                textPaint
            )
            return@Canvas
        }

        drawRect(color = SoundcorsetPalette.PitchDisplayDarkBlue)

        val clampedScore = min(state.score, 10)
        val isFullGreen = clampedScore == 10
        if (clampedScore > 0) {
            val halfW = (w * 0.5f) * (clampedScore / 10f)
            val barHeight = if (isFullGreen) h else h / 20f
            val barColor = if (state.isLive) {
                SoundcorsetPalette.TunerLightGreen
            } else {
                SoundcorsetPalette.TunerLightGreen.copy(alpha = 0.62f)
            }
            drawRect(
                color = barColor,
                topLeft = Offset(w * 0.5f - halfW, 0f),
                size = Size(halfW * 2f, barHeight)
            )
        }

        val noteText = SoundcorsetFrequencyPool.noteName(
            state.currentNoteIndex,
            state.useFlat,
            state.useSolfege,
            includeOctave = false
        )
        textPaint.color = when {
            isFullGreen -> SoundcorsetPalette.TunerDarkBlue
            state.isLive -> SoundcorsetPalette.White
            else -> SoundcorsetPalette.DimmedTint
        }.toArgb()
        textPaint.textSize = (minDim / if (isPortrait) 1.2f else 1.8f).coerceAtLeast(36f)
        val fm = textPaint.fontMetrics
        val baseline = h * 0.5f - (fm.ascent + fm.descent) * 0.5f
        drawContext.canvas.nativeCanvas.drawText(noteText, w * 0.5f, baseline, textPaint)
    }
}

@Composable
fun TunerVolumeEnvelope(
    envelope: List<Int>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h * 0.5f
        val barPitch = 4.dp.toPx()
        val maxBars = (w / barPitch).toInt().coerceAtLeast(1)
        val visible = if (envelope.size > maxBars) envelope.takeLast(maxBars) else envelope

        drawRect(color = SoundcorsetPalette.PitchVeryBottom)

        val startOffset = maxBars - visible.size
        val strokeW = 2.2.dp.toPx()
        visible.forEachIndexed { idx, amp ->
            val norm = (sqrt(amp.toFloat()) / 164.3198f).coerceIn(0.03f, 1f)
            val halfBar = centerY * norm * 0.9f
            val x = (startOffset + idx) * barPitch + barPitch * 0.5f
            drawLine(
                color = SoundcorsetPalette.VolumeEnvelopeBar,
                start = Offset(x, centerY - halfBar),
                end = Offset(x, centerY + halfBar),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }
}
