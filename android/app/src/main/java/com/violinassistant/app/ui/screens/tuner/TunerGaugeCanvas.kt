package com.violinassistant.app.ui.screens.tuner

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.violinassistant.app.tuner.TunerUiState
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min

private const val NEEDLE_SPEED = 10f

@Composable
fun TunerGaugeCanvas(
    state: TunerUiState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetCents = if (state.hasActiveNote) state.cents.coerceIn(-50f, 50f) else 0f
    var displayCents by remember { mutableStateOf(0f) }

    LaunchedEffect(targetCents, state.hasActiveNote) {
        while (abs(targetCents - displayCents) > 0.35f) {
            val delta = ((targetCents - displayCents) / 2f).coerceIn(-NEEDLE_SPEED, NEEDLE_SPEED)
            displayCents += delta
            delay(33L)
        }
        displayCents = targetCents
    }

    val checkScale by animateFloatAsState(
        targetValue = if (state.hasActiveNote && state.score >= 10) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "checkScale"
    )

    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }

    Box(modifier = modifier.background(SoundcorsetPalette.TunerDarkBlue)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = min(w, h)
            val cx = w * 0.5f
            val baselineY = h - minDim * 0.13f
            val topHeadroom = minDim * 0.22f
            val radius = min(w * 0.43f, (baselineY - topHeadroom) * 0.88f).coerceAtLeast(24f)

            drawGaugeTicksAndSector(cx, baselineY, radius, minDim, textPaint)
            drawGaugeReadoutsAndNeedle(state, displayCents, checkScale, cx, baselineY, radius, minDim, textPaint)
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .size(width = 140.dp, height = 36.dp)
                .clickable(onClick = onOpenSettings)
        )
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Tuner Settings",
                tint = SoundcorsetPalette.SettingsGrey,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
