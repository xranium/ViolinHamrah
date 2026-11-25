package com.violinassistant.app.ui.screens.tuner

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.violinassistant.app.tuner.PitchHistoryPoint
import com.violinassistant.app.tuner.SoundcorsetFrequencyPool

/**
 * Soundcorset's 65-point rolling pitch trace with -10..+10 target zone highlight band.
 */
@Composable
fun TunerPitchGraph(
    history: List<PitchHistoryPoint>,
    modifier: Modifier = Modifier
) {
    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
    }
    val noteTagPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            color = SoundcorsetPalette.TunerLightBlue.toArgb()
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val gutter = 28.dp.toPx()
        val plotW = (w - gutter).coerceAtLeast(10f)

        drawRect(color = SoundcorsetPalette.TunerDarkBlue)

        fun centsToY(c: Float): Float =
            h * 0.5f - (c.coerceIn(-48f, 48f) / 48f) * (h * 0.45f)

        val top10 = centsToY(10f)
        val bot10 = centsToY(-10f)
        drawRect(
            color = SoundcorsetPalette.CenterBlock,
            topLeft = Offset(gutter, top10),
            size = Size(plotW, bot10 - top10)
        )

        labelPaint.textSize = (h / 10f).coerceIn(14f, 26f)
        for (tick in -40..40 step 10) {
            val y = centsToY(tick.toFloat())
            val lineColor = if (tick == 0) SoundcorsetPalette.GridDarkBlue else SoundcorsetPalette.GridBlue
            drawLine(
                color = lineColor,
                start = Offset(gutter, y),
                end = Offset(w, y),
                strokeWidth = if (tick == 0) 1.5.dp.toPx() else 1.dp.toPx()
            )
            labelPaint.color = (if (tick in -10..10) {
                SoundcorsetPalette.TunerLightGreen
            } else {
                SoundcorsetPalette.SettingsGrey
            }).toArgb()
            drawContext.canvas.nativeCanvas.drawText(
                tick.toString(),
                gutter - 4.dp.toPx(),
                y + labelPaint.textSize * 0.35f,
                labelPaint
            )
        }

        val maxPts = SoundcorsetFrequencyPool.HISTORY_SIZE
        val stepX = plotW / (maxPts - 1).coerceAtLeast(1)
        val startSlot = (maxPts - history.size).coerceAtLeast(0)
        val path = Path()
        var inSegment = false
        var prevNoteIdx = -1
        noteTagPaint.textSize = (h / 9f).coerceIn(16f, 28f)

        history.forEachIndexed { idx, pt ->
            val x = gutter + (startSlot + idx) * stepX
            val c = pt.centsOffset
            if (c == null) {
                inSegment = false
                prevNoteIdx = -1
            } else {
                val y = centsToY(c)
                if (!inSegment) {
                    path.moveTo(x, y)
                    inSegment = true
                } else {
                    path.lineTo(x, y)
                }
                if (pt.noteIndex != prevNoteIdx && pt.noteShortLabel.isNotEmpty()) {
                    val labelY = (y - 6.dp.toPx()).coerceIn(noteTagPaint.textSize, h - 4.dp.toPx())
                    drawContext.canvas.nativeCanvas.drawText(pt.noteShortLabel, x, labelY, noteTagPaint)
                    prevNoteIdx = pt.noteIndex
                }
            }
        }
        drawPath(
            path = path,
            color = SoundcorsetPalette.White,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
