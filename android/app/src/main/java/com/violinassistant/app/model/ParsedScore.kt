package com.violinassistant.app.model

data class TimeSignature(
    val beats: Int = 4,
    val beatType: Int = 4
)

data class KeySignature(
    val fifths: Int = 0,
    val mode: String = "major"
)

data class ParsedScore(
    val metadata: ScoreMetadata,
    val tempo: Float,
    val timeSignature: TimeSignature,
    val keySignature: KeySignature,
    val notes: List<NoteEvent>,
    val xmlContent: String
)
