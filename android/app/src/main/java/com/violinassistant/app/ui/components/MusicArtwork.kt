package com.violinassistant.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Custom vector emblem evoking a violin scroll & bridge strings (replaces generic emojis).
 */
@Composable
fun ViolinEmblem(
    strokeColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = (w * 0.08f).coerceAtLeast(1.5.dp.toPx())

        // Violin waist silhouette
        val body = Path().apply {
            moveTo(w * 0.50f, h * 0.14f)
            cubicTo(w * 0.72f, h * 0.14f, w * 0.76f, h * 0.34f, w * 0.62f, h * 0.45f)
            cubicTo(w * 0.56f, h * 0.50f, w * 0.56f, h * 0.56f, w * 0.68f, h * 0.64f)
            cubicTo(w * 0.80f, h * 0.75f, w * 0.74f, h * 0.90f, w * 0.50f, h * 0.90f)
            cubicTo(w * 0.26f, h * 0.90f, w * 0.20f, h * 0.75f, w * 0.32f, h * 0.64f)
            cubicTo(w * 0.44f, h * 0.56f, w * 0.44f, h * 0.50f, w * 0.38f, h * 0.45f)
            cubicTo(w * 0.24f, h * 0.34f, w * 0.28f, h * 0.14f, w * 0.50f, h * 0.14f)
            close()
        }
        drawPath(body, color = strokeColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))

        // Center string + bridge tick
        drawLine(
            color = accentColor,
            start = Offset(w * 0.50f, h * 0.05f),
            end = Offset(w * 0.50f, h * 0.82f),
            strokeWidth = strokeW * 0.75f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = strokeColor,
            start = Offset(w * 0.36f, h * 0.60f),
            end = Offset(w * 0.64f, h * 0.60f),
            strokeWidth = strokeW * 0.7f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Subtle 5-line musical stave and quarter-note pattern for editorial surfaces.
 */
@Composable
fun MusicalStaffWatermark(
    lineColor: Color,
    noteColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val staffTop = h * 0.24f
        val spacing = (h * 0.13f).coerceAtLeast(4.dp.toPx())
        val lineStroke = 1.dp.toPx()

        for (i in 0 until 5) {
            val y = staffTop + i * spacing
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = lineStroke
            )
        }

        // Minimalist rhythmic noteheads across staff lines (LTR visual rhythm)
        val notePositions = listOf(0.18f to 3, 0.36f to 2, 0.54f to 1, 0.72f to 2, 0.88f to 0)
        val headRadius = spacing * 0.44f
        notePositions.forEach { (xRatio, lineIdx) ->
            val nx = w * xRatio
            val ny = staffTop + lineIdx * spacing
            drawOval(
                color = noteColor,
                topLeft = Offset(nx - headRadius * 1.2f, ny - headRadius * 0.85f),
                size = Size(headRadius * 2.4f, headRadius * 1.7f)
            )
            drawLine(
                color = noteColor,
                start = Offset(nx + headRadius * 1.05f, ny),
                end = Offset(nx + headRadius * 1.05f, ny - spacing * 2.4f),
                strokeWidth = 1.4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Animated radial score arc for session summary.
 */
@Composable
fun ScoreArcGauge(
    score: Int,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    var target by remember { mutableStateOf(0f) }
    LaunchedEffect(score) {
        target = (score.coerceIn(0, 100) / 100f)
    }
    val sweep by animateFloatAsState(
        targetValue = target * 360f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "scoreSweep"
    )

    Canvas(modifier = modifier) {
        val stroke = 8.dp.toPx()
        val inset = stroke * 0.5f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

/**
 * Minimal 7-session accuracy sparkline bars (chronological left-to-right).
 */
@Composable
fun PracticeTrendSparkline(
    recentScores: List<Int>,
    barColor: Color,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    val points = recentScores.take(7).reversed()
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val slotW = w / points.size.coerceAtLeast(1)
        val barW = (slotW * 0.52f).coerceAtMost(18.dp.toPx())

        points.forEachIndexed { idx, value ->
            val ratio = (value.coerceIn(10, 100) / 100f)
            val barH = (h * ratio).coerceAtLeast(4.dp.toPx())
            val x = idx * slotW + (slotW - barW) * 0.5f
            val isLatest = idx == points.lastIndex
            drawRoundRect(
                color = if (isLatest) highlightColor else barColor,
                topLeft = Offset(x, h - barH),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}
