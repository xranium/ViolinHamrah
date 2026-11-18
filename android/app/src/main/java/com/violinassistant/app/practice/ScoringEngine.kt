package com.violinassistant.app.practice

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

data class EvaluationRecord(
    val noteId: String,
    val expectedMidi: Int,
    val pitchCorrect: Boolean,
    val durationCorrect: Boolean,
    val centsDeviation: Int,
    val expectedDurationSec: Float,
    val playedDurationSec: Float,
    val wrongAttemptsCount: Int
)

object ScoringEngine {
    fun calculateOverallScore(
        totalNotes: Int,
        evaluations: List<EvaluationRecord>,
        totalWrongAttempts: Int
    ): Int {
        if (totalNotes <= 0) return 0

        var pitchPoints = 0f
        var durationPoints = 0f

        for (ev in evaluations) {
            if (ev.pitchCorrect) {
                val centsPenalty = min(20f, abs(ev.centsDeviation) * 0.4f)
                pitchPoints += (100f - centsPenalty).coerceAtLeast(70f)
            }

            if (ev.durationCorrect) {
                durationPoints += 100f
            } else if (ev.pitchCorrect) {
                val ratio = if (ev.expectedDurationSec > 0f) {
                    min(ev.playedDurationSec / ev.expectedDurationSec, ev.expectedDurationSec / (ev.playedDurationSec.coerceAtLeast(0.01f)))
                } else 0.5f
                durationPoints += (ratio * 70f).roundToInt().coerceAtLeast(25).toFloat()
            }
        }

        val avgPitch = pitchPoints / totalNotes
        val avgDuration = durationPoints / totalNotes

        var combinedScore = (avgPitch * 0.60f) + (avgDuration * 0.40f)

        // Deduct penalty for wrong attempts
        val penalty = min(20f, totalWrongAttempts * 1.5f)
        combinedScore = (combinedScore - penalty).coerceAtLeast(0f)

        return combinedScore.roundToInt().coerceIn(0, 100)
    }
}
