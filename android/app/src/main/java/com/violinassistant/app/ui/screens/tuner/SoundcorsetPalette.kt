package com.violinassistant.app.ui.screens.tuner

import androidx.compose.ui.graphics.Color

/**
 * Exact ARGB palette extracted from Soundcorset (com.soundcorset.client.android.Styles$Tuner
 * and VolumeEnvelopeView) so the Violin Hamrah tuner tab matches its visual display cleanly.
 */
object SoundcorsetPalette {
    val TunerDarkBlue = Color(0xFF2D343E)
    val TunerDimBlue = Color(0xFF3B4B60)
    val CenterBlock = Color(0xFF3A4A60)
    val TunerDimCyan = Color(0xFF29434C)
    val GridBlue = Color(0xFF3B4B61)
    val GridDarkBlue = Color(0xFF465D7C)

    val PitchDisplayDarkBlue = Color(0xFF3C4854)
    val PitchDisplayBottom = Color(0xFF252B34)
    val PitchVeryBottom = Color(0xFF1D2128)

    val TunerLightBlue = Color(0xFF72A3E7)
    val GaugeDimBlue = Color(0x8072A3E7)
    val SettingsGrey = Color(0xFF8B99AC)

    val TunerListeningBg = Color(0xFF6081ED)
    val ListeningWhiteInPitchText = Color(0xFFEFF3FE)
    val ListeningWhiteInPitchGraph = Color(0xFF9DA5B0)

    val TunerLightGreen = Color(0xFF9DFE0F)
    val GaugeRed = Color(0xFFFF0000)
    val VolumeEnvelopeBar = Color(0xFF476288)
    val White = Color(0xFFFFFFFF)
    val DimmedTint = Color(0xFF9F9F9F)
}
