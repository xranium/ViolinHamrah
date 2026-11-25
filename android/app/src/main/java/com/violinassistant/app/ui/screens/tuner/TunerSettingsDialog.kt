package com.violinassistant.app.ui.screens.tuner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Soundcorset tuner calibration & notation preferences dialog (A4 Hz, #/b, ABC/Solfege).
 */
@Composable
fun TunerSettingsDialog(
    currentA4Hz: Float,
    currentUseFlat: Boolean,
    currentUseSolfege: Boolean,
    onApply: (a4Hz: Float, useFlat: Boolean, useSolfege: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var a4 by remember { mutableStateOf(currentA4Hz.roundToInt().toFloat()) }
    var useFlat by remember { mutableStateOf(currentUseFlat) }
    var useSolfege by remember { mutableStateOf(currentUseSolfege) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoundcorsetPalette.TunerDarkBlue,
        titleContentColor = SoundcorsetPalette.White,
        textContentColor = SoundcorsetPalette.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(text = "Tuner Calibration", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "A4 = ${a4.roundToInt()} Hz",
                    color = SoundcorsetPalette.TunerLightBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Slider(
                    value = a4,
                    onValueChange = { a4 = it.roundToInt().toFloat() },
                    valueRange = 415f..465f,
                    colors = SliderDefaults.colors(
                        thumbColor = SoundcorsetPalette.TunerLightGreen,
                        activeTrackColor = SoundcorsetPalette.TunerLightGreen,
                        inactiveTrackColor = SoundcorsetPalette.TunerDimBlue
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { a4 = (a4 - 1f).coerceAtLeast(415f) }) { Text("-1 Hz") }
                    OutlinedButton(onClick = { a4 = 440f }) { Text("440 Hz") }
                    OutlinedButton(onClick = { a4 = 442f }) { Text("442 Hz") }
                    OutlinedButton(onClick = { a4 = (a4 + 1f).coerceAtMost(465f) }) { Text("+1 Hz") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Accidentals (# / \u266D)")
                    Switch(
                        checked = useFlat,
                        onCheckedChange = { useFlat = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SoundcorsetPalette.TunerDarkBlue,
                            checkedTrackColor = SoundcorsetPalette.TunerLightGreen
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Solfege (Do-Re-Mi)")
                    Switch(
                        checked = useSolfege,
                        onCheckedChange = { useSolfege = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SoundcorsetPalette.TunerDarkBlue,
                            checkedTrackColor = SoundcorsetPalette.TunerLightGreen
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(a4, useFlat, useSolfege)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoundcorsetPalette.TunerLightGreen,
                    contentColor = SoundcorsetPalette.TunerDarkBlue
                )
            ) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SoundcorsetPalette.SettingsGrey)
            }
        }
    )
}
