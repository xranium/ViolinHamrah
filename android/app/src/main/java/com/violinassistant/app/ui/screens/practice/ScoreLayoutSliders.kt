package com.violinassistant.app.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinassistant.app.ui.theme.PrimaryBlue
import com.violinassistant.app.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * Sliders bar at the bottom of the sheet music tab inspired by Trala (Screenshot 2).
 * Allows dynamic configuration of:
 * 1. Score scale ("سایز"): zoom / notation font size (70% - 180%)
 * 2. Measures per line ("میزان"): 1 to 4 measures per line
 */
@Composable
fun ScoreLayoutSliders(
    scale: Float,
    onScaleChange: (Float) -> Unit,
    measuresPerLine: Int,
    onMeasuresChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF161F2E))
            .padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // سایز (Scale) Slider
        Row(
            modifier = Modifier.weight(1.1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "سایز",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(end = 4.dp)
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

        // میزان (Measures per line) Slider
        Row(
            modifier = Modifier.weight(0.9f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "میزان: $measuresPerLine",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(end = 4.dp)
            )
            Slider(
                value = measuresPerLine.toFloat(),
                onValueChange = { onMeasuresChange(it.roundToInt().coerceIn(1, 4)) },
                valueRange = 1f..4f,
                steps = 2,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryBlue,
                    activeTrackColor = PrimaryBlue,
                    inactiveTrackColor = Color(0xFF334155)
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
