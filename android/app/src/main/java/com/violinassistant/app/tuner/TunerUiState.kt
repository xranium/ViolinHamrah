package com.violinassistant.app.tuner

data class PitchHistoryPoint(
    val centsOffset: Float?,
    val noteIndex: Int,
    val noteShortLabel: String
)

data class TunerUiState(
    val a4Hz: Float = SoundcorsetFrequencyPool.DEFAULT_A4_HZ,
    val useFlat: Boolean = false,
    val useSolfege: Boolean = false,
    val isListening: Boolean = false,
    val hasActiveNote: Boolean = false,
    val isLive: Boolean = false,
    val detectedFreqHz: Double = 0.0,
    val targetFreqHz: Double = 0.0,
    val currentNoteIndex: Int = SoundcorsetFrequencyPool.A4_INDEX,
    val cents: Float = -999f,
    val score: Int = 0,
    val confusionLevel: Double = 0.0,
    val pitchHistory: List<PitchHistoryPoint> = emptyList(),
    val volumeEnvelope: List<Int> = emptyList()
)
