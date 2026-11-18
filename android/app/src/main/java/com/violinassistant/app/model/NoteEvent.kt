package com.violinassistant.app.model

enum class NoteStatus {
    PENDING,
    ACTIVE,
    CORRECT,
    WRONG_PITCH,
    WRONG_DURATION
}

data class NoteEvent(
    val id: String,
    val step: String, // 'C', 'D', 'E', 'F', 'G', 'A', 'B'
    val alter: Int,   // -1 flat, 0 natural, 1 sharp
    val octave: Int,
    val midi: Int,    // 60 = C4, 69 = A4
    val pitchName: String, // "A4", "F#4", "سکوت"
    val durationInBeats: Float,
    val expectedDurationSec: Float,
    val measure: Int,
    val voice: Int,
    val isRest: Boolean,
    val isTiedToNext: Boolean = false,
    val isTiedFromPrev: Boolean = false,
    var status: NoteStatus = NoteStatus.PENDING,
    var playedMidi: Int? = null,
    var playedDurationSec: Float? = null,
    var centsDeviation: Int? = null
)
