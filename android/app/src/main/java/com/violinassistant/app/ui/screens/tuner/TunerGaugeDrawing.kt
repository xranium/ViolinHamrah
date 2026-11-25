package com.violinassistant.app.ui.screens.tuner

import android.graphics.Paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.violinassistant.app.tuner.SoundcorsetFrequencyPool
import com.violinassistant.app.tuner.TunerUiState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val TUNER_HALF_SPAN_DEG = 67.5f // 3*pi/4 arc centered at top

internal fun DrawScope.drawGaugeTicksAndSector(
    cx: Float,
    baselineY: Float,
    radius: Float,
    minDim: Float,
    textPaint: Paint
) {
    val greenSpanDeg = TUNER_HALF_SPAN_DEG * (10f / 50f)
    drawArc(
        color = SoundcorsetPalette.TunerDimCyan,
        startAngle = -90f - greenSpanDeg,
        sweepAngle = greenSpanDeg * 2f,
        useCenter = true,
        topLeft = Offset(cx - radius, baselineY - radius),
        size = Size(radius * 2f, radius * 2f)
    )

    for (c in -50..50) {
        val angleDeg = -90f + (c / 50f) * TUNER_HALF_SPAN_DEG
        val rad = angleDeg * (PI / 180.0)
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val isMajor = (c % 10 == 0)
        val inTuneBand = abs(c) <= 10
        val tickColor = when {
            inTuneBand && isMajor -> SoundcorsetPalette.TunerLightGreen
            inTuneBand -> SoundcorsetPalette.TunerLightGreen.copy(alpha = 0.55f)
            isMajor -> SoundcorsetPalette.TunerLightBlue
            else -> SoundcorsetPalette.GaugeDimBlue
        }
        val tickLen = if (isMajor) radius * 0.095f else radius * 0.045f
        drawLine(
            color = tickColor,
            start = Offset(cx + radius * cosA, baselineY + radius * sinA),
            end = Offset(cx + (radius - tickLen) * cosA, baselineY + (radius - tickLen) * sinA),
            strokeWidth = if (isMajor) 2.dp.toPx() else 1.1.dp.toPx(),
            cap = StrokeCap.Round
        )

        if (isMajor) {
            textPaint.color = (if (inTuneBand) {
                SoundcorsetPalette.TunerLightGreen
            } else {
                SoundcorsetPalette.TunerLightBlue
            }).toArgb()
            textPaint.textSize = (minDim * 0.044f).coerceIn(18f, 40f)
            textPaint.textAlign = Paint.Align.CENTER
            val labelR = radius + minDim * 0.055f
            drawContext.canvas.nativeCanvas.drawText(
                c.toString(),
                cx + labelR * cosA,
                baselineY + labelR * sinA + textPaint.textSize * 0.35f,
                textPaint
            )
        }
    }
}

internal fun DrawScope.drawGaugeReadoutsAndNeedle(
    state: TunerUiState,
    displayCents: Float,
    checkScale: Float,
    cx: Float,
    baselineY: Float,
    radius: Float,
    minDim: Float,
    textPaint: Paint
) {
    val a4Int = state.a4Hz.roundToInt()
    val a4Text = if (a4Int == 440) "A4 = 440Hz" else "A4 = ${a4Int}Hz \u26A0"
    textPaint.color = SoundcorsetPalette.SettingsGrey.toArgb()
    textPaint.textSize = (minDim * 0.046f).coerceIn(20f, 38f)
    textPaint.textAlign = Paint.Align.LEFT
    drawContext.canvas.nativeCanvas.drawText(a4Text, minDim * 0.05f, minDim * 0.11f, textPaint)

    if (state.hasActiveNote) {
        val fullNote = SoundcorsetFrequencyPool.noteName(
            state.currentNoteIndex, state.useFlat, state.useSolfege, includeOctave = true
        )
        textPaint.color = (if (state.isLive) SoundcorsetPalette.TunerLightBlue else SoundcorsetPalette.SettingsGrey).toArgb()
        textPaint.textSize = (minDim * 0.054f).coerceIn(22f, 44f)
        textPaint.textAlign = Paint.Align.CENTER
        drawContext.canvas.nativeCanvas.drawText(
            "$fullNote: ${state.targetFreqHz.roundToInt()}Hz", cx, minDim * 0.11f, textPaint
        )

        val prevName = SoundcorsetFrequencyPool.noteName(
            (state.currentNoteIndex - 1).coerceAtLeast(0), state.useFlat, state.useSolfege, includeOctave = true
        )
        val nextName = SoundcorsetFrequencyPool.noteName(
            (state.currentNoteIndex + 1).coerceAtMost(SoundcorsetFrequencyPool.NOTE_COUNT - 1),
            state.useFlat, state.useSolfege, includeOctave = true
        )
        textPaint.color = SoundcorsetPalette.SettingsGrey.toArgb()
        textPaint.textSize = (minDim * 0.048f).coerceIn(20f, 40f)
        val endOffsetX = radius * cos((90f - TUNER_HALF_SPAN_DEG) * PI / 180.0).toFloat()
        val endOffsetY = baselineY - radius * sin((90f - TUNER_HALF_SPAN_DEG) * PI / 180.0).toFloat() + minDim * 0.075f
        drawContext.canvas.nativeCanvas.drawText(prevName, cx - endOffsetX, endOffsetY, textPaint)
        drawContext.canvas.nativeCanvas.drawText(nextName, cx + endOffsetX, endOffsetY, textPaint)
    }

    val noisyAlpha = (state.confusionLevel * 2.5).toFloat().coerceIn(0f, 1f)
    if (noisyAlpha > 0.05f) {
        textPaint.color = SoundcorsetPalette.TunerLightGreen.copy(alpha = noisyAlpha).toArgb()
        textPaint.textSize = (minDim * 0.046f).coerceIn(20f, 38f)
        textPaint.textAlign = Paint.Align.LEFT
        drawContext.canvas.nativeCanvas.drawText("NOISY", minDim * 0.06f, baselineY + minDim * 0.03f, textPaint)
    }

    val boxW = minDim * 0.19f
    val boxH = minDim * 0.095f
    val boxLeft = cx + minDim * 0.18f
    val boxTop = baselineY - boxH * 0.65f
    val boxColor = if (state.hasActiveNote && abs(state.cents) < 10f) {
        SoundcorsetPalette.TunerLightGreen
    } else {
        SoundcorsetPalette.TunerLightBlue
    }
    drawRoundRect(
        color = boxColor.copy(alpha = 0.85f),
        topLeft = Offset(boxLeft, boxTop),
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        style = Stroke(width = 1.4.dp.toPx())
    )
    textPaint.color = boxColor.toArgb()
    textPaint.textSize = (minDim * 0.048f).coerceIn(20f, 38f)
    textPaint.textAlign = Paint.Align.CENTER
    val centsStr = if (state.hasActiveNote) "${state.cents.roundToInt()}\u00A2" else "0\u00A2"
    drawContext.canvas.nativeCanvas.drawText(centsStr, boxLeft + boxW * 0.5f, boxTop + boxH * 0.72f, textPaint)

    val needleRad = (-90f + (displayCents.coerceIn(-50f, 50f) / 50f) * TUNER_HALF_SPAN_DEG) * (PI / 180.0)
    val needleLen = radius * 0.94f
    drawLine(
        color = SoundcorsetPalette.GaugeRed,
        start = Offset(cx, baselineY),
        end = Offset(cx + needleLen * cos(needleRad).toFloat(), baselineY + needleLen * sin(needleRad).toFloat()),
        strokeWidth = 3.5.dp.toPx(),
        cap = StrokeCap.Round
    )

    drawCircle(color = Color(0xFF9AA4B2), radius = minDim * 0.025f, center = Offset(cx, baselineY))
    if (checkScale > 0.02f) {
        val badgeR = (minDim * 0.085f) * checkScale
        drawCircle(color = SoundcorsetPalette.TunerLightGreen, radius = badgeR, center = Offset(cx, baselineY))
        val checkPath = Path().apply {
            moveTo(cx - badgeR * 0.42f, baselineY + badgeR * 0.02f)
            lineTo(cx - badgeR * 0.10f, baselineY + badgeR * 0.34f)
            lineTo(cx + badgeR * 0.45f, baselineY - badgeR * 0.30f)
        }
        drawPath(checkPath, SoundcorsetPalette.TunerDarkBlue, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round))
    }
}
