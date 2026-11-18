package com.violinassistant.app.practice

import com.violinassistant.app.model.NoteEvent
import com.violinassistant.app.model.NoteStatus
import com.violinassistant.app.model.PitchResult

enum class EngineStatus {
    IDLE,
    WAITING_FOR_NOTE,
    MEASURING_DURATION,
    COMPLETED_CORRECTLY,
    COMPLETED_WITH_ERROR,
    FINISHED
}

data class PracticeEngineState(
    val status: EngineStatus = EngineStatus.IDLE,
    val currentIndex: Int = 0,
    val totalNotes: Int = 0,
    val noteStatuses: Map<Int, NoteStatus> = emptyMap(),
    val evaluations: List<EvaluationRecord> = emptyList(),
    val totalWrongAttempts: Int = 0,
    val overallScore: Int? = null,
    val currentExpectedNote: NoteEvent? = null,
    val detectedPitch: PitchResult? = null,
    val sessionStartTime: Long = 0L,
    val sessionDurationSec: Long = 0L
)
